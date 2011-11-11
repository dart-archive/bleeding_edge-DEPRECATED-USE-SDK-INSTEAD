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
import com.google.dart.compiler.metrics.CompilerMetrics;
import com.google.dart.compiler.resolver.CoreTypeProvider;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Compiles to optimized javascript
 */
public class CompileOptimized {

  /**
   * An artifact provider for tracking prerequisite projects. All artifacts are cached in memory via
   * {@link RootArtifactProvider} except for the final opt.js file which is written to disk.
   */
  private class ArtifactProvider extends DartArtifactProvider {
    private final RootArtifactProvider rootProvider = RootArtifactProvider.getInstance();
    private final Collection<IProject> prerequisiteProjects = new HashSet<IProject>();

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
      return rootProvider.getArtifactWriter(source, part, extension);
    }

    @Override
    public boolean isOutOfDate(Source source, Source base, String extension) {
      return false;
    }

    /**
     * Answer the final application JS file if that is what is specified
     * 
     * @return the file or <code>null</code> if it is not specified
     */
    private File getAppJsFile(Source source, String part, String extension) throws AssertionError {
      if (!extension.equals("opt.js") || !"".equals(part)) { //$NON-NLS-N$
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
      return jsFile;
    }

  }

  private final DartArtifactProvider provider = new ArtifactProvider();

  private DartLibrary library;
  private static File jsFile;

  public CompileOptimized(DartLibrary library, File outputfile) {
    this.library = library;
    jsFile = outputfile;
  }

  public IStatus compileToJs(final IProgressMonitor monitor) {

    IStatus status = Status.OK_STATUS;

    DartLibraryImpl libImpl = (DartLibraryImpl) library;
    try {

      monitor.beginTask("Compiling " + library.getElementName(),
          library.getCompilationUnits().length * 2 + 630);

      // Delete the previous compiler output, if it exists.
      File file = jsFile;
      if (file != null && file.exists()) {
        file.delete();
      }

      // Call the Dart to JS closure compiler
      final LibrarySource libSource = libImpl.getLibrarySourceFile();
      final CompilerMetrics metrics = new CompilerMetrics();
      final SystemLibraryManager libraryManager = SystemLibraryManagerProvider.getSystemLibraryManager();
      CompilerOptions options = new CompilerOptions();
      options.setOptimize(true);
      final CompilerConfiguration config = new DefaultCompilerConfiguration(options, libraryManager) {
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
      final CompilerListener listener = new CompilerListener(library, null);

      //Try:
      //1. Have the compiler build the Library
      //2. Tell the CompilerMetrics that the Compiler is done
      DartCompilerUtilities.secureCompileLib(libSource, config, provider, listener);
      config.getCompilerMetrics().done();
      emitArtifactDetailsToConsole(libImpl);

    } catch (Throwable exception) {
      DartCore.logInformation("Exception caught while building " + library.getElementName(),
          exception);
      status = new Status(Status.ERROR, DartBuilderMessages.CompileOptmized_title, 1, NLS.bind(
          DartBuilderMessages.CompileOtimized_errorMessage, library.getElementName()), exception);

    } finally {
      monitor.done();

    }
    return status;
  }

  private void emitArtifactDetailsToConsole(DartLibraryImpl libImpl) throws DartModelException {

    if (jsFile != null) {
      DartCore.getConsole().println(
          DartBuilderMessages.DartBuilder_console_js_file_description + ": "
              + jsFile.getAbsolutePath());
    }
  }

}
