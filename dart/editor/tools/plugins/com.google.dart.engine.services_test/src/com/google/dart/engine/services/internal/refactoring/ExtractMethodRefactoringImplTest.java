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

import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.refactoring.ExtractMethodRefactoring;
import com.google.dart.engine.services.refactoring.Parameter;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.services.status.RefactoringStatusSeverity;

import org.apache.commons.lang3.ArrayUtils;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for {@link ExtractMethodRefactoringImpl}.
 */
public class ExtractMethodRefactoringImplTest extends RefactoringImplTest {
  private ExtractMethodRefactoringImpl refactoring;
  private int selectionStart = -1;
  private int selectionEnd = -1;
  private String methodName = "res";
  private boolean replaceAllOccurences = true;
  private boolean keepGetterFlag = false;
  private RefactoringStatus refactoringStatus;

  public void test_bad_assignmentLeftHandSide() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int aaa;",
        "// start",
        "  aaa ",
        "// end",
        "   = 0;",
        "}",
        "");
    setSelectionString("aaa ");
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Cannot extract the left-hand side of an assignment.");
  }

  public void test_bad_comment_selectionEndsInside() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  print(0);",
        "/*",
        "// end",
        "*/",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Selection ends inside a comment.");
  }

  public void test_bad_comment_selectionStartsInside() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "/*",
        "// start",
        "*/",
        "  print(0);",
        "// end",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Selection begins inside a comment.");
  }

  public void test_bad_conflict_method_alreadyDeclaresMethod() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  void res() {}",
        "  main() {",
        "// start",
        "    print(0);",
        "// end",
        "  }",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.ERROR,
        "Class 'A' already declares method with name 'res'.",
        findRangeIdentifier("res() {}"));
  }

  public void test_bad_conflict_method_shadowsSuperDeclaration() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  void res() {} // marker",
        "}",
        "class B extends A {",
        "  main() {",
        "    res();",
        "// start",
        "    print(0);",
        "// end",
        "  }",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.ERROR,
        "Created method will shadow method 'A.res'.",
        findRangeIdentifier("res() {} // marker"));
  }

  public void test_bad_conflict_topLevel_alreadyDeclaresFunction() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "void res() {}",
        "main() {",
        "// start",
        "  print(0);",
        "// end",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.ERROR,
        "Library already declares function with name 'res'.",
        findRangeIdentifier("res() {}"));
  }

  // TODO(scheglov) waiting for "library namespace" in Engine
