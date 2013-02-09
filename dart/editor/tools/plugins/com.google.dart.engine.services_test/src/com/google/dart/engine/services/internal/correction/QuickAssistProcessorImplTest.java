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

package com.google.dart.engine.services.internal.correction;

import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.correction.CorrectionProcessors;
import com.google.dart.engine.services.correction.CorrectionProposal;
import com.google.dart.engine.services.correction.QuickAssistProcessor;
import com.google.dart.engine.services.correction.SourceChange;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class QuickAssistProcessorImplTest extends AbstractDartTest {
  private static final QuickAssistProcessor PROCESSOR = CorrectionProcessors.getQuickAssistProcessor();

  /**
   * @return <code>true</code> if given {@link CorrectionProposal} has required name.
   */
  private static boolean isProposal(CorrectionProposal proposal, String requiredName) {
    String proposalName = proposal.getName();
    return requiredName.equals(proposalName);
  }

  //  private int selectionStart = 0;
  private int selectionLength = 0;

  public void test_convertToBlockBody_OK_closure() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "setup(x) {}",
        "main() {",
        "  setup(() => print('done'));",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "setup(x) {}",
        "main() {",
        "  setup(() {",
        "    return print('done');",
        "  });",
        "}",
        "");
    assert_convertToBlockBody(initial, "() => print", expected);
  }

  public void test_convertToBlockBody_OK_method() throws Exception {
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

  public void test_convertToBlockBody_OK_onName() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  fff() => 123;",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  fff() {",
        "    return 123;",
        "  }",
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

  public void test_splitVariableDeclaration_OK() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v = 1;",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v;",
        "  v = 1;",
        "}");
    assert_splitVariableDeclaration(initial, "v ", expected);
  }

  public void test_splitVariableDeclaration_wrong_notOneVariable() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v = 1, v2;",
        "}");
    assert_splitVariableDeclaration_wrong(initial, "v = 1");
  }

  /**
   * @return the result of applying {@link CorrectionProposal} with single {@link SourceChange} to
   *         the {@link #testCode}.
   */
  private String applyProposal(CorrectionProposal proposal) {
    List<SourceChange> changes = proposal.getChanges();
    assertThat(changes).hasSize(1);
    SourceChange change = changes.get(0);
    assertSame(testSource, change.getSource());
    // prepare edits
    List<Edit> edits = change.getEdits();
    Collections.sort(edits, new Comparator<Edit>() {
      @Override
      public int compare(Edit o1, Edit o2) {
        return o2.offset - o1.offset;
      }
    });
    // apply edits
    String result = testCode;
    for (Edit edit : edits) {
      result = result.substring(0, edit.offset) + edit.replacement
          + result.substring(edit.offset + edit.length);
    }
    return result;
  }

  private void assert_convertToBlockBody(String initialSource, String offsetPattern,
      String expectedSource) throws Exception {
    assert_runProcessor("Convert to block body", initialSource, offsetPattern, expectedSource);
  }

  private void assert_convertToBlockBody_wrong(String initialSource, String offsetPattern)
      throws Exception {
    assert_convertToBlockBody(initialSource, offsetPattern, initialSource);
  }

  private void assert_exchangeBinaryExpressionArguments_success(String initialExpression,
      String offsetPattern, String expectedExpression) throws Exception {
    String initialSource = "var v = " + initialExpression + ";";
    String expectedSource = "var v = " + expectedExpression + ";";
    assert_runProcessor("Exchange operands", initialSource, offsetPattern, expectedSource);
  }

  private void assert_exchangeBinaryExpressionArguments_wrong(String expression,
      String offsetPattern) throws Exception {
    assert_exchangeBinaryExpressionArguments_success(expression, offsetPattern, expression);
  }

  private void assert_joinVariableDeclaration(String initialSource, String offsetPattern,
      String expectedSource) throws Exception {
    assert_runProcessor("Join variable declaration", initialSource, offsetPattern, expectedSource);
  }

  private void assert_joinVariableDeclaration_wrong(String expression, String offsetPattern)
      throws Exception {
    assert_joinVariableDeclaration(expression, offsetPattern, expression);
  }

  /**
   * Asserts that running proposal with given name produces expected source.
   */
  private void assert_runProcessor(String proposalName, String initialSource, String offsetPattern,
      String expectedSource) throws Exception {
    // XXX used to see coverage of only one quick assist
//    if (!proposalName.equals(CorrectionMessages.QuickAssistProcessor_splitAndCondition)) {
//      return;
//    }
    CorrectionProposal[] proposals = getProposals(initialSource, offsetPattern);
    // find and apply required proposal
    String result = initialSource;
    for (CorrectionProposal proposal : proposals) {
      if (isProposal(proposal, proposalName)) {
        result = applyProposal(proposal);
      }
    }
    // assert result
    assertEquals(expectedSource, result);
  }

  private void assert_splitVariableDeclaration(String initialSource, String offsetPattern,
      String expectedSource) throws Exception {
    assert_runProcessor("Split variable declaration", initialSource, offsetPattern, expectedSource);
  }

  private void assert_splitVariableDeclaration_wrong(String initialSource, String offsetPattern)
      throws Exception {
    assert_splitVariableDeclaration(initialSource, offsetPattern, initialSource);
  }

  private CorrectionProposal[] getProposals(String code, String offsetPattern) throws Exception {
    parseTestUnit(code);
    // prepare proposals
    int offset = findOffset(offsetPattern);
    AssistContext context = new AssistContext(testUnit, offset, selectionLength);
    return PROCESSOR.getProposals(context);
  }
}
