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
import com.google.dart.tools.ui.refactoring.AbstractDartTest;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;
import com.google.dart.tools.ui.text.dart.IProblemLocation;
import com.google.dart.tools.ui.text.dart.IQuickAssistProcessor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link QuickAssistProcessor}.
 */
public final class QuickAssistProcessorTest extends AbstractDartTest {
  private static final IQuickAssistProcessor PROCESSOR = new QuickAssistProcessor();
  private static final IProblemLocation[] NO_PROBLEMS = new IProblemLocation[0];

  /**
   * @return <code>true</code> if given {@link IDartCompletionProposal} has required name.
   */
  private static boolean isProposal(IDartCompletionProposal proposal, String requiredName) {
    String proposalName = proposal.getDisplayString();
    return requiredName.equals(proposalName);
  }

  private IProblemLocation problemLocations[] = NO_PROBLEMS;
  private int selectionLength = 0;

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

  /**
   * Asserts that running proposal with given name produces expected source.
   */
  private void assert_runProcessor(
      String proposalName,
      String initialSource,
      String offsetPattern,
      String expectedSource) throws Exception {
    // set initial source
    setTestUnitContent(initialSource);
    // just to get coverage
    PROCESSOR.hasAssists(null);
    // prepare proposals
    int offset = findOffset(offsetPattern);
    AssistContext context = new AssistContext(testUnit, offset, selectionLength);
    IDartCompletionProposal[] proposals = PROCESSOR.getAssists(context, problemLocations);
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
}
