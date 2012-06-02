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

  /**
   * We should go up only until we have same operator.
   */
  public void test_exchangeBinaryExpressionArguments_OK_extended_mixOperator_1() throws Exception {
    assert_exchangeBinaryExpressionArguments_success("1 * 2 * 3 + 4", "* 2", 0, "2 * 3 * 1 + 4");
  }

  /**
   * We should go up only until we have same operator.
   */
  public void test_exchangeBinaryExpressionArguments_OK_extended_mixOperator_2() throws Exception {
    assert_exchangeBinaryExpressionArguments_success("1 + 2 - 3 + 4", "+ 2", 0, "2 + 1 - 3 + 4");
  }

  /**
   * Even if as AST level we have tree of "+" expressions, for user this is single expression. So,
   * exchange should happen correctly (from user POV) at any point.
   */
  public void test_exchangeBinaryExpressionArguments_OK_extended_sameOperator_afterFirst()
      throws Exception {
    assert_exchangeBinaryExpressionArguments_success("1 + 2 + 3", "+ 2", 0, "2 + 3 + 1");
  }

  /**
   * Even if as AST level we have tree of "+" expressions, for user this is single expression. So,
   * exchange should happen correctly (from user POV) at any point.
   */
  public void test_exchangeBinaryExpressionArguments_OK_extended_sameOperator_afterSecond()
      throws Exception {
    assert_exchangeBinaryExpressionArguments_success("1 + 2 + 3", "+ 3", 0, "3 + 1 + 2");
  }

  public void test_exchangeBinaryExpressionArguments_OK_simple_afterOperator() throws Exception {
    assert_exchangeBinaryExpressionArguments_success("1 + 2", " 2", 0, "2 + 1");
  }

  public void test_exchangeBinaryExpressionArguments_OK_simple_beforeOperator() throws Exception {
    assert_exchangeBinaryExpressionArguments_success("1 + 2", "+ 2", 0, "2 + 1");
  }

  public void test_exchangeBinaryExpressionArguments_OK_simple_fullSelection() throws Exception {
    assert_exchangeBinaryExpressionArguments_success("1 + 2", "1 + 2", 5, "2 + 1");
  }

  public void test_exchangeBinaryExpressionArguments_OK_simple_withLength() throws Exception {
    assert_exchangeBinaryExpressionArguments_success("1 + 2", "+ 2", 2, "2 + 1");
  }

  public void test_exchangeBinaryExpressionArguments_wrong_errorAtLocation() throws Exception {
    // prepare IProblemLocation stub
    IProblemLocation problemLocation = mock(IProblemLocation.class);
    when(problemLocation.isError()).thenReturn(true);
    problemLocations = new IProblemLocation[] {problemLocation};
    // run proposal
    assert_exchangeBinaryExpressionArguments_wrong("1 + unknown", "+ unknown", 0);
  }

  public void test_exchangeBinaryExpressionArguments_wrong_extraLength() throws Exception {
    assert_exchangeBinaryExpressionArguments_wrong("111 + 222", "+ 222", 3);
  }

  public void test_exchangeBinaryExpressionArguments_wrong_onOperand() throws Exception {
    assert_exchangeBinaryExpressionArguments_wrong("111 + 222", "11 +", 0);
  }

  public void test_exchangeBinaryExpressionArguments_wrong_selectionWithBinary() throws Exception {
    assert_exchangeBinaryExpressionArguments_wrong("1 + 2 + 3", "1 + 2 + 3", 9);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    waitEventLoop(0);
  }

  /**
   * Validates {@link CorrectionMessages#QuickAssistProcessor_exchangeOperands}.
   */
  private void assert_exchangeBinaryExpressionArguments_success(
      String initialExpression,
      String offsetPattern,
      int length,
      String expectedExpression) throws Exception {
    // set initial source
    String initialSource = "var v = " + initialExpression + ";";
    setTestUnitContent(initialSource);
    // just to get coverage
    PROCESSOR.hasAssists(null);
    // prepare proposals
    int offset = findOffset(offsetPattern);
    AssistContext context = new AssistContext(testUnit, offset, length);
    IDartCompletionProposal[] proposals = PROCESSOR.getAssists(context, problemLocations);
    // find and apply required proposal
    String result = initialSource;
    for (IDartCompletionProposal proposal : proposals) {
      if (isProposal(proposal, CorrectionMessages.QuickAssistProcessor_exchangeOperands)) {
        result = ((CUCorrectionProposal) proposal).getPreviewContent();
      }
    }
    // assert result
    String expectedSource = "var v = " + expectedExpression + ";";
    assertEquals(expectedSource, result);
  }

  private void assert_exchangeBinaryExpressionArguments_wrong(
      String expression,
      String offsetPattern,
      int offsetDelta) throws Exception {
    assert_exchangeBinaryExpressionArguments_success(
        expression,
        offsetPattern,
        offsetDelta,
        expression);
  }
}
