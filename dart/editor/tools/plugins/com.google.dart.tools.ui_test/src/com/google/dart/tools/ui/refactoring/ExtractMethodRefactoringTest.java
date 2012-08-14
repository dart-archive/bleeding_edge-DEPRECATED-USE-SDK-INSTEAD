/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.ui.refactoring;

import com.google.dart.tools.core.test.util.TestProject;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.code.ExtractMethodRefactoring;
import com.google.dart.tools.internal.corext.refactoring.code.ParameterInfo;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

/**
 * Test for {@link ExtractMethodRefactoring}.
 */
public final class ExtractMethodRefactoringTest extends RefactoringTest {
  private static final IProgressMonitor pm = new NullProgressMonitor();

  private int selectionStart;
  private int selectionEnd;
  private boolean replaceAllOccurences = true;
  private int expectedNumberOfDuplicates = -1;
  private ExtractMethodRefactoring refactoring;
  private RefactoringStatus refactoringStatus;

  public void test_access() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 1 + 2;",
        "}",
        "");
    selectionStart = findOffset("1");
    selectionEnd = findOffset("2;") + 1;
    createRefactoring();
    assertEquals("Extract Method", refactoring.getName());
    assertEquals(true, refactoring.getReplaceAllOccurrences());
  }

//  public void test_bad_sameVariable_after() throws Exception {
//    setTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "f() {",
//        "  int a = 1 + 2;",
//        "  var res;",
//        "}");
//    selectionStart = findOffset("1");
//    selectionEnd = findOffset("2;") + 1;
//    // check conditions
//    createRefactoring("res");
//    assert_warning_alreadyDefined();
//    // use checkLocalName() - conflicting name
//    {
//      refactoringStatus = refactoring.checkLocalName("res");
//      assert_warning_alreadyDefined();
//    }
//    // use checkLocalName() - unique name
//    {
//      refactoringStatus = refactoring.checkLocalName("uniqueName");
//      assertTrue(refactoringStatus.isOK());
//    }
//  }
//
//  public void test_bad_sameVariable_before() throws Exception {
//    setTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "f() {",
//        "  var res;",
//        "  int a = 1 + 2;",
//        "}");
//    selectionStart = findOffset("1");
//    selectionEnd = findOffset("2;") + 1;
//    // check conditions
//    createRefactoring("res");
//    assert_warning_alreadyDefined();
//    // use checkLocalName() - conflicting name
//    {
//      refactoringStatus = refactoring.checkLocalName("res");
//      assert_warning_alreadyDefined();
//    }
//    // use checkLocalName() - unique name
//    {
//      refactoringStatus = refactoring.checkLocalName("uniqueName");
//      assertTrue(refactoringStatus.isOK());
//    }
//  }
//
//  public void test_fragmentExpression_leadingNotWhitespace() throws Exception {
//    setTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "f() {",
//        "  int a = 1 + 2 + 3 + 4;",
//        "}");
//    selectionStart = findOffset("+ 2");
//    selectionEnd = findOffset("3 + ") + 1;
//    // check conditions
//    createRefactoring("res");
//    assert_fatalError_selection();
//  }
//
//  public void test_fragmentExpression_leadingPartialSelection() throws Exception {
//    setTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "f() {",
//        "  int a = 111 + 2 + 3 + 4;",
//        "}");
//    selectionStart = findOffset("11 +");
//    selectionEnd = findOffset("3 +") + 3;
//    // check conditions
//    createRefactoring("res");
//    assert_fatalError_selection();
//  }
//
//  public void test_fragmentExpression_leadingWhitespace() throws Exception {
//    setTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "f() {",
//        "  int a = 1 + 2 + 3 + 4;",
//        "}");
//    selectionStart = findOffset(" 2");
//    selectionEnd = findOffset("3 + ") + 1;
//    doSuccessfullRefactoring();
//    assertTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "f() {",
//        "  int res =  2 + 3;",
//        "  int a = 1 +res + 4;",
//        "}");
//  }
//
//  public void test_fragmentExpression_notAssociativeOperator() throws Exception {
//    setTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "f() {",
//        "  int a = 1 - 2 - 3 - 4;",
//        "}");
//    selectionStart = findOffset("2");
//    selectionEnd = findOffset("3 - ") + 1;
//    // check conditions
//    createRefactoring("res");
//    assert_fatalError_selection();
//  }
//
//  public void test_fragmentExpression_OK() throws Exception {
//    setTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "f() {",
//        "  int a = 1 + 2 + 3 + 4;",
//        "}");
//    selectionStart = findOffset("2");
//    selectionEnd = findOffset("3 + ") + 1;
//    doSuccessfullRefactoring();
//    assertTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "f() {",
//        "  int res = 2 + 3;",
//        "  int a = 1 + res + 4;",
//        "}");
//  }
//
//  public void test_fragmentExpression_trailingNotWhitespace() throws Exception {
//    setTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "f() {",
//        "  int a = 1 + 2 + 3 + 4;",
//        "}");
//    selectionStart = findOffset("2");
//    selectionEnd = findOffset("3 +") + 3;
//    // check conditions
//    createRefactoring("res");
//    assert_fatalError_selection();
//  }
//
//  public void test_fragmentExpression_trailingPartialSelection() throws Exception {
//    setTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "f() {",
//        "  int a = 1 + 2 + 3 + 444;",
//        "}");
//    selectionStart = findOffset("2");
//    selectionEnd = findOffset("44;");
//    // check conditions
//    createRefactoring("res");
//    assert_fatalError_selection();
//  }
//
//  public void test_fragmentExpression_trailingWhitespace() throws Exception {
//    setTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "f() {",
//        "  int a = 1 + 2 + 3 + 4;",
//        "}");
//    selectionStart = findOffset("2");
//    selectionEnd = findOffset("3 + ") + 2;
//    doSuccessfullRefactoring();
//    assertTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "f() {",
//        "  int res = 2 + 3 ;",
//        "  int a = 1 + res+ 4;",
//        "}");
//  }
//
//  public void test_occurences_disableOccurences() throws Exception {
//    setTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "int foo() => 42;",
//        "main() {",
//        "  int a = 1 + foo();",
//        "  int b = 2 +  foo(); // marker",
//        "}");
//    selectionStart = findOffset("  foo();") + 2;
//    selectionEnd = findOffset("; // marker");
//    replaceAllOccurences = false;
//    doSuccessfullRefactoring();
//    assertTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "int foo() => 42;",
//        "main() {",
//        "  int a = 1 + foo();",
//        "  int res = foo();",
//        "  int b = 2 +  res; // marker",
//        "}");
//  }
//
//  public void test_occurences_fragmentExpression() throws Exception {
//    setTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "int foo() => 42;",
//        "main() {",
//        "  int a = 1 + 2 + foo() + 3;",
//        "  int b = 1 +  2 + foo() + 3; // marker",
//        "}");
//    selectionStart = findOffset("  2 +") + 2;
//    selectionEnd = findOffset("; // marker");
//    doSuccessfullRefactoring();
//    assertTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "int foo() => 42;",
//        "main() {",
//        "  int res = 2 + foo() + 3;",
//        "  int a = 1 + res;",
//        "  int b = 1 +  res; // marker",
//        "}");
//  }
//
//  public void test_occurences_singleExpression() throws Exception {
//    setTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "int foo() => 42;",
//        "main() {",
//        "  int a = 1 + foo();",
//        "  int b = 2 +  foo(); // marker",
//        "}");
//    selectionStart = findOffset("  foo();") + 2;
//    selectionEnd = findOffset("; // marker");
//    doSuccessfullRefactoring();
//    assertTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "int foo() => 42;",
//        "main() {",
//        "  int res = foo();",
//        "  int a = 1 + res;",
//        "  int b = 2 +  res; // marker",
//        "}");
//  }
//
//  public void test_occurences_whenComment() throws Exception {
//    setTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "int foo() => 42;",
//        "main() {",
//        "  /*int a = 1 + foo();*/",
//        "  int b = 2 +  foo(); // marker",
//        "}");
//    selectionStart = findOffset("  foo();") + 2;
//    selectionEnd = findOffset("; // marker");
//    doSuccessfullRefactoring();
//    assertTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "int foo() => 42;",
//        "main() {",
//        "  /*int a = 1 + foo();*/",
//        "  int res = foo();",
//        "  int b = 2 +  res; // marker",
//        "}");
//  }

  public void test_bad_assignmentLeftHandSide() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int aaa;",
        "// start",
        "  aaa ",
        "// end",
        "   = 0;",
        "}",
        "");
    setSelectionFromStartEndComments();
    selectionStart = findOffset("aaa ");
    createRefactoring();
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(RefactoringCoreMessages.ExtractMethodAnalyzer_leftHandSideOfAssignment, msg);
    }
  }

  public void test_bad_comment_selectionEndsInside() throws Exception {
    setTestUnitContent(
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
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(RefactoringCoreMessages.CommentAnalyzer_ends_inside_comment, msg);
    }
  }

  public void test_bad_comment_selectionStartsInside() throws Exception {
    setTestUnitContent(
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
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(RefactoringCoreMessages.CommentAnalyzer_starts_inside_comment, msg);
    }
  }

  public void test_bad_conflict_method_willHideTopLevel() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "void res() {}",
        "class B extends A {",
        "  foo() {",
        "// start",
        "    print(0);",
        "// end",
        "  }",
        "  foo() {",
        "    res();",
        "  }",
        "}",
        "");
    TestProject.waitForAutoBuild();
    setSelectionFromStartEndComments();
    createRefactoring();
    assertTrue(refactoringStatus.hasError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.ERROR);
      assertEquals(
          "Usage of function 'res' in file 'Test/Test.dart' in library 'Test' will be shadowed by created function",
          msg);
    }
  }

  public void test_bad_conflict_topLevel_alreadyDeclaresFunction() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "void res() {}",
        "main() {",
        "// start",
        "  print(0);",
        "// end",
        "}",
        "");
    TestProject.waitForAutoBuild();
    setSelectionFromStartEndComments();
    createRefactoring();
    assertTrue(refactoringStatus.hasError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.ERROR);
      assertEquals(
          "File 'Test/Test.dart' in library 'Test' already declares top-level function 'res'",
          msg);
    }
  }

  public void test_bad_conflict_topLevel_alreadyDeclaresType() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class res {}",
        "main() {",
        "// start",
        "  print(0);",
        "// end",
        "}",
        "");
    TestProject.waitForAutoBuild();
    setSelectionFromStartEndComments();
    createRefactoring();
    assertTrue(refactoringStatus.hasError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.ERROR);
      assertEquals(
          "File 'Test/Test.dart' in library 'Test' already declares top-level type 'res'",
          msg);
    }
  }

  public void test_bad_conflict_topLevel_willHideInheritedMemberUsage() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  void res() {}",
        "}",
        "class B extends A {",
        "  foo() {",
        "    res();",
        "  }",
        "}",
        "main() {",
        "// start",
        "  print(0);",
        "// end",
        "}",
        "");
    TestProject.waitForAutoBuild();
    setSelectionFromStartEndComments();
    createRefactoring();
    assertTrue(refactoringStatus.hasError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.ERROR);
      assertEquals(
          "Usage of method 'A.res' declared in 'Test/Test.dart' will be shadowed by created function",
          msg);
    }
  }

  public void test_bad_constructor_initializer() throws Exception {
    setTestUnitContent(
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
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_initializer, msg);
    }
  }

  public void test_bad_constructor_redirectingConstructor() throws Exception {
    setTestUnitContent(
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
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_initializer, msg);
    }
  }

  public void test_bad_constructor_superConstructor() throws Exception {
    setTestUnitContent(
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
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_initializer, msg);
    }
  }

  public void test_bad_doWhile_body() throws Exception {
    setTestUnitContent(
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
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(RefactoringCoreMessages.StatementAnalyzer_do_body, msg);
    }
  }

  public void test_bad_emptySelection() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "// end",
        "  int v = varA + varB;",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(RefactoringCoreMessages.ExtractMethodAnalyzer_single_expression_or_set, msg);
    }
  }

  public void test_bad_forLoop_conditionAndUpdaters() throws Exception {
    setTestUnitContent(
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
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(RefactoringCoreMessages.StatementAnalyzer_for_condition_updaters, msg);
    }
  }

  public void test_bad_forLoop_init() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  for ( ",
        "// start",
        "    int i = 0;",
        "// end",
        "    i < 10; ",
        "    i++",
        "  ) {}",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(
          RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_for_initializer,
          msg);
    }
  }

  public void test_bad_forLoop_initAndCondition() throws Exception {
    setTestUnitContent(
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
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(RefactoringCoreMessages.StatementAnalyzer_for_initializer_condition, msg);
    }
  }

  public void test_bad_forLoop_updaters() throws Exception {
    setTestUnitContent(
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
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_for_updater, msg);
    }
  }

  public void test_bad_forLoop_updatersAndBody() throws Exception {
    setTestUnitContent(
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
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(RefactoringCoreMessages.StatementAnalyzer_for_updaters_body, msg);
    }
  }

  public void test_bad_methodName_reference() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  main();",
        "}",
        "");
    selectionStart = findOffset("main();");
    selectionEnd = selectionStart + "main".length();
    createRefactoring();
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(
          RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_method_name_reference,
          msg);
    }
  }

  public void test_bad_namePartOfDeclaration_method() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a;",
        "}",
        "");
    selectionStart = findOffset("main() {");
    selectionEnd = selectionStart + "main".length();
    createRefactoring();
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(
          RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_name_in_declaration,
          msg);
    }
  }

  public void test_bad_namePartOfDeclaration_variable() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int vvv = 0;",
        "}",
        "");
    selectionStart = findOffset("vvv =");
    selectionEnd = selectionStart + "vvv".length();
    createRefactoring();
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(
          RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_name_in_declaration,
          msg);
    }
  }

  public void test_bad_namePartOfQualified() throws Exception {
    setTestUnitContent(
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
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(
          RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_part_of_qualified_name,
          msg);
    }
  }

  public void test_bad_newMethodName_notIdentifier() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  print(0);",
        "// end",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring("badName-");
    // check status
    assertTrue(refactoringStatus.hasError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.ERROR);
      assertEquals("The method name 'badName-' is not a valid identifier", msg);
    }
  }

  public void test_bad_notSameParent() throws Exception {
    setTestUnitContent(
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
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(RefactoringCoreMessages.ExtractMethodAnalyzer_parent_mismatch, msg);
    }
  }

  public void test_bad_parameterName_duplicate() throws Exception {
    setTestUnitContent(
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
    // prepare
    createRefactoring();
    // update parameters
    {
      List<ParameterInfo> parameters = refactoring.getParameters();
      assertThat(parameters).hasSize(2);
      parameters.get(0).setNewName("dup");
      parameters.get(1).setNewName("dup");
    }
    // check status
    refactoringStatus = refactoring.checkFinalConditions(pm);
    assertTrue(refactoringStatus.hasError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.ERROR);
      assertEquals("A parameter 'dup' already exists", msg);
    }
  }

  public void test_bad_parameterName_inUse() throws Exception {
    setTestUnitContent(
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
    // prepare
    createRefactoring();
    // update parameters
    {
      List<ParameterInfo> parameters = refactoring.getParameters();
      assertThat(parameters).hasSize(2);
      parameters.get(0).setNewName("a");
    }
    // check status
    refactoringStatus = refactoring.checkFinalConditions(pm);
    assertTrue(refactoringStatus.hasError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.ERROR);
      assertEquals("'a' is already used as a name in the selected code", msg);
    }
  }

  public void test_bad_selectionEndsInSomeNode() throws Exception {
    setTestUnitContent(
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
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(RefactoringCoreMessages.StatementAnalyzer_doesNotCover, msg);
    }
  }

  public void test_bad_switch_switchMember() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  switch (0) ",
        "// start",
        "     switch (0) {}",
        "// end",
        "  } ",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(RefactoringCoreMessages.StatementAnalyzer_switch_statement, msg);
    }
  }

  public void test_bad_tokensBetweenLastNodeAndSelectionEnd() throws Exception {
    setTestUnitContent(
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
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(RefactoringCoreMessages.StatementAnalyzer_end_of_selection, msg);
    }
  }

  public void test_bad_tokensBetweenSelectionStartAndFirstNode() throws Exception {
    setTestUnitContent(
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
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(RefactoringCoreMessages.StatementAnalyzer_beginning_of_selection, msg);
    }
  }

  public void test_bad_try_catchBlock_block() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  try",
        "  {} ",
        "  catch (",
        "    Exception e",
        "  )",
        "// start",
        "  {}",
        "// end",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(RefactoringCoreMessages.StatementAnalyzer_try_statement, msg);
    }
  }

  public void test_bad_try_catchBlock_complete() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  try",
        "  {} ",
        "// start",
        "  catch (",
        "    Exception e",
        "  )",
        "  {}",
        "// end",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(RefactoringCoreMessages.StatementAnalyzer_try_statement, msg);
    }
  }

  public void test_bad_try_catchBlock_exception() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  try",
        "  {} ",
        "  catch (",
        "// start",
        "    Exception e",
        "// end",
        "  )",
        "  {}",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(RefactoringCoreMessages.StatementAnalyzer_try_statement, msg);
    }
  }

  public void test_bad_try_finallyBlock() throws Exception {
    setTestUnitContent(
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
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(RefactoringCoreMessages.StatementAnalyzer_try_statement, msg);
    }
  }

  public void test_bad_try_tryBlock() throws Exception {
    setTestUnitContent(
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
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(RefactoringCoreMessages.StatementAnalyzer_try_statement, msg);
    }
  }

  public void test_bad_typeReference() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a;",
        "}",
        "");
    selectionStart = findOffset("int");
    selectionEnd = selectionStart + "int".length();
    createRefactoring();
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_type_reference, msg);
    }
  }

  public void test_bad_variableDeclarationFragment() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int ",
        "// start",
        "    a = 1,",
        "// end",
        "    b = 2;",
        "}",
        "");
    setSelectionFromStartEndComments();
    createRefactoring();
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(
          RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_variable_declaration_fragment,
          msg);
    }
  }

  public void test_bad_while_conditionAndBody() throws Exception {
    setTestUnitContent(
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
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertEquals(RefactoringCoreMessages.StatementAnalyzer_while_expression_body, msg);
    }
  }

  public void test_singleExpression() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 1 + 2;",
        "}",
        "");
    selectionStart = findOffset("1");
    selectionEnd = findOffset("2;") + 1;
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = res();",
        "}",
        "int res() => 1 + 2;",
        "");
  }

  public void test_singleExpression_Dynamic() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "dynaFunction() {}",
        "main() {",
        "  int a = dynaFunction(); // marker",
        "}",
        "");
    selectionStart = findOffset("dynaFunction();");
    selectionEnd = findOffset("; // marker");
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "dynaFunction() {}",
        "main() {",
        "  int a = res(); // marker",
        "}",
        "res() => dynaFunction();",
        "");
  }

  public void test_singleExpression_occurrences() throws Exception {
    setTestUnitContent(
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
    doSuccessfullRefactoring();
    assertTestUnitContent(
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
        "int res(int v1, int v2) => v1 + v2;",
        "");
  }

  public void test_singleExpression_occurrences_disabled() throws Exception {
    setTestUnitContent(
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
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v1 = 1;",
        "  int v2 = 2;",
        "  int v3 = 3;",
        "  int a = res(v1, v2); // marker",
        "  int b = v2 + v3;",
        "}",
        "int res(int v1, int v2) => v1 + v2;",
        "");
  }

  public void test_singleExpression_occurrences_inClassOnly() throws Exception {
    setTestUnitContent(
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
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  myMethod() {",
        "    int v1 = 1;",
        "    int v2 = 2;",
        "    int positiveA = res(v1, v2); // marker",
        "  }",
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
    setTestUnitContent(
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
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v1 = 1;",
        "  int v2 = 2;",
        "  int positiveA = res(v1, v2); // marker",
        "}",
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

  public void test_singleExpression_staticContext_extractFromInitializer() throws Exception {
    setTestUnitContent(
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
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A(int v) {}",
        "}",
        "class B extends A {",
        "  B() : super(res()) {}",
        "  static int res() => 1 + 2;",
        "}",
        "");
  }

  public void test_singleExpression_staticContext_extractFromInstance() throws Exception {
    setTestUnitContent(
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
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  instanceMethodA() {",
        "    int v1 = 1;",
        "    int v2 = 2;",
        "    int positiveA = res(v1, v2); // marker",
        "  }",
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
    setTestUnitContent(
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
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static staticMethodA() {",
        "    int v1 = 1;",
        "    int v2 = 2;",
        "    int positiveA = res(v1, v2); // marker",
        "  }",
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
    setTestUnitContent(
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
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A(int v) {}",
        "}",
        "class B extends A {",
        "  B() : super(res()) {}",
        "  foo() {",
        "    print(res()); // marker",
        "  }",
        "  static int res() => 1 + 2;",
        "}",
        "");
  }

  public void test_singleExpression_usesParameter() throws Exception {
    setTestUnitContent(
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
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "fooA(int a1) {",
        "  int a2 = 2;",
        "  int a = res(a1, a2); // marker",
        "}",
        "int res(int a1, int a2) => a1 + a2;",
        "fooB(int b1) {",
        "  int b2 = 2;",
        "  int b = res(b1, b2);",
        "}",
        "");
  }

  public void test_singleExpression_withVariables() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v1 = 1;",
        "  int v2 = 2;",
        "  int a = v1 + v2 + v1; // marker",
        "}",
        "");
    selectionStart = findOffset("v1 +");
    selectionEnd = findOffset("; // marker");
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v1 = 1;",
        "  int v2 = 2;",
        "  int a = res(v1, v2); // marker",
        "}",
        "int res(int v1, int v2) => v1 + v2 + v1;",
        "");
  }

  public void test_singleExpression_withVariables_doRename() throws Exception {
    setTestUnitContent(
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
    // prepare
    prepareSuccessfullRefactoring();
    // update parameters
    {
      List<ParameterInfo> parameters = refactoring.getParameters();
      assertThat(parameters).hasSize(2);
      parameters.get(0).setNewName("par1");
      parameters.get(1).setNewName("param2");
    }
    // apply change
    performRefactoringChange();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v1 = 1;",
        "  int v2 = 2;",
        "  int v3 = 3;",
        "  int a = res(v1, v2); // marker",
        "  int b = res(v2, v3);",
        "}",
        "int res(int par1, int param2) => par1 + param2 + par1;",
        "");
  }

  public void test_singleExpression_withVariables_doReorder() throws Exception {
    setTestUnitContent(
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
    // prepare
    prepareSuccessfullRefactoring();
    // update parameters
    {
      List<ParameterInfo> parameters = refactoring.getParameters();
      assertThat(parameters).hasSize(2);
      ParameterInfo p = parameters.remove(1);
      parameters.add(0, p);
    }
    // apply change
    performRefactoringChange();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v1 = 1;",
        "  int v2 = 2;",
        "  int v3 = 3;",
        "  int a = res(v2, v1); // marker",
        "  int b = res(v3, v2);",
        "}",
        "int res(int v2, int v1) => v1 + v2;",
        "");
  }

  public void test_statements_changeIndentation() throws Exception {
    setTestUnitContent(
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
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  {",
        "// start",
        "    res();",
        "// end",
        "  }",
        "}",
        "void res() {",
        "  if (true) {",
        "    print(0);",
        "  }",
        "}",
        "");
  }

  public void test_statements_definesVariable_notUsedOutside() throws Exception {
    setTestUnitContent(
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
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 1;",
        "  int b = 1;",
        "// start",
        "  res(a, b);",
        "// end",
        "}",
        "void res(int a, int b) {",
        "  int v = a + b;",
        "  print(v);",
        "}",
        "");
  }

  public void test_statements_definesVariable_oneUsedOutside_assignment() throws Exception {
    setTestUnitContent(
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
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "myFunctionA() {",
        "  int a = 1;",
        "// start",
        "  int a = res(a);",
        "// end",
        "  print(a);",
        "}",
        "int res(int a) {",
        "  a += 10;",
        "  return a;",
        "}",
        "myFunctionB() {",
        "  int b = 2;",
        "  int b = res(b);",
        "  print(b);",
        "}",
        "");
  }

  public void test_statements_definesVariable_oneUsedOutside_declaration() throws Exception {
    setTestUnitContent(
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
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "myFunctionA() {",
        "  int a = 1;",
        "  int b = 2;",
        "// start",
        "  int v1 = res(a, b);",
        "// end",
        "  print(v1);",
        "}",
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
    setTestUnitContent(
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
    assertTrue(refactoringStatus.hasFatalError());
    {
      String msg = refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL);
      assertThat(msg).contains("varA");
      assertThat(msg).contains("varB");
    }
  }

  public void test_statements_duplicate_absolutelySame() throws Exception {
    setTestUnitContent(
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
    expectedNumberOfDuplicates = 1;
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "myFunctionA() {",
        "  res();",
        "}",
        "myFunctionB() {",
        "// start",
        "  res();",
        "// end",
        "}",
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
    setTestUnitContent(
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
    expectedNumberOfDuplicates = 1;
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "myFunctionA() {",
        "  res();",
        "}",
        "myFunctionB() {",
        "// start",
        "  res();",
        "// end",
        "}",
        "void res() {",
        "  int varB = 1;",
        "  print(varB);",
        "}",
        "");
  }

  /**
   * We should always add ";" when invoke method with extracted statements.
   */
  public void test_statements_endsWithBlock() throws Exception {
    setTestUnitContent(
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
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  res();",
        "// end",
        "}",
        "void res() {",
        "  if (true) {",
        "    print(0);",
        "  }",
        "}",
        "");
  }

  public void test_statements_noDuplicates() throws Exception {
    setTestUnitContent(
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
    expectedNumberOfDuplicates = 0;
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 1;",
        "  int b = 1;",
        "// start",
        "  res(a);",
        "// end",
        "}",
        "void res(int a) {",
        "  print(a);",
        "}",
        "");
  }

  /**
   * We have 3 identical statements, but select only 2. This should not cause problems.
   */
  public void test_statements_twoOfThree() throws Exception {
    setTestUnitContent(
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
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  res();",
        "// end",
        "  print(0);",
        "}",
        "void res() {",
        "  print(0);",
        "  print(0);",
        "}",
        "");
  }

  private void createRefactoring() throws Exception {
    createRefactoring("res");
  }

//  public void test_singleExpression_getter() throws Exception {
//    setTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "class A {",
//        "  int get foo() => 42;",
//        "}",
//        "main() {",
//        "  A a = new A();",
//        "  int b = 1 + a.foo; // marker",
//        "}");
//    selectionStart = findOffset("a.foo;");
//    selectionEnd = findOffset("; // marker");
//    doSuccessfullRefactoring();
//    assertTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "class A {",
//        "  int get foo() => 42;",
//        "}",
//        "main() {",
//        "  A a = new A();",
//        "  int res = a.foo;",
//        "  int b = 1 + res; // marker",
//        "}");
//  }
//
//  public void test_singleExpression_leadingNotWhitespace() throws Exception {
//    setTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "f() {",
//        "  int a = 12 + 345; // marker",
//        "}");
//    selectionStart = findOffset("345") - 2;
//    selectionEnd = findOffset("; // marker");
//    // check conditions
//    createRefactoring("res");
//    assert_fatalError_selection();
//  }
//
//  public void test_singleExpression_leadingWhitespace() throws Exception {
//    setTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "f() {",
//        "  int a = 1 + 2; // marker",
//        "}");
//    selectionStart = findOffset(" 1");
//    selectionEnd = findOffset("; // marker");
//    doSuccessfullRefactoring();
//    assertTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "f() {",
//        "  int res =  1 + 2;",
//        "  int a =res; // marker",
//        "}");
//  }
//
//  /**
//   * We use here knowledge how exactly <code>1 + 2 + 3 + 4</code> is parsed. We know that
//   * <code>1 + 2</code> will be separate and complete {@link DartBinaryExpression}, so can be
//   * handled as single expression.
//   */
//  public void test_singleExpression_partOfBinaryExpression() throws Exception {
//    setTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "f() {",
//        "  int a = 1 + 2 + 3 + 4;",
//        "}");
//    selectionStart = findOffset("1");
//    selectionEnd = findOffset("2 + ") + 1;
//    doSuccessfullRefactoring();
//    assertTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "f() {",
//        "  int res = 1 + 2;",
//        "  int a = res + 3 + 4;",
//        "}");
//  }
//
//  public void test_singleExpression_trailingComment() throws Exception {
//    setTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "f() {",
//        "  int a = 12 /*abc*/ + 345;",
//        "}");
//    selectionStart = findOffset("12");
//    selectionEnd = findOffset("*/ + 345") + 2;
//    doSuccessfullRefactoring();
//    assertTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "f() {",
//        "  int res = 12 /*abc*/;",
//        "  int a = res + 345;",
//        "}");
//  }
//
//  public void test_singleExpression_trailingNotWhitespace() throws Exception {
//    setTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "f() {",
//        "  int a = 12 + 345; // marker",
//        "}");
//    selectionStart = findOffset("12");
//    selectionEnd = findOffset(" 345");
//    // check conditions
//    createRefactoring("res");
//    assert_fatalError_selection();
//  }
//
//  public void test_singleExpression_trailingWhitespace() throws Exception {
//    setTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "f() {",
//        "  int a = 1 + 2 ; // marker",
//        "}");
//    selectionStart = findOffset("1");
//    selectionEnd = findOffset("; // marker");
//    doSuccessfullRefactoring();
//    assertTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "f() {",
//        "  int res = 1 + 2 ;",
//        "  int a = res; // marker",
//        "}");
//  }
//
//  /**
//   * Asserts that {@link refactoringStatus} has fatal error caused by selection.
//   */
//  private void assert_fatalError_selection() {
//    assertTrue(refactoringStatus.hasFatalError());
//    assertEquals(
//        "An expression must be selected to activate this refactoring.",
//        refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL));
//  }
//
//  private void assert_warning_alreadyDefined() {
//    assertTrue(refactoringStatus.hasWarning());
//    assertEquals(
//        "A variable with name 'res' is already defined in the visible scope.",
//        refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.WARNING));
//  }

  /**
   * Creates refactoring and checks all conditions.
   */
  private void createRefactoring(String name) throws Exception {
    int selectionLength = selectionEnd - selectionStart;
    refactoring = new ExtractMethodRefactoring(testUnit, selectionStart, selectionLength);
    refactoring.setMethodName(name);
    refactoring.setReplaceAllOccurrences(replaceAllOccurences);
    refactoringStatus = refactoring.checkAllConditions(pm);
    // just for coverage
    assertEquals(replaceAllOccurences, refactoring.getReplaceAllOccurrences());
  }

  private void doSuccessfullRefactoring() throws Exception {
    prepareSuccessfullRefactoring();
    performRefactoringChange();
  }

  private void performRefactoringChange() throws Exception {
    TestProject.waitForAutoBuild();
    ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        Change change = refactoring.createChange(pm);
        change.initializeValidationData(pm);
        new PerformChangeOperation(change).run(pm);
      }
    }, null);
    TestProject.waitForAutoBuild();
  }

  private void prepareSuccessfullRefactoring() throws Exception {
    createRefactoring();
    // OK status
    if (!refactoringStatus.isOK()) {
      fail(refactoringStatus.toString());
    }
    // may be check number of duplicates
    if (expectedNumberOfDuplicates != -1) {
      assertEquals(expectedNumberOfDuplicates, refactoring.getNumberOfDuplicates());
    }
  }

  private void setSelectionFromStartEndComments() throws Exception {
    selectionStart = findOffset("// start") + "// start".length() + "\n".length();
    selectionEnd = findOffset("// end");
  }
}
