/*
 * Copyright (c) 2013, the Dart project authors.
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

import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.utilities.source.LineInfo;

/**
 * The interface {@code AnalysisErrorInfo} contains the analysis errors and line information for the
 * errors.
 */
public interface AnalysisErrorInfo {
  /**
   * Return the errors that as a result of the analysis, or {@code null} if there were no errors.
   * 
   * @return the errors as a result of the analysis
   */
  public AnalysisError[] getErrors();

  /**
   * Return the line information associated with the errors, or {@code null} if there were no
   * errors.
   * 
   * @return the line information associated with the errors
   */
  public LineInfo getLineInfo();

}
