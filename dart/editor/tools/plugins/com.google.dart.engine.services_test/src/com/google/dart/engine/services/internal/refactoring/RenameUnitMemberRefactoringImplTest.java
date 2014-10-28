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
import com.google.dart.engine.services.status.RefactoringStatusSeverity;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;

import java.io.File;

/**
 * Test for {@link RenameUnitMemberRefactoringImpl}.
 */
public class RenameUnitMemberRefactoringImplTest extends RenameRefactoringImplTest {
  public void fail_createChange_sharedBetweenTwoLibraries() throws Exception {
    // TODO(scheglov) This test fails with new split index.
    // Problem is that we rename for element "A" with location "libA.dart ; test.dart; A".
    // So, we search using this location. But in "libB.dart" class "A" from "test.dart" has a
    // different location.
    Source libSourceA = addSource(
        "/libA.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library lib;",
            "part 'test.dart';",
            "A f() {}"));
    Source libSourceB = addSource(
        "/libB.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library lib;",
            "part 'test.dart';",
            "A f() {}"));
    testCode = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "part of lib;",
        "class A {}");
    testSource = addSource("/test.dart", testCode);
    // index unit in libraries A and B
    analysisContext.computeLibraryElement(libSourceA);
    analysisContext.computeLibraryElement(libSourceB);
    CompilationUnit libUnitA = analysisContext.getResolvedCompilationUnit(libSourceA, libSourceA);
    CompilationUnit libUnitB = analysisContext.getResolvedCompilationUnit(libSourceB, libSourceB);
    CompilationUnit unitA = analysisContext.getResolvedCompilationUnit(testSource, libSourceA);
    CompilationUnit unitB = analysisContext.getResolvedCompilationUnit(testSource, libSourceB);
    index.indexUnit(analysisContext, libUnitA);
    index.indexUnit(analysisContext, libUnitB);
    index.indexUnit(analysisContext, unitA);
    index.indexUnit(analysisContext, unitB);
    // Set "testUnit" to "unitA", which was indexed before "unitB" with the same Source.
    // So, if index does not support separate information for the same Source in different
    // libraries, then information about "testSource" in "A" was removed, and this test will fail.
    testUnit = unitA;
    // configure refactoring
    createRenameRefactoring("A {}");
    assertEquals("Rename Class", refactoring.getRefactoringName());
    refactoring.setNewName("NewName");
    // perform refactoring
    assertRefactoringStatusOK();
    refactoringChange = refactoring.createChange(pm);
    // validate change
    assertChangeResult(
        refactoringChange,
        testSource,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "part of lib;",
            "class NewName {}"));
    assertChangeResult(
        refactoringChange,
        libSourceA,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library lib;",
            "part 'test.dart';",
            "NewName f() {}"));
    assertChangeResult(
        refactoringChange,
        libSourceB,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library lib;",
            "part 'test.dart';",
            "NewName f() {}"));
  }

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

  public void test_checkFinalConditions_shadowsInSubClass_importedLib() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library test;",
        "class Test {}");
    String libCode = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library my.lib;",
        "import 'Test.dart';",
        "class A {",
        "  NewName() {}",
        "}",
        "class B extends A {",
        "  main() {",
        "    NewName(); // super-ref",
        "  }",
        "}");
    indexUnit("/lib.dart", libCode);
    createRenameRefactoring("Test {}");
    // check status
    refactoring.setNewName("NewName");
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Renamed class will shadow method 'A.NewName'.",
        findRangeIdentifier(libCode, "NewName(); // super-ref"));
  }

  public void test_checkFinalConditions_shadowsInSubClass_importedLib_hideCombinator()
      throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {}");
    indexUnit(
        "/lib.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "import 'Test.dart' hide Test;",
            "class A {",
            "  NewName() {}",
            "}",
            "class B extends A {",
            "  main() {",
            "    NewName(); // super-ref",
            "  }",
            "}"));
    createRenameRefactoring("Test {}");
    // check status
    refactoring.setNewName("NewName");
    assertRefactoringStatusOK(refactoring.checkFinalConditions(pm));
  }

  public void test_checkFinalConditions_shadowsInSubClass_MethodElement() throws Exception {
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

  public void test_checkFinalConditions_shadowsInSubClass_notImportedLib() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {}");
    indexUnit(
        "/lib.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "class A {",
            "  NewName() {}",
            "}",
            "class B extends A {",
            "  main() {",
            "    NewName(); // super-ref",
            "  }",
            "}"));
    createRenameRefactoring("Test {}");
    // check status
    refactoring.setNewName("NewName");
    assertRefactoringStatusOK(refactoring.checkFinalConditions(pm));
  }

  public void test_checkFinalConditions_shadowsInSubClass_notSubClass() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {}",
        "class A {",
        "  NewName() {}",
        "}",
        "class B {",
        "  main(A a) {",
        "    a.NewName();",
        "  }",
        "}");
    createRenameRefactoring("Test {}");
    // check status
    refactoring.setNewName("NewName");
    assertRefactoringStatusOK(refactoring.checkFinalConditions(pm));
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
        "class Test implements Other {",
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
    createRenameRefactoring("Test implements");
    assertEquals("Rename Class", refactoring.getRefactoringName());
    refactoring.setNewName("NewName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class NewName implements Other {",
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

  public void test_createChange_ClassElement_multipleUnits() throws Exception {
    testCode = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library libA;",
        "class Test implements Other {",
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
    Source libA = addSource(testCode);
    Source sourceB = addSource(
        "/B.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "import 'test.dart';",
            "main() {",
            "  Test t = new Test();",
            "}"));
    indexTestUnit(libA);
    indexUnit(sourceB);
    // configure refactoring
    createRenameRefactoring("Test implements");
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
            "class NewName implements Other {",
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
            "import 'test.dart';",
            "main() {",
            "  NewName t = new NewName();",
            "}"));
  }

  public void test_createChange_ClassElement_parameterTypeNested() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {",
        "}",
        "method(f(Test p)) {",
        "}");
    // configure refactoring
    createRenameRefactoring("Test {");
    assertEquals("Rename Class", refactoring.getRefactoringName());
    refactoring.setNewName("NewName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class NewName {",
        "}",
        "method(f(NewName p)) {",
        "}");
  }

  public void test_createChange_ClassElement_typeAlias() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {}",
        "class Test = Object with A;",
        "f(Test t) {",
        "}");
    // configure refactoring
    createRenameRefactoring("Test =");
    assertEquals("Rename Class", refactoring.getRefactoringName());
    refactoring.setNewName("NewName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {}",
        "class NewName = Object with A;",
        "f(NewName t) {",
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

  public void test_createChange_oneUnitInTwoContexts() throws Exception {
    String code = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  main();",
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
    createRenameRefactoring("main() {");
    assertEquals("Rename Top-Level Function", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "newName() {",
        "  newName();",
        "}");
  }

  public void test_createChange_PropertyAccessorElement_getter_declaration() throws Exception {
    check_createChange_PropertyAccessorElement("test {}");
  }

  public void test_createChange_PropertyAccessorElement_getter_usage() throws Exception {
    check_createChange_PropertyAccessorElement("test)");
  }

  public void test_createChange_PropertyAccessorElement_mix() throws Exception {
    check_createChange_PropertyAccessorElement("test += 2");
  }

  public void test_createChange_PropertyAccessorElement_setter_declaration() throws Exception {
    check_createChange_PropertyAccessorElement("test(x) {}");
  }

  public void test_createChange_PropertyAccessorElement_setter_usage() throws Exception {
    check_createChange_PropertyAccessorElement("test = 1");
  }

  public void test_createChange_TopLevelVariableElement_field() throws Exception {
    check_createChange_TopLevelVariableElement("test = 42;");
  }

  public void test_createChange_TopLevelVariableElement_getter() throws Exception {
    check_createChange_TopLevelVariableElement("test);");
  }

  public void test_createChange_TopLevelVariableElement_mix() throws Exception {
    check_createChange_TopLevelVariableElement("test += 2;");
  }

  public void test_createChange_TopLevelVariableElement_setter() throws Exception {
    check_createChange_TopLevelVariableElement("test = 1;");
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

  public void test_shouldReportUnsafeRefactoringSource() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class MyPublic {}",
        "class _MyPrivate {}");
    Source externalSource = new FileBasedSource(new File("other.dart"));
    // check public
    createRenameRefactoring("MyPublic {}");
    assertTrue(refactoring.shouldReportUnsafeRefactoringSource(analysisContext, testSource));
    assertTrue(refactoring.shouldReportUnsafeRefactoringSource(analysisContext, externalSource));
    assertFalse(refactoring.shouldReportUnsafeRefactoringSource(null, testSource));
    // check private
    createRenameRefactoring("_MyPrivate {}");
    assertTrue(refactoring.shouldReportUnsafeRefactoringSource(analysisContext, testSource));
    assertFalse(refactoring.shouldReportUnsafeRefactoringSource(analysisContext, externalSource));
  }

  public void xtest_createChange_oneLibInTwoContexts() throws Exception {
    String libCode = "test() {}";
    String code = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'Lib.dart';",
        "main() {",
        "  test();",
        "}");
    // index unit in separate context
    Source source2;
    {
      AnalysisContextHelper helper = new AnalysisContextHelper();
      helper.addSource("/Lib.dart", libCode);
      source2 = helper.addSource("/Test2.dart", code);
      CompilationUnit unit = helper.resolveDefiningUnit(source2);
      index.indexUnit(helper.context, unit);
    }
    // index unit Lib.dart in "test"
    addSource("/Lib.dart", libCode);
    indexTestUnit(code);
    // configure refactoring
    createRenameRefactoring("test()");
    assertEquals("Rename Top-Level Function", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'Lib.dart';",
        "main() {",
        "  newName();",
        "}");
    assertChangeResult(
        refactoringChange,
        source2,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "import 'Lib.dart';",
            "main() {",
            "  newName();",
            "}"));
  }

  private void check_createChange_PropertyAccessorElement(String search) throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "get test {}",
        "set test(x) {}",
        "main() {",
        "  print(test);",
        "  test = 1;",
        "  test += 2;",
        "}");
    // configure refactoring
    createRenameRefactoring(search);
    assertEquals("Rename Top-Level Variable", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "get newName {}",
        "set newName(x) {}",
        "main() {",
        "  print(newName);",
        "  newName = 1;",
        "  newName += 2;",
        "}");
  }

  private void check_createChange_TopLevelVariableElement(String search) throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int test = 42;",
        "main() {",
        "  print(test);",
        "  test = 1;",
        "  test += 2;",
        "}");
    // configure refactoring
    createRenameRefactoring(search);
    assertEquals("Rename Top-Level Variable", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int newName = 42;",
        "main() {",
        "  print(newName);",
        "  newName = 1;",
        "  newName += 2;",
        "}");
  }
}
