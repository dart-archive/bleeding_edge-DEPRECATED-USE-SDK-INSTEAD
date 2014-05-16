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
package com.google.dart.engine.context;

import com.google.dart.engine.utilities.translation.DartOmit;
import com.google.dart.engine.utilities.translation.DartOptional;

/**
 * Instances of the class {@code AnalysisException} represent an exception that occurred during the
 * analysis of one or more sources.
 * 
 * @coverage dart.engine
 */
@DartOmit
public class AnalysisException extends Exception {
  /**
   * Initialize a newly created exception.
   */
  @DartOmit
  public AnalysisException() {
    super();
  }

  /**
   * Initialize a newly created exception to have the given message.
   * 
   * @param message the message associated with the exception
   */
  @DartOmit
  public AnalysisException(String message) {
    super(message);
  }

  /**
   * Initialize a newly created exception to have the given message and cause.
   * 
   * @param message the message associated with the exception
   * @param cause the underlying exception that caused this exception
   */
  public AnalysisException(@DartOptional String message, @DartOptional Throwable cause) {
    super(message, cause);
  }
}
