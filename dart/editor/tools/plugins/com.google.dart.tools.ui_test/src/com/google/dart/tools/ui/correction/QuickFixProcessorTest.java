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

  // TODO(scheglov) uncomment when ClassElement will have optionally LibraryPrefixElement 
  public void _test_useStaticAccess_method_importWithPrefix() throws Exception {
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

  @Override
  protected void tearDown() throws Exception {
    waitEventLoop(0);
    super.tearDown();
    waitEventLoop(0);
  }

  /**
   * Runs single proposal created for {@link IProblemLocation} using "problem*" fields.
   */
  private void assertQuickFix(String... expectedLines) throws CoreException {
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
    assertThat(proposals).hasSize(1);
    String result = ((CUCorrectionProposal) proposals[0]).getPreviewContent();
    // assert result
    String expectedSource = makeSource(expectedLines);
    assertEquals(expectedSource, result);
  }
}
