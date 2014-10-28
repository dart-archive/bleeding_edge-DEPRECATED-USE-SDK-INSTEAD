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
import com.google.dart.engine.services.status.RefactoringStatusSeverity;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;

import java.io.File;

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

  public void test_checkFinalConditions_hasLocalVariable_otherBlock() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  {",
        "    var test = 1;",
        "  }",
        "  {",
        "    var newName = 2;",
        "  }",
        "}");
    createRenameRefactoring("test = 1");
    // check status
    refactoring.setNewName("newName");
    assertRefactoringStatusOK(refactoring.checkFinalConditions(pm));
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

  public void test_checkFinalConditions_shadows_typeParameter() throws Exception {
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
        "Usage of type parameter 'newName' declared in 'Test.dart' will be shadowed by renamed local variable.",
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

  public void test_createChange_localFunction_sameNameDifferenceScopes() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  {",
        "    int test() => 0;",
        "    print(test);",
        "  }",
        "  {",
        "    int test() => 1;",
        "    print(test);",
        "  }",
        "  {",
        "    int test() => 2;",
        "    print(test);",
        "  }",
        "}");
    // configure refactoring
    createRenameRefactoring("test() => 1");
    assertEquals("Rename Local Function", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  {",
        "    int test() => 0;",
        "    print(test);",
        "  }",
        "  {",
        "    int newName() => 1;",
        "    print(newName);",
        "  }",
        "  {",
        "    int test() => 2;",
        "    print(test);",
        "  }",
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

  public void test_createChange_localVariable_sameNameDifferenceScopes() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  {",
        "    int test = 0;",
        "    print(test);",
        "  }",
        "  {",
        "    int test = 1;",
        "    print(test);",
        "  }",
        "  {",
        "    int test = 2;",
        "    print(test);",
        "  }",
        "}");
    // configure refactoring
    createRenameRefactoring("test = 1");
    assertEquals("Rename Local Variable", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  {",
        "    int test = 0;",
        "    print(test);",
        "  }",
        "  {",
        "    int newName = 1;",
        "    print(newName);",
        "  }",
        "  {",
        "    int test = 2;",
        "    print(test);",
        "  }",
        "}");
  }

  public void test_createChange_oneUnitInTwoContexts() throws Exception {
    String code = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int test = 0;",
        "  print(test);",
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
    createRenameRefactoring("test = 0");
    assertEquals("Rename Local Variable", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int newName = 0;",
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

  public void test_createChange_parameter_namedInOtherFile() throws Exception {
    indexTestUnit(//
        "class A {",
        "  A({test});",
        "}");
    Source source2 = addSource("/test2.dart", makeSource(//
        "import 'Test.dart';",
        "main() {",
        "  new A(test: 2);",
        "}"));
    indexUnit(source2);
    // configure refactoring
    createRenameRefactoring("test});");
    assertEquals("Rename Parameter", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertSuccessfulRename(//
        "class A {",
        "  A({newName});",
        "}");
    assertChangeResult(refactoringChange, source2, makeSource(//
        "import 'Test.dart';",
        "main() {",
        "  new A(newName: 2);",
        "}"));
  }

  public void test_createChange_sharedBetweenTwoLibraries() throws Exception {
    Source libSourceA = addSource(
        "/libA.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library lib;",
            "part 'test.dart';",
            ""));
    Source libSourceB = addSource(
        "/libB.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library lib;",
            "part 'test.dart';",
            ""));
    testCode = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "part of lib;",
        "main() {",
        "  int test = 0;",
        "  test = 1;",
        "  test += 2;",
        "  print(test);",
        "}");
    testSource = addSource("/test.dart", testCode);
    // index unit in libraries A and B
    analysisContext.computeLibraryElement(libSourceA);
    analysisContext.computeLibraryElement(libSourceB);
    CompilationUnit unitA = analysisContext.getResolvedCompilationUnit(testSource, libSourceA);
    CompilationUnit unitB = analysisContext.getResolvedCompilationUnit(testSource, libSourceB);
    index.indexUnit(analysisContext, unitA);
    index.indexUnit(analysisContext, unitB);
    // Set "testUnit" to "unitA", which was indexed before "unitB" with the same Source.
    // So, if index does not support separate information for the same Source in different
    // libraries, then information about "testSource" in "A" was removed, and this test will fail.
    testUnit = unitA;
    // configure refactoring
    createRenameRefactoring("test = 0");
    assertEquals("Rename Local Variable", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "part of lib;",
        "main() {",
        "  int newName = 0;",
        "  newName = 1;",
        "  newName += 2;",
        "  print(newName);",
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

  public void test_shouldReportUnsafeRefactoringSource() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var test = 0;",
        "}");
    createRenameRefactoring("test = 0");
    // check
    assertTrue(refactoring.shouldReportUnsafeRefactoringSource(analysisContext, testSource));
    assertFalse(refactoring.shouldReportUnsafeRefactoringSource(
        analysisContext,
        new FileBasedSource(new File("other.dart"))));
  }
}
