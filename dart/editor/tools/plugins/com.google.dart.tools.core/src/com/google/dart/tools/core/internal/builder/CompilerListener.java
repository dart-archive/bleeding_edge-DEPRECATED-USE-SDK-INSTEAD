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
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;

import static com.google.dart.tools.core.internal.builder.BuilderUtil.createErrorMarker;
import static com.google.dart.tools.core.internal.builder.BuilderUtil.createWarningMarker;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

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

  CompilerListener(DartLibrary library, IProject project) {
    this.project = project;
    this.library = library;
  }

  @Override
  public void compilationError(DartCompilationError error) {
    processError(error);
  }

  @Override
  public void compilationWarning(DartCompilationError error) {
    processWarning(error);
  }

  @Override
  public void typeError(DartCompilationError error) {
    processWarning(error);
  }

  /**
   * Create a marker for the given compilation error.
   * 
   * @param error the compilation error for which a marker is to be created
   */
  private IResource getResource(DartCompilationError error) {
    // Find the Eclipse resource associated with the source
    IResource res = ResourceUtil.getResource(error.getSource());
    if (res == null) {
      // Don't flood the log
      missingSourceCount++;
      if (missingSourceCount == 5) {
        RuntimeException exception = new RuntimeException(
            "No source associated with compilation error (" + missingSourceCount
                + ", final warning): " + error.getMessage());
        DartCore.logInformation(exception.getMessage(), exception);
      } else if (missingSourceCount < 5) {
        RuntimeException exception = new RuntimeException(
            "No source associated with compilation error (" + missingSourceCount + "): "
                + error.getMessage());
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

  @Override
  public void unitCompiled(DartUnit unit) {
  }
}
