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
 * Test for {@link RenameClassMemberRefactoringImpl}.
 */
public class RenameClassMemberRefactoringImplTest extends RenameRefactoringImplTest {

  public void test_checkFinalConditions_hasMember_MethodElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test() {}",
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

  public void test_checkFinalConditions_OK_noShadow() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int newName; // marker",
        "}",
        "class B {",
        "  test() {}",
        "}",
        "class C extends B {",
        "  main() {",
        "    print(newName);",
        "  }",
        "}");
    createRenameRefactoring("test() {}");
    // check status
    refactoring.setNewName("newName");
    assertRefactoringStatusOK(refactoring.checkFinalConditions(pm));
  }

  public void test_checkFinalConditions_shadowed_inSubClass() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test() {}",
        "}",
        "class B extends A {",
        "  newName() {} // marker",
        "  main() {",
        "    test();",
        "  }",
        "}");
    createRenameRefactoring("test() {}");
    // check status
    refactoring.setNewName("newName");
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Renamed method will be shadowed by method 'B.newName'.",
        findRangeIdentifier("newName() {} // marker"));
  }

  public void test_checkFinalConditions_shadowsSuper_inSubClass_FieldElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int newName; // marker",
        "}",
        "class B extends A {",
        "  test() {}",
        "}",
        "class C extends B {",
        "  main() {",
        "    print(newName);",
        "  }",
        "}");
    createRenameRefactoring("test() {}");
    // check status
    refactoring.setNewName("newName");
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Renamed method will shadow field 'A.newName'.",
        findRangeIdentifier("newName; // marker"));
  }

  public void test_checkFinalConditions_shadowsSuper_MethodElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  newName() {} // marker",
        "}",
        "class B extends A {",
        "  test() {}",
        "  main() {",
        "    newName();",
        "  }",
        "}");
    createRenameRefactoring("test() {}");
    // check status
    refactoring.setNewName("newName");
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Renamed method will shadow method 'A.newName'.",
        findRangeIdentifier("newName() {} // marker"));
  }

  public void test_checkNewName_FieldElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int test;",
        "}");
    createRenameRefactoring("test;");
    // null
    assertRefactoringStatus(
        refactoring.checkNewName(null),
        RefactoringStatusSeverity.ERROR,
        "Field name must not be null.");
    // empty
    assertRefactoringStatus(
        refactoring.checkNewName(""),
        RefactoringStatusSeverity.ERROR,
        "Field name must not be empty.");
    // same name
    assertRefactoringStatus(
        refactoring.checkNewName("test"),
        RefactoringStatusSeverity.FATAL,
        "Choose another name.");
  }

  public void test_checkNewName_MethodElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test() {}",
        "}");
    createRenameRefactoring("test() {}");
    // null
    assertRefactoringStatus(
        refactoring.checkNewName(null),
        RefactoringStatusSeverity.ERROR,
        "Method name must not be null.");
    // empty
    assertRefactoringStatus(
        refactoring.checkNewName(""),
        RefactoringStatusSeverity.ERROR,
        "Method name must not be empty.");
    // same name
    assertRefactoringStatus(
        refactoring.checkNewName("test"),
        RefactoringStatusSeverity.FATAL,
        "Choose another name.");
  }

