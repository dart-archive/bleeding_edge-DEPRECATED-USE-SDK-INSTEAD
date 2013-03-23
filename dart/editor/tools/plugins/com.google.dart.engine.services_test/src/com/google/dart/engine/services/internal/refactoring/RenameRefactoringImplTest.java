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

package com.google.dart.engine.services.internal.refactoring;

import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.refactoring.RefactoringFactory;
import com.google.dart.engine.services.refactoring.RenameRefactoring;
import com.google.dart.engine.services.status.RefactoringStatusSeverity;

/**
 * Abstract test for testing {@link RenameRefactoring}s.
 */
public abstract class RenameRefactoringImplTest extends RefactoringImplTest {
  protected RenameRefactoring refactoring;

  /**
   * Asserts that {@link #refactoring} status is OK.
   */
  protected final void assertRefactoringStatusOK() throws Exception {
    assertRefactoringStatus(
        refactoring.checkInitialConditions(pm),
        RefactoringStatusSeverity.OK,
        null);
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.OK,
        null);
  }

  /**
   * Checks that all conditions are <code>OK</code> and applying {@link Change} to the
   * {@link #testUnit} is same source as given lines.
   */
  protected final void assertSuccessfulRename(String... lines) throws Exception {
    assertRefactoringStatusOK();
    Change change = refactoring.createChange(pm);
    assertTestChangeResult(change, makeSource(lines));
  }

  /**
   * @return the {@link RenameRefactoring} for given {@link Element}.
   */
  protected final void createRenameRefactoring(Element element) {
    refactoring = RefactoringFactory.createRenameRefactoring(searchEngine, element);
  }

  /**
   * @return the {@link RenameRefactoring} for {@link Element} of the {@link SimpleIdentifier} at
   *         the given search pattern.
   */
  protected final void createRenameRefactoring(String search) {
    Element element = findIdentifierElement(search);
    createRenameRefactoring(element);
  }
}