//  public void test_bad_conflict_method_willHideTopLevel() throws Exception {
//    indexTestUnit(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "void res() {} // marker",
//        "class B {",
//        "  main() {",
//        "// start",
//        "    print(0);",
//        "// end",
//        "  }",
//        "  foo() {",
//        "    res();",
//        "  }",
//        "}",
//        "");
//    setSelectionFromStartEndComments();
//    createRefactoring();
//    // check conditions
//    assertRefactoringStatus(
//        refactoringStatus,
//        RefactoringStatusSeverity.ERROR,
//        "Created method will shadow method 'A.res'.",
//        findRangeIdentifier("res() {} // marker"));
////    assertTrue(refactoringStatus.hasError());
////    {
////      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.ERROR);
////      assertEquals(
////          "Usage of function 'res' in file 'Test/Test.dart' in library 'Test' will be shadowed by created function",
////          msg);
////    }
//  }

  public void test_bad_conflict_topLevel_willHideInheritedMemberUsage() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  void res() {}",
        "}",
        "class B extends A {",
        "  foo() {",
        "    res(); // marker",
        "  }",
        "}",
        "main() {",
        "// start",
        "  print(0);",
        "// end",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.ERROR,
        "Created function will shadow method 'A.res'.",
        findRangeIdentifier("res(); // marker"));
  }

  public void test_bad_constructor_initializer() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int f;",
        "  A() :",
        "// start",
        "    f = 0",
        "// end",
        "  {}",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Cannot extract a constructor initializer. Select expression part of initializer.");
  }

  public void test_bad_constructor_redirectingConstructor() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A() :",
        "// start",
        "    this.named()",
        "// end",
        "  ;",
        "  A.named() {}",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Cannot extract a constructor initializer. Select expression part of initializer.");
  }

  public void test_bad_constructor_superConstructor() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class B {}",
        "class A extends B {",
        "  A() :",
        "// start",
        "    super()",
        "// end",
        "  {}",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Cannot extract a constructor initializer. Select expression part of initializer.");
  }

  public void test_bad_doWhile_body() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  do ",
        "// start",
        "  { ",
        "  }",
        "// end",
        "  while (true);",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Operation not applicable to a 'do' statement's body and expression.");
  }

  public void test_bad_emptySelection() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "// end",
        "  int v = 1 + 2;",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Can only extract a single expression or a set of statements.");
  }

  public void test_bad_forLoop_conditionAndUpdaters() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  for ( ",
        "    int i = 0;",
        "// start",
        "    i < 10; ",
        "    i++",
        "// end",
        "  ) {}",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Operation not applicable to a 'for' statement's condition and updaters.");
  }

  public void test_bad_forLoop_init() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  for ( ",
        "// start",
        "    int i = 0",
        "// end",
        "    ; i < 10; ",
        "    i++",
        "  ) {}",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Cannot extract initialization part of a 'for' statement.");
  }

  public void test_bad_forLoop_initAndCondition() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  for ( ",
        "// start",
        "    int i = 0;",
        "    i < 10; ",
        "// end",
        "    i++",
        "  ) {}",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Operation not applicable to a 'for' statement's initializer and condition.");
  }

  public void test_bad_forLoop_updaters() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  for ( ",
        "    int i = 0;",
        "    i < 10; ",
        "// start",
        "    i++",
        "// end",
        "  ) {}",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Cannot extract increment part of a 'for' statement.");
  }

  public void test_bad_forLoop_updatersAndBody() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  for ( ",
        "    int i = 0;",
        "    i < 10; ",
        "// start",
        "    i++",
        "  ) {}",
        "// end",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Operation not applicable to a 'for' statement's updaters and body.");
  }

  public void test_bad_methodName_reference() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  main();",
        "}",
        "");
    selectionStart = findOffset("main();");
    selectionEnd = selectionStart + "main".length();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Cannot extract a single method name.");
  }

  public void test_bad_namePartOfDeclaration_function() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a;",
        "}",
        "");
    selectionStart = findOffset("main() {");
    selectionEnd = selectionStart + "main".length();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Cannot extract the name part of a declaration.");
  }

  public void test_bad_namePartOfDeclaration_variable() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int vvv = 0;",
        "}",
        "");
    selectionStart = findOffset("vvv =");
    selectionEnd = selectionStart + "vvv".length();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Cannot extract the name part of a declaration.");
  }

  public void test_bad_namePartOfQualified() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var fff;",
        "}",
        "main() {",
        "  A a;",
        "  a.fff = 1;",
        "}",
        "");
    selectionStart = findOffset("fff =");
    selectionEnd = selectionStart + "fff".length();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Can not extract name part of a property access.");
  }

  public void test_bad_newMethodName_notIdentifier() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  print(0);",
        "// end",
        "}",
        "");
    setSelectionFromStartEndComments();
    methodName = "bad-name";
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Method name must not contain '-'.");
  }

  public void test_bad_notSameParent() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  while (false) ",
        "// start",
        "  { ",
        "  } ",
        "  print(0);",
        "// end",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Not all selected statements are enclosed by the same parent statement.");
  }

  public void test_bad_parameterName_duplicate() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v1 = 1;",
        "  int v2 = 2;",
        "// start",
        "  int a = v1 + v2; // marker",
        "// end",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // update parameters
    {
      Parameter[] parameters = refactoring.getParameters();
      assertThat(parameters).hasSize(2);
      parameters[0].setNewName("dup");
      parameters[1].setNewName("dup");
      refactoring.setParameters(parameters);
    }
    // check conditions
    refactoringStatus = refactoring.checkFinalConditions(pm);
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.ERROR,
        "Parameter 'dup' already exists");
  }

  public void test_bad_parameterName_inUse() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v1 = 1;",
        "  int v2 = 2;",
        "// start",
        "  int a = v1 + v2; // marker",
        "// end",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // update parameters
    {
      Parameter[] parameters = refactoring.getParameters();
      assertThat(parameters).hasSize(2);
      parameters[0].setNewName("a");
      refactoring.setParameters(parameters);
    }
    // check conditions
    refactoringStatus = refactoring.checkFinalConditions(pm);
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.ERROR,
        "'a' is already used as a name in the selected code");
  }

  public void test_bad_selectionEndsInSomeNode() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  print(0);",
        "  print(1);",
        "// end",
        "}",
        "");
    setSelectionFromStartEndComments();
    selectionEnd = findOffset("print(1)") + "pri".length();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "The selection does not cover a set of statements or an expression. Extend selection to a valid range.");
  }

  public void test_bad_statements_return_andAssignsVariable() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  var v = 0;",
        "  return 42;",
        "// end",
        "  print(v);",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Ambiguous return value: "
            + "Selected block contains assignment(s) to local variables and return statement.");
  }

  public void test_bad_switchCase() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  switch (1) { ",
        "// start",
        "    case 0: break;",
        "// end",
        "  }",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Selection must either cover whole switch statement or parts of a single case block.");
  }

  public void test_bad_tokensBetweenLastNodeAndSelectionEnd() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  print(0);",
        "  print(1);",
        "}",
        "// end",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "The end of the selection contains characters that do not belong to a statement.");
  }

  public void test_bad_tokensBetweenSelectionStartAndFirstNode() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  print(0); // marker",
        "  print(1);",
        "// end",
        "}",
        "");
    setSelectionFromStartEndComments();
    selectionStart = findOffset("// marker") - "); ".length();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "The beginning of the selection contains characters that do not belong to a statement.");
  }

  public void test_bad_try_catchBlock_block() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  try",
        "  {} ",
        "  catch (e)",
        "// start",
        "  {}",
        "// end",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Selection must either cover whole try statement or parts of try, catch, or finally block.");
  }

  public void test_bad_try_catchBlock_complete() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  try",
        "  {} ",
        "// start",
        "  catch (e)",
        "  {}",
        "// end",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Selection must either cover whole try statement or parts of try, catch, or finally block.");
  }

  public void test_bad_try_catchBlock_exception() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  try",
        "  {} ",
        "  catch (",
        "// start",
        "    e",
        "// end",
        "  )",
        "  {}",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Cannot extract the name part of a declaration.");
  }

  public void test_bad_try_finallyBlock() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  try",
        "  {} ",
        "  finally",
        "// start",
        "  {}",
        "// end",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Selection must either cover whole try statement or parts of try, catch, or finally block.");
  }

  public void test_bad_try_tryBlock() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  try",
        "// start",
        "  {} ",
        "// end",
        "  finally",
        "  {}",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Selection must either cover whole try statement or parts of try, catch, or finally block.");
  }

  public void test_bad_typeReference() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a;",
        "}",
        "");
    selectionStart = findOffset("int");
    selectionEnd = selectionStart + "int".length();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Cannot extract a single type reference.");
  }

  public void test_bad_variableDeclarationFragment() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int ",
        "// start",
        "    a = 1",
        "// end",
        "    ,b = 2;",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Cannot extract a variable declaration fragment. Select whole declaration statement.");
  }

  public void test_bad_while_conditionAndBody() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  while ",
        "// start",
        "    (false) ",
        "  { ",
        "  } ",
        "// end",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Operation not applicable to a while statement's expression and body.");
  }

  public void test_canExtractGetter_false_fieldAssignment() throws Exception {
    keepGetterFlag = true;
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var f;",
        "  main() {",
        "// start",
        "    f = 1;",
        "// end",
        "  }",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    //
    assertFalse(refactoring.canExtractGetter());
    assertFalse(refactoring.getExtractGetter());
  }

  public void test_canExtractGetter_false_hasParameters() throws Exception {
    keepGetterFlag = true;
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f(int p) {",
        "  int a = p + 1;",
        "}",
        "");
    setSelectionString("p + 1");
    createRefactoring();
    //
    assertFalse(refactoring.canExtractGetter());
    assertFalse(refactoring.getExtractGetter());
  }

  public void test_canExtractGetter_false_returnNotUsed_assignment() throws Exception {
    keepGetterFlag = true;
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var topVar = 0;",
        "f(int p) {",
        "  topVar = 5;",
        "}",
        "");
    setSelectionString("topVar = 5");
    createRefactoring();
    //
    assertFalse(refactoring.canExtractGetter());
    assertFalse(refactoring.getExtractGetter());
  }

  public void test_canExtractGetter_false_returnNotUsed_noReturn() throws Exception {
    keepGetterFlag = true;
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var topVar = 0;",
        "main() {",
        "// start",
        "  int a = 1;",
        "  int b = 2;",
        "  topVar = a + b;",
        "// end",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    //
    assertFalse(refactoring.canExtractGetter());
    assertFalse(refactoring.getExtractGetter());
  }

  public void test_canExtractGetter_true() throws Exception {
    keepGetterFlag = true;
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 1 + 2;",
        "}",
        "");
    setSelectionString("1 + 2");
    createRefactoring();
    // no parameters
    assertTrue(refactoring.canExtractGetter());
    assertTrue(refactoring.getExtractGetter());
  }

  public void test_checkMethodName() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(1 + 2);",
        "}");
    setSelectionString("1 + 2");
    methodName = "bad-name";
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoring.checkMethodName(),
        RefactoringStatusSeverity.ERROR,
        "Method name must not contain '-'.");
  }

  public void test_closure_asFunction_singleExpression() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "process(f(x)) {}",
        "main() {",
        "  process((x) => x * 2);",
        "}");
    setSelectionString("(x) => x * 2");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "process(f(x)) {}",
        "main() {",
        "  process(res);",
        "}",
        "",
        "res(x) => x * 2;");
  }

  public void test_closure_asFunction_statements() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "process(f(x)) {}",
        "main() {",
        "  process((x) {",
        "    print(x);",
        "    return x * 2;",
        "  }); // marker",
        "}");
    selectionStart = findOffset("(x) {");
    selectionEnd = findOffset("); // marker");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "process(f(x)) {}",
        "main() {",
        "  process(res); // marker",
        "}",
        "",
        "res(x) {",
        "  print(x);",
        "  return x * 2;",
        "}");
  }

  public void test_closure_asMethod_statements() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "process(f(x)) {}",
        "class A {",
        "  int k = 2;",
        "  main() {",
        "    process((x) {",
        "      print(x);",
        "      return x * k;",
        "    }); // marker",
        "  }",
        "}");
    selectionStart = findOffset("(x) {");
    selectionEnd = findOffset("); // marker");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "process(f(x)) {}",
        "class A {",
        "  int k = 2;",
        "  main() {",
        "    process(res); // marker",
        "  }",
        "",
        "  res(x) {",
        "    print(x);",
        "    return x * k;",
        "  }",
        "}");
  }

  public void test_closure_bad_referencesLocalVariable() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "process(f(x)) {}",
        "main() {",
        "  int k = 2;",
        "  process((x) => x * k);",
        "}");
    setSelectionString("(x) => x * k");
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Cannot extract closure as method, it references 1 external variable(s).");
  }

  public void test_closure_bad_referencesParameter() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "process(f(x)) {}",
        "foo(int k) {",
        "  process((x) => x * k);",
        "}");
    setSelectionString("(x) => x * k");
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Cannot extract closure as method, it references 1 external variable(s).");
  }

  public void test_fromTopLevelVariableInitializerClosure() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var X = 1;",
        "",
        "var Y = () {",
        "  return 1 + X;",
        "};");
    setSelectionString("1 + X");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var X = 1;",
        "",
        "var Y = () {",
        "  return res();",
        "};",
        "",
        "num res() => 1 + X;");
  }

  public void test_getExtractGetter_false_do() throws Exception {
    keepGetterFlag = true;
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  int v = 0;",
        "  do {",
        "    v++;",
        "  } while (v < 10);",
        "// end",
        "  print(v);",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    //
    assertFalse(refactoring.getExtractGetter());
  }

  public void test_getExtractGetter_false_for() throws Exception {
    keepGetterFlag = true;
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  int v = 0;",
        "  for (int i = 0; i < 10; i++) {",
        "    v += i;",
        "  }",
        "// end",
        "  print(v);",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    //
    assertFalse(refactoring.getExtractGetter());
  }

  public void test_getExtractGetter_false_forEach() throws Exception {
    keepGetterFlag = true;
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "List<int> L = [];",
        "main() {",
        "// start",
        "  int v = 0;",
        "  for (int i in L) {",
        "    v += i;",
        "  }",
        "// end",
        "  print(v);",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    //
    assertFalse(refactoring.getExtractGetter());
  }

  public void test_getExtractGetter_false_methodInvocation_expression() throws Exception {
    keepGetterFlag = true;
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "List<int> L = [];",
        "main() {",
        "  int v = calculateSomething() + 5;",
        "}",
        "int calculateSomething() => 42;",
        "");
    setSelectionString("calculateSomething() + 5");
    createRefactoring();
    //
    assertFalse(refactoring.getExtractGetter());
  }

  public void test_getExtractGetter_false_methodInvocation_statements() throws Exception {
    keepGetterFlag = true;
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "List<int> L = [];",
        "main() {",
        "// start",
        "  int v = calculateSomething();",
        "// end",
        "  print(v);",
        "}",
        "int calculateSomething() => 42;",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    //
    assertFalse(refactoring.getExtractGetter());
  }

  public void test_getExtractGetter_false_while() throws Exception {
    keepGetterFlag = true;
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  int v = 0;",
        "  while (v < 10) {",
        "    v++;",
        "  }",
        "// end",
        "  print(v);",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    //
    assertFalse(refactoring.getExtractGetter());
  }

  public void test_getExtractGetter_true_simpleBlock() throws Exception {
    keepGetterFlag = true;
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  int v = 1 + 2;",
        "// end",
        "  print(v);",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    //
    assertTrue(refactoring.getExtractGetter());
  }

  public void test_getExtractGetter_true_singleExpression() throws Exception {
    keepGetterFlag = true;
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 1 + 2;",
        "}",
        "");
    setSelectionString("1 + 2");
    createRefactoring();
    //
    assertTrue(refactoring.getExtractGetter());
  }

  public void test_getRefactoringName_function() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(1 + 2);",
        "}");
    // create refactoring
    setSelectionString("1 + 2");
    createRefactoring();
    // access
    assertEquals("Extract Function", refactoring.getRefactoringName());
  }

  public void test_getRefactoringName_method() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  main() {",
        "    print(1 + 2);",
        "  }",
        "}");
    // create refactoring
    setSelectionString("1 + 2");
    createRefactoring();
    // access
    assertEquals("Extract Method", refactoring.getRefactoringName());
  }

  public void test_setExtractGetter() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 1 + 2;",
        "}",
        "");
    setSelectionString("1 + 2");
    createRefactoring();
    // false
    refactoring.setExtractGetter(false);
    assertEquals(false, refactoring.getExtractGetter());
    // true
    refactoring.setExtractGetter(true);
    assertEquals(true, refactoring.getExtractGetter());
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = res;",
        "}",
        "",
        "int get res => 1 + 2;",
        "");
  }

  public void test_singleExpression() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 1 + 2;",
        "}",
        "");
    setSelectionString("1 + 2");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = res();",
        "}",
        "",
        "int res() => 1 + 2;",
        "");
  }

  public void test_singleExpression_cascade() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "dynaFunction() {}",
        "main() {",
        "  String s = '';",
        "  var v = s..length;",
        "}",
        "");
    setSelectionString("s..length");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "dynaFunction() {}",
        "main() {",
        "  String s = '';",
        "  var v = res(s);",
        "}",
        "",
        "String res(String s) => s..length;",
        "");
  }

  public void test_singleExpression_Dynamic() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "dynaFunction() {}",
        "main() {",
        "  int a = dynaFunction(); // marker",
        "}",
        "");
    selectionStart = findOffset("dynaFunction();");
    selectionEnd = findOffset("; // marker");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "dynaFunction() {}",
        "main() {",
        "  int a = res(); // marker",
        "}",
        "",
        "res() => dynaFunction();",
        "");
  }

  public void test_singleExpression_ignore_assignmentLeftHandSize() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  getButton().text = 'txt';",
        "  print(getButton().text); // marker",
        "}",
        "getButton() {}",
        "");
    selectionStart = findOffset("getButton().text); // marker");
    selectionEnd = findOffset("); // marker");
    createRefactoring();
    // check number of occurrences
    assertEquals(1, refactoring.getNumberOfOccurrences());
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  getButton().text = 'txt';",
        "  print(res()); // marker",
        "}",
        "",
        "res() => getButton().text;",
        "getButton() {}",
        "");
  }

  public void test_singleExpression_occurrences() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v1 = 1;",
        "  int v2 = 2;",
        "  int v3 = 3;",
        "  int positiveA = v1 + v2; // marker",
        "  int positiveB = v2 + v3;",
        "  int positiveC = v1 +  v2;",
        "  int positiveD = v1/*abc*/ + v2;",
        "  int negA = 1 + 2;",
        "  int negB = 1 + v2;",
        "  int negC = v1 + 2;",
        "  int negD = v1 * v2;",
        "}",
        "");
    selectionStart = findOffset("v1 +");
    selectionEnd = findOffset("; // marker");
    createRefactoring();
    // check number of occurrences
    assertEquals(4, refactoring.getNumberOfOccurrences());
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v1 = 1;",
        "  int v2 = 2;",
        "  int v3 = 3;",
        "  int positiveA = res(v1, v2); // marker",
        "  int positiveB = res(v2, v3);",
        "  int positiveC = res(v1, v2);",
        "  int positiveD = res(v1, v2);",
        "  int negA = 1 + 2;",
        "  int negB = 1 + v2;",
        "  int negC = v1 + 2;",
        "  int negD = v1 * v2;",
        "}",
        "",
        "int res(int v1, int v2) => v1 + v2;",
        "");
  }

  public void test_singleExpression_occurrences_disabled() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v1 = 1;",
        "  int v2 = 2;",
        "  int v3 = 3;",
        "  int a = v1 + v2; // marker",
        "  int b = v2 + v3;",
        "}",
        "");
    selectionStart = findOffset("v1 +");
    selectionEnd = findOffset("; // marker");
    replaceAllOccurences = false;
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v1 = 1;",
        "  int v2 = 2;",
        "  int v3 = 3;",
        "  int a = res(v1, v2); // marker",
        "  int b = v2 + v3;",
        "}",
        "",
        "int res(int v1, int v2) => v1 + v2;",
        "");
  }

  public void test_singleExpression_occurrences_inClassOnly() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  myMethod() {",
        "    int v1 = 1;",
        "    int v2 = 2;",
        "    int positiveA = v1 + v2; // marker",
        "  }",
        "}",
        "main() {",
        "  int v1 = 1;",
        "  int v2 = 2;",
        "  int negA = v1 + v2;",
        "}",
        "");
    selectionStart = findOffset("v1 +");
    selectionEnd = findOffset("; // marker");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  myMethod() {",
        "    int v1 = 1;",
        "    int v2 = 2;",
        "    int positiveA = res(v1, v2); // marker",
        "  }",
        "",
        "  int res(int v1, int v2) => v1 + v2;",
        "}",
        "main() {",
        "  int v1 = 1;",
        "  int v2 = 2;",
        "  int negA = v1 + v2;",
        "}",
        "");
  }

  public void test_singleExpression_occurrences_inWholeUnit() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v1 = 1;",
        "  int v2 = 2;",
        "  int positiveA = v1 + v2; // marker",
        "}",
        "class A {",
        "  myMethod() {",
        "    int v1 = 1;",
        "    int v2 = 2;",
        "    int positiveB = v1 + v2;",
        "  }",
        "}",
        "");
    selectionStart = findOffset("v1 +");
    selectionEnd = findOffset("; // marker");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v1 = 1;",
        "  int v2 = 2;",
        "  int positiveA = res(v1, v2); // marker",
        "}",
        "",
        "int res(int v1, int v2) => v1 + v2;",
        "class A {",
        "  myMethod() {",
        "    int v1 = 1;",
        "    int v2 = 2;",
        "    int positiveB = res(v1, v2);",
        "  }",
        "}",
        "");
  }

  public void test_singleExpression_returnTypeGeneric() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v = new List<String>();",
        "}",
        "");
    setSelectionString("new List<String>()");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v = res();",
        "}",
        "",
        "List<String> res() => new List<String>();",
        "");
  }

  public void test_singleExpression_returnTypePrefix() throws Exception {
    setFileContent(
        "MyLib.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library my_lib;",
            "class A {}",
            ""));
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'MyLib.dart' as ml;",
        "main() {",
        "  var a = new ml.A();",
        "}",
        "");
    setSelectionString("new ml.A()");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'MyLib.dart' as ml;",
        "main() {",
        "  var a = res();",
        "}",
        "",
        "ml.A res() => new ml.A();",
        "");
  }

  public void test_singleExpression_staticContext_extractFromInitializer() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A(int v) {}",
        "}",
        "class B extends A {",
        "  B() : super(1 + 2) {}",
        "}",
        "");
    selectionStart = findOffset("1 + 2");
    selectionEnd = findOffset("2) {}") + 1;
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A(int v) {}",
        "}",
        "class B extends A {",
        "  B() : super(res()) {}",
        "",
        "  static int res() => 1 + 2;",
        "}",
        "");
  }

  public void test_singleExpression_staticContext_extractFromInstance() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  instanceMethodA() {",
        "    int v1 = 1;",
        "    int v2 = 2;",
        "    int positiveA = v1 + v2; // marker",
        "  }",
        "  instanceMethodB() {",
        "    int v1 = 1;",
        "    int v2 = 2;",
        "    int positiveB = v1 + v2;",
        "  }",
        "  static staticMethodA() {",
        "    int v1 = 1;",
        "    int v2 = 2;",
        "    int positiveA = v1 + v2;",
        "  }",
        "}",
        "");
    selectionStart = findOffset("v1 +");
    selectionEnd = findOffset("; // marker");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  instanceMethodA() {",
        "    int v1 = 1;",
        "    int v2 = 2;",
        "    int positiveA = res(v1, v2); // marker",
        "  }",
        "",
        "  static int res(int v1, int v2) => v1 + v2;",
        "  instanceMethodB() {",
        "    int v1 = 1;",
        "    int v2 = 2;",
        "    int positiveB = res(v1, v2);",
        "  }",
        "  static staticMethodA() {",
        "    int v1 = 1;",
        "    int v2 = 2;",
        "    int positiveA = res(v1, v2);",
        "  }",
        "}",
        "");
  }

  public void test_singleExpression_staticContext_extractFromStatic() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static staticMethodA() {",
        "    int v1 = 1;",
        "    int v2 = 2;",
        "    int positiveA = v1 + v2; // marker",
        "  }",
        "  static staticMethodB() {",
        "    int v1 = 1;",
        "    int v2 = 2;",
        "    int positiveB = v1 + v2;",
        "  }",
        "  instanceMethodA() {",
        "    int v1 = 1;",
        "    int v2 = 2;",
        "    int positiveA = v1 + v2;",
        "  }",
        "}",
        "");
    selectionStart = findOffset("v1 +");
    selectionEnd = findOffset("; // marker");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static staticMethodA() {",
        "    int v1 = 1;",
        "    int v2 = 2;",
        "    int positiveA = res(v1, v2); // marker",
        "  }",
        "",
        "  static int res(int v1, int v2) => v1 + v2;",
        "  static staticMethodB() {",
        "    int v1 = 1;",
        "    int v2 = 2;",
        "    int positiveB = res(v1, v2);",
        "  }",
        "  instanceMethodA() {",
        "    int v1 = 1;",
        "    int v2 = 2;",
        "    int positiveA = res(v1, v2);",
        "  }",
        "}",
        "");
  }

  public void test_singleExpression_staticContext_hasInInitializer() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A(int v) {}",
        "}",
        "class B extends A {",
        "  B() : super(1 + 2) {}",
        "  foo() {",
        "    print(1 + 2); // marker",
        "  }",
        "}",
        "");
    selectionStart = findOffset("1 + 2); // marker");
    selectionEnd = findOffset("2); // marker") + 1;
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A(int v) {}",
        "}",
        "class B extends A {",
        "  B() : super(res()) {}",
        "  foo() {",
        "    print(res()); // marker",
        "  }",
        "",
        "  static int res() => 1 + 2;",
        "}",
        "");
  }

  public void test_singleExpression_usesParameter() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "fooA(int a1) {",
        "  int a2 = 2;",
        "  int a = a1 + a2; // marker",
        "}",
        "fooB(int b1) {",
        "  int b2 = 2;",
        "  int b = b1 + b2;",
        "}",
        "");
    selectionStart = findOffset("a1 +");
    selectionEnd = findOffset("; // marker");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "fooA(int a1) {",
        "  int a2 = 2;",
        "  int a = res(a1, a2); // marker",
        "}",
        "",
        "int res(int a1, int a2) => a1 + a2;",
        "fooB(int b1) {",
        "  int b2 = 2;",
        "  int b = res(b1, b2);",
        "}",
        "");
  }

  public void test_singleExpression_withVariables() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v1 = 1;",
        "  int v2 = 2;",
        "  int a = v1 + v2 + v1; // marker",
        "}",
        "");
    selectionStart = findOffset("v1 +");
    selectionEnd = findOffset("; // marker");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v1 = 1;",
        "  int v2 = 2;",
        "  int a = res(v1, v2); // marker",
        "}",
        "",
        "int res(int v1, int v2) => v1 + v2 + v1;",
        "");
  }

  public void test_singleExpression_withVariables_doRename() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v1 = 1;",
        "  int v2 = 2;",
        "  int v3 = 3;",
        "  int a = v1 + v2 + v1; // marker",
        "  int b = v2 + v3 + v2;",
        "}",
        "");
    selectionStart = findOffset("v1 +");
    selectionEnd = findOffset("; // marker");
    createRefactoring();
    // update parameters
    {
      Parameter[] parameters = refactoring.getParameters();
      assertThat(parameters).hasSize(2);
      parameters[0].setNewName("par1");
      parameters[1].setNewName("param2");
      refactoring.setParameters(parameters);
    }
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v1 = 1;",
        "  int v2 = 2;",
        "  int v3 = 3;",
        "  int a = res(v1, v2); // marker",
        "  int b = res(v2, v3);",
        "}",
        "",
        "int res(int par1, int param2) => par1 + param2 + par1;",
        "");
  }

  public void test_singleExpression_withVariables_doReorder() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v1 = 1;",
        "  int v2 = 2;",
        "  int v3 = 3;",
        "  int a = v1 + v2; // marker",
        "  int b = v2 + v3;",
        "}",
        "");
    selectionStart = findOffset("v1 +");
    selectionEnd = findOffset("; // marker");
    createRefactoring();
    // update parameters
    {
      Parameter[] parameters = refactoring.getParameters();
      assertThat(parameters).hasSize(2);
      Parameter p = parameters[1];
      parameters = ArrayUtils.remove(parameters, 1);
      parameters = ArrayUtils.add(parameters, 0, p);
      refactoring.setParameters(parameters);
    }
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v1 = 1;",
        "  int v2 = 2;",
        "  int v3 = 3;",
        "  int a = res(v2, v1); // marker",
        "  int b = res(v3, v2);",
        "}",
        "",
        "int res(int v2, int v1) => v1 + v2;",
        "");
  }

  /**
   * <p>
   * https://code.google.com/p/dart/issues/detail?id=10942
   */
  public void test_singleExpression_withVariables_namedExpression() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v1 = 1;",
        "  int v2 = 2;",
        "  int a = process(arg: v1 + v2); // marker",
        "}",
        "process({arg}) {}");
    setSelectionString("process(arg: v1 + v2)");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v1 = 1;",
        "  int v2 = 2;",
        "  int a = res(v1, v2); // marker",
        "}",
        "",
        "res(int v1, int v2) => process(arg: v1 + v2);",
        "process({arg}) {}");
  }

  public void test_singleExpression_withVariables_newType() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v1 = 1;",
        "  int v2 = 2;",
        "  int v3 = 3;",
        "  int a = v1 + v2 + v3; // marker",
        "}",
        "");
    selectionStart = findOffset("v1 +");
    selectionEnd = findOffset("; // marker");
    createRefactoring();
    // update parameters
    {
      Parameter[] parameters = refactoring.getParameters();
      assertThat(parameters).hasSize(3);
      parameters[0].setNewTypeName("num");
      parameters[1].setNewTypeName("dynamic");
      parameters[2].setNewTypeName("");
      refactoring.setParameters(parameters);
    }
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v1 = 1;",
        "  int v2 = 2;",
        "  int v3 = 3;",
        "  int a = res(v1, v2, v3); // marker",
        "}",
        "",
        "int res(num v1, v2, v3) => v1 + v2 + v3;",
        "");
  }

  public void test_singleExpression_withVariables_useBestType() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v1 = 1;",
        "  var v2 = 2;",
        "  var a = v1 + v2 + v1; // marker",
        "}",
        "");
    selectionStart = findOffset("v1 +");
    selectionEnd = findOffset("; // marker");
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v1 = 1;",
        "  var v2 = 2;",
        "  var a = res(v1, v2); // marker",
        "}",
        "",
        "num res(int v1, int v2) => v1 + v2 + v1;",
        "");
  }

  public void test_statements_assignment() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v;",
        "// start",
        "  v = 5;",
        "// end",
        "  print(v);",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v;",
        "// start",
        "  v = res(v);",
        "// end",
        "  print(v);",
        "}",
        "",
        "int res(int v) {",
        "  v = 5;",
        "  return v;",
        "}",
        "");
  }

  public void test_statements_changeIndentation() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  {",
        "// start",
        "    if (true) {",
        "      print(0);",
        "    }",
        "// end",
        "  }",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  {",
        "// start",
        "    res();",
        "// end",
        "  }",
        "}",
        "",
        "void res() {",
        "  if (true) {",
        "    print(0);",
        "  }",
        "}",
        "");
  }

  public void test_statements_changeIndentation_multilineString() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  {",
        "// start",
        "    print('''",
        "first line",
        "second line",
        "    ''');",
        "// end",
        "  }",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  {",
        "// start",
        "    res();",
        "// end",
        "  }",
        "}",
        "",
        "void res() {",
        "  print('''",
        "first line",
        "second line",
        "    ''');",
        "}",
        "");
  }

  public void test_statements_definesVariable_notUsedOutside() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 1;",
        "  int b = 1;",
        "// start",
        "  int v = a + b;",
        "  print(v);",
        "// end",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 1;",
        "  int b = 1;",
        "// start",
        "  res(a, b);",
        "// end",
        "}",
        "",
        "void res(int a, int b) {",
        "  int v = a + b;",
        "  print(v);",
        "}",
        "");
  }

  public void test_statements_definesVariable_oneUsedOutside_assignment() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "myFunctionA() {",
        "  int a = 1;",
        "// start",
        "  a += 10;",
        "// end",
        "  print(a);",
        "}",
        "myFunctionB() {",
        "  int b = 2;",
        "  b += 10;",
        "  print(b);",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "myFunctionA() {",
        "  int a = 1;",
        "// start",
        "  a = res(a);",
        "// end",
        "  print(a);",
        "}",
        "",
        "int res(int a) {",
        "  a += 10;",
        "  return a;",
        "}",
        "myFunctionB() {",
        "  int b = 2;",
        "  b = res(b);",
        "  print(b);",
        "}",
        "");
  }

  public void test_statements_definesVariable_oneUsedOutside_declaration() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "myFunctionA() {",
        "  int a = 1;",
        "  int b = 2;",
        "// start",
        "  int v1 = a + b;",
        "// end",
        "  print(v1);",
        "}",
        "myFunctionB() {",
        "  int a = 3;",
        "  int b = 4;",
        "  int v2 = a + b;",
        "  print(v2);",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "myFunctionA() {",
        "  int a = 1;",
        "  int b = 2;",
        "// start",
        "  int v1 = res(a, b);",
        "// end",
        "  print(v1);",
        "}",
        "",
        "int res(int a, int b) {",
        "  int v1 = a + b;",
        "  return v1;",
        "}",
        "myFunctionB() {",
        "  int a = 3;",
        "  int b = 4;",
        "  int v2 = res(a, b);",
        "  print(v2);",
        "}",
        "");
  }

  public void test_statements_definesVariable_twoUsedOutside() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "myFunctionA() {",
        "// start",
        "  int varA = 1;",
        "  int varB = 2;",
        "// end",
        "  int v = varA + varB;",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Ambiguous return value: "
            + "Selected block contains more than one assignment to local variables. "
            + "Affected variables are:\\n\\nvarA\nvarB");
  }

  public void test_statements_duplicate_absolutelySame() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "myFunctionA() {",
        "  print(0);",
        "  print(1);",
        "}",
        "myFunctionB() {",
        "// start",
        "  print(0);",
        "  print(1);",
        "// end",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "myFunctionA() {",
        "  res();",
        "}",
        "myFunctionB() {",
        "// start",
        "  res();",
        "// end",
        "}",
        "",
        "void res() {",
        "  print(0);",
        "  print(1);",
        "}",
        "");
  }

  /**
   * We match code fragments regardless of the used variable names.
   */
  public void test_statements_duplicate_declaresDifferentlyNamedVariable() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "myFunctionA() {",
        "  int varA = 1;",
        "  print(varA);",
        "}",
        "myFunctionB() {",
        "// start",
        "  int varB = 1;",
        "  print(varB);",
        "// end",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "myFunctionA() {",
        "  res();",
        "}",
        "myFunctionB() {",
        "// start",
        "  res();",
        "// end",
        "}",
        "",
        "void res() {",
        "  int varB = 1;",
        "  print(varB);",
        "}",
        "");
  }

  public void test_statements_dynamic() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "dynaFunction(p) => 0;",
        "main() {",
        "// start",
        "  var a = 1;",
        "  var v = dynaFunction(a);",
        "// end",
        "  print(v);",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "dynaFunction(p) => 0;",
        "main() {",
        "// start",
        "  var v = res();",
        "// end",
        "  print(v);",
        "}",
        "",
        "res() {",
        "  var a = 1;",
        "  var v = dynaFunction(a);",
        "  return v;",
        "}",
        "");
  }

  /**
   * We should always add ";" when invoke method with extracted statements.
   */
  public void test_statements_endsWithBlock() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  if (true) {",
        "    print(0);",
        "  }",
        "// end",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  res();",
        "// end",
        "}",
        "",
        "void res() {",
        "  if (true) {",
        "    print(0);",
        "  }",
        "}",
        "");
  }

  public void test_statements_inSwitchMember() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo(int p) {",
        "    switch (p) {",
        "      case 0:",
        "// start",
        "        print(0);",
        "// end",
        "        break;",
        "      default:",
        "        break;",
        "    }",
        "  }",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo(int p) {",
        "    switch (p) {",
        "      case 0:",
        "// start",
        "        res();",
        "// end",
        "        break;",
        "      default:",
        "        break;",
        "    }",
        "  }",
        "",
        "  void res() {",
        "    print(0);",
        "  }",
        "}",
        "");
  }

  public void test_statements_method() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo() {",
        "// start",
        "    print(0);",
        "// end",
        "  }",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo() {",
        "// start",
        "    res();",
        "// end",
        "  }",
        "",
        "  void res() {",
        "    print(0);",
        "  }",
        "}",
        "");
  }

  public void test_statements_noDuplicates() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 1;",
        "  int b = 1;",
        "// start",
        "  print(a);",
        "// end",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // check number of occurrences
    assertEquals(1, refactoring.getNumberOfOccurrences());
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 1;",
        "  int b = 1;",
        "// start",
        "  res(a);",
        "// end",
        "}",
        "",
        "void res(int a) {",
        "  print(a);",
        "}",
        "");
  }

  public void test_statements_return_last() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  int v = 5;",
        "  return v + 1;",
        "// end",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  return res();",
        "// end",
        "}",
        "",
        "int res() {",
        "  int v = 5;",
        "  return v + 1;",
        "}",
        "");
  }

  public void test_statements_return_single() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  return 42;",
        "// end",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  return res();",
        "// end",
        "}",
        "",
        "int res() {",
        "  return 42;",
        "}",
        "");
  }

  /**
   * We have 3 identical statements, but select only 2. This should not cause problems.
   */
  public void test_statements_twoOfThree() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  print(0);",
        "  print(0);",
        "// end",
        "  print(0);",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    // apply refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  res();",
        "// end",
        "  print(0);",
        "}",
        "",
        "void res() {",
        "  print(0);",
        "  print(0);",
        "}",
        "");
  }

  /**
   * Checks that all conditions are <code>OK</code> and applying {@link Change} to the
   * {@link #testUnit} is same source as given lines.
   */
  protected final void assertSuccessfulRefactoring(String... lines) throws Exception {
    assertRefactoringStatus(refactoringStatus, RefactoringStatusSeverity.OK, null);
    Change change = refactoring.createChange(pm);
    assertTestChangeResult(change, makeSource(lines));
  }

  @Override
  protected void tearDown() throws Exception {
    refactoring = null;
    refactoringStatus = null;
    super.tearDown();
  }

  /**
   * Creates {@link ExtractMethodRefactoring} in {@link #refactoring}.
   */
  private void createRefactoring() throws Exception {
    int selectionLength = selectionEnd - selectionStart;
    AssistContext context = new AssistContext(
        searchEngine,
        analysisContext,
        null,
        testSource,
        testUnit,
        selectionStart,
        selectionLength);
    refactoring = new ExtractMethodRefactoringImpl(context);
    refactoring.setMethodName(methodName);
    refactoring.setReplaceAllOccurrences(replaceAllOccurences);
    // just for coverage
    assertEquals(replaceAllOccurences, refactoring.getReplaceAllOccurrences());
    // prepare status
    refactoringStatus = refactoring.checkAllConditions(pm);
    // reset some flags
    if (!keepGetterFlag) {
      refactoring.setExtractGetter(false);
    }
  }

  private void setSelectionFromStartEndComments() throws Exception {
    selectionStart = findEnd("// start") + EOL.length();
    selectionEnd = findOffset("// end");
  }

  /**
   * Sets selection to the start of the first occurrence of the given string.
   */
  private void setSelectionString(String pattern) {
    selectionStart = findOffset(pattern);
    selectionEnd = findEnd(pattern);
  }
}
