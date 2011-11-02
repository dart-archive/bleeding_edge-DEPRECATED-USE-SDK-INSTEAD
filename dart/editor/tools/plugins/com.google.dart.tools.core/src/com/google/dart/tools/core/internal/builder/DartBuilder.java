/*
 * Copyright (c) 2011, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.core.internal.builder;

import com.google.dart.compiler.CommandLineOptions.CompilerOptions;
import com.google.dart.compiler.CompilerConfiguration;
import com.google.dart.compiler.DartArtifactProvider;
import com.google.dart.compiler.DartCompilationPhase;
import com.google.dart.compiler.DartCompilerContext;
import com.google.dart.compiler.DefaultCompilerConfiguration;
import com.google.dart.compiler.LibrarySource;
import com.google.dart.compiler.Source;
import com.google.dart.compiler.SystemLibraryManager;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.backend.js.AbstractJsBackend;
import com.google.dart.compiler.backend.js.JavascriptBackend;
import com.google.dart.compiler.metrics.CompilerMetrics;
import com.google.dart.compiler.resolver.CoreTypeProvider;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;
import com.google.dart.tools.core.internal.util.Extensions;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.HTMLFile;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;

import static com.google.dart.tools.core.internal.builder.BuilderUtil.clearErrorMarkers;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Instances of the class <code>DartBuilder</code> implement the incremental builder for Dart
 * projects.
 */
public class DartBuilder extends IncrementalProjectBuilder {

  /**
   * An artifact provider for tracking prerequisite projects. All artifacts are cached in memory via
   * {@link RootArtifactProvider} except for the final app.js file which is written to disk.
   */
  private class ArtifactProvider extends DartArtifactProvider {
    private final RootArtifactProvider rootProvider = RootArtifactProvider.getInstance();
    private final Collection<IProject> prerequisiteProjects = new HashSet<IProject>();
    private int writeArtifactCount;
    private int outOfDateCount;

    public void beginBuild() {
      writeArtifactCount = 0;
      outOfDateCount = 0;
    }

    public void clean(IProgressMonitor monitor) {
      prerequisiteProjects.clear();
      rootProvider.clearCachedArtifacts();

      for (DartLibrary library : getDartLibraries()) {
        try {
          File file = getJsAppArtifactFile(library.getCorrespondingResource());

          if (file != null && file.exists()) {
            file.delete();
          }
        } catch (DartModelException exception) {
          DartCore.logError(exception);
        }
      }
    }

    @Override
    public Reader getArtifactReader(Source source, String part, String extension)
        throws IOException {
      IResource res = ResourceUtil.getResource(source);
      if (res != null) {
        IProject project = res.getProject();
        prerequisiteProjects.add(project);
      }
      File appJsFile = getAppJsFile(source, part, extension);
      if (appJsFile != null) {
        return new BufferedReader(new FileReader(appJsFile));
      }
      return rootProvider.getArtifactReader(source, part, extension);
    }

    @Override
    public URI getArtifactUri(Source source, String part, String extension) {
      return rootProvider.getArtifactUri(source, part, extension);
    }

    @Override
    public Writer getArtifactWriter(Source source, String part, String extension)
        throws IOException {
      final File appJsFile = getAppJsFile(source, part, extension);
      if (appJsFile != null) {
        return new BufferedWriter(new FileWriter(appJsFile));
      }
      writeArtifactCount++;
      return rootProvider.getArtifactWriter(source, part, extension);
    }

    public int getOutOfDateCount() {
      return outOfDateCount;
    }

    public IProject[] getPrerequisiteProjects() {
      return prerequisiteProjects.toArray(new IProject[prerequisiteProjects.size()]);
    }

    public int getWriteArtifactCount() {
      return writeArtifactCount;
    }

    @Override
    public boolean isOutOfDate(Source source, Source base, String extension) {
      boolean isOutOfDate = rootProvider.isOutOfDate(source, base, extension);
      if (isOutOfDate) {
        outOfDateCount++;
      }
      return isOutOfDate;
    }

    /**
     * Answer the final application JS file if that is what is specified
     * 
     * @return the file or <code>null</code> if it is not specified
     */
    private File getAppJsFile(Source source, String part, String extension) throws AssertionError {
      if (!AbstractJsBackend.EXTENSION_APP_JS.equals(extension) || !"".equals(part)) {
        return null;
      }
      File srcFile = ResourceUtil.getFile(source);
      if (srcFile == null) {
        if (source == null) {
          throw new AssertionError("Cannot write " + AbstractJsBackend.EXTENSION_APP_JS
              + " for null source");
        }
        throw new AssertionError("Expected file for " + source.getName());
      }
      return getJsAppArtifactFile(new Path(srcFile.getPath()));
    }

