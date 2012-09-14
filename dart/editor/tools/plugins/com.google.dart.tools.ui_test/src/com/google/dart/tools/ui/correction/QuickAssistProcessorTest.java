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
package com.google.dart.tools.ui.correction;

import com.google.dart.tools.ui.internal.text.correction.AssistContext;
import com.google.dart.tools.ui.internal.text.correction.CorrectionMessages;
import com.google.dart.tools.ui.internal.text.correction.QuickAssistProcessor;
import com.google.dart.tools.ui.internal.text.correction.proposals.CUCorrectionProposal;
import com.google.dart.tools.ui.internal.text.editor.CompilationUnitEditor;
import com.google.dart.tools.ui.refactoring.AbstractDartTest;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;
import com.google.dart.tools.ui.text.dart.IProblemLocation;
import com.google.dart.tools.ui.text.dart.IQuickAssistProcessor;

import org.eclipse.jface.text.source.ISourceViewer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link QuickAssistProcessor}.
 */
public final class QuickAssistProcessorTest extends AbstractDartTest {
  private static final IQuickAssistProcessor PROCESSOR = new QuickAssistProcessor();
  private static final IProblemLocation[] NO_PROBLEMS = new IProblemLocation[0];

  private static void assertHasProposal(IDartCompletionProposal[] proposals, String proposalName) {
    for (IDartCompletionProposal proposal : proposals) {
      if (proposal.getDisplayString().equals(proposalName)) {
        return;
      }
    }
    fail("Expected: " + proposalName);
  }

  private static void assertNoProposal(IDartCompletionProposal[] proposals, String proposalName) {
    for (IDartCompletionProposal proposal : proposals) {
      if (proposal.getDisplayString().equals(proposalName)) {
        fail("Not expected: " + proposalName);
      }
    }
  }

  /**
   * @return <code>true</code> if given {@link IDartCompletionProposal} has required name.
   */
  private static boolean isProposal(IDartCompletionProposal proposal, String requiredName) {
    String proposalName = proposal.getDisplayString();
    return requiredName.equals(proposalName);
  }

  private IProblemLocation problemLocations[] = NO_PROBLEMS;

  private int selectionLength = 0;

  public void test_addTypeAnnotation_classField_OK_final() throws Exception {
    assert_addTypeAnnotation_classField("final v = 1;", " = 1", "final int v = 1;");
  }

  public void test_addTypeAnnotation_classField_OK_int() throws Exception {
    assert_addTypeAnnotation_classField("var v = 1;", " = 1", "int v = 1;");
  }

  public void test_addTypeAnnotation_local_OK_int() throws Exception {
    assert_addTypeAnnotation_localVariable("var v = 1;", " = 1", "int v = 1;");
  }

  public void test_addTypeAnnotation_local_OK_List() throws Exception {
    assert_addTypeAnnotation_localVariable(
        "var v = new List<String>();",
        " = new",
        "List<String> v = new List<String>();");
  }

  public void test_addTypeAnnotation_local_OK_onInitializer() throws Exception {
    assert_addTypeAnnotation_localVariable("var v = 123;", "23;", "int v = 123;");
  }

  public void test_addTypeAnnotation_local_OK_onName() throws Exception {
    assert_addTypeAnnotation_localVariable("var abc = 1;", "bc ", "int abc = 1;");
  }

  public void test_addTypeAnnotation_local_OK_onVar() throws Exception {
    assert_addTypeAnnotation_localVariable("var v = 1;", "var ", "int v = 1;");
  }

  public void test_addTypeAnnotation_local_wrong_multiple() throws Exception {
    String source = "var a = 1, b = '';";
    assert_addTypeAnnotation_localVariable(source, "var ", source);
  }

  public void test_addTypeAnnotation_local_wrong_null() throws Exception {
    String source = "var v = null;";
    assert_addTypeAnnotation_localVariable(source, " = null", source);
  }

  public void test_addTypeAnnotation_local_wrong_unknown() throws Exception {
    String source = "var v = unknownVar;";
    assert_addTypeAnnotation_localVariable(source, " = unknown", source);
  }

  public void test_addTypeAnnotation_topLevelField_OK_int() throws Exception {
    assert_addTypeAnnotation_topLevelField("var v = 1;", " = 1", "int v = 1;");
  }

