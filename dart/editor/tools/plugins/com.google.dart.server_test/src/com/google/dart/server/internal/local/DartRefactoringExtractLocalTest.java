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

package com.google.dart.server.internal.local;


// TODO(scheglov) restore or remove for the new API
public class DartRefactoringExtractLocalTest extends DartRefactoringTest {
  private boolean refactoring_hasSeveralOccurrences;
  private String[] refactoring_proposedNames;

  public void test_apply_warning() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "main() {",
//        "  var res = 0;",
//        "  print(1 + 2);",
//        "}"));
//    // no refactorings initially
//    Map<String, Refactoring> refactoringMap = server.test_getRefactoringMap();
//    assertThat(refactoringMap).isEmpty();
//    // prepare refactoring
//    setSelectionAtRange("1 + 2");
//    createRefactoring();
//    assertRefactoringStatusOK(refactoring_status);
//    // set options
//    setOptions(false, "res");
//    assertRefactoringStatusOK(refactoring_status);
//    // apply refactoring
//    prepareChange();
//    assertRefactoringStatus(refactoring_status, RefactoringStatusSeverity.WARNING);
  }

//  public void test_bad_create_incompleteExpression() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "main() {",
//        "  print(1 + 2);",
//        "}"));
//    setSelectionAtRange("1 + ");
//    createRefactoring();
//    assertNull(refactoring_id);
//    assertRefactoringStatus(refactoring_status, RefactoringStatusSeverity.FATAL);
//    assertFalse(refactoring_hasSeveralOccurrences);
//    assertNull(refactoring_proposedNames);
//  }
//
//  public void test_create_hasSeveralOccurrences_false() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "main() {",
//        "  print(1 + 2);",
//        "}"));
//    // prepare refactoring
//    setSelectionAtRange("1 + 2");
//    createRefactoring();
//    assertRefactoringStatusOK(refactoring_status);
//    assertFalse(refactoring_hasSeveralOccurrences);
//  }
//
//  public void test_create_hasSeveralOccurrences_true() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "main() {",
//        "  print(1 + 2);",
//        "  print(1 + 2);",
//        "}"));
//    // prepare refactoring
//    setSelectionAtRange("1 + 2");
//    createRefactoring();
//    assertRefactoringStatusOK(refactoring_status);
//    assertTrue(refactoring_hasSeveralOccurrences);
//  }
//
//  public void test_deleteRefactoring_noSuchId() throws Exception {
//    server.deleteRefactoring("no-such-id");
//    server.test_waitForWorkerComplete();
//    serverListener.assertServerErrorsWithCodes(AnalysisServerErrorCode.INVALID_REFACTORING_ID);
//  }
//
//  public void test_OK() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "main() {",
//        "  print(1 + 2);",
//        "}"));
//    // no refactorings initially
//    Map<String, Refactoring> refactoringMap = server.test_getRefactoringMap();
//    assertThat(refactoringMap).isEmpty();
//    // prepare refactoring
//    setSelectionAtRange("1 + 2");
//    createRefactoring();
//    assertNotNull(refactoring_id);
//    assertRefactoringStatusOK(refactoring_status);
//    assertThat(refactoring_proposedNames).contains("i");
//    // there is only our refactoring
//    assertThat(refactoringMap).hasSize(1);
//    assertThat(refactoringMap.keySet()).containsOnly(refactoring_id);
//    // set options
//    setOptions(true, "res");
//    assertRefactoringStatusOK(refactoring_status);
//    // apply refactoring
//    prepareChange();
//    assertSuccessfulRefactoring(//
//        "main() {",
//        "  var res = 1 + 2;",
//        "  print(res);",
//        "}");
//    // delete the refactoring
//    server.deleteRefactoring(refactoring_id);
//    server.test_waitForWorkerComplete();
//    // no any refactorings left
//    assertThat(refactoringMap).isEmpty();
//  }
//
//  public void test_setOptions_error() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "main() {",
//        "  print(1 + 2);",
//        "}"));
//    setSelectionAtRange("1 + 2");
//    createRefactoring();
//    assertNotNull(refactoring_id);
//    assertRefactoringStatusOK(refactoring_status);
//    // set options
//    setOptions(true, "not-an-identifier");
//    assertRefactoringStatus(refactoring_status, RefactoringStatusSeverity.ERROR);
//  }
//
//  public void test_setOptions_noSuchId() throws Exception {
//    server.setRefactoringExtractLocalOptions("no-such-id", true, "name", null);
//    server.test_waitForWorkerComplete();
//    serverListener.assertServerErrorsWithCodes(AnalysisServerErrorCode.INVALID_REFACTORING_ID);
//  }
//
//  public void test_setOptions_warning() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "main() {",
//        "  print(1 + 2);",
//        "}"));
//    setSelectionAtRange("1 + 2");
//    createRefactoring();
//    assertNotNull(refactoring_id);
//    assertRefactoringStatusOK(refactoring_status);
//    // set options
//    setOptions(true, "Res");
//    assertRefactoringStatus(refactoring_status, RefactoringStatusSeverity.WARNING);
//  }
//
//  /**
//   * Checks that all conditions are <code>OK</code> and the result of applying
//   * {@link #refactoring_change} to the {@link #source} is the same as the given lines.
//   */
//  protected final void assertSuccessfulRefactoring(String... lines) throws Exception {
//    assertRefactoringStatusOK(refactoring_status);
//    assertEquals(makeSource(lines), getChangeResult(source, code));
//  }
//
//  private void createRefactoring() {
//    final CountDownLatch latch = new CountDownLatch(1);
//    server.createRefactoringExtractLocal(
//        contextId,
//        source,
//        selectionOffset,
//        selectionLength,
//        new RefactoringExtractLocalConsumer() {
//          @Override
//          public void computed(String refactoringId, RefactoringStatus status,
//              boolean hasSeveralOccurrences, String[] proposedNames) {
//            refactoring_id = refactoringId;
//            refactoring_status = status;
//            refactoring_hasSeveralOccurrences = hasSeveralOccurrences;
//            refactoring_proposedNames = proposedNames;
//            latch.countDown();
//          }
//        });
//    Uninterruptibles.awaitUninterruptibly(latch, 600, TimeUnit.SECONDS);
//    serverListener.assertNoServerErrors();
//  }
//
//  private String getChangeResult(Source source, String initialCode) {
//    SourceChange sourceChange = (SourceChange) refactoring_change;
//    assertSame(source, sourceChange.getSource());
//    return CorrectionUtils.applyReplaceEdits(code, sourceChange.getEdits());
//  }
//
//  private void prepareChange() {
//    final CountDownLatch latch = new CountDownLatch(1);
//    server.applyRefactoring(refactoring_id, new RefactoringApplyConsumer() {
//      @Override
//      public void computed(RefactoringStatus status, Change change) {
//        refactoring_status = status;
//        refactoring_change = change;
//        latch.countDown();
//      }
//    });
//    Uninterruptibles.awaitUninterruptibly(latch, 600, TimeUnit.SECONDS);
//  }
//
//  private void setOptions(boolean allOccurrences, String name) {
//    final CountDownLatch latch = new CountDownLatch(1);
//    server.setRefactoringExtractLocalOptions(
//        refactoring_id,
//        allOccurrences,
//        name,
//        new RefactoringOptionsValidationConsumer() {
//          @Override
//          public void computed(RefactoringStatus status) {
//            refactoring_status = status;
//            latch.countDown();
//          }
//        });
//    Uninterruptibles.awaitUninterruptibly(latch, 600, TimeUnit.SECONDS);
//  }
}
