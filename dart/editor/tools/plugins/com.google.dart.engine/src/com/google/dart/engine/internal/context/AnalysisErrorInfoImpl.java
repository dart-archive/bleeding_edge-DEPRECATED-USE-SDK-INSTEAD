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
package com.google.dart.engine.internal.context;

import com.google.dart.engine.context.AnalysisErrorInfo;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.utilities.source.LineInfo;

/**
 * Instances of the class {@code AnalysisErrorInfoImpl} represent the analysis errors and line info
 * associated with a source.
 */
public class AnalysisErrorInfoImpl implements AnalysisErrorInfo {

  /**
   * The analysis errors associated with a source, or {@code null} if there are no errors.
   */
  private AnalysisError[] errors;

  /**
   * The line information associated with the errors, or {@code null} if there are no errors.
   */
  private LineInfo lineInfo;

  /**
   * Initialize an newly created error info with the errors and line information
   * 
   * @param errors the errors as a result of analysis
   * @param lineinfo the line info for the errors
   */
  public AnalysisErrorInfoImpl(AnalysisError[] errors, LineInfo lineInfo) {
    this.errors = errors;
    this.lineInfo = lineInfo;
  }

  /**
   * Return the errors of analysis, or {@code null} if there were no errors.
   * 
   * @return the errors as a result of the analysis
   */
  @Override
  public AnalysisError[] getErrors() {
    return errors;
  }

  /**
   * Return the line information associated with the errors, or {@code null} if there were no
   * errors.
   * 
   * @return the line information associated with the errors
   */
  @Override
  public LineInfo getLineInfo() {
    return lineInfo;
  }

}
