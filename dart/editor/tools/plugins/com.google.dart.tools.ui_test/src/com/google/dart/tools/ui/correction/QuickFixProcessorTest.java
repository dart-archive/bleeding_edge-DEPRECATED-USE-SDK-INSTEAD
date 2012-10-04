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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Test for {@link QuickFixProcessor}.
 */
public final class QuickFixProcessorTest extends AbstractDartTest {
  private static final IQuickFixProcessor PROCESSOR = new QuickFixProcessor();

  private String proposalNamePrefix;

  public void test_createClass() throws Exception {
    proposalNamePrefix = "Create class";
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  new Test();",
        "}",
        "");
    assertQuickFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  new Test();",
        "}",
        "",
        "class Test {",
        "}",
        "");
  }

  public void test_createClass_privateName() throws Exception {
    proposalNamePrefix = "Create class";
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  _Test t = null;",
        "}",
        "");
    assertQuickFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  _Test t = null;",
        "}",
        "",
        "class _Test {",
        "}",
        "");
  }

  public void test_createClass_whenInvocationTarget() throws Exception {
    proposalNamePrefix = "Create class";
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  Test.foo();",
        "}",
        "");
    assertQuickFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  Test.foo();",
        "}",
        "",
        "class Test {",
        "}",
        "");
  }

  public void test_createConstructor_default() throws Exception {
    proposalNamePrefix = "Create constructor";
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  new Test(1, 2.0);",
        "}",
        "class Test {",
        "  someExistingMethod() {}",
        "}",
        "");
    assertQuickFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  new Test(1, 2.0);",
        "}",
        "class Test {",
        "  Test(int i, double d) {",
        "  }",
        "",
        "  someExistingMethod() {}",
        "}",
        "");
  }

  public void test_createConstructor_named() throws Exception {
    proposalNamePrefix = "Create constructor";
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  new Test.name(1, 2.0);",
        "}",
        "class Test {",
        "  someExistingMethod() {}",
        "}",
        "");
    assertQuickFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  new Test.name(1, 2.0);",
        "}",
        "class Test {",
        "  Test.name(int i, double d) {",
        "  }",
        "",
        "  someExistingMethod() {}",
        "}",
        "");
  }

  public void test_createFunction_hasParameters() throws Exception {
    proposalNamePrefix = "Create method";
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  foo(1, 2.0, '');",
        "}",
        "");
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
    proposalNamePrefix = "Create method";
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  foo(null);",
        "}",
        "");
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
    proposalNamePrefix = "Create method";
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v = foo();",
        "}",
        "");
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
    proposalNamePrefix = "Create method";
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v = foo();",
        "}",
        "");
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
    proposalNamePrefix = "Create method";
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  foo();",
        "}",
        "");
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
    proposalNamePrefix = "Create method";
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
    proposalNamePrefix = "Create method";
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
    proposalNamePrefix = "Create method";
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo() {",
        "    test();",
        "  }",
        "}",
        "");
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
    proposalNamePrefix = "Create method";
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static foo() {",
        "    test();",
        "  }",
        "}",
        "");
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

  public void test_importLibrary_withField_fromSDK() throws Exception {
    proposalNamePrefix = "Import library";
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  process(PI);",
        "}",
        "process(x) {}",
        "");
    assertQuickFix(
        "import 'dart:math';",
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  process(PI);",
        "}",
        "process(x) {}",
        "");
  }

  public void test_importLibrary_withFunction() throws Exception {
    proposalNamePrefix = "Import library";
    setUnitContent("Lib.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {}",
        ""});
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  test();",
        "}",
        "");
    assertQuickFix(
        "import 'Lib.dart';",
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  test();",
        "}",
        "");
  }

  public void test_importLibrary_withFunction_fromSDK() throws Exception {
    proposalNamePrefix = "Import library";
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  min(1, 2);",
        "}",
        "");
    assertQuickFix(
        "import 'dart:math';",
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  min(1, 2);",
        "}",
        "");
  }

  public void test_importLibrary_withFunction_hasImportWithPrefix() throws Exception {
    proposalNamePrefix = "Use imported library 'Lib.dart' with prefix 'lib'";
    setUnitContent("Lib.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "library Lib;",
        "test() {}",
        ""});
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'Lib.dart' as lib;",
        "main() {",
        "  test();",
        "}",
        "");
    assertQuickFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'Lib.dart' as lib;",
        "main() {",
        "  lib.test();",
        "}",
        "");
  }

  public void test_importLibrary_withFunction_privateName() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  _test();",
        "}",
        "");
    // no "import" proposal
    IDartCompletionProposal[] proposals = prepareQuickFixes();
    for (IDartCompletionProposal proposal : proposals) {
      String title = proposal.getDisplayString().toLowerCase();
      assertThat(title).excludes("import");
    }
  }

  public void test_importLibrary_withType_fromSDK() throws Exception {
    proposalNamePrefix = "Import library";
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  TableElement t = null;",
        "}",
        "");
    assertQuickFix(
        "import 'dart:html';",
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  TableElement t = null;",
        "}",
        "");
  }

  public void test_importLibrary_withType_hasDirectiveImport() throws Exception {
    proposalNamePrefix = "Import library";
    setUnitContent("LibA.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "library A;",
        "class AAA {",
        "}",
        ""});
    setUnitContent("App.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "library App;",
        "import 'dart:core';",
        "part 'Test.dart';",
        ""});
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  AAA a = null;",
        "}",
        "");
    // we have "fix", note that preview is for library
    assertQuickFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library App;",
        "import 'dart:core';",
        "import 'LibA.dart';",
        "part 'Test.dart';",
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
    proposalNamePrefix = "Use imported library 'Lib.dart' with prefix 'lib'";
    setUnitContent("Lib.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "library Lib;",
        "class Test {",
        "}",
        ""});
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'Lib.dart' as lib;",
        "main() {",
        "  Test t = null;",
        "}",
        "");
    // do check
    assertQuickFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'Lib.dart' as lib;",
        "main() {",
        "  lib.Test t = null;",
        "}",
        "");
  }

  public void test_importLibrary_withType_noDirectives() throws Exception {
    proposalNamePrefix = "Import library";
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
    assertQuickFix(
        "import 'Lib.dart';",
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  Test t = null;",
        "}",
        "");
  }

  public void test_importLibrary_withType_whenInvocationTarget() throws Exception {
    proposalNamePrefix = "Import library";
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var foo = JSON.parse('{\"foo\":1}');",
        "}",
        "");
    assertQuickFix(
        "import 'dart:json';",
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var foo = JSON.parse('{\"foo\":1}');",
        "}",
        "");
  }

  public void test_unresolvedClass_useSimilar() throws Exception {
    proposalNamePrefix = "Change to 'Person'";
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Person {}",
        "main() {",
        "  Prson p;",
        "}",
        "");
    assertQuickFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Person {}",
        "main() {",
        "  Person p;",
        "}",
        "");
  }

  // XXX
  public void test_unresolvedFunction_useSimilar_qualified_fromClass() throws Exception {
    proposalNamePrefix = "Change to";
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  sayHello() {}",
        "}",
        "main() {",
        "  A a = new A();",
        "  a.syaHelol();",
        "}",
        "");
    assertQuickFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  sayHello() {}",
        "}",
        "main() {",
        "  A a = new A();",
        "  a.sayHello();",
        "}",
        "");
  }

  public void test_unresolvedFunction_useSimilar_unqualified_fromClass_same() throws Exception {
    proposalNamePrefix = "Change to";
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  sayHello() {}",
        "  foo() {",
        "    syaHelol();",
        "  }",
        "}",
        "");
    assertQuickFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  sayHello() {}",
        "  foo() {",
        "    sayHello();",
        "  }",
        "}",
        "");
  }

  public void test_unresolvedFunction_useSimilar_unqualified_fromClass_super() throws Exception {
    proposalNamePrefix = "Change to";
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  sayHello() {}",
        "}",
        "class B extends A{",
        "  foo() {",
        "    syaHelol();",
        "  }",
        "}",
        "");
    assertQuickFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  sayHello() {}",
        "}",
        "class B extends A{",
        "  foo() {",
        "    sayHello();",
        "  }",
        "}",
        "");
  }

  public void test_unresolvedFunction_useSimilar_unqualified_fromLibrary_import() throws Exception {
    proposalNamePrefix = "Change to";
    setUnitContent("MyLib.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "library my_lib;",
        "sayHello() {}",
        ""});
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library app;",
        "import 'MyLib.dart';",
        "main() {",
        "  syaHelol();",
        "}",
        "");
    assertQuickFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library app;",
        "import 'MyLib.dart';",
        "main() {",
        "  sayHello();",
        "}",
        "");
  }

  public void test_unresolvedFunction_useSimilar_unqualified_fromLibrary_same() throws Exception {
    proposalNamePrefix = "Change to";
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  tset();",
        "}",
        "test() {}",
        "");
    assertQuickFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  test();",
        "}",
        "test() {}",
        "");
  }

  public void test_useStaticAccess_method() throws Exception {
    proposalNamePrefix = "Change access to static using 'A'";
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        " static foo() {}",
        "}",
        "main() {",
        "  A aaaa = new A();",
        "  aaaa.foo();",
        "}");
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
    proposalNamePrefix = "Change access to static using 'lib.A'";
    setUnitContent("Lib.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "library Lib;",
        "class A {",
        " static foo() {}",
        "}",
        ""});
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'Lib.dart' as lib;",
        "main() {",
        "  lib.A aaaa = new lib.A();",
        "  aaaa.foo();",
        "}",
        "");
    assertQuickFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'Lib.dart' as lib;",
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
  @SuppressWarnings("unused")
  private void assertNoQuickFix() throws CoreException {
    IDartCompletionProposal[] proposals = prepareQuickFixes();
    assertThat(proposals).isEmpty();
  }

  /**
   * Runs single proposal created for {@link IProblemLocation} using "problem*" fields.
   */
  private void assertQuickFix(String... expectedLines) throws CoreException {
    IDartCompletionProposal[] proposals = prepareQuickFixes();
    // select proposal using name prefix
    IDartCompletionProposal selectedProposal = null;
    assertNotNull(proposalNamePrefix);
    for (IDartCompletionProposal proposal : proposals) {
      if (proposal.getDisplayString().startsWith(proposalNamePrefix)) {
        assertNull(selectedProposal);
        selectedProposal = proposal;
      }
    }
    assertNotNull(selectedProposal);
    // prepare result
    String result = ((CUCorrectionProposal) selectedProposal).getPreviewContent();
    // assert result
    String expectedSource = makeSource(expectedLines);
    assertEquals(expectedSource, result);
  }

  /**
   * Waits for a find single Dart problem in {@link #testUnit}.
   */
  private IProblemLocation findProblemLocation() throws CoreException {
    long start = System.currentTimeMillis();
    while (System.currentTimeMillis() - start < 5000) {
      IMarker[] markers = testUnit.getResource().findMarkers(
          DartCore.DART_PROBLEM_MARKER_TYPE,
          true,
          IResource.DEPTH_INFINITE);
      if (markers.length != 0) {
        assertThat(markers).hasSize(1);
        IMarker marker = markers[0];
        // if "errorCode" is not set, then marker is not fully configured yet
        ErrorCode problemCode;
        {
          String qualifiedName = marker.getAttribute("errorCode", (String) null);
          if (qualifiedName == null) {
            continue;
          }
          assertThat(qualifiedName).isNotNull();
          problemCode = ErrorCode.Helper.forQualifiedName(qualifiedName);
        }
        // get location
        int problemOffset = marker.getAttribute(IMarker.CHAR_START, -1);
        int problemLength = marker.getAttribute(IMarker.CHAR_END, -1) - problemOffset;
        // done
        return new ProblemLocation(
            problemOffset,
            problemLength,
            problemCode,
            ArrayUtils.EMPTY_STRING_ARRAY,
            true,
            DartCore.DART_PROBLEM_MARKER_TYPE);
      }
      // wait
      waitEventLoop(0);
    }
    fail("Could not find Dart problem in 5000 ms");
    return null;
  }

  /**
   * @return proposals created for {@link IProblemLocation} using "problem*" fields.
   */
  private IDartCompletionProposal[] prepareQuickFixes() throws CoreException {
    IProblemLocation problemLocation = findProblemLocation();
    IProblemLocation problemLocations[] = new IProblemLocation[] {problemLocation};
    // prepare context
    AssistContext context = new AssistContext(
        testUnit,
        problemLocation.getOffset(),
        problemLocation.getLength());
    assertTrue(PROCESSOR.hasCorrections(testUnit, problemLocation.getProblemId()));
    // run single proposal
    IDartCompletionProposal[] proposals = PROCESSOR.getCorrections(context, problemLocations);
    sortByRelavance(proposals);
    return proposals;
  }

  private void sortByRelavance(IDartCompletionProposal[] proposals) {
    Arrays.sort(proposals, new Comparator<IDartCompletionProposal>() {
      @Override
      public int compare(IDartCompletionProposal o1, IDartCompletionProposal o2) {
        return o2.getRelevance() - o1.getRelevance();
      }
    });
  }
}
