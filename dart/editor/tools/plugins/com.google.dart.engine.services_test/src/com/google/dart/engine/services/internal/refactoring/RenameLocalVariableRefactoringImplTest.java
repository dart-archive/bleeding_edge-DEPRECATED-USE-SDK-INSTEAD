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

import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.refactoring.RenameRefactoring;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.services.status.RefactoringStatusSeverity;

public class RenameLocalVariableRefactoringImplTest extends RenameRefactoringImplTest {
  /**
   * Asserts that given {@link RefactoringStatus} has expected severity and message.
   */
  protected static void assertRefactoringStatus(RefactoringStatus status,
      RefactoringStatusSeverity expectedSeverity, String expectedMessage) {
    assertSame(expectedSeverity, status.getSeverity());
    assertEquals(expectedMessage, status.getMessage());
  }

  public void test_checkInitialConditions() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int test = 0;",
        "}");
    RenameRefactoring refactoring = createRenameRefactoring("test = 0");
    // null
    refactoring.setNewName(null);
    assertRefactoringStatus(
        refactoring.checkInitialConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Variable name must not be null");
    // empty
    refactoring.setNewName("");
    assertRefactoringStatus(
        refactoring.checkInitialConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Variable name must not be empty");
    // leading blanks
    refactoring.setNewName(" newName");
    assertRefactoringStatus(
        refactoring.checkInitialConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "A variable name must not start or end with a blank");
    // trailing blanks
    refactoring.setNewName("newName  ");
    assertRefactoringStatus(
        refactoring.checkInitialConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "A variable name must not start or end with a blank");
    // not identifier start
    refactoring.setNewName("2name");
    assertRefactoringStatus(
        refactoring.checkInitialConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "The variable name '2name' is not a valid identifier");
    // not identifier middle
    refactoring.setNewName("na-me");
    assertRefactoringStatus(
        refactoring.checkInitialConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "The variable name 'na-me' is not a valid identifier");
    // private
    refactoring.setNewName("_name");
    assertRefactoringStatus(
        refactoring.checkInitialConditions(pm),
        RefactoringStatusSeverity.OK,
        null);
    // does not start with lower case
    refactoring.setNewName("NewName");
    assertRefactoringStatus(
        refactoring.checkInitialConditions(pm),
        RefactoringStatusSeverity.WARNING,
        "By convention, variable names usually start with a lowercase letter");
    // OK
    refactoring.setNewName("newName");
    assertRefactoringStatus(
        refactoring.checkInitialConditions(pm),
        RefactoringStatusSeverity.OK,
        null);
  }

  public void test_createChange() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int test = 0;",
        "  test = 1;",
        "  test += 2;",
        "  print(test);",
        "}");
    // configure refactoring
    RenameRefactoring refactoring = createRenameRefactoring("test = 0");
    refactoring.setNewName("newName");
    // validate change
    Change change = refactoring.createChange(pm);
    assertChangeResult(
        change,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "main() {",
            "  int newName = 0;",
            "  newName = 1;",
            "  newName += 2;",
            "  print(newName);",
            "}"));
  }
}
