/*
 * Copyright 2011 Dart project authors.
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
package com.google.dart.tools.core.internal.compiler;

import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.DartCompilerListener;
import com.google.dart.compiler.ErrorSeverity;
import com.google.dart.compiler.SubSystem;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.DartCore;

/**
 * Log compilation errors for debugging purposes
 */
public class LoggingDartCompilerListener extends DartCompilerListener {
  /**
   * A compiler listener that can be shared.
   */
  public static final LoggingDartCompilerListener INSTANCE = new LoggingDartCompilerListener();
  
  @Override
  public void onError(DartCompilationError event) {
    if (event.getErrorCode().getErrorSeverity() == ErrorSeverity.ERROR) {
      DartCore.logError("Compilation error: " + event);
    }
    if (event.getErrorCode().getErrorSeverity() == ErrorSeverity.WARNING) {
      DartCore.logError("Compilation warning: " + event);
    }
    if (event.getErrorCode().getSubSystem() == SubSystem.STATIC_TYPE) {
      DartCore.logError("Type error: " + event);
    }
  }

  @Override
  public void unitCompiled(DartUnit unit) {
  }
}
