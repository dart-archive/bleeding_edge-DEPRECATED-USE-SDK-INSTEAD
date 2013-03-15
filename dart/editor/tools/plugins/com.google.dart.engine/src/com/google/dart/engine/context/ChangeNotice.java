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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.LineInfo;

/**
 * Instances of the class {@code ChangeNotice} represent a change to the analysis results associated
 * with a given source.
 */
public class ChangeNotice {
  /**
   * The source for which the result is being reported.
   */
  private Source source;

  /**
   * The fully resolved AST that changed as a result of the analysis, or {@code null} if the AST was
   * not changed.
   */
  private CompilationUnit compilationUnit;

  /**
   * The errors that changed as a result of the analysis, or {@code null} if errors were not
   * changed.
   */
  private AnalysisError[] errors;

  /**
   * The line information associated with the source, or {@code null} if errors were not changed.
   */
  private LineInfo lineInfo;

  /**
   * An empty array of change notices.
   */
  public static final ChangeNotice[] EMPTY_ARRAY = new ChangeNotice[0];

  /**
   * Initialize a newly created result representing the fact that the errors associated with a
   * source have changed.
   * 
   * @param source the source for which the result is being reported
   * @param errors the errors that changed as a result of the analysis
   * @param the line information associated with the source
   */
  public ChangeNotice(Source source, AnalysisError[] errors, LineInfo lineInfo) {
    this.source = source;
    this.errors = errors;
    this.lineInfo = lineInfo;
  }

  /**
   * Initialize a newly created result representing the fact that the resolution of a source has
   * changed.
   * 
   * @param source the source for which the result is being reported
   * @param compilationUnit the fully resolved AST produced as a result of the analysis
   */
  public ChangeNotice(Source source, CompilationUnit compilationUnit) {
    this.source = source;
    this.compilationUnit = compilationUnit;
  }

  /**
   * Return the fully resolved AST that changed as a result of the analysis, or {@code null} if the
   * AST was not changed.
   * 
   * @return the fully resolved AST that changed as a result of the analysis
   */
  public CompilationUnit getCompilationUnit() {
    return compilationUnit;
  }

  /**
   * Return the errors that changed as a result of the analysis, or {@code null} if errors were not
   * changed.
   * 
   * @return the errors that changed as a result of the analysis
   */
  public AnalysisError[] getErrors() {
    return errors;
  }

  /**
   * Return the line information associated with the source, or {@code null} if errors were not
   * changed.
   * 
   * @return the line information associated with the source
   */
  public LineInfo getLineInfo() {
    return lineInfo;
  }

  /**
   * Return the source for which the result is being reported.
   * 
   * @return the source for which the result is being reported
   */
  public Source getSource() {
    return source;
  }
}