  public void test_addTypeAnnotation_topLevelField_OK_onVar() throws Exception {
    assert_addTypeAnnotation_topLevelField("var v = 1;", "var", "int v = 1;");
  }

  public void test_addTypeAnnotation_topLevelField_wrong_multiple() throws Exception {
    String source = "var a = 1, b = '';";
    assert_addTypeAnnotation_topLevelField(source, "var ", source);
  }

  public void test_addTypeAnnotation_topLevelField_wrong_noValue() throws Exception {
    String source = "var v;";
    assert_addTypeAnnotation_topLevelField(source, "var ", source);
  }

  public void test_convertMethodToGetterRefactoring_OK() throws Exception {
    String source = "int test() => 42;";
    String offsetPattern = "test()";
    assert_convertMethodToGetterRefactoring(source, offsetPattern, true);
  }

  public void test_convertMethodToGetterRefactoring_wrong_hasParameters() throws Exception {
    String source = "int test(p) => 42;";
    String offsetPattern = "test(p)";
    assert_convertMethodToGetterRefactoring(source, offsetPattern, false);
  }

  public void test_convertMethodToGetterRefactoring_wrong_notFunction() throws Exception {
    String source = "class AAA {}";
    String offsetPattern = "AAA {}";
    assert_convertMethodToGetterRefactoring(source, offsetPattern, false);
  }

  public void test_convertOptionalParametersToNamedRefactoring_OK() throws Exception {
    String source = "test(a, [b = 2]) {}";
    String offsetPattern = "test(a";
    assert_convertOptionalParametersToNamedRefactoring(source, offsetPattern, true);
  }

  public void test_convertOptionalParametersToNamedRefactoring_wrong_noOptional() throws Exception {
    String source = "test(a) {}";
    String offsetPattern = "test(a";
    assert_convertOptionalParametersToNamedRefactoring(source, offsetPattern, false);
  }

  public void test_convertOptionalParametersToNamedRefactoring_wrong_notFunction() throws Exception {
    String source = "class AAA {}";
    String offsetPattern = "AAA {}";
    assert_convertOptionalParametersToNamedRefactoring(source, offsetPattern, false);
  }

  public void test_convertOptionalParametersToNamedRefactoring_wrong_notOptionalPositional()
      throws Exception {
    String source = "test(a, {b: 2}) {}";
    String offsetPattern = "test(a";
    assert_convertOptionalParametersToNamedRefactoring(source, offsetPattern, false);
  }

