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
import com.google.dart.engine.context.AnalysisContextHelper;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.status.RefactoringStatusSeverity;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;

import java.io.File;

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
    refactoring.checkInitialConditions(pm);
    refactoring.setNewName("newName");
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Class 'A' already declares method with name 'newName'.",
        findRangeIdentifier("newName() {} // existing"));
  }

  public void test_checkFinalConditions_ignoredSdkElements() throws Exception {
    verifyNoTestUnitErrors = false;
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A extends List<int> {",
        "  add(x) {}",
        "}");
    createRenameRefactoring("add(x) {}");
    // check status
    refactoring.checkInitialConditions(pm);
    refactoring.setNewName("newName");
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.WARNING,
        "Elements and references in SDK and external packages will not be renamed.");
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
        "class C extends A {",
        "  main() {",
        "    print(newName);",
        "  }",
        "}");
    createRenameRefactoring("test() {}");
    // check status
    refactoring.checkInitialConditions(pm);
    refactoring.setNewName("newName");
    assertRefactoringStatusOK(refactoring.checkFinalConditions(pm));
  }

  public void test_checkFinalConditions_shadowed_byLocal_inSameClass() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test() {}",
        "  main() {",
        "    var newName;",
        "    test(); // marker",
        "  }",
        "}");
    createRenameRefactoring("test() {}");
    // check status
    refactoring.checkInitialConditions(pm);
    refactoring.setNewName("newName");
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Usage of renamed method will be shadowed by local variable 'newName'.",
        findRangeIdentifier("test(); // marker"));
  }

  public void test_checkFinalConditions_shadowed_byLocal_inSubClass() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test() {}",
        "}",
        "class B extends A {",
        "  main() {",
        "    var newName;",
        "    test(); // marker",
        "  }",
        "}",
        "");
    createRenameRefactoring("test() {}");
    // check status
    refactoring.checkInitialConditions(pm);
    refactoring.setNewName("newName");
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Usage of renamed method will be shadowed by local variable 'newName'.",
        findRangeIdentifier("test(); // marker"));
  }

  public void test_checkFinalConditions_shadowed_byLocal_OK_qualifiedReference() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test() {}",
        "  main() {",
        "    var newName;",
        "  }",
        "}");
    createRenameRefactoring("test() {}");
    // check status
    refactoring.checkInitialConditions(pm);
    refactoring.setNewName("newName");
    assertRefactoringStatusOK(refactoring.checkFinalConditions(pm));
  }

  public void test_checkFinalConditions_shadowed_byLocal_OK_renamedNotUsed() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test() {}",
        "  main() {",
        "    var newName;",
        "    this.test(); // marker",
        "  }",
        "}");
    createRenameRefactoring("test() {}");
    // check status
    refactoring.checkInitialConditions(pm);
    refactoring.setNewName("newName");
    assertRefactoringStatusOK(refactoring.checkFinalConditions(pm));
  }

  public void test_checkFinalConditions_shadowed_byParameter_inSameClass() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test() {}",
        "  A(newName) {",
        "    test(); // marker",
        "  }",
        "}");
    createRenameRefactoring("test() {}");
    // check status
    refactoring.checkInitialConditions(pm);
    refactoring.setNewName("newName");
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Usage of renamed method will be shadowed by parameter 'newName'.",
        findRangeIdentifier("test(); // marker"));
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
    refactoring.checkInitialConditions(pm);
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
    refactoring.checkInitialConditions(pm);
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
    refactoring.checkInitialConditions(pm);
    refactoring.setNewName("newName");
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Renamed method will shadow method 'A.newName'.",
        findRangeIdentifier("newName() {} // marker"));
  }

  public void test_checkInitialConditions_operator() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  operator -(other) => this;",
        "}");
    createRenameRefactoring("-(other)");
    // check status
    assertRefactoringStatus(
        refactoring.checkInitialConditions(pm),
        RefactoringStatusSeverity.FATAL,
        "Cannot rename operator.");
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

  public void test_createChange_FieldElement_constructorFieldInitializer() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  final test;",
        "  A() : test = 5;",
        "}");
    // configure refactoring
    createRenameRefactoring("test;");
    assertEquals("Rename Field", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  final newName;",
        "  A() : newName = 5;",
        "}");
  }

  public void test_createChange_FieldElement_fieldFormalParameter() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int test;",
        "  A(this.test) {}",
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
        "  A(this.newName) {}",
        "}");
  }

  public void test_createChange_FieldElement_invocation() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef F(a);",
        "class A {",
        "  F test;",
        "  main() {",
        "    test(1);",
        "  }",
        "}",
        "main() {",
        "  A a = new A();",
        "  a.test(2);",
        "}");
    // configure refactoring
    createRenameRefactoring("test;");
    assertEquals("Rename Field", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef F(a);",
        "class A {",
        "  F newName;",
        "  main() {",
        "    newName(1);",
        "  }",
        "}",
        "main() {",
        "  A a = new A();",
        "  a.newName(2);",
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
        "class D implements A {",
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
        "class D implements A {",
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
    assertFalse(refactoring.requiresPreview());
  }

  public void test_createChange_MethodElement_potential() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test() {}",
        "}",
        "main(var a) {",
        "  a.test();",
        "  new A().test();",
        "}");
    // configure refactoring
    createRenameRefactoring("test() {}");
    assertEquals("Rename Method", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  newName() {}",
        "}",
        "main(var a) {",
        "  a.newName();",
        "  new A().newName();",
        "}");
    assertTrue(refactoring.requiresPreview());
  }

  public void test_createChange_MethodElement_potential_private_otherLibrary() throws Exception {
    Source lib = addSource(
        "/lib.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library lib;",
            "f(p) {",
            "  p._test();",
            "}"));
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'lib.dart';",
        "class A {",
        "  _test() {}",
        "}",
        "f(A a) {",
        "  a._test();",
        "}");
    indexUnit(lib);
    // configure refactoring
    createRenameRefactoring("_test() {}");
    assertEquals("Rename Method", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertRefactoringStatusOK();
    Change refactoringChange = refactoring.createChange(pm);
    assertChangeResult(
        refactoringChange,
        testSource,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "import 'lib.dart';",
            "class A {",
            "  newName() {}",
            "}",
            "f(A a) {",
            "  a.newName();",
            "}"));
    SourceChange noChangeExpected = getSourceChange(refactoringChange, lib);
    assertNull(noChangeExpected);
  }

  public void test_createChange_multipleUnits() throws Exception {
    testCode = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library libA;",
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
    Source libA = addSource(testCode);
    Source libB = addSource(
        "/B.dart",
        makeSource(
            "import 'test.dart';",
            "main() {",
            "  A a = new A();",
            "  print(a.test);",
            "  a.test = 1;",
            "  a.test += 2;",
            "}"));
    indexTestUnit(libA);
    indexUnit(libB);
    Source sourceB = libB;
    // configure refactoring
    createRenameRefactoring("test;");
    assertEquals("Rename Field", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertRefactoringStatusOK();
    Change refactoringChange = refactoring.createChange(pm);
    assertChangeResult(
        refactoringChange,
        testSource,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library libA;",
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
            "}"));
    assertChangeResult(
        refactoringChange,
        sourceB,
        makeSource(
            "import 'test.dart';",
            "main() {",
            "  A a = new A();",
            "  print(a.newName);",
            "  a.newName = 1;",
            "  a.newName += 2;",
            "}"));
  }

  public void test_createChange_oneUnitInTwoContexts() throws Exception {
    String code = makeSource(
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
        "main(var e) {",
        "  A a = new A();",
        "  B b = new B();",
        "  C c = new C();",
        "  D d = new D();",
        "  a.test();",
        "  b.test();",
        "  c.test();",
        "  d.test();",
        "  e.test();",
        "}");
    // index unit in separate context
    {
      AnalysisContextHelper helper = new AnalysisContextHelper();
      Source source = helper.addSource("/Test.dart", code);
      CompilationUnit unit = helper.resolveDefiningUnit(source);
      index.indexUnit(helper.context, unit);
    }
    // index same unit as "test"
    indexTestUnit(code);
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
        "main(var e) {",
        "  A a = new A();",
        "  B b = new B();",
        "  C c = new C();",
        "  D d = new D();",
        "  a.newName();",
        "  b.newName();",
        "  c.newName();",
        "  d.newName();",
        "  e.newName();",
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
    assertEquals("Rename Field", refactoring.getRefactoringName());
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
    assertEquals("Rename Field", refactoring.getRefactoringName());
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

  public void test_createChange_TypeParameterElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A<Test> {",
        "  Test f;",
        "  Test m(Test p) => null;",
        "}");
    // configure refactoring
    createRenameRefactoring("Test>");
    assertEquals("Rename Type Parameter", refactoring.getRefactoringName());
    refactoring.setNewName("NewName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A<NewName> {",
        "  NewName f;",
        "  NewName m(NewName p) => null;",
        "}");
  }

  public void test_shouldReportUnsafeRefactoringSource() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  myPublic() {}",
        "  _myPrivate() {}",
        "}");
    Source externalSource = new FileBasedSource(new File("other.dart"));
    // check public
    createRenameRefactoring("myPublic() {}");
    assertTrue(refactoring.shouldReportUnsafeRefactoringSource(analysisContext, testSource));
    assertTrue(refactoring.shouldReportUnsafeRefactoringSource(analysisContext, externalSource));
    assertFalse(refactoring.shouldReportUnsafeRefactoringSource(null, testSource));
    // check private
    createRenameRefactoring("_myPrivate() {}");
    assertTrue(refactoring.shouldReportUnsafeRefactoringSource(analysisContext, testSource));
    assertFalse(refactoring.shouldReportUnsafeRefactoringSource(analysisContext, externalSource));
  }
}
