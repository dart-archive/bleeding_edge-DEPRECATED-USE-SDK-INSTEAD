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
import com.google.dart.engine.services.status.RefactoringStatus;

/**
 * {@link Refactoring} to extract {@link Expression} into separate local variable declaration.
 */
public interface ExtractLocalRefactoring extends Refactoring {
  /**
   * Validates the name for new local variable.
   */
  RefactoringStatus checkLocalName(String newName);

  /**
   * @return proposed variable names (may be empty, but not null). The first proposal should be used
   *         as "best guess" (if it exists).
   */
  String[] guessNames();

  /**
   * @return <code>true</code> if all occurrences of the selected expression should be replaced.
   */
  boolean replaceAllOccurrences();

  /**
   * Sets the name for new local variable.
   */
  void setLocalName(String localName);

  /**
   * Specifies if all occurrences of the selected expression should be replaced.
   */
  void setReplaceAllOccurrences(boolean replaceAllOccurrences);
}
