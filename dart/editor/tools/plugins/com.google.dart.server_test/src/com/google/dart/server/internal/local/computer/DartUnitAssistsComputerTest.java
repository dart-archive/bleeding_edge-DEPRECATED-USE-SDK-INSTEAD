/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.server.internal.local.computer;

import com.google.common.collect.Lists;
import com.google.dart.engine.services.change.Edit;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.correction.CorrectionProposal;
import com.google.dart.engine.services.correction.SourceCorrectionProposal;
import com.google.dart.engine.services.internal.correction.CorrectionUtils;
import com.google.dart.engine.source.Source;
import com.google.dart.server.internal.local.AbstractLocalServerTest;

import java.util.List;

// TODO(scheglov) restore or remove for the new API
public class DartUnitAssistsComputerTest extends AbstractLocalServerTest {
  private final static String EOL = "\n";

  /**
   * @return the result of applying {@link SourceCorrectionProposal} to the given code.
   */
  private static String applyProposal(String code, SourceCorrectionProposal proposal) {
    SourceChange change = proposal.getChange();
    List<Edit> edits = change.getEdits();
    return CorrectionUtils.applyReplaceEdits(code, edits);
  }

  private List<CorrectionProposal> proposals = Lists.newArrayList();
  private String contextId;
  private String code;
  private Source source;
  private int selectionOffset;
  private int selectionLength;

  public void test_exchangeOperands() throws Exception {
    // TODO(scheglov) restore or remove for the new API
//    String initial = makeSource(//
//        "main() {",
//        "  print(111 + 222);",
//        "}");
//    String expected = makeSource(//
//        "main() {",
//        "  print(222 + 111);",
//        "}");
//    createContextWithSingleSource(initial);
//    setSelectionAtOffset("+ 222");
//    assert_applySourceProposal(CorrectionKind.QA_EXCHANGE_OPERANDS, expected);
  }

//  public void test_invertIfStatement_blocks() throws Exception {
//    String initial = makeSource(//
//        "main() {",
//        "  if (true) {",
//        "    0;",
//        "  } else {",
//        "    1;",
//        "  }",
//        "}");
//    String expected = makeSource(//
//        "main() {",
//        "  if (false) {",
//        "    1;",
//        "  } else {",
//        "    0;",
//        "  }",
//        "}");
//    createContextWithSingleSource(initial);
//    setSelectionAtOffset("if (");
//    assert_applySourceProposal(CorrectionKind.QA_INVERT_IF_STATEMENT, expected);
//  }
//
//  public void test_surroundWith_block() throws Exception {
//    String initial = makeSource(//
//        "main() {",
//        "// start",
//        "  print(0);",
//        "  print(1);",
//        "// end",
//        "}");
//    String expected = makeSource(//
//        "main() {",
//        "// start",
//        "  {",
//        "    print(0);",
//        "    print(1);",
//        "  }",
//        "// end",
//        "}");
//    createContextWithSingleSource(initial);
//    setSelectionFromStartEndComments();
//    assert_applySourceProposal(CorrectionKind.QA_SURROUND_WITH_BLOCK, expected);
//  }
//
//  /**
//   * Requests all available minor refactorings and applies the one with the given kind.
//   */
//  private void assert_applySourceProposal(CorrectionKind kind, String expectedSource)
//      throws Exception {
//    String resultCode = code;
//    // find proposal
//    computeProposals();
//    CorrectionProposal proposal = findProposal(kind);
//    // maybe apply proposal
//    if (proposal != null) {
//      assertThat(proposal).isInstanceOf(SourceCorrectionProposal.class);
//      SourceCorrectionProposal sourceProposal = (SourceCorrectionProposal) proposal;
//      resultCode = applyProposal(code, sourceProposal);
//    }
//    // assert result
//    assertEquals(expectedSource, resultCode);
//  }
//
//  private void computeProposals() throws Exception {
//    proposals.clear();
//    final CountDownLatch latch = new CountDownLatch(1);
//    server.computeMinorRefactorings(
//        contextId,
//        source,
//        selectionOffset,
//        selectionLength,
//        new MinorRefactoringsConsumer() {
//          @Override
//          public void computedProposals(CorrectionProposal[] _proposals, boolean isLastResult) {
//            Collections.addAll(proposals, _proposals);
//            if (isLastResult) {
//              latch.countDown();
//            }
//          }
//        });
//    latch.await(600, TimeUnit.SECONDS);
//  }
//
//  private void createContextWithSingleSource(String code) {
//    this.contextId = createContext("test");
//    this.code = code;
//    this.source = addSource(contextId, "/test.dart", code);
//  }
//
//  /**
//   * @return the offset directly after the given "search" string in {@link code}. Fails test if not
//   *         found.
//   */
//  private int findEnd(String search) {
//    return findOffset(search) + search.length();
//  }
//
//  /**
//   * @return the offset of given "search" string in {@link #code}. Fails test if not found.
//   */
//  private int findOffset(String search) {
//    int offset = code.indexOf(search);
//    assertThat(offset).describedAs(code).isNotEqualTo(-1);
//    return offset;
//  }
//
//  /**
//   * Returns the {@link CorrectionProposal} with the given {@link CorrectionKind} in
//   * {@link #proposals}, maybe {@code null}.
//   */
//  private CorrectionProposal findProposal(CorrectionKind kind) {
//    for (CorrectionProposal proposal : proposals) {
//      if (proposal.getKind() == kind) {
//        return proposal;
//      }
//    }
//    return null;
//  }
//
//  private void setSelectionAtOffset(String search) {
//    selectionOffset = code.indexOf(search);
//    selectionLength = 0;
//  }
//
//  private void setSelectionFromStartEndComments() throws Exception {
//    selectionOffset = findEnd("// start") + EOL.length();
//    selectionLength = findOffset("// end") - selectionOffset;
//  }
}
