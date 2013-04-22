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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.resolver.ResolverErrorCode;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.correction.CorrectionKind;
import com.google.dart.engine.services.correction.CorrectionProcessors;
import com.google.dart.engine.services.correction.CorrectionProposal;
import com.google.dart.engine.services.correction.QuickFixProcessor;
import com.google.dart.engine.services.internal.refactoring.RefactoringImplTest;
import com.google.dart.engine.source.Source;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

public class QuickFixProcessorImplTest extends RefactoringImplTest {
  private static final QuickFixProcessor PROCESSOR = CorrectionProcessors.getQuickFixProcessor();

  private AnalysisError error;

  public void test_boolean() throws Exception {
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  boolean v;",
        "}");
    assert_runProcessor(
        CorrectionKind.QF_REPLACE_BOOLEAN_WITH_BOOL,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "main() {",
            "  bool v;",
            "}"));
  }

  public void test_computeProposals_noContext() throws Exception {
    AnalysisError emptyError = new AnalysisError(
        testSource,
        ResolverErrorCode.MISSING_LIBRARY_DIRECTIVE_WITH_PART);
    CorrectionProposal[] proposals = PROCESSOR.computeProposals(null, emptyError);
    assertThat(proposals).isEmpty();
  }

  public void test_computeProposals_noLibraryElement() throws Exception {
    // prepare CompilationUnit with CompilationUnitElement, but without LibraryElement
    CompilationUnit unit = mock(CompilationUnit.class);
    CompilationUnitElement unitElement = mock(CompilationUnitElement.class);
    when(unit.getElement()).thenReturn(unitElement);
    // prepare context
    AssistContext context = new AssistContext(null, unit, 0, 0);
    AnalysisError problem = new AnalysisError(
        testSource,
        ResolverErrorCode.MISSING_LIBRARY_DIRECTIVE_WITH_PART);
    CorrectionProposal[] proposals = PROCESSOR.computeProposals(context, problem);
    assertThat(proposals).isEmpty();
  }

  public void test_computeProposals_noProblem() throws Exception {
    AssistContext emptyContext = new AssistContext(null, null, 0, 0);
    CorrectionProposal[] proposals = PROCESSOR.computeProposals(emptyContext, null);
    assertThat(proposals).isEmpty();
  }

  public void test_computeProposals_noUnitElement() throws Exception {
    // prepare CompilationUnit without CompilationUnitElement
    CompilationUnit unit = mock(CompilationUnit.class);
    // prepare context
    AssistContext context = new AssistContext(null, unit, 0, 0);
    AnalysisError problem = new AnalysisError(
        testSource,
        ResolverErrorCode.MISSING_LIBRARY_DIRECTIVE_WITH_PART);
    CorrectionProposal[] proposals = PROCESSOR.computeProposals(context, problem);
    assertThat(proposals).isEmpty();
  }

  public void test_expectedToken_semicolon() throws Exception {
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print()",
        "}");
    assert_runProcessor(
        CorrectionKind.QF_INSERT_SEMICOLON,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "main() {",
            "  print();",
            "}"));
  }

  public void test_getSourceFile_notFileBasedSource() throws Exception {
    Source source = mock(Source.class);
    assertNull(QuickFixProcessorImpl.getSourceFile(source));
  }

  // TODO(scheglov) waiting https://code.google.com/p/dart/issues/detail?id=10116
  public void test_importLibrary_fromSDK_notType() throws Exception {
//    ensureSdkLibraryAsync();
//    prepareProblemWithFix(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "main() {",
//        "  print v = null;",
//        "}",
//        "");
//    // "print" is used as type, but it isn't
//    assertNoFix();
  }

  public void test_importLibrary_privateName() throws Exception {
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  _PrivateName v = null;",
        "}",
        "");
    assertNoFix();
  }

  public void test_importLibrary_withTopLevelFunction() throws Exception {
    Source libSource = setFileContent(
        "LibA.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library A;",
            "myFunction() {}",
            ""));
    // prepare AnalysisContext
    ensureAnalysisContext();
    // process "libSource"
    {
      ChangeSet changeSet = new ChangeSet();
      changeSet.added(libSource);
      analysisContext.applyChanges(changeSet);
      analysisContext.computeLibraryElement(libSource);
    }
    // process unit
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  myFunction();",
        "}",
        "");
    assert_runProcessor(
        CorrectionKind.QF_IMPORT_LIBRARY_PROJECT,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "",
            "import 'LibA.dart';",
            "",
            "main() {",
            "  myFunction();",
            "}",
            ""));
  }

  public void test_importLibrary_withTopLevelVariable() throws Exception {
    Source libSource = setFileContent(
        "LibA.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library A;",
            "var myTopLevelVariable;",
            ""));
    // prepare AnalysisContext
    ensureAnalysisContext();
    // process "libSource"
    {
      ChangeSet changeSet = new ChangeSet();
      changeSet.added(libSource);
      analysisContext.applyChanges(changeSet);
      analysisContext.computeLibraryElement(libSource);
    }
    // process unit
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  myTopLevelVariable = null;",
        "}",
        "");
    assert_runProcessor(
        CorrectionKind.QF_IMPORT_LIBRARY_PROJECT,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "",
            "import 'LibA.dart';",
            "",
            "main() {",
            "  myTopLevelVariable = null;",
            "}",
            ""));
  }

  public void test_importLibrary_withType_hasDirectiveImport() throws Exception {
    Source libSource = setFileContent(
        "LibA.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library A;",
            "class AAA {",
            "}",
            ""));
    Source appSource = setFileContent(
        "App.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library App;",
            "import 'dart:core';",
            "part 'Test.dart';",
            ""));
    Source partSource = setFileContent(
        "Test.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "part of App;",
            "main() {",
            "  AAA a = null;",
            "}",
            ""));
    // prepare AnalysisContext
    ensureAnalysisContext();
    {
      ChangeSet changeSet = new ChangeSet();
      changeSet.added(libSource);
      changeSet.added(appSource);
      changeSet.added(partSource);
      analysisContext.applyChanges(changeSet);
    }
    // fill "test*" fields
    testLibraryElement = analysisContext.computeLibraryElement(appSource);
    testUnit = analysisContext.resolveCompilationUnit(partSource, testLibraryElement);
    // process "libSource"
    analysisContext.computeLibraryElement(libSource);
    // prepare proposal
    prepareProblemWithFix();
    CorrectionProposal proposal = findProposal(CorrectionKind.QF_IMPORT_LIBRARY_PROJECT);
    assertNotNull(proposal);
    // we have "fix", note that preview is for library
    SourceChange compositeChange = proposal.getChanges().get(0);
    assertChangeResult(
        compositeChange,
        appSource,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library App;",
            "import 'dart:core';",
            "import 'LibA.dart';",
            "part 'Test.dart';",
            ""));
    // unit itself is not changed
    assertNull(getSourceChange(compositeChange, partSource));
  }

  public void test_importLibrary_withType_hasImportWithPrefix() throws Exception {
    ensureSdkLibraryAsync();
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:async' as pref;",
        "main() {",
        "  Future f = null;",
        "}",
        "");
    assert_runProcessor(
        CorrectionKind.QF_IMPORT_LIBRARY_PREFIX,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "import 'dart:async' as pref;",
            "main() {",
            "  pref.Future f = null;",
            "}",
            ""));
  }

  public void test_importLibrary_withType_invocationTarget_fromSDK() throws Exception {
    ensureSdkLibraryAsync();
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  Future.wait(null);",
        "}",
        "");
    assert_runProcessor(
        CorrectionKind.QF_IMPORT_LIBRARY_SDK,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "",
            "import 'dart:async';",
            "",
            "main() {",
            "  Future.wait(null);",
            "}",
            ""));
  }

  public void test_importLibrary_withType_typeAnnotation_fromSDK() throws Exception {
    ensureSdkLibraryAsync();
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  Future f = null;",
        "}",
        "");
    assert_runProcessor(
        CorrectionKind.QF_IMPORT_LIBRARY_SDK,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "",
            "import 'dart:async';",
            "",
            "main() {",
            "  Future f = null;",
            "}",
            ""));
  }

  public void test_removeParentheses_inGetterDeclaration() throws Exception {
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int get foo() => 0;",
        "}",
        "");
    assert_runProcessor(
        CorrectionKind.QF_REMOVE_PARAMETERS_IN_GETTER_DECLARATION,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "class A {",
            "  int get foo => 0;",
            "}",
            ""));
  }

  public void test_removeParentheses_inGetterInvocation() throws Exception {
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int get foo => 0;",
        "}",
        "main() {",
        "  A a = new A();",
        "  a.foo();",
        "}",
        "");
    assert_runProcessor(
        CorrectionKind.QF_REMOVE_PARENTHESIS_IN_GETTER_INVOCATION,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "class A {",
            "  int get foo => 0;",
            "}",
            "main() {",
            "  A a = new A();",
            "  a.foo;",
            "}",
            ""));
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
  private void assert_runProcessor(CorrectionKind kind, String expectedSource) throws Exception {
    CorrectionProposal proposal = findProposal(kind);
    assertNotNull(proposal);
    String result = applyProposal(proposal);
    assertEquals(expectedSource, result);
  }

  private void assertNoFix() throws Exception {
    prepareProblem();
    CorrectionProposal[] proposals = getProposals();
    assertThat(proposals).isEmpty();
  }

  /**
   * Parse unit with library 'async' to have its {@link LibraryElement} ready.
   */
  private void ensureSdkLibraryAsync() throws Exception {
    parseTestUnit("import 'dart:async';");
  }

  /**
   * @return the {@link CorrectionProposal} with the given {@link CorrectionKind}.
   */
  private CorrectionProposal findProposal(CorrectionKind kind) throws Exception {
    CorrectionProposal[] proposals = getProposals();
    // find and apply required proposal
    for (CorrectionProposal proposal : proposals) {
      if (proposal.getKind() == kind) {
        return proposal;
      }
    }
    // not found
    return null;
  }

  private CorrectionProposal[] getProposals() throws Exception {
    AssistContext context = new AssistContext(null, testUnit, 0, 0);
    return PROCESSOR.computeProposals(context, error);
  }

  /**
   * Prepares single error to fix and stores to {@link #error}.
   */
  private void prepareProblem() {
    AnalysisError[] errors = testUnit.getErrors();
    assertThat(errors).hasSize(1);
    error = errors[0];
  }

  /**
   * Analyzes {@link #testUnit} and checks that {@link QuickFixProcessor#hasFix(AnalysisError)}.
   */
  private void prepareProblemWithFix() {
    prepareProblem();
    {
      boolean hasFix = PROCESSOR.hasFix(error);
      ErrorCode errorCode = error.getErrorCode();
      String errorCodeStr = errorCode.getClass().getSimpleName() + "." + errorCode;
      assertTrue(errorCodeStr + " " + error.getMessage(), hasFix);
    }
  }

  /**
   * Prepares {@link #error} and checks that {@link QuickFixProcessor#hasFix(AnalysisError)}.
   */
  private void prepareProblemWithFix(String... lines) throws Exception {
    parseTestUnit(makeSource(lines));
    prepareProblemWithFix();
  }
}