//  public void test_checkFinalConditions_hasTopLevel_FunctionTypeAliasElement() throws Exception {
//    indexTestUnit(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "class Test {}",
//        "typedef NewName(); // existing");
//    createRenameRefactoring("Test {}");
//    // check status
//    refactoring.setNewName("NewName");
//    assertRefactoringStatus(
//        refactoring.checkFinalConditions(pm),
//        RefactoringStatusSeverity.ERROR,
//        "Library already declares function type alias with name 'NewName'.",
//        findRangeIdentifier("NewName(); // existing"));
//  }
//
//  public void test_checkFinalConditions_shadowedBy_MethodElement() throws Exception {
//    indexTestUnit(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "class Test {}",
//        "class A {",
//        "  NewName() {}",
//        "  main() {",
//        "    new Test();",
//        "  }",
//        "}");
//    createRenameRefactoring("Test {}");
//    // check status
//    refactoring.setNewName("NewName");
//    assertRefactoringStatus(
//        refactoring.checkFinalConditions(pm),
//        RefactoringStatusSeverity.ERROR,
//        "Reference to renamed class will shadowed by method 'A.NewName'.",
//        findRangeIdentifier("NewName() {}"));
//  }
//
//  public void test_checkFinalConditions_shadows_MethodElement() throws Exception {
//    indexTestUnit(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "class Test {}",
//        "class A {",
//        "  NewName() {}",
//        "}",
//        "class B extends A {",
//        "  main() {",
//        "    NewName(); // super-ref",
//        "  }",
//        "}",
//        "",
//        "",
//        "");
//    createRenameRefactoring("Test {}");
//    // check status
//    refactoring.setNewName("NewName");
//    assertRefactoringStatus(
//        refactoring.checkFinalConditions(pm),
//        RefactoringStatusSeverity.ERROR,
//        "Renamed class will shadow method 'A.NewName'.",
//        findRangeIdentifier("NewName(); // super-ref"));
//  }
//
//  public void test_checkInitialConditions_ClassElement() throws Exception {
//    indexTestUnit(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "class Test {}");
//    createRenameRefactoring("Test {}");
//    // null
//    refactoring.setNewName(null);
//    assertRefactoringStatus(
//        refactoring.checkInitialConditions(pm),
//        RefactoringStatusSeverity.ERROR,
//        "Class name must not be null");
//    // empty
//    refactoring.setNewName("");
//    assertRefactoringStatus(
//        refactoring.checkInitialConditions(pm),
//        RefactoringStatusSeverity.ERROR,
//        "Class name must not be empty");
//    // same name
//    refactoring.setNewName("Test");
//    assertRefactoringStatus(
//        refactoring.checkInitialConditions(pm),
//        RefactoringStatusSeverity.FATAL,
//        "Choose another name.");
//  }
//
//  public void test_checkInitialConditions_FunctionElement() throws Exception {
//    indexTestUnit(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "test() {}");
//    createRenameRefactoring("test() {");
//    // null
//    refactoring.setNewName(null);
//    assertRefactoringStatus(
//        refactoring.checkInitialConditions(pm),
//        RefactoringStatusSeverity.ERROR,
//        "Function name must not be null");
//    // empty
//    refactoring.setNewName("");
//    assertRefactoringStatus(
//        refactoring.checkInitialConditions(pm),
//        RefactoringStatusSeverity.ERROR,
//        "Function name must not be empty");
//    // same name
//    refactoring.setNewName("test");
//    assertRefactoringStatus(
//        refactoring.checkInitialConditions(pm),
//        RefactoringStatusSeverity.FATAL,
//        "Choose another name.");
//  }

  public void test_createChange_FieldElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int test;",
        "  main() {",
        "    print(test);",
        "    test = 1;",
        "    test += 2;",
        "  }",
        "}",
        "main() {",
        "  A a = new A();",
        "  print(a.test);",
        "  a.test = 1;",
        "  a.test += 2;",
        "}");
    // configure refactoring
    createRenameRefactoring("test;");
    assertEquals("Rename Field", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int newName;",
        "  main() {",
        "    print(newName);",
        "    newName = 1;",
        "    newName += 2;",
        "  }",
        "}",
        "main() {",
        "  A a = new A();",
        "  print(a.newName);",
        "  a.newName = 1;",
        "  a.newName += 2;",
        "}");
  }

  public void test_createChange_MethodElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test() {}",
        "}",
        "class B extends A {",
        "  test() {} // marker",
        "}",
        "class C extends B {",
        "  test() {}",
        "}",
        "class D implements B {",
        "  test() {}",
        "}",
        "main() {",
        "  A a = new A();",
        "  B b = new B();",
        "  C c = new C();",
        "  D d = new D();",
        "  a.test();",
        "  b.test();",
        "  c.test();",
        "  d.test();",
        "}");
    // configure refactoring
    createRenameRefactoring("test() {} // marker");
    assertEquals("Rename Method", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  newName() {}",
        "}",
        "class B extends A {",
        "  newName() {} // marker",
        "}",
        "class C extends B {",
        "  newName() {}",
        "}",
        "class D implements B {",
        "  newName() {}",
        "}",
        "main() {",
        "  A a = new A();",
        "  B b = new B();",
        "  C c = new C();",
        "  D d = new D();",
        "  a.newName();",
        "  b.newName();",
        "  c.newName();",
        "  d.newName();",
        "}");
  }

  public void test_createChange_PropertyAccessorElement_getter() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  get test {} // marker",
        "  set test(x) {}",
        "  main() {",
        "    print(test);",
        "    test = 1;",
        "  }",
        "}",
        "class B extends A {",
        "  get test {}",
        "  set test(x) {}",
        "}",
        "main() {",
        "  A a = new A();",
        "  print(a.test);",
        "  a.test = 2;",
        "  ",
        "  B b = new B();",
        "  print(b.test);",
        "  b.test = 2;",
        "}");
    // configure refactoring
    createRenameRefactoring("test {} // marker");
    assertEquals("Rename Method", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  get newName {} // marker",
        "  set newName(x) {}",
        "  main() {",
        "    print(newName);",
        "    newName = 1;",
        "  }",
        "}",
        "class B extends A {",
        "  get newName {}",
        "  set newName(x) {}",
        "}",
        "main() {",
        "  A a = new A();",
        "  print(a.newName);",
        "  a.newName = 2;",
        "  ",
        "  B b = new B();",
        "  print(b.newName);",
        "  b.newName = 2;",
        "}");
  }

  public void test_createChange_PropertyAccessorElement_setter() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  get test {}",
        "  set test(x) {}",
        "  main() {",
        "    print(test);",
        "    test = 1;",
        "  }",
        "}",
        "main() {",
        "  A a = new A();",
        "  print(a.test);",
        "  a.test = 2;",
        "}");
    // configure refactoring
    createRenameRefactoring("test(x) {}");
    assertEquals("Rename Method", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  get newName {}",
        "  set newName(x) {}",
        "  main() {",
        "    print(newName);",
        "    newName = 1;",
        "  }",
        "}",
        "main() {",
        "  A a = new A();",
        "  print(a.newName);",
        "  a.newName = 2;",
        "}");
  }

  public void test_createChange_TypeVariableElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A<Test> {",
        "  Test f;",
        "  Test m(Test p) => null;",
        "}");
    // configure refactoring
    createRenameRefactoring("Test>");
    assertEquals("Rename Type Variable", refactoring.getRefactoringName());
    refactoring.setNewName("NewName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A<NewName> {",
        "  NewName f;",
        "  NewName m(NewName p) => null;",
        "}");
  }
}