    private List<DartLibrary> getDartLibraries() {
      List<DartLibrary> libraries = new ArrayList<DartLibrary>();

      IProject currentProject = getProject();

      try {
        for (DartLibrary library : DartModelManager.getInstance().getDartModel().getDartLibraries()) {
          if (currentProject.equals(library.getDartProject().getProject())) {
            libraries.add(library);
          }
        }
      } catch (DartModelException exception) {
        DartCore.logError(exception);
      }

      return libraries;
    }
  }

  private class ErrorCheckingPhase implements DartCompilationPhase {
    @Override
    public DartUnit exec(DartUnit unit, DartCompilerContext context, CoreTypeProvider typeProvider) {
      for (ErrorChecker checker : getErrorCheckers()) {
        unit.accept(checker);
      }
      return unit;
    }

    private ErrorChecker[] getErrorCheckers() {
      // TODO(brianwilkerson) Get the list of error checkers from an extension
      // point.
      return new ErrorChecker[0];
    }
  }

  /**
   * Answer the JavaScript application file for the specified source.
   * 
   * @param source the application source file (not <code>null</code>)
   * @return the application file (may not exist)
   */
  public static File getJsAppArtifactFile(IPath sourceLocation) {
    return sourceLocation.addFileExtension(JavascriptBackend.EXTENSION_APP_JS).toFile();
  }

  /**
   * Answer the JavaScript application file for the specified source.
   * 
   * @param source the application source file (not <code>null</code>)
   * @return the application file (may not exist)
   */
  public static File getJsAppArtifactFile(IResource source) {
    return getJsAppArtifactFile(source.getLocation());
  }

  /**
   * The artifact provider for this source.
   */
  private final ArtifactProvider provider = new ArtifactProvider();

  @SuppressWarnings("rawtypes")
  @Override
  protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
    // If *anything* changes then find each Dart App and build it
    if (hasDartSourceChanged()) {
      buildAllApplications(monitor);
    }

