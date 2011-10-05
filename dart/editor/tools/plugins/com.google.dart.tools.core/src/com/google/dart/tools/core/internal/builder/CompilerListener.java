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
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.internal.util.Util;

import static com.google.dart.tools.core.internal.builder.BuilderUtil.createErrorMarker;
import static com.google.dart.tools.core.internal.builder.BuilderUtil.createWarningMarker;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

/**
 * An Eclipse specific implementation of {@link DartCompilerContext} for intercepting compilation
 * errors and translating them into {@link IResource} markers.
 */
class CompilerListener extends DartCompilerListener {
  private static int missingSourceCount = 0;
  private final IProject project;

  CompilerListener(IProject project) {
    this.project = project;
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
      if (missingSourceCount <= 5) {
        RuntimeException ex = new RuntimeException("No source associated with compilation error ("
            + missingSourceCount + "): " + error.getMessage());
        Util.log(ex, ex.getMessage());
        // TODO (danrubel): generalize the logging mechanism for all plugins,
        // include code to prevent log flooding
        // include message in log indicating that further messages will not be logged
      }
      res = project;
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
