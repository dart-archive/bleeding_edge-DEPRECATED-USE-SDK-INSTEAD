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

import com.google.common.collect.Lists;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.correction.CorrectionProcessors;
import com.google.dart.engine.services.correction.CorrectionProposal;
import com.google.dart.engine.services.correction.ProblemLocation;
import com.google.dart.engine.services.correction.QuickFixProcessor;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

public class QuickFixProcessorImplTest extends AbstractDartTest {
  private static final QuickFixProcessor PROCESSOR = CorrectionProcessors.getQuickFixProcessor();

  /**
   * @return <code>true</code> if given {@link CorrectionProposal} has required name.
   */
  private static boolean isProposal(CorrectionProposal proposal, String requiredName) {
    String proposalName = proposal.getName();
    return requiredName.equals(proposalName);
  }

  private ProblemLocation problem;

  public void test_boolean() throws Exception {
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  boolean v;",
        "}");
    assert_runProcessor(
        "Replace 'boolean' with 'bool'",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "main() {",
            "  bool v;",
            "}"));
  }

  public void test_computeProposals_noContext() throws Exception {
    ProblemLocation emptyProblem = new ProblemLocation(null, 0, 0, "");
    CorrectionProposal[] proposals = PROCESSOR.computeProposals(null, emptyProblem);
    assertThat(proposals).isEmpty();
  }

  public void test_computeProposals_noProblem() throws Exception {
    AssistContext emptyContext = new AssistContext(null, null, 0, 0);
    CorrectionProposal[] proposals = PROCESSOR.computeProposals(emptyContext, null);
    assertThat(proposals).isEmpty();
  }

  public void test_expectedToken_semicolon() throws Exception {
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print()",
        "}");
    assert_runProcessor(
        "Insert ';'",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "main() {",
            "  print();",
            "}"));
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    verifyNoTestUnitErrors = false;
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
    return CorrectionUtils.applyReplaceEdits(testCode, edits);
  }

  /**
   * Asserts that running proposal with given name produces expected source.
   */
  private void assert_runProcessor(String proposalName, String expectedSource) throws Exception {
    CorrectionProposal[] proposals = getProposals();
    // find and apply required proposal
    String result = testCode;
    for (CorrectionProposal proposal : proposals) {
      if (isProposal(proposal, proposalName)) {
        result = applyProposal(proposal);
        break;
      }
    }
    // assert result
    assertEquals(expectedSource, result);
  }

  private CorrectionProposal[] getProposals() throws Exception {
    AssistContext context = new AssistContext(null, testUnit, 0, 0);
    return PROCESSOR.computeProposals(context, problem);
  }

  /**
   * Prepares single problem to fix and stores to {@link #problem}.
   */
  private void prepareProblem() {
    List<AnalysisError> errors = Lists.newArrayList();
    Collections.addAll(errors, testUnit.getParsingErrors());
    Collections.addAll(errors, testUnit.getResolutionErrors());
    assertThat(errors).hasSize(1);
    AnalysisError error = errors.get(0);
    problem = new ProblemLocation(
        error.getErrorCode(),
        error.getOffset(),
        error.getLength(),
        error.getMessage());
  }

  /**
   * Prepares {@link #problem} and checks that {@link QuickFixProcessor#hasFix(ProblemLocation)}.
   */
  private void prepareProblemWithFix(String... lines) throws Exception {
    parseTestUnit(makeSource(lines));
    prepareProblem();
    {
      boolean hasFix = PROCESSOR.hasFix(problem);
      ErrorCode errorCode = problem.getErrorCode();
      String errorCodeStr = errorCode.getClass().getSimpleName() + "." + errorCode;
      assertTrue(errorCodeStr + " " + problem.getMessage(), hasFix);
    }
  }
}
