/*
 * Copyright (c) 2012, the Dart project authors.
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
import com.google.dart.compiler.DartCompilationPhase;
import com.google.dart.compiler.DartCompilerContext;
import com.google.dart.compiler.DefaultCompilerConfiguration;
import com.google.dart.compiler.LibrarySource;
import com.google.dart.compiler.Source;
import com.google.dart.compiler.SystemLibraryManager;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.metrics.CompilerMetrics;
import com.google.dart.compiler.resolver.CoreTypeProvider;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Handles the builds using the dartc compiler
 */
public class DartcBuildHandler {

  /**
   * An artifact provider for tracking prerequisite projects. All artifacts are cached in memory via
   * {@link RootArtifactProvider} except for the final app.js file which is written to disk as a
   * *.js file
   */
  private class ArtifactProvider extends CachingArtifactProvider {
    private final RootArtifactProvider rootProvider = RootArtifactProvider.getInstance();
    private final Collection<IProject> prerequisiteProjects = new HashSet<IProject>();
    private int writeArtifactCount;
    private int outOfDateCount;

    public void beginBuild() {
      writeArtifactCount = 0;
      outOfDateCount = 0;
    }

    public void clean(IProject project, IProgressMonitor monitor) {
      prerequisiteProjects.clear();
      rootProvider.clearCachedArtifacts();

      for (DartLibrary library : getDartLibraries(project)) {
        cleanLibraryArtifactFile(library);
      }
    }

    public void cleanLibrary(DartLibrary library) {
      rootProvider.removeArtifactsFor(((DartLibraryImpl) library).getLibrarySourceFile());
      cleanLibraryArtifactFile(library);
    }

    public void cleanLibraryArtifactFile(DartLibrary library) {
      try {
        File file = DartBuilder.getJsAppArtifactFile(library.getCorrespondingResource());

        if (file != null && file.exists()) {
          file.delete();
        }
      } catch (DartModelException exception) {
        DartCore.logError(exception);
      }
    }

    public void endBuild() {
      // Clear any artifacts cached for the duration of this compilation
      super.clearCachedArtifacts();
    }

