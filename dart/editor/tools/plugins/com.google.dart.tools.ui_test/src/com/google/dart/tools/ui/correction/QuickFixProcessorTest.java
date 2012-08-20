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

import com.google.dart.compiler.ErrorCode;
import com.google.dart.compiler.resolver.ResolverErrorCode;
import com.google.dart.compiler.resolver.TypeErrorCode;
import com.google.dart.compiler.util.apache.ArrayUtils;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.internal.text.correction.AssistContext;
import com.google.dart.tools.ui.internal.text.correction.ProblemLocation;
import com.google.dart.tools.ui.internal.text.correction.QuickFixProcessor;
import com.google.dart.tools.ui.internal.text.correction.proposals.CUCorrectionProposal;
import com.google.dart.tools.ui.refactoring.AbstractDartTest;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;
import com.google.dart.tools.ui.text.dart.IProblemLocation;
import com.google.dart.tools.ui.text.dart.IQuickFixProcessor;

import org.eclipse.core.runtime.CoreException;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for {@link QuickFixProcessor}.
 */
public final class QuickFixProcessorTest extends AbstractDartTest {
  private static final IQuickFixProcessor PROCESSOR = new QuickFixProcessor();

  private ErrorCode problemCode;
  private int problemOffset;
  private int problemLength;

  private int proposalsExpectedNumber = 1;
  private int proposalsIndexToCheck = 0;

