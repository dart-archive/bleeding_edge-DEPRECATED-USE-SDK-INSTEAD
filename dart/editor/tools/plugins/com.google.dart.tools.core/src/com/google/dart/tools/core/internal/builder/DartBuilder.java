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
import com.google.dart.compiler.DartCompilationPhase;
import com.google.dart.compiler.DartCompiler;
import com.google.dart.compiler.DartCompilerContext;
import com.google.dart.compiler.DefaultCompilerConfiguration;
import com.google.dart.compiler.LibrarySource;
import com.google.dart.compiler.SystemLibraryManager;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.metrics.CompilerMetrics;
import com.google.dart.compiler.resolver.CoreTypeProvider;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.internal.model.DartProjectImpl;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;
import com.google.dart.tools.core.internal.util.Extensions;
import com.google.dart.tools.core.internal.util.Util;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.HTMLFile;

import static com.google.dart.tools.core.internal.builder.BuilderUtil.clearErrorMarkers;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Instances of the class <code>DartBuilder</code> implement the incremental builder for Dart
 * projects.
 */
public class DartBuilder extends IncrementalProjectBuilder {
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

  private final ArtifactProvider provider = new ArtifactProvider();

  @SuppressWarnings("rawtypes")
  @Override
  protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
    // If *anything* changes then find each Dart App and build it
    if (hasDartSourceChanged()) {
      try {
        buildAllApplications(monitor);
      } finally {
        provider.refreshAndMarkDerived(new NullProgressMonitor());
      }
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
    if (!(lib instanceof DartLibraryImpl)) {
      lib = new DartLibraryImpl((DartProjectImpl) lib.getDartProject(),
          (IFile) ((DartLibraryImpl) lib).getCorrespondingResource());
    }

    DartLibraryImpl libImpl = (DartLibraryImpl) lib;

    try {
      // # compilation units * # phases (3) 
      //     + fudge factor for bundled library such as core and dom (# classes * 3 phases)
      monitor.beginTask("Building " + lib.getElementName(),
          lib.getCompilationUnits().length * 2 + 630);

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
          return false; // TODO Restore to true
        }

        @Override
        public boolean resolveDespiteParseErrors() {
          return true;
        }
      };
      final CompilerListener listener = new CompilerListener(getProject());

      //Try:
      //1. Have the compiler build the Library
      //2. Tell the CompilerMetrics that the Compiler is done
      //3. Have the Messenger tell the MetricsManager that a new build is in
      DartCompiler.compileLib(libSource, config, provider, listener);
      config.getCompilerMetrics().done();
      MetricsMessenger.getSingleton().fireUpdates(config,
          new Path(libSource.getName()).lastSegment());

      emitArtifactDetailsToConsole(libImpl);

    } catch (Throwable exception) {
//      createErrorMarker(getProject(), 0, 0, 0, exception.getMessage());
      Util.log(exception, "Exception while building " + lib.getElementName());
    } finally {
      monitor.done();
    }
  }

  @Override
  protected void clean(IProgressMonitor monitor) throws CoreException {
    IProject project = getProject();
    // TODO(brianwilkerson) Figure out how to give reasonable feedback.
//    DartProject dartProject = DartCore.create(project);
//    monitor.beginTask(
//        "Cleaning " + (dartProject == null ? project.getName() : dartProject.getElementName()), 1);
//    try {
    provider.deleteDerivedDartResources(project, new NullProgressMonitor());
    provider.deleteBundledLibraryOutput();
    provider.clearPrerequisiteProjects();
//    } finally {
//      monitor.done();
//    }
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
    DartCore.getConsole().clear();
    File artifactFile = ArtifactProvider.getJsAppArtifactFile(libImpl.getCorrespondingResource());
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
          return true;
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
}
