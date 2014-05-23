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

package com.google.dart.engine.services.refactoring;

import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.services.status.RefactoringStatus;

/**
 * {@link Refactoring} to extract {@link Expression} or {@link Statement}s into method.
 */
public interface ExtractMethodRefactoring extends Refactoring {
  /**
   * @return {@code true} if selected code can be extracted as "getter".
   */
  boolean canExtractGetter();

  /**
   * Validates the name set using {@link #setMethodName(String)}.
   */
  RefactoringStatus checkMethodName();

  /**
   * Checks if the parameter names are valid.
   */
  RefactoringStatus checkParameterNames();

  /**
   * @return {@code true} if getter should be extracted instead of normal method.
   */
  boolean getExtractGetter();

  /**
   * @return the number of occurrences of the same source as selection.
   */
  int getNumberOfOccurrences();

  /**
   * @return {@link Parameter}s describing parameters of the extracted expression of statements.
   */
  Parameter[] getParameters();

  /**
   * @return <code>true</code> if all occurrences of selected expression or statement should be
   *         replaced.
   */
  boolean getReplaceAllOccurrences();

  /**
   * @return the signature of the extracted method.
   */
  String getSignature();

  /**
   * Specifies if getter should be extracted instead of normal method.
   */
  void setExtractGetter(boolean extractGetter);

  /**
   * Sets the name for new method.
   */
  void setMethodName(String methodName);

  /**
   * Sets reordered or/and updated {@link Parameter}s.
   */
  void setParameters(Parameter[] parameters);

  /**
   * Specifies if all occurrences of the selected expression or statements should be replaced.
   */
  void setReplaceAllOccurrences(boolean replaceAllOccurrences);
}
