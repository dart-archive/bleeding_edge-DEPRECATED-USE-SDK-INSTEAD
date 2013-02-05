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
import com.google.dart.engine.parser.ParserTestCase;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.correction.CorrectionProcessors;
import com.google.dart.engine.services.correction.CorrectionProposal;
import com.google.dart.engine.services.correction.QuickAssistProcessor;
import com.google.dart.engine.services.correction.SourceChange;
import com.google.dart.engine.source.Source;

import junit.framework.TestCase;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class QuickAssistProcessorImplTest extends TestCase {
  private static final QuickAssistProcessor PROCESSOR = CorrectionProcessors.getQuickAssistProcessor();

  /**
   * @return <code>true</code> if given {@link CorrectionProposal} has required name.
   */
  private static boolean isProposal(CorrectionProposal proposal, String requiredName) {
    String proposalName = proposal.getName();
    return requiredName.equals(proposalName);
  }

  private final Source testSource = mock(Source.class);
  private String testCode;
//  private int selectionStart = 0;
  private int selectionLength = 0;

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

  /**
   * @return the offset of given <code>search</code> string in {@link testSource}. Fails test if not
   *         found.
   */
  private int findOffset(String search) throws Exception {
    int offset = testCode.indexOf(search);
    assertThat(offset).describedAs(testCode).isNotEqualTo(-1);
    return offset;
  }

  private CorrectionProposal[] getProposals(String code, String offsetPattern) throws Exception {
    // set initial code
    testCode = code;
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        ((Source.ContentReceiver) invocation.getArguments()[0]).accept(testCode);
        return null;
      }
    }).when(testSource).getContents(any(Source.ContentReceiver.class));
    // prepare proposals
    int offset = findOffset(offsetPattern);
    AssistContext context = new AssistContext(
        testSource,
        ParserTestCase.parseCompilationUnit(testCode),
        offset,
        selectionLength);
    return PROCESSOR.getProposals(context);
  }
}
