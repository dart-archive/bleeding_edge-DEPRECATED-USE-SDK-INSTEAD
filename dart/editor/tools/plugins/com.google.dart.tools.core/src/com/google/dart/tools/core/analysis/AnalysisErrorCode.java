/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.core.analysis;

import com.google.dart.compiler.ErrorCode;
import com.google.dart.compiler.ErrorSeverity;
import com.google.dart.compiler.SubSystem;

enum AnalysisErrorCode implements ErrorCode {

  PARSE_FAILURE("Failed to parse '%s"),
  RESOLUTION_FAILURE("Failed to resolve '%s");

  private final ErrorSeverity severity;
  private final String message;

  /**
   * Initialize a newly created error code to have the given severity and message.
   */
  private AnalysisErrorCode(ErrorSeverity severity, String message) {
    this.severity = severity;
    this.message = message;
  }

  /**
   * Initialize a newly created error code to have the given message and ERROR severity.
   */
  private AnalysisErrorCode(String message) {
    this(ErrorSeverity.ERROR, message);
  }

  @Override
  public ErrorSeverity getErrorSeverity() {
    return severity;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public SubSystem getSubSystem() {
    // TODO (danrubel): Introduce an analysis subsystema
    return SubSystem.RESOLVER;
  }

  @Override
  public boolean needsRecompilation() {
    return true;
  }
}