  public void test_createFunction_hasParameters() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  foo(1, 2.0, '');",
        "}",
        "");
    problemCode = ResolverErrorCode.CANNOT_RESOLVE_METHOD;
    problemOffset = findOffset("foo(");
    problemLength = "foo".length();
    assertQuickFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  foo(1, 2.0, '');",
        "}",
        "",
        "foo(int i, double d, String string) {",
        "}",
        "");
  }

  public void test_createFunction_hasParameters_Dynamic() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  foo(null);",
        "}",
        "");
    problemCode = ResolverErrorCode.CANNOT_RESOLVE_METHOD;
    problemOffset = findOffset("foo(");
    problemLength = "foo".length();
    assertQuickFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  foo(null);",
        "}",
        "",
        "foo(arg0) {",
        "}",
        "");
  }

  public void test_createFunction_noParameters_hasReturnType_Dynamic() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v = foo();",
        "}",
        "");
    problemCode = ResolverErrorCode.CANNOT_RESOLVE_METHOD;
    problemOffset = findOffset("foo();");
    problemLength = "foo".length();
    assertQuickFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v = foo();",
        "}",
        "",
        "foo() {",
        "}",
        "");
  }

  public void test_createFunction_noParameters_hasReturnType_fromVariable() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v = foo();",
        "}",
        "");
    problemCode = ResolverErrorCode.CANNOT_RESOLVE_METHOD;
    problemOffset = findOffset("foo();");
    problemLength = "foo".length();
    assertQuickFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v = foo();",
        "}",
        "",
        "int foo() {",
        "}",
        "");
  }

  public void test_createFunction_noParameters_noReturnType() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  foo();",
        "}",
        "");
    problemCode = ResolverErrorCode.CANNOT_RESOLVE_METHOD;
    problemOffset = findOffset("foo();");
    problemLength = "foo".length();
    assertQuickFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  foo();",
        "}",
        "",
        "foo() {",
        "}",
        "");
  }

  public void test_createMethod_qualified() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo() {",
        "  }",
        "}",
        "main() {",
        "  A a = new A();",
        "  a.test();",
        "}",
        "");
    problemCode = TypeErrorCode.INTERFACE_HAS_NO_METHOD_NAMED;
    problemOffset = findOffset("test(");
    problemLength = "foo".length();
    assertQuickFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo() {",
        "  }",
        "",
        "  test() {",
        "  }",
        "}",
        "main() {",
        "  A a = new A();",
        "  a.test();",
        "}",
        "");
  }

  public void test_createMethod_qualified_static() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo() {",
        "  }",
        "}",
        "main() {",
        "  A.test();",
        "}",
        "");
    problemCode = ResolverErrorCode.CANNOT_RESOLVE_METHOD_IN_CLASS;
    problemOffset = findOffset("test(");
    problemLength = "foo".length();
    assertQuickFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo() {",
        "  }",
        "",
        "  static test() {",
        "  }",
        "}",
        "main() {",
        "  A.test();",
        "}",
        "");
  }

  public void test_createMethod_unqualified() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo() {",
        "    test();",
        "  }",
        "}",
        "");
    problemCode = TypeErrorCode.INTERFACE_HAS_NO_METHOD_NAMED;
    problemOffset = findOffset("test(");
    problemLength = "test".length();
    assertQuickFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo() {",
        "    test();",
        "  }",
        "",
        "  test() {",
        "  }",
        "}",
        "");
  }

  public void test_createMethod_unqualified_static() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static foo() {",
        "    test();",
        "  }",
        "}",
        "");
    problemCode = ResolverErrorCode.CANNOT_RESOLVE_METHOD;
    problemOffset = findOffset("test(");
    problemLength = "test".length();
    assertQuickFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static foo() {",
        "    test();",
        "  }",
        "",
        "  static test() {",
        "  }",
        "}",
        "");
  }

  public void test_importLibrary_withType_fromSDK() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  TableElement t = null;",
        "}",
        "");
    problemCode = TypeErrorCode.NO_SUCH_TYPE;
    problemOffset = findOffset("TableElement");
    problemLength = "TableElement".length();
    assertQuickFix(
        "#import('dart:html');",
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  TableElement t = null;",
        "}",
        "");
  }

  public void test_importLibrary_withType_hasDirectiveImport() throws Exception {
    setUnitContent("AAA.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('AAA');",
        "class AAA {",
        "}",
        ""});
    setUnitContent("App.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('App');",
        "#import('dart:core');",
        "#source('Test.dart');",
        ""});
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  AAA a = null;",
        "}",
        "");
    problemCode = TypeErrorCode.NO_SUCH_TYPE;
    problemOffset = findOffset("AAA");
    problemLength = "AAA".length();
    // we have "fix", note that preview is for library
    assertQuickFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('App');",
        "#import('dart:core');",
        "#import('AAA.dart');",
        "#source('Test.dart');",
        "");
    // unit itself is not changed
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  AAA a = null;",
        "}",
        "");
  }

  public void test_importLibrary_withType_hasImportWithPrefix() throws Exception {
    setUnitContent("Lib.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {",
        "}",
        ""});
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#import('Lib.dart', prefix: 'lib');",
        "main() {",
        "  Test t = null;",
        "}",
        "");
    problemCode = TypeErrorCode.NO_SUCH_TYPE;
    problemOffset = findOffset("Test");
    problemLength = "Test".length();
    // do check
    proposalsExpectedNumber = 2;
    proposalsIndexToCheck = 0;
    assertQuickFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#import('Lib.dart', prefix: 'lib');",
        "main() {",
        "  lib.Test t = null;",
        "}",
        "");
  }

  public void test_importLibrary_withType_noDirectives() throws Exception {
    setUnitContent("Lib.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {",
        "}",
        ""});
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  Test t = null;",
        "}",
        "");
    problemCode = TypeErrorCode.NO_SUCH_TYPE;
    problemOffset = findOffset("Test");
    problemLength = "Test".length();
    assertQuickFix(
        "#import('Lib.dart');",
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  Test t = null;",
        "}",
        "");
  }

  public void test_importLibrary_withType_privateName() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  _Test t = null;",
        "}",
        "");
    problemCode = TypeErrorCode.NO_SUCH_TYPE;
    problemOffset = findOffset("_Test");
    problemLength = "_Test".length();
    assertNoQuickFix();
  }

  public void test_useStaticAccess_method() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        " static foo() {}",
        "}",
        "main() {",
        "  A aaaa = new A();",
        "  aaaa.foo();",
        "}");
    problemCode = TypeErrorCode.IS_STATIC_METHOD_IN;
    problemOffset = findOffset("foo();");
    problemLength = "foo".length();
    assertQuickFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        " static foo() {}",
        "}",
        "main() {",
        "  A aaaa = new A();",
        "  A.foo();",
        "}");
  }

  public void test_useStaticAccess_method_importWithPrefix() throws Exception {
    setUnitContent("Lib.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Lib');",
        "class A {",
        " static foo() {}",
        "}",
        ""});
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#import('Lib.dart', prefix: 'lib');",
        "main() {",
        "  lib.A aaaa = new lib.A();",
        "  aaaa.foo();",
        "}",
        "");
    problemCode = TypeErrorCode.IS_STATIC_METHOD_IN;
    problemOffset = findOffset("foo();");
    problemLength = "foo".length();
    assertQuickFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#import('Lib.dart', prefix: 'lib');",
        "main() {",
        "  lib.A aaaa = new lib.A();",
        "  lib.A.foo();",
        "}",
        "");
  }

  @Override
  protected void tearDown() throws Exception {
    waitEventLoop(0);
    super.tearDown();
    waitEventLoop(0);
  }

  /**
   * Asserts that there are no quick fixes for {@link IProblemLocation} using "problem*" fields.
   */
  private void assertNoQuickFix() throws CoreException {
    IDartCompletionProposal[] proposals = prepareQuickFixes();
    assertThat(proposals).isEmpty();
  }

  /**
   * Runs single proposal created for {@link IProblemLocation} using "problem*" fields.
   */
  private void assertQuickFix(String... expectedLines) throws CoreException {
    IDartCompletionProposal[] proposals = prepareQuickFixes();
    assertThat(proposals).hasSize(proposalsExpectedNumber);
    String result = ((CUCorrectionProposal) proposals[proposalsIndexToCheck]).getPreviewContent();
    // assert result
    String expectedSource = makeSource(expectedLines);
    assertEquals(expectedSource, result);
  }

  /**
   * @return proposals created for {@link IProblemLocation} using "problem*" fields.
   */
  private IDartCompletionProposal[] prepareQuickFixes() throws CoreException {
    IProblemLocation problemLocation = new ProblemLocation(
        problemOffset,
        problemLength,
        problemCode,
        ArrayUtils.EMPTY_STRING_ARRAY,
        true,
        DartCore.DART_PROBLEM_MARKER_TYPE);
    IProblemLocation problemLocations[] = new IProblemLocation[] {problemLocation};
    // prepare context
    AssistContext context = new AssistContext(
        testUnit,
        problemLocation.getOffset(),
        problemLocation.getLength());
    assertTrue(PROCESSOR.hasCorrections(testUnit, problemCode));
    // run single proposal
    IDartCompletionProposal[] proposals = PROCESSOR.getCorrections(context, problemLocations);
    return proposals;
  }
}