    @Override
    public Reader getArtifactReader(Source source, String part, String extension)
        throws IOException {
      IResource res = ResourceUtil.getResource(source);
      if (res != null) {
        IProject project = res.getProject();
        prerequisiteProjects.add(project);
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

    private List<DartLibrary> getDartLibraries(IProject project) {
      List<DartLibrary> libraries = new ArrayList<DartLibrary>();

      try {
        for (DartLibrary library : DartModelManager.getInstance().getDartModel().getDartLibraries()) {
          if (project.equals(library.getDartProject().getProject())) {
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
   * The artifact provider for this source.
   */
  private final ArtifactProvider provider = new ArtifactProvider();

  /**
   * Build all of the libraries in the given project that are contained within the given parent.
   * 
   * @param project the project containing the libraries to be built
   * @param parent the parent of the libraries to be built
   * @param monitor the monitor used to provide feedback to the user, or <code>null</code> if no
   *          feedback is requested
   * @throws CoreException if the build fails for some reason
   */
  public void buildLibrariesIn(IProject project, IResource parent, IProgressMonitor monitor)
      throws CoreException {
    DartProject dartProject = DartCore.create(project);
    DartLibrary[] allLibraries = dartProject.getDartLibraries();
    SubMonitor subMonitor = SubMonitor.convert(monitor, "Building " + dartProject.getElementName()
        + "...", allLibraries.length * 100);
    try {
      for (DartLibrary library : allLibraries) {
        if (monitor.isCanceled()) {
          throw new OperationCanceledException();
        }
        CompilationUnit definingUnit = library.getDefiningCompilationUnit();
        IResource resource = definingUnit.getResource();
        if (isOrContains(parent, resource)) {
          provider.cleanLibrary(library);
          buildLibrary(project, library, false, subMonitor.newChild(100));
        }
      }
    } finally {
      monitor.done();
    }
  }

  public void clean(IProject project, IProgressMonitor monitor) throws CoreException {
    BuilderUtil.clearErrorMarkers(project);
    provider.clean(project, monitor);
  }

  public IProject[] getPrerequisiteProjects() {
    return provider.getPrerequisiteProjects();
  }

  /**
   * When first launched, the prerequisite projects have not been set thus any dependent libraries
   * will not be compiled. This method looks for dependent libraries and explicitly triggers a
   * build.
   */
  public void triggerDependentBuilds(IProject project, IProgressMonitor monitor)
      throws CoreException {
    DartProject proj = DartCore.create(project);
    for (DartLibrary lib : proj.getDartLibraries()) {
      for (DartLibrary refLib : ((DartLibraryImpl) lib).getReferencingLibraries()) {
        CompilationUnit unit = ((DartLibraryImpl) refLib).getDefiningCompilationUnit();
        IResource resource = unit.getResource();
        if (resource != null) {
          // The resource can be null when the unit is an external compilation unit.
          resource.touch(monitor);
        }
      }
    }
  }

  /**
   * Build all the libraries in the project associated with the receiver
   * 
   * @param monitor the progress monitor
   */
  protected void buildAllApplications(IProject project, boolean shouldGenerateJs,
      IProgressMonitor monitor) throws CoreException {
    DartProject proj = DartCore.create(project);
    DartLibrary[] allLibraries = proj.getDartLibraries();

    SubMonitor subMonitor = SubMonitor.convert(
        monitor,
        "Building " + proj.getElementName() + "...",
        allLibraries.length * 100);

    try {
      for (DartLibrary lib : allLibraries) {
        if (monitor.isCanceled()) {
          throw new OperationCanceledException();
        }

        buildLibrary(project, lib, shouldGenerateJs, subMonitor.newChild(100));
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
  protected void buildLibrary(IProject project, DartLibrary lib, final boolean shouldGenerateJs,
      final IProgressMonitor monitor) {

    final DartLibraryImpl libImpl = (DartLibraryImpl) lib;

    IResource libResource = project;

    try {
      // # compilation units * # phases (3) 
      //     + fudge factor for bundled library such as core and dom (# classes * 3 phases)
      monitor.beginTask(
          "Building " + lib.getElementName(),
          lib.getCompilationUnits().length * 2 + 630);

      // Delete the previous compiler output, if it exists.
      libResource = lib.getCorrespondingResource();
      if (!DartCore.isAnalyzed(libResource)) {
        return;
      }
      File file = DartBuilder.getJsAppArtifactFile(libResource);

      if (shouldGenerateJs) {
        if (file != null && file.exists()) {
          file.delete();
        }
      }

      // Call the Dart to JS compiler
      final LibrarySource libSource = libImpl.getLibrarySourceFile();
      final CompilerMetrics metrics = new CompilerMetrics();
      final SystemLibraryManager libraryManager = SystemLibraryManagerProvider.getSystemLibraryManager();
      final CompilerConfiguration config = new DefaultCompilerConfiguration(
          new CompilerOptions(),
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
      final CompilerListener listener = new CompilerListener(lib, project, true);

      //Try:
      //1. Have the compiler build the Library
      //2. Tell the CompilerMetrics that the Compiler is done
      //3. Have the Messenger tell the MetricsManager that a new build is in
      provider.beginBuild();
      DartCompilerUtilities.secureCompileLib(libSource, config, provider, listener);
      provider.endBuild();
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
      MetricsMessenger.getSingleton().fireUpdates(
          config,
          new Path(libSource.getName()).lastSegment());
    } catch (Throwable exception) {
      if (DartCore.isAnalyzed(libResource)) {
        BuilderUtil.createErrorMarker(
            libResource,
            0,
            0,
            0,
            "Internal compiler error: " + exception.toString());
      }
      DartCore.logError("Exception caught while building " + lib.getElementName(), exception);
    } finally {
      monitor.done();
    }
  }

  /**
   * Return <code>true</code> if the parent resource is either the same as or contains the child
   * resource.
   * 
   * @param parent the resource that might contain the child
   * @param child the resource being tested for containment in the parent
   * @return <code>true</code> if the parent resource is either the same as or contains the child
   *         resource
   */
  private boolean isOrContains(IResource parent, IResource child) {
    String parentPath = parent.getFullPath().toPortableString();
    String childPath = child.getFullPath().toPortableString();
    return childPath.equals(parentPath) || childPath.startsWith(parentPath + "/");
  }
}