  public void test_convertToBlockBody_OK_onMethodName() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "fff() => 123;");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "fff() {",
        "  return 123;",
        "}");
    assert_convertToBlockBody(initial, "fff() ", expected);
  }

  public void test_convertToBlockBody_OK_onValue() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() => 123;");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  return 123;",
        "}");
    assert_convertToBlockBody(initial, "23;", expected);
  }

  public void test_convertToBlockBody_wrong_noEnclosingFunction() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var v = 123;");
    assert_convertToBlockBody_wrong(initial, "v = 123");
  }

  public void test_convertToBlockBody_wrong_notExpressionBlock() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  return 123;",
        "}");
    assert_convertToBlockBody_wrong(initial, "return 123;");
  }

  public void test_convertToExpressionBody_OK_onBlock() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() { // marker",
        "  return 0;",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() => 0;");
    assert_convertToExpressionBody(initial, "{ // marker", expected);
  }

  public void test_convertToExpressionBody_OK_onMethodName() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "fff() {",
        "  return 0;",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "fff() => 0;");
    assert_convertToExpressionBody(initial, "ff() {", expected);
  }

  public void test_convertToExpressionBody_OK_onReturnStatement() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  return 0;",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() => 0;");
    assert_convertToExpressionBody(initial, "return 0;", expected);
  }

  public void test_convertToExpressionBody_wrong_already() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() => 0;");
    assertNoProposal(
        initial,
        "f()",
        CorrectionMessages.QuickAssistProcessor_convertToExpressionBody);
  }

  public void test_convertToExpressionBody_wrong_moreThanOneStatement() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var v1 = 1;",
        "  var v2 = 2;",
        "}");
    assert_convertToExpressionBody_wrong(initial, "v1 = 1");
  }

  public void test_convertToExpressionBody_wrong_noEnclosingFunction() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var v = 0;");
    assert_convertToExpressionBody_wrong(initial, "v = 0");
  }

  public void test_convertToExpressionBody_wrong_noReturn() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var v = 0;",
        "}");
    assert_convertToExpressionBody_wrong(initial, "v = 0");
  }

  public void test_convertToExpressionBody_wrong_noReturnValue() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  return;",
        "}");
    assert_convertToExpressionBody_wrong(initial, "return;");
  }

  /**
   * We should go up only until we have same operator.
   */
  public void test_exchangeBinaryExpressionArguments_OK_extended_mixOperator_1() throws Exception {
    assert_exchangeBinaryExpressionArguments_success("1 * 2 * 3 + 4", "* 2", "2 * 3 * 1 + 4");
  }

  /**
   * We should go up only until we have same operator.
   */
  public void test_exchangeBinaryExpressionArguments_OK_extended_mixOperator_2() throws Exception {
    assert_exchangeBinaryExpressionArguments_success("1 + 2 - 3 + 4", "+ 2", "2 + 1 - 3 + 4");
  }

  /**
   * Even if as AST level we have tree of "+" expressions, for user this is single expression. So,
   * exchange should happen correctly (from user POV) at any point.
   */
  public void test_exchangeBinaryExpressionArguments_OK_extended_sameOperator_afterFirst()
      throws Exception {
    assert_exchangeBinaryExpressionArguments_success("1 + 2 + 3", "+ 2", "2 + 3 + 1");
  }

  /**
   * Even if as AST level we have tree of "+" expressions, for user this is single expression. So,
   * exchange should happen correctly (from user POV) at any point.
   */
  public void test_exchangeBinaryExpressionArguments_OK_extended_sameOperator_afterSecond()
      throws Exception {
    assert_exchangeBinaryExpressionArguments_success("1 + 2 + 3", "+ 3", "3 + 1 + 2");
  }

  public void test_exchangeBinaryExpressionArguments_OK_simple_afterOperator() throws Exception {
    assert_exchangeBinaryExpressionArguments_success("1 + 2", " 2", "2 + 1");
  }

  public void test_exchangeBinaryExpressionArguments_OK_simple_beforeOperator() throws Exception {
    assert_exchangeBinaryExpressionArguments_success("1 + 2", "+ 2", "2 + 1");
  }

  public void test_exchangeBinaryExpressionArguments_OK_simple_fullSelection() throws Exception {
    selectionLength = 5;
    assert_exchangeBinaryExpressionArguments_success("1 + 2", "1 + 2", "2 + 1");
  }

  public void test_exchangeBinaryExpressionArguments_OK_simple_withLength() throws Exception {
    selectionLength = 2;
    assert_exchangeBinaryExpressionArguments_success("1 + 2", "+ 2", "2 + 1");
  }

  public void test_exchangeBinaryExpressionArguments_wrong_errorAtLocation() throws Exception {
    // prepare IProblemLocation stub
    IProblemLocation problemLocation = mock(IProblemLocation.class);
    when(problemLocation.isError()).thenReturn(true);
    problemLocations = new IProblemLocation[] {problemLocation};
    // run proposal
    assert_exchangeBinaryExpressionArguments_wrong("1 + unknown", "+ unknown");
  }

  public void test_exchangeBinaryExpressionArguments_wrong_extraLength() throws Exception {
    selectionLength = 3;
    assert_exchangeBinaryExpressionArguments_wrong("111 + 222", "+ 222");
  }

  public void test_exchangeBinaryExpressionArguments_wrong_onOperand() throws Exception {
    assert_exchangeBinaryExpressionArguments_wrong("111 + 222", "11 +");
  }

  public void test_exchangeBinaryExpressionArguments_wrong_selectionWithBinary() throws Exception {
    selectionLength = 9;
    assert_exchangeBinaryExpressionArguments_wrong("1 + 2 + 3", "1 + 2 + 3");
  }

  public void test_joinVariableDeclaration_OK() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var v;",
        "  v = 1;",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var v = 1;",
        "}");
    assert_joinVariableDeclaration(initial, "v ", expected);
  }

  public void test_joinVariableDeclaration_wrong_notAdjacent() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var v;",
        "  var bar;",
        "  v = 1;",
        "}");
    assert_joinVariableDeclaration_wrong(initial, "v =");
  }

  public void test_joinVariableDeclaration_wrong_notAssignment() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var v;",
        "  v + 1;",
        "}");
    assert_joinVariableDeclaration_wrong(initial, "v +");
  }

  public void test_joinVariableDeclaration_wrong_notDeclaration() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f(var v) {",
        "  v = 1;",
        "}");
    assert_joinVariableDeclaration_wrong(initial, "v =");
  }

  public void test_joinVariableDeclaration_wrong_notLeftArgument() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var v;",
        "  1 + v; // marker",
        "}");
    assert_joinVariableDeclaration_wrong(initial, "v; // marker");
  }

  public void test_joinVariableDeclaration_wrong_notOneVariable() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var v, v2;",
        "  v = 1;",
        "}");
    assert_joinVariableDeclaration_wrong(initial, "v =");
  }

  public void test_joinVariableDeclaration_wrong_notSameBlock() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var v;",
        "  {",
        "    v = 1;",
        "  }",
        "}");
    assert_joinVariableDeclaration_wrong(initial, "v =");
  }

  public void test_removeTypeAnnotation_classField_OK() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int v = 1;",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var v = 1;",
        "}");
    assert_removeTypeAnnotation(initial, "int ", expected);
  }

  public void test_removeTypeAnnotation_local_OK_multiple() throws Exception {
    assert_removeTypeAnnotation_localVariable("int a = 1, b = 2;", "int ", "var a = 1, b = 2;");
  }

  public void test_removeTypeAnnotation_local_OK_single() throws Exception {
    assert_removeTypeAnnotation_localVariable("int v = 1;", "int ", "var v = 1;");
  }

  public void test_removeTypeAnnotation_topLevelField_OK() throws Exception {
    assert_removeTypeAnnotation("int v = 1;", "int ", "var v = 1;");
  }

  public void test_renameRefactoring_OK() throws Exception {
    String initialSource = "var test;";
    String offsetPattern = "test;";
    assertHasProposal(
        initialSource,
        offsetPattern,
        CorrectionMessages.RenameRefactoringProposal_name);
  }

  public void test_renameRefactoring_wrong_emptyUnit() throws Exception {
    String initialSource = "";
    String offsetPattern = "";
    assertNoProposal(
        initialSource,
        offsetPattern,
        CorrectionMessages.RenameRefactoringProposal_name);
  }

  public void test_replaceConditionalWithIfElse_OK_assignment() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int vvv;",
        "  vvv = true ? 111 : 222;",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int vvv;",
        "  if (true) {",
        "    vvv = 111;",
        "  } else {",
        "    vvv = 222;",
        "  }",
        "}");
    // on conditional
    assert_replaceConditionalWithIfElse(initial, "11 :", expected);
    // on variable
    assert_replaceConditionalWithIfElse(initial, "vv =", expected);
  }

  public void test_replaceConditionalWithIfElse_OK_return() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  return true ? 111 : 222;",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  if (true) {",
        "    return 111;",
        "  } else {",
        "    return 222;",
        "  }",
        "}");
    // on conditional
    assert_replaceConditionalWithIfElse(initial, "11 :", expected);
    // on statement
    assert_replaceConditionalWithIfElse(initial, "return ", expected);
  }

  public void test_replaceConditionalWithIfElse_OK_variableDeclaration() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1, vvv = true ? 111 : 222, b = 2;",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1, vvv, b = 2;",
        "  if (true) {",
        "    vvv = 111;",
        "  } else {",
        "    vvv = 222;",
        "  }",
        "}");
    // on conditional
    assert_replaceConditionalWithIfElse(initial, "11 :", expected);
    // on variable
    assert_replaceConditionalWithIfElse(initial, "vv =", expected);
    // on statement
    assert_replaceConditionalWithIfElse(initial, "int ", expected);
  }

  public void test_replaceIfElseWithConditional_OK_assignment() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int vvv;",
        "  if (true) {",
        "    vvv = 111;",
        "  } else {",
        "    vvv = 222;",
        "  }",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int vvv;",
        "  vvv = true ? 111 : 222;",
        "}");
    assert_replaceIfElseWithConditional(initial, "if (true)", expected);
  }

  public void test_replaceIfElseWithConditional_OK_return() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  if (true) {",
        "    return 111;",
        "  } else {",
        "    return 222;",
        "  }",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  return true ? 111 : 222;",
        "}");
    assert_replaceIfElseWithConditional(initial, "if (true)", expected);
  }

  public void test_replaceIfElseWithConditional_wrong_notIfStatement() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  print(0);",
        "}");
    assert_replaceIfElseWithConditional_wrong(initial, "print(0)");
  }

  public void test_replaceIfElseWithConditional_wrong_notSingleStatememt() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int vvv;",
        "  if (true) {",
        "    print(0);",
        "    vvv = 111;",
        "  } else {",
        "    print(0);",
        "    vvv = 222;",
        "  }",
        "}");
    assert_replaceIfElseWithConditional_wrong(initial, "if (true)");
  }

  public void test_splitAndCondition_OK_innerAndExpression() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1 && 2 == 2 && 3 == 3) {",
        "    print(0);",
        "  }",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1) {",
        "    if (2 == 2 && 3 == 3) {",
        "      print(0);",
        "    }",
        "  }",
        "}");
    assert_splitAndCondition(initial, "&& 2 == 2", expected);
  }

  public void test_splitAndCondition_OK_thenBlock() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true && false) {",
        "    print(0);",
        "    if (3 == 3) {",
        "      print(1);",
        "    }",
        "  }",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true) {",
        "    if (false) {",
        "      print(0);",
        "      if (3 == 3) {",
        "        print(1);",
        "      }",
        "    }",
        "  }",
        "}");
    assert_splitAndCondition(initial, "&& false)", expected);
  }

  public void test_splitAndCondition_OK_thenBlock_elseBlock() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true && false) {",
        "    print(0);",
        "  } else {",
        "    print(1);",
        "    if (2 == 2) {",
        "      print(2);",
        "    }",
        "  }",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true) {",
        "    if (false) {",
        "      print(0);",
        "    } else {",
        "      print(1);",
        "      if (2 == 2) {",
        "        print(2);",
        "      }",
        "    }",
        "  }",
        "}");
    assert_splitAndCondition(initial, "&& false)", expected);
  }

  public void test_splitAndCondition_OK_thenStatement() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true && false)",
        "    print(0);",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true)",
        "    if (false)",
        "      print(0);",
        "}");
    assert_splitAndCondition(initial, "&& false)", expected);
  }

  public void test_splitAndCondition_OK_thenStatement_elseStatement() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true && false)",
        "    print(0);",
        "  else",
        "    print(1);",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true)",
        "    if (false)",
        "      print(0);",
        "    else",
        "      print(1);",
        "}");
    assert_splitAndCondition(initial, "&& false)", expected);
  }

  public void test_splitAndCondition_wrong() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1 && 2 == 2) {",
        "    print(0);",
        "  }",
        "  print(3 == 3 && 4 == 4);",
        "}");
    // not binary expression
    assert_splitAndCondition_wrong(initial, "main() {");
    // selection is not empty and includes more than just operator
    {
      selectionLength = 5;
      assert_splitAndCondition_wrong(initial, "&& 2 == 2");
      selectionLength = 0;
    }
  }

  public void test_splitAndCondition_wrong_notPartOfIf() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(1 == 1 && 2 == 2);",
        "}");
    assert_splitAndCondition_wrong(initial, "&& 2");
  }

  public void test_splitAndCondition_wrong_notTopLevelAnd() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true || (1 == 1 && 2 == 2)) {",
        "    print(0);",
        "  }",
        "  if (true && (3 == 3 && 4 == 4)) {",
        "    print(0);",
        "  }",
        "}");
    assert_splitAndCondition_wrong(initial, "&& 2");
    assert_splitAndCondition_wrong(initial, "&& 4");
  }

  public void test_splitVariableDeclaration_OK() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var v = 1;",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var v;",
        "  v = 1;",
        "}");
    assert_splitVariableDeclaration(initial, "v ", expected);
  }

  public void test_splitVariableDeclaration_wrong_notOneVariable() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var v = 1, v2;",
        "}");
    assert_splitVariableDeclaration_wrong(initial, "v = 1");
  }

  @Override
  protected void tearDown() throws Exception {
    waitEventLoop(0);
    super.tearDown();
    waitEventLoop(0);
  }

  private void assert_addTypeAnnotation(
      String initialSource,
      String offsetPattern,
      String expectedSource) throws Exception {
    assert_runProcessor(
        CorrectionMessages.QuickAssistProcessor_addTypeAnnotation,
        initialSource,
        offsetPattern,
        expectedSource);
  }

  private void assert_addTypeAnnotation_classField(
      String initialDeclaration,
      String offsetPattern,
      String expectedDeclaration) throws Exception {
    String initialSource = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  " + initialDeclaration,
        "}",
        "");
    String expectedSource = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  " + expectedDeclaration,
        "}",
        "");
    assert_addTypeAnnotation(initialSource, offsetPattern, expectedSource);
  }

  private void assert_addTypeAnnotation_localVariable(
      String initialStatement,
      String offsetPattern,
      String expectedStatement) throws Exception {
    String initialSource = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  " + initialStatement,
        "}",
        "");
    String expectedSource = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  " + expectedStatement,
        "}",
        "");
    assert_addTypeAnnotation(initialSource, offsetPattern, expectedSource);
  }

  private void assert_addTypeAnnotation_topLevelField(
      String initialDeclaration,
      String offsetPattern,
      String expectedDeclaration) throws Exception {
    String initialSource = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        initialDeclaration,
        "");
    String expectedSource = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        expectedDeclaration,
        "");
    assert_addTypeAnnotation(initialSource, offsetPattern, expectedSource);
  }

  private void assert_convertMethodToGetterRefactoring(
      String source,
      String offsetPattern,
      boolean expected) throws Exception {
    String name = CorrectionMessages.ConvertMethodToGetterRefactoringProposal_name;
    if (expected) {
      assertHasProposal(source, offsetPattern, name);
    } else {
      assertNoProposal(source, offsetPattern, name);
    }
  }

  private void assert_convertOptionalParametersToNamedRefactoring(
      String source,
      String offsetPattern,
      boolean expected) throws Exception {
    String name = CorrectionMessages.ConvertOptionalParametersToNamedRefactoringProposal_name;
    if (expected) {
      assertHasProposal(source, offsetPattern, name);
    } else {
      assertNoProposal(source, offsetPattern, name);
    }
  }

  private void assert_convertToBlockBody(
      String initialSource,
      String offsetPattern,
      String expectedSource) throws Exception {
    assert_runProcessor(
        CorrectionMessages.QuickAssistProcessor_convertToBlockBody,
        initialSource,
        offsetPattern,
        expectedSource);
  }

  private void assert_convertToBlockBody_wrong(String initialSource, String offsetPattern)
      throws Exception {
    assert_convertToBlockBody(initialSource, offsetPattern, initialSource);
  }

  private void assert_convertToExpressionBody(
      String initialSource,
      String offsetPattern,
      String expectedSource) throws Exception {
    assert_runProcessor(
        CorrectionMessages.QuickAssistProcessor_convertToExpressionBody,
        initialSource,
        offsetPattern,
        expectedSource);
  }

  private void assert_convertToExpressionBody_wrong(String initialSource, String offsetPattern)
      throws Exception {
    assert_convertToExpressionBody(initialSource, offsetPattern, initialSource);
  }

  private void assert_exchangeBinaryExpressionArguments_success(
      String initialExpression,
      String offsetPattern,
      String expectedExpression) throws Exception {
    String initialSource = "var v = " + initialExpression + ";";
    String expectedSource = "var v = " + expectedExpression + ";";
    assert_runProcessor(
        CorrectionMessages.QuickAssistProcessor_exchangeOperands,
        initialSource,
        offsetPattern,
        expectedSource);
  }

  private void assert_exchangeBinaryExpressionArguments_wrong(
      String expression,
      String offsetPattern) throws Exception {
    assert_exchangeBinaryExpressionArguments_success(expression, offsetPattern, expression);
  }

  private void assert_joinVariableDeclaration(
      String initialSource,
      String offsetPattern,
      String expectedSource) throws Exception {
    assert_runProcessor(
        CorrectionMessages.QuickAssistProcessor_joinVariableDeclaration,
        initialSource,
        offsetPattern,
        expectedSource);
  }

  private void assert_joinVariableDeclaration_wrong(String expression, String offsetPattern)
      throws Exception {
    assert_joinVariableDeclaration(expression, offsetPattern, expression);
  }

  private void assert_removeTypeAnnotation(
      String initialSource,
      String offsetPattern,
      String expectedSource) throws Exception {
    assert_runProcessor(
        CorrectionMessages.QuickAssistProcessor_removeTypeAnnotation,
        initialSource,
        offsetPattern,
        expectedSource);
  }

  private void assert_removeTypeAnnotation_localVariable(
      String initialStatement,
      String offsetPattern,
      String expectedStatement) throws Exception {
    String initialSource = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  " + initialStatement,
        "}",
        "");
    String expectedSource = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  " + expectedStatement,
        "}",
        "");
    assert_removeTypeAnnotation(initialSource, offsetPattern, expectedSource);
  }

  private void assert_replaceConditionalWithIfElse(
      String initialSource,
      String offsetPattern,
      String expectedSource) throws Exception {
    assert_runProcessor(
        CorrectionMessages.QuickAssistProcessor_replaceConditionalWithIfElse,
        initialSource,
        offsetPattern,
        expectedSource);
  }

  private void assert_replaceIfElseWithConditional(
      String initialSource,
      String offsetPattern,
      String expectedSource) throws Exception {
    assert_runProcessor(
        CorrectionMessages.QuickAssistProcessor_replaceIfElseWithConditional,
        initialSource,
        offsetPattern,
        expectedSource);
  }

  private void assert_replaceIfElseWithConditional_wrong(String initialSource, String offsetPattern)
      throws Exception {
    assert_replaceIfElseWithConditional(initialSource, offsetPattern, initialSource);
  }

  /**
   * Asserts that running proposal with given name produces expected source.
   */
  private void assert_runProcessor(
      String proposalName,
      String initialSource,
      String offsetPattern,
      String expectedSource) throws Exception {
    // XXX used to see coverage of only one quick assist
//    if (!proposalName.equals(CorrectionMessages.QuickAssistProcessor_splitAndCondition)) {
//      return;
//    }
    IDartCompletionProposal[] proposals = getProposals(initialSource, offsetPattern);
    // find and apply required proposal
    String result = initialSource;
    for (IDartCompletionProposal proposal : proposals) {
      if (isProposal(proposal, proposalName)) {
        result = ((CUCorrectionProposal) proposal).getPreviewContent();
      }
    }
    // assert result
    assertEquals(expectedSource, result);
  }

  private void assert_splitAndCondition(
      String initialSource,
      String offsetPattern,
      String expectedSource) throws Exception {
    assert_runProcessor(
        CorrectionMessages.QuickAssistProcessor_splitAndCondition,
        initialSource,
        offsetPattern,
        expectedSource);
  }

  private void assert_splitAndCondition_wrong(String initialSource, String offsetPattern)
      throws Exception {
    assert_splitAndCondition(initialSource, offsetPattern, initialSource);
  }

  private void assert_splitVariableDeclaration(
      String initialSource,
      String offsetPattern,
      String expectedSource) throws Exception {
    assert_runProcessor(
        CorrectionMessages.QuickAssistProcessor_splitVariableDeclaration,
        initialSource,
        offsetPattern,
        expectedSource);
  }

  private void assert_splitVariableDeclaration_wrong(String initialSource, String offsetPattern)
      throws Exception {
    assert_splitVariableDeclaration(initialSource, offsetPattern, initialSource);
  }

  private void assertHasProposal(String initialSource, String offsetPattern, String proposalName)
      throws Exception {
    IDartCompletionProposal[] proposals = getProposals(initialSource, offsetPattern);
    assertHasProposal(proposals, proposalName);
  }

  private void assertNoProposal(String initial, String offsetPattern, String proposalName)
      throws Exception {
    IDartCompletionProposal[] proposals = getProposals(initial, offsetPattern);
    assertNoProposal(proposals, proposalName);
  }

  private IDartCompletionProposal[] getProposals(String source, String offsetPattern)
      throws Exception {
    // set initial source
    setTestUnitContent(source);
    // just to get coverage
    PROCESSOR.hasAssists(null);
    // prepare proposals
    int offset = findOffset(offsetPattern);
    AssistContext context = new AssistContext(
        testUnit,
        (ISourceViewer) null,
        new CompilationUnitEditor(),
        offset,
        selectionLength);
    return PROCESSOR.getAssists(context, problemLocations);
  }
}
