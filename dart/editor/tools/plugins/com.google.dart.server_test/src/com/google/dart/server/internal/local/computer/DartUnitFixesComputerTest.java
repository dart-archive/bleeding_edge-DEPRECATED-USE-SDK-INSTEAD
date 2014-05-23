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
public class DartUnitFixesComputerTest extends AbstractLocalServerTest {
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

  public void test_createMissingOverrides_generics() throws Exception {
//    String initial = makeSource(//
//        "import 'dart:collection';",
//        "class Test extends IterableMixin<int> {",
//        "}");
//    String expected = makeSource(//
//        "import 'dart:collection';",
//        "class Test extends IterableMixin<int> {",
//        "  // TODO: implement iterator",
//        "  @override",
//        "  Iterator<int> get iterator => null;",
//        "}");
//    createContextWithSingleSource(initial);
//    assert_applySourceProposal(CorrectionKind.QF_CREATE_MISSING_OVERRIDES, expected);
  }

//  public void test_insertSemicolon() throws Exception {
//    String initial = makeSource(//
//        "main() {",
//        "  print(42)",
//        "}");
//    String expected = makeSource(//
//        "main() {",
//        "  print(42);",
//        "}");
//    createContextWithSingleSource(initial);
//    assert_applySourceProposal(CorrectionKind.QF_INSERT_SEMICOLON, expected);
//  }
//
//  public void test_removeUnusedImport() throws Exception {
//    String initial = makeSource(//
//        "import 'dart:math';",
//        "",
//        "main() {",
//        "}");
//    String expected = makeSource(//
//        "",
//        "main() {",
//        "}");
//    createContextWithSingleSource(initial);
//    assert_applySourceProposal(CorrectionKind.QF_REMOVE_UNUSED_IMPORT, expected);
//  }
//
//  @Override
//  protected void setUp() throws Exception {
//    super.setUp();
//    server.test_setLog(true);
//  }
//
//  @Override
//  protected void tearDown() throws Exception {
//    server.test_setLog(false);
//    super.tearDown();
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
//    // prepare errors
//    server.test_waitForWorkerComplete();
//    log("computeProposals: worker completed");
//    serverListener.assertNoServerErrors();
//    AnalysisError[] errors = serverListener.getErrors(source);
//    log("computeProposals: %s error(s)", errors.length);
//    if (errors.length != 1) {
//      fail("Exactly 1 error expected, but " + errors.length + " found.\n"
//          + StringUtils.join(errors, "\n"));
//    }
//    // request fixes
//    proposals.clear();
//    final CountDownLatch latch = new CountDownLatch(1);
//    server.computeFixes(contextId, errors, new FixesConsumer() {
//      @Override
//      public void computedFixes(Map<AnalysisError, CorrectionProposal[]> fixesMap,
//          boolean isLastResult) {
//        if (fixesMap.size() == 1) {
//          Collections.addAll(proposals, fixesMap.values().iterator().next());
//        }
//        if (isLastResult) {
//          latch.countDown();
//        }
//      }
//    });
//    latch.await(600, TimeUnit.SECONDS);
//  }
//
//  private void createContextWithSingleSource(String code) {
//    this.contextId = createContext("test");
//    this.code = code;
//    this.source = addSource(contextId, "/test.dart", code);
//    server.subscribe(
//        contextId,
//        ImmutableMap.of(NotificationKind.ERRORS, SourceSet.EXPLICITLY_ADDED));
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
//  private void log(String msg, Object... arguments) {
//    String message = String.format(msg, arguments);
//    System.out.println(message);
//  }
}
