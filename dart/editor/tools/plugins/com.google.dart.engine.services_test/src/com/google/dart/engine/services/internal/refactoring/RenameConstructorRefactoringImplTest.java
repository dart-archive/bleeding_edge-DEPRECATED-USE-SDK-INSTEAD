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

import com.google.dart.engine.services.status.RefactoringStatusSeverity;

/**
 * Test for {@link RenameConstructorRefactoringImpl}.
 */
public class RenameConstructorRefactoringImplTest extends RenameRefactoringImplTest {
  public void test_checkFinalConditions_hasMember_MethodElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A.test() {}",
        "  newName() {} // existing",
        "}");
    createRenameRefactoring("test() {}");
    // check status
    refactoring.setNewName("newName");
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Class 'A' already declares method with name 'newName'.",
        findRangeIdentifier("newName() {} // existing"));
  }

  public void test_checkInitialConditions() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A.test() {} // marker",
        "}");
    createRenameRefactoring("test() {} // marker");
    // null
    refactoring.setNewName(null);
    assertRefactoringStatus(
        refactoring.checkInitialConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Constructor name must not be null.");
    // empty
    refactoring.setNewName("");
    assertRefactoringStatusOK(refactoring.checkInitialConditions(pm));
    // same name
    refactoring.setNewName("test");
    assertRefactoringStatus(
        refactoring.checkInitialConditions(pm),
        RefactoringStatusSeverity.FATAL,
        "Choose another name.");
  }

  public void test_createChange_changeName() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A.test() {} // marker",
        "}",
        "class B extends A {",
        "  B() : super.test() {}",
        "  factory B.named() = A.test;",
        "}",
        "main() {",
        "  new A.test();",
        "}");
    // configure refactoring
    createRenameRefactoring("test() {} // marker");
    assertEquals("Rename Constructor", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    // TODO(scheglov) restore after resolver fix
//    assertSuccessfulRename(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "class A {",
//        "  A.newName() {} // marker",
//        "}",
//        "class B extends A {",
//        "  B() : super.newName() {}",
//        "  factory B.named() = A.newName;",
//        "}",
//        "main() {",
//        "  new A.newName();",
//        "}");
  }
}
