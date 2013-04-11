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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.status.RefactoringStatusSeverity;
import com.google.dart.engine.source.Source;

/**
 * Test for {@link RenameUnitMemberRefactoringImpl}.
 */
public class RenameUnitMemberRefactoringImplTest extends RenameRefactoringImplTest {
  public void test_checkFinalConditions_hasTopLevel_ClassElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {}",
        "class NewName {} // existing");
    createRenameRefactoring("Test {}");
    // check status
    refactoring.setNewName("NewName");
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Library already declares class with name 'NewName'.",
        findRangeIdentifier("NewName {} // existing"));
  }

  public void test_checkFinalConditions_hasTopLevel_FunctionTypeAliasElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {}",
        "typedef NewName(); // existing");
    createRenameRefactoring("Test {}");
    // check status
    refactoring.setNewName("NewName");
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Library already declares function type alias with name 'NewName'.",
        findRangeIdentifier("NewName(); // existing"));
  }

  public void test_checkFinalConditions_shadowedBy_MethodElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {}",
        "class A {",
        "  NewName() {}",
        "  main() {",
        "    new Test();",
        "  }",
        "}");
    createRenameRefactoring("Test {}");
    // check status
    refactoring.setNewName("NewName");
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Reference to renamed class will shadowed by method 'A.NewName'.",
        findRangeIdentifier("NewName() {}"));
  }

  public void test_checkFinalConditions_shadows_MethodElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {}",
        "class A {",
        "  NewName() {}",
        "}",
        "class B extends A {",
        "  main() {",
        "    NewName(); // super-ref",
        "  }",
        "}");
    createRenameRefactoring("Test {}");
    // check status
    refactoring.setNewName("NewName");
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Renamed class will shadow method 'A.NewName'.",
        findRangeIdentifier("NewName(); // super-ref"));
  }

  public void test_checkFinalConditionsOK_qualifiedSuper_MethodElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {}",
        "class A {",
        "  NewName() {}",
        "}",
        "class B extends A {",
        "  main() {",
        "    super.NewName(); // super-ref",
        "  }",
        "}");
    createRenameRefactoring("Test {}");
    // check status
    refactoring.setNewName("NewName");
    assertRefactoringStatusOK(refactoring.checkFinalConditions(pm));
  }

  public void test_checkNewName_ClassElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {}");
    createRenameRefactoring("Test {}");
    // null
    assertRefactoringStatus(
        refactoring.checkNewName(null),
        RefactoringStatusSeverity.ERROR,
        "Class name must not be null.");
    // empty
    refactoring.setNewName("");
    assertRefactoringStatus(
        refactoring.checkNewName(""),
        RefactoringStatusSeverity.ERROR,
        "Class name must not be empty.");
    // same name
    assertRefactoringStatus(
        refactoring.checkNewName("Test"),
        RefactoringStatusSeverity.FATAL,
        "Choose another name.");
  }

  public void test_checkNewName_FunctionElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {}");
    createRenameRefactoring("test() {");
    // null
    assertRefactoringStatus(
        refactoring.checkNewName(null),
        RefactoringStatusSeverity.ERROR,
        "Function name must not be null.");
    // empty
    assertRefactoringStatus(
        refactoring.checkNewName(""),
        RefactoringStatusSeverity.ERROR,
        "Function name must not be empty.");
    // same name
    assertRefactoringStatus(
        refactoring.checkNewName("test"),
        RefactoringStatusSeverity.FATAL,
        "Choose another name.");
  }

  public void test_checkNewName_TopLevelVariableElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var test;");
    createRenameRefactoring("test;");
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

  public void test_checkNewName_TopLevelVariableElement_const() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "const TEST = 0;");
    createRenameRefactoring("TEST");
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

  public void test_checkNewName_TypeAliasElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef Test();");
    createRenameRefactoring("Test();");
    // null
    assertRefactoringStatus(
        refactoring.checkNewName(null),
        RefactoringStatusSeverity.ERROR,
        "Function type alias name must not be null.");
    // empty
    assertRefactoringStatus(
        refactoring.checkNewName(""),
        RefactoringStatusSeverity.ERROR,
        "Function type alias name must not be empty.");
    // same name
    assertRefactoringStatus(
        refactoring.checkNewName("Test"),
        RefactoringStatusSeverity.FATAL,
        "Choose another name.");
  }

  public void test_createChange_ClassElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {",
        "  Test() {}",
        "  Test.named() {}",
        "}",
        "class Other {",
        "  factory Other.a() = Test;",
        "  factory Other.b() = Test.named;",
        "}",
        "main() {",
        "  Test t1 = new Test();",
        "  Test t2 = new Test.named();",
        "}");
    // configure refactoring
    createRenameRefactoring("Test {");
    assertEquals("Rename Class", refactoring.getRefactoringName());
    refactoring.setNewName("NewName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class NewName {",
        "  NewName() {}",
        "  NewName.named() {}",
        "}",
        "class Other {",
        "  factory Other.a() = NewName;",
        "  factory Other.b() = NewName.named;",
        "}",
        "main() {",
        "  NewName t1 = new NewName();",
        "  NewName t2 = new NewName.named();",
        "}");
  }

  // XXX
  public void test_createChange_ClassElement_multipleUnits() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library libA;",
        "class Test {",
        "  Test() {}",
        "  Test.named() {}",
        "}",
        "class Other {",
        "  factory Other.a() = Test;",
        "  factory Other.b() = Test.named;",
        "}",
        "main() {",
        "  Test t1 = new Test();",
        "  Test t2 = new Test.named();",
        "}");
    CompilationUnit unitB = indexUnit(
        "/B.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "import 'Test.dart';",
            "main() {",
            "  Test t = new Test();",
            "}"));
    Source sourceB = unitB.getElement().getSource();
    // configure refactoring
    createRenameRefactoring("Test {");
    assertEquals("Rename Class", refactoring.getRefactoringName());
    refactoring.setNewName("NewName");
    // validate change
    assertRefactoringStatusOK();
    Change refactoringChange = refactoring.createChange(pm);
    assertChangeResult(
        refactoringChange,
        testSource,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library libA;",
            "class NewName {",
            "  NewName() {}",
            "  NewName.named() {}",
            "}",
            "class Other {",
            "  factory Other.a() = NewName;",
            "  factory Other.b() = NewName.named;",
            "}",
            "main() {",
            "  NewName t1 = new NewName();",
            "  NewName t2 = new NewName.named();",
            "}"));
    assertChangeResult(
        refactoringChange,
        sourceB,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "import 'Test.dart';",
            "main() {",
            "  NewName t = new NewName();",
            "}"));
  }

  public void test_createChange_ClassElement_typedef() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {}",
        "typedef Test = Object with A;",
        "main() {",
        "  Test t = new Test();",
        "}");
    // configure refactoring
    createRenameRefactoring("Test =");
    assertEquals("Rename Class", refactoring.getRefactoringName());
    refactoring.setNewName("NewName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {}",
        "typedef NewName = Object with A;",
        "main() {",
        "  NewName t = new NewName();",
        "}");
  }

  public void test_createChange_FunctionElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {}",
        "main() {",
        "  print(test);",
        "  print(test());",
        "}");
    // configure refactoring
    createRenameRefactoring("test() {}");
    assertEquals("Rename Top-Level Function", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "newName() {}",
        "main() {",
        "  print(newName);",
        "  print(newName());",
        "}");
  }

  // TODO(scheglov) we actually want to rename ImportElement here, not PrefixElement
  public void test_createChange_ImportElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:math' as test;",
        "main() {",
        "  print(test.PI);",
        "}");
    // configure refactoring
    createRenameRefactoring("test.PI");
//    assertEquals("Rename Import Prefix", refactoring.getRefactoringName());
//    refactoring.setNewName("newName");
//    // validate change
//    assertSuccessfulRename(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "import 'dart:math' as newName;",
//        "main() {",
//        "  print(newName.PI);",
//        "}");
  }

  public void test_createChange_TopLevelVariableElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int test = 42;",
        "main() {",
        "  print(test);",
        "}");
    // configure refactoring
    createRenameRefactoring("test = 42;");
    assertEquals("Rename Top-Level Variable", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int newName = 42;",
        "main() {",
        "  print(newName);",
        "}");
  }

  public void test_createChange_TypeAliasElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef Test();",
        "main2(Test t) {",
        "}");
    // configure refactoring
    createRenameRefactoring("Test();");
    assertEquals("Rename Function Type Alias", refactoring.getRefactoringName());
    refactoring.setNewName("NewName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef NewName();",
        "main2(NewName t) {",
        "}");
  }
}
