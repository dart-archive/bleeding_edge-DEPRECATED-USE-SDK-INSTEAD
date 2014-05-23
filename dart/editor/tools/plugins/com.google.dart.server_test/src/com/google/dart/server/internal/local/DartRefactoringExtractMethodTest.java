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

import com.google.dart.engine.services.refactoring.Parameter;

// TODO(scheglov) restore or remove for the new API
public class DartRefactoringExtractMethodTest extends DartRefactoringTest {
  private int refactoring_numOccurrences;
  private boolean refactoring_canExtractGetter;
  private Parameter[] refactoring_parameters;
  private String refactoring_signature;

  public void test_apply_error() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "var res = 0;",
//        "main() {",
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
//    setOptions("res", false, true, refactoring_parameters);
//    assertRefactoringStatusOK(refactoring_status);
//    // apply refactoring
//    prepareChange();
//    assertRefactoringStatus(refactoring_status, RefactoringStatusSeverity.ERROR);
  }

//  public void test_create_FATAL_incompleteExpression() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "main() {",
//        "  print(1 + 2);",
//        "}"));
//    setSelectionAtRange("1 + ");
//    createRefactoring();
//    assertNull(refactoring_id);
//    assertRefactoringStatus(refactoring_status, RefactoringStatusSeverity.FATAL);
//  }
//
//  public void test_create_numOccurrences_1() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "main() {",
//        "  print(1 + 2);",
//        "}"));
//    // prepare refactoring
//    setSelectionAtRange("1 + 2");
//    createRefactoring();
//    assertRefactoringStatusOK(refactoring_status);
//    assertEquals(1, refactoring_numOccurrences);
//  }
//
//  public void test_create_numOccurrences_2() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "main() {",
//        "  print(1 + 2);",
//        "  print(1 + 2);",
//        "}"));
//    // prepare refactoring
//    setSelectionAtRange("1 + 2");
//    createRefactoring();
//    assertRefactoringStatusOK(refactoring_status);
//    assertEquals(2, refactoring_numOccurrences);
//  }
//
//  public void test_OK_expression() throws Exception {
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
//    assertEquals(1, refactoring_numOccurrences);
//    assertTrue(refactoring_canExtractGetter);
//    assertThat(refactoring_parameters).isEmpty();
//    // there is only our refactoring
//    assertThat(refactoringMap).hasSize(1);
//    assertThat(refactoringMap.keySet()).containsOnly(refactoring_id);
//    // set options
//    setOptions("res", false, true, refactoring_parameters);
//    assertRefactoringStatusOK(refactoring_status);
//    assertEquals("res()", refactoring_signature);
//    // apply refactoring
//    prepareChange();
//    assertSuccessfulRefactoring(//
//        "main() {",
//        "  print(res());",
//        "}",
//        "",
//        "int res() => 1 + 2;");
//    // delete the refactoring
//    server.deleteRefactoring(refactoring_id);
//    server.test_waitForWorkerComplete();
//    // no any refactorings left
//    assertThat(refactoringMap).isEmpty();
//  }
//
//  public void test_OK_getter() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "class A {",
//        "  main() {",
//        "    print(1 + 2);",
//        "  }",
//        "}"));
//    // prepare refactoring
//    setSelectionAtRange("1 + 2");
//    createRefactoring();
//    assertNotNull(refactoring_id);
//    assertRefactoringStatusOK(refactoring_status);
//    assertEquals(1, refactoring_numOccurrences);
//    assertTrue(refactoring_canExtractGetter);
//    // set options
//    setOptions("res", true, true, refactoring_parameters);
//    assertRefactoringStatusOK(refactoring_status);
//    assertEquals("get res", refactoring_signature);
//    // apply refactoring
//    prepareChange();
//    assertSuccessfulRefactoring(//
//        "class A {",
//        "  main() {",
//        "    print(res);",
//        "  }",
//        "",
//        "  int get res => 1 + 2;",
//        "}");
//  }
//
//  public void test_OK_statements() throws Exception {
//    createContextWithSingleSource(makeSource(//
//        "main() {",
//        "  int a = 1;",
//        "  int b = 2;",
//        "// start",
//        "  int c = a + 2;",
//        "  int d = c + b;",
//        "// end",
//        "  print(d);",
//        "}"));
//    // prepare refactoring
//    setSelectionFromStartEndComments();
//    createRefactoring();
//    assertNotNull(refactoring_id);
//    assertRefactoringStatusOK(refactoring_status);
//    assertEquals(1, refactoring_numOccurrences);
//    assertFalse(refactoring_canExtractGetter);
//    // parameters
//    {
//      assertThat(refactoring_parameters).hasSize(2);
//      assertEquals("a", refactoring_parameters[0].getOldName());
//      assertEquals("b", refactoring_parameters[1].getOldName());
//    }
//    // set options
//    setOptions("res", false, true, refactoring_parameters);
//    assertRefactoringStatusOK(refactoring_status);
//    assertEquals("res(int a, int b)", refactoring_signature);
//    // apply refactoring
//    prepareChange();
//    assertSuccessfulRefactoring(//
//        "main() {",
//        "  int a = 1;",
//        "  int b = 2;",
//        "// start",
//        "  int d = res(a, b);",
//        "// end",
//        "  print(d);",
//        "}",
//        "",
//        "int res(int a, int b) {",
//        "  int c = a + 2;",
//        "  int d = c + b;",
//        "  return d;",
//        "}");
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
//    setOptions("not-an-identifier", false, false, refactoring_parameters);
//    assertRefactoringStatus(refactoring_status, RefactoringStatusSeverity.ERROR);
//  }
//
//  public void test_setOptions_noSuchId() throws Exception {
//    server.setRefactoringExtractMethodOptions("no-such-id", "name", false, false, null, null);
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
//    setOptions("Res", false, true, refactoring_parameters);
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
//    server.createRefactoringExtractMethod(
//        contextId,
//        source,
//        selectionOffset,
//        selectionLength,
//        new RefactoringExtractMethodConsumer() {
//          @Override
//          public void computed(String refactoringId, RefactoringStatus status, int numOccurrences,
//              boolean canExtractGetter, Parameter[] parameters) {
//            refactoring_id = refactoringId;
//            refactoring_status = status;
//            refactoring_numOccurrences = numOccurrences;
//            refactoring_canExtractGetter = canExtractGetter;
//            refactoring_parameters = parameters;
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
//  private void setOptions(String name, boolean asGetter, boolean allOccurrences,
//      Parameter[] parameters) {
//    final CountDownLatch latch = new CountDownLatch(1);
//    server.setRefactoringExtractMethodOptions(
//        refactoring_id,
//        name,
//        asGetter,
//        allOccurrences,
//        parameters,
//        new RefactoringExtractMethodOptionsValidationConsumer() {
//          @Override
//          public void computed(RefactoringStatus status, String signature) {
//            refactoring_status = status;
//            refactoring_signature = signature;
//            latch.countDown();
//          }
//        });
//    Uninterruptibles.awaitUninterruptibly(latch, 600, TimeUnit.SECONDS);
//  }
}