    // Return the projects upon which this project depends
    return provider.getPrerequisiteProjects();
  }

  /**
   * Build all the libraries in the project associated with the receiver
   * 
   * @param monitor the progress monitor (not <code>null</code>)
   */
  protected void buildAllApplications(IProgressMonitor monitor) throws CoreException {
    DartProject proj = DartCore.create(getProject());
    clearErrorMarkers(getProject());
    DartLibrary[] allLibraries = proj.getDartLibraries();

    SubMonitor subMonitor = SubMonitor.convert(monitor,
        "Building " + proj.getElementName() + "...", allLibraries.length * 100);

    try {
      for (DartLibrary lib : allLibraries) {
        if (monitor.isCanceled()) {
          throw new OperationCanceledException();
        }

        buildLibrary(lib, subMonitor.newChild(100));
      }
    } finally {
      monitor.done();
    }
  }

  /**
   * Build the specified Dart library
   * 
   * @param lib the library (not <code>null</code>)
   * @param monitor the progress monitor (not <code>null</code>)
   */
  protected void buildLibrary(DartLibrary lib, final IProgressMonitor monitor) {
    DartLibraryImpl libImpl = (DartLibraryImpl) lib;
    try {
      // # compilation units * # phases (3) 
      //     + fudge factor for bundled library such as core and dom (# classes * 3 phases)
      monitor.beginTask("Building " + lib.getElementName(),
          lib.getCompilationUnits().length * 2 + 630);

      // Delete the previous compiler output, if it exists.
      File file = getJsAppArtifactFile(lib.getCorrespondingResource());

      if (file != null && file.exists()) {
        file.delete();
      }

      // Call the Dart to JS compiler
      final LibrarySource libSource = libImpl.getLibrarySourceFile();
      final CompilerMetrics metrics = new CompilerMetrics();
      final SystemLibraryManager libraryManager = SystemLibraryManagerProvider.getSystemLibraryManager();
      final CompilerConfiguration config = new DefaultCompilerConfiguration(new CompilerOptions(),
          libraryManager) {
        @Override
        public CompilerMetrics getCompilerMetrics() {
          return metrics;
        }

        @Override
        public List<DartCompilationPhase> getPhases() {
          List<DartCompilationPhase> phases = super.getPhases();

          // The assumption is that we can add the new phase at the end because
          // the preceding phases do not alter the AST structure in any way that
          // violates the basic requirement that it accurately reflects the
          // original source code.
          phases.add(new ErrorCheckingPhase());

          // Wrapper all phases to provide progress feedback
          for (int i = 0; i < phases.size(); i++) {
            final DartCompilationPhase oldPhase = phases.get(i);
            phases.set(i, new DartCompilationPhase() {
              @Override
              public DartUnit exec(DartUnit unit, DartCompilerContext context,
                  CoreTypeProvider typeProvider) {
                monitor.worked(1);
                return oldPhase.exec(unit, context, typeProvider);
              }
            });
          }
          return phases;
        }

        @Override
        public boolean incremental() {
          return true;
        }

        @Override
        public boolean resolveDespiteParseErrors() {
          return true;
        }
      };
      final CompilerListener listener = new CompilerListener(lib, getProject());

      //Try:
      //1. Have the compiler build the Library
      //2. Tell the CompilerMetrics that the Compiler is done
      //3. Have the Messenger tell the MetricsManager that a new build is in
      provider.beginBuild();
      DartCompilerUtilities.secureCompileLib(libSource, config, provider, listener);
      config.getCompilerMetrics().done();
      if (DartCoreDebug.BUILD) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(400);
        PrintStream ps = new PrintStream(out);
        ps.println("Built Library " + libSource.getName());
        ps.println(provider.getOutOfDateCount() + " artifacts out of date");
        ps.println(provider.getWriteArtifactCount() + " artifacts written");
        metrics.write(ps);
        DartCore.logInformation(out.toString());
      }
      MetricsMessenger.getSingleton().fireUpdates(config,
          new Path(libSource.getName()).lastSegment());

      emitArtifactDetailsToConsole(libImpl);

      // TODO(brianwilkerson) Figure out how to get the library units out of the compiler so that
      // they can be used to drive the indexer.
      // queueFilesForIndexer(...);
    } catch (Throwable exception) {
//      createErrorMarker(getProject(), 0, 0, 0, exception.getMessage());
      DartCore.logInformation("Exception caught while building " + lib.getElementName(), exception);
    } finally {
      monitor.done();
    }
  }

  @Override
  protected void clean(IProgressMonitor monitor) throws CoreException {
    provider.clean(monitor);
  }

  /**
   * Obtain the current resource changed delta(s) to determine if any of the resources that have
   * changed were Dart related source files.
   * 
   * @return <code>true</code> if at least one Dart related source file has changed.
   */
  protected boolean hasDartSourceChanged() throws CoreException {
    if (hasDartSourceChanged(getDelta(getProject()))) {
      return true;
    }
    for (IProject project : provider.getPrerequisiteProjects()) {
      if (hasDartSourceChanged(getDelta(project))) {
        return true;
      }
    }
    return false;
  }

  private void emitArtifactDetailsToConsole(DartLibraryImpl libImpl) throws DartModelException {
    File artifactFile = getJsAppArtifactFile(libImpl.getCorrespondingResource());
    if (artifactFile != null) {
      DartCore.getConsole().println(
          DartBuilderMessages.DartBuilder_console_js_file_description + ": "
              + artifactFile.getAbsolutePath());
    }
    List<HTMLFile> htmlFiles = libImpl.getChildrenOfType(HTMLFile.class);
    IResource res;
    for (HTMLFile htmlFile : htmlFiles) {
      res = htmlFile.getCorrespondingResource();
      DartCore.getConsole().println(
          DartBuilderMessages.DartBuilder_console_html_file_description + ": "
              + res.getLocation().toOSString());
    }
  }

  private boolean hasDartSourceChanged(IResourceDelta delta) throws CoreException {
    if (delta == null) {
      return true;
    }
    final boolean shouldBuild[] = new boolean[1];
    delta.accept(new IResourceDeltaVisitor() {
      @Override
      public boolean visit(IResourceDelta delta) {
        IResource resource = delta.getResource();
        if (resource.getType() != IResource.FILE) {
          // Visit children only if we have not already found a changed source file
          return !shouldBuild[0];
        }
        String name = resource.getName();
        if (name.endsWith(Extensions.DOT_DART)) {
          shouldBuild[0] = true;
        }
        return false;
      }
    });
    return shouldBuild[0];
  }

//  private void queueFilesForIndexer(Collection<LibraryUnit> libraries) {
//    ArrayList<IndexingTarget> targets = new ArrayList<IndexingTarget>();
//    for (LibraryUnit libraryUnit : libraries) {
//      DartLibrary library = null; //DartModelManager.getInstance().getLibraryWithUri(libraryUnit.getSource().getUri());
//      if (library != null) {
//        // TODO(brianwilkerson) Remove the enclosing test once the indexer is no longer tied to
//        // resources.
//        for (DartUnit unit : libraryUnit.getUnits()) {
//          if (!unit.isDiet()) {
//            CompilationUnit compilationUnit = library.getCompilationUnit(unit.getSourceName());
//            targets.add(new CompilationUnitIndexingTarget(compilationUnit, unit));
//          }
//        }
//      }
//    }
//    StandardDriver.getInstance().enqueueTargets(targets.toArray(new IndexingTarget[targets.size()]));
//  }
}
