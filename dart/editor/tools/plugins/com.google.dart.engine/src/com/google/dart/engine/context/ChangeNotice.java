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
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.source.Source;

/**
 * The interface {@code ChangeNotice} defines the behavior of objects that represent a change to the
 * analysis results associated with a given source.
 * 
 * @coverage dart.engine
 */
public interface ChangeNotice extends AnalysisErrorInfo {
  /**
   * Return the fully resolved AST that changed as a result of the analysis, or {@code null} if the
   * AST was not changed.
   * 
   * @return the fully resolved AST that changed as a result of the analysis
   */
  public CompilationUnit getCompilationUnit();

  /**
   * Return the fully resolved HTML that changed as a result of the analysis, or {@code null} if the
   * HTML was not changed.
   * 
   * @return the fully resolved HTML that changed as a result of the analysis
   */
  public HtmlUnit getHtmlUnit();

  /**
   * Return the source for which the result is being reported.
   * 
   * @return the source for which the result is being reported
   */
  public Source getSource();
}
