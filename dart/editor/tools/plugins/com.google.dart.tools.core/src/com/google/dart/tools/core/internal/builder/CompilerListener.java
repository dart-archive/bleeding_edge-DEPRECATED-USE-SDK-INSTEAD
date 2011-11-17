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

import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.DartCompilerContext;
import com.google.dart.compiler.DartCompilerListener;
import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.ErrorSeverity;
import com.google.dart.compiler.Source;
import com.google.dart.compiler.SubSystem;
import com.google.dart.compiler.SystemLibraryManager;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.indexer.standard.StandardDriver;
import com.google.dart.indexer.workspace.index.IndexingTarget;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.indexer.task.CompilationUnitIndexingTarget;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;

import static com.google.dart.tools.core.internal.builder.BuilderUtil.createErrorMarker;
import static com.google.dart.tools.core.internal.builder.BuilderUtil.createWarningMarker;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import java.net.URI;

/**
 * An Eclipse specific implementation of {@link DartCompilerContext} for intercepting compilation
 * errors and translating them into {@link IResource} markers.
 */
class CompilerListener extends DartCompilerListener {
  /**
   * The number of times that we have logged a message about compilation errors that were reported
   * for which we could not associate a source file and were forced to associate the marker with the
   * top-level library.
   */
  private static int missingSourceCount = 0;

  /**
   * The top-level library containing the code being compiled.
   */
  private DartLibrary library;

  /**
   * The project associated with the library.
   */
  private final IProject project;

  private static final int MISSING_SOURCE_REPORT_LIMIT = 5;

  CompilerListener(DartLibrary library, IProject project) {
    this.project = project;
    this.library = library;
  }

  @Override
  public void onError(DartCompilationError event) {
    if (event.getErrorCode().getSubSystem() == SubSystem.STATIC_TYPE) {
      processWarning(event);
    } else if (event.getErrorCode().getErrorSeverity() == ErrorSeverity.ERROR) {
      processError(event);
    } else if (event.getErrorCode().getErrorSeverity() == ErrorSeverity.WARNING) {
      processWarning(event);
    }
  }

  @Override
  public void unitCompiled(DartUnit unit) {
    if (unit.isDiet()) {
      return;
    }
    DartSource source = unit.getSource();
    if (source != null) {
      IFile[] resources = ResourceUtil.getResources(source);
      if (resources == null || resources.length != 1) {
        URI sourceUri = source.getUri();
        if (!SystemLibraryManager.isDartUri(sourceUri)) {
          DartCore.logError("Could not find compilation unit corresponding to " + sourceUri + " ("
              + (resources == null ? "no" : resources.length) + " files found)");
        }
        return;
      }
      CompilationUnit compilationUnit = (CompilationUnit) DartCore.create(resources[0]);
      if (compilationUnit == null) {
        DartCore.logError("Could not find compilation unit corresponding to " + source.getUri()
            + " (resource did not map)");
        return;
      }
      StandardDriver.getInstance().enqueueTargets(
          new IndexingTarget[] {new CompilationUnitIndexingTarget(compilationUnit, unit)});
    }
  }

  /**
   * Return the Eclipse resource associated with the given error's source, or the resource
   * associated with the library's defining compilation unit if the real resource cannot be found.
   * 
   * @param error the compilation error defining the source
   * @return the resource associated with the error's source
   */
  private IResource getResource(DartCompilationError error) {
    Source source = error.getSource();
    IResource res = ResourceUtil.getResource(source);
    if (res == null) {
      if (missingSourceCount <= MISSING_SOURCE_REPORT_LIMIT) {
        // Don't flood the log
        missingSourceCount++;
        StringBuilder builder = new StringBuilder();
        if (source == null) {
          builder.append("No source associated with compilation error (");
        } else {
          builder.append("Could not find file for source \"");
          builder.append(source.getUri().toString());
          builder.append("\" (");
        }
        builder.append(missingSourceCount);
        if (missingSourceCount == MISSING_SOURCE_REPORT_LIMIT) {
          builder.append(", final warning): ");
        } else {
          builder.append("): ");
        }
        builder.append(error.getMessage());
        RuntimeException exception = new RuntimeException(builder.toString());
        DartCore.logInformation(exception.getMessage(), exception);
        // TODO (danrubel): generalize the logging mechanism for all plugins
      }
      try {
        res = library.getDefiningCompilationUnit().getCorrespondingResource();
      } catch (DartModelException exception) {
        // Fall through to use the project as a resource
      }
      if (res == null) {
        res = project;
      }
    }
    return res;
  }

  /**
   * Create a marker for the given compilation error.
   * 
   * @param error the compilation error for which a marker is to be created
   */
  private void processError(DartCompilationError error) {
    IResource res = getResource(error);
    if (res != null && res.exists() && res.getProject().equals(project)) {
      createErrorMarker(res, error.getStartPosition(), error.getLength(), error.getLineNumber(),
          error.getMessage());
    }
  }

  /**
   * Create a marker for the given compilation error.
   * 
   * @param error the compilation error for which a marker is to be created
   */
  private void processWarning(DartCompilationError error) {
    IResource res = getResource(error);
    if (res != null && res.exists() && res.getProject().equals(project)) {
      createWarningMarker(res, error.getStartPosition(), error.getLength(), error.getLineNumber(),
          error.getMessage());
    }
  }
}
