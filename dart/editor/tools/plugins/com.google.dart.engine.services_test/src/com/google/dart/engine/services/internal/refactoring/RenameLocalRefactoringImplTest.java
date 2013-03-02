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
 * Test for {@link RenameLocalRefactoringImpl}.
 */
public class RenameLocalRefactoringImplTest extends RenameRefactoringImplTest {
  public void test_checkFinalConditions_hasLocalFunction_before() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  newName() => 1;",
        "  var test = 0;",
        "} // main");
    createRenameRefactoring("test = 0");
    // check status
    refactoring.setNewName("newName");
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Duplicate local function 'newName'.",
        findRangeIdentifier("newName() =>"));
  }

  public void test_checkFinalConditions_hasLocalVariable_after() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var test = 0;",
        "  var newName = 1;",
        "} // main");
    createRenameRefactoring("test = 0");
    // check status
    refactoring.setNewName("newName");
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Duplicate local local variable 'newName'.",
        findRangeIdentifier("newName = 1"));
  }

  public void test_checkFinalConditions_hasLocalVariable_before() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var newName = 1;",
        "  var test = 0;",
        "} // main");
    createRenameRefactoring("test = 0");
    // check status
    refactoring.setNewName("newName");
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Duplicate local local variable 'newName'.",
        findRangeIdentifier("newName = 1"));
  }

  public void test_checkFinalConditions_hasLocalVariable_otherFunction() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main2() {",
        "  var newName = 1;",
        "}",
        "main() {",
        "  var test = 0;",
        "} // main");
    createRenameRefactoring("test = 0");
    // check status
    refactoring.setNewName("newName");
    assertRefactoringStatusOK(refactoring.checkFinalConditions(pm));
  }

  public void test_checkFinalConditions_shadows_classMember() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var newName = 1;",
        "  main() {",
        "    var test = 0;",
        "    print(newName);",
        "  } // main",
        "}");
    createRenameRefactoring("test = 0");
    // check status
    refactoring.setNewName("newName");
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Usage of field 'A.newName' declared in 'Test.dart' will be shadowed by renamed local variable.",
        findRangeIdentifier("newName);"));
  }

  public void test_checkFinalConditions_shadows_classMemberOK_qualifiedReference() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var newName = 1;",
        "  main() {",
        "    var test = 0;",
        "    print(this.newName);",
        "  } // main",
        "}");
    createRenameRefactoring("test = 0");
    // check status
    refactoring.setNewName("newName");
    assertRefactoringStatusOK(refactoring.checkFinalConditions(pm));
  }

  public void test_checkFinalConditions_shadows_topLevelFunction() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "newName() {}",
        "main() {",
        "  var test = 0;",
        "  newName(); // ref",
        "} // main");
    createRenameRefactoring("test = 0");
    // check status
    refactoring.setNewName("newName");
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Usage of function 'newName' declared in 'Test.dart' will be shadowed by renamed local variable.",
        findRangeIdentifier("newName(); // ref"));
  }

  public void test_checkFinalConditions_shadows_typeVariable() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A<newName> {",
        "  main() {",
        "    var test = 0;",
        "    newName v;",
        "  } // main",
        "}");
    createRenameRefactoring("test = 0");
    // check status
    refactoring.setNewName("newName");
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Usage of type variable 'newName' declared in 'Test.dart' will be shadowed by renamed local variable.",
        findRangeIdentifier("newName v;"));
  }

  public void test_checkNewName_FunctionElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  test() {}",
        "}");
    createRenameRefactoring("test() {}");
    // null
    refactoring.setNewName(null);
    assertRefactoringStatus(
        refactoring.checkNewName(null),
        RefactoringStatusSeverity.ERROR,
        "Function name must not be null.");
    // OK
    assertRefactoringStatusOK(refactoring.checkNewName("newName"));
  }

  public void test_checkNewName_LocalVariableElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int test = 0;",
        "}");
    createRenameRefactoring("test = 0");
    // null
    assertRefactoringStatus(
        refactoring.checkNewName(null),
        RefactoringStatusSeverity.ERROR,
        "Variable name must not be null.");
    // empty
    assertRefactoringStatus(
        refactoring.checkNewName(""),
        RefactoringStatusSeverity.ERROR,
        "Variable name must not be empty.");
    // same name
    assertRefactoringStatus(
        refactoring.checkNewName("test"),
        RefactoringStatusSeverity.FATAL,
        "Choose another name.");
  }

  public void test_checkNewName_LocalVariableElement_const() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  const int TEST = 0;",
        "}");
    createRenameRefactoring("TEST = 0");
    // null
    assertRefactoringStatus(
        refactoring.checkNewName(null),
        RefactoringStatusSeverity.ERROR,
        "Constant name must not be null.");
    // empty
    assertRefactoringStatus(
        refactoring.checkNewName(""),
        RefactoringStatusSeverity.ERROR,
        "Constant name must not be empty.");
    // same name
    assertRefactoringStatus(
        refactoring.checkNewName("TEST"),
        RefactoringStatusSeverity.FATAL,
        "Choose another name.");
    // with underscore
    assertRefactoringStatusOK(refactoring.checkNewName("NEW_NAME"));
  }

  public void test_checkNewName_ParameterVariableElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main2(int test) {",
        "}");
    createRenameRefactoring("test) {");
    // null
    assertRefactoringStatus(
        refactoring.checkNewName(null),
        RefactoringStatusSeverity.ERROR,
        "Parameter name must not be null.");
    // OK
    assertRefactoringStatusOK(refactoring.checkNewName("newName"));
  }

  public void test_createChange_localFunction() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int test() => 0;",
        "  print(test);",
        "  print(test());",
        "}");
    // configure refactoring
    createRenameRefactoring("test() => 0");
    assertEquals("Rename Local Function", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int newName() => 0;",
        "  print(newName);",
        "  print(newName());",
        "}");
  }

  public void test_createChange_localVariable() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int test = 0;",
        "  test = 1;",
        "  test += 2;",
        "  print(test);",
        "}");
    // configure refactoring
    createRenameRefactoring("test = 0");
    assertEquals("Rename Local Variable", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int newName = 0;",
        "  newName = 1;",
        "  newName += 2;",
        "  print(newName);",
        "}");
  }

  public void test_createChange_parameter() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "myFunction({int test}) {",
        "  test = 1;",
        "  test += 2;",
        "  print(test);",
        "}",
        "main() {",
        "  myFunction(test: 2);",
        "}");
    // configure refactoring
    createRenameRefactoring("test}) {");
    assertEquals("Rename Parameter", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "myFunction({int newName}) {",
        "  newName = 1;",
        "  newName += 2;",
        "  print(newName);",
        "}",
        "main() {",
        "  myFunction(newName: 2);",
        "}");
  }

  public void test_RenameRefactoringImpl_getName() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int test = 0;",
        "}");
    createRenameRefactoring("test = 0");
    // old name
    assertEquals("test", refactoring.getCurrentName());
    // no new name yet
    assertEquals(null, refactoring.getNewName());
    // new name
    refactoring.setNewName("newName");
    assertEquals("newName", refactoring.getNewName());
  }
}
