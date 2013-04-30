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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
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
import com.google.dart.engine.services.correction.CreateFileCorrectionProposal;
import com.google.dart.engine.services.correction.QuickFixProcessor;
import com.google.dart.engine.services.correction.SourceCorrectionProposal;
import com.google.dart.engine.services.internal.refactoring.RefactoringImplTest;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.SourceRange;

import org.apache.commons.lang3.StringUtils;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

public class QuickFixProcessorImplTest extends RefactoringImplTest {
  private static final QuickFixProcessor PROCESSOR = CorrectionProcessors.getQuickFixProcessor();

  private AnalysisError error;
  private SourceCorrectionProposal resultProposal;
  private String resultCode;

  public void fail_test_importLibrary_withTopLevelVariable() throws Exception {
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
    }
    // process unit
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  myTopLevelVariable = null;",
        "}",
        "");
    analysisContext.computeLibraryElement(libSource);
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

  public void test_createClass() throws Exception {
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  Test v = null;",
        "}");
    assert_runProcessor(
        CorrectionKind.QF_CREATE_CLASS,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "main() {",
            "  Test v = null;",
            "}",
            "",
            "class Test {",
            "}"));
    assertEquals(
        ImmutableMap.of("NAME", getResultRanges("Test v =", "Test {")),
        resultProposal.getLinkedPositions());
  }

  public void test_createPart() throws Exception {
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library app;",
        "part 'my_part.dart';",
        "");
    CreateFileCorrectionProposal proposal = (CreateFileCorrectionProposal) findProposal(CorrectionKind.QF_CREATE_PART);
    assertThat(proposal.getFile().getPath()).endsWith("my_part.dart");
    {
      String eol = getTestCorrectionUtils().getEndOfLine();
      assertEquals("part of app;" + eol + eol, proposal.getContent());
    }
  }

  public void test_createPart_absoluteUri() throws Exception {
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library app;",
        "part 'package:my_part.dart';",
        "");
    assertNoFix(CorrectionKind.QF_CREATE_PART);
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
    assertNoFix(CorrectionKind.QF_IMPORT_LIBRARY_PREFIX);
    assertNoFix(CorrectionKind.QF_IMPORT_LIBRARY_PROJECT);
    assertNoFix(CorrectionKind.QF_IMPORT_LIBRARY_SDK);
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
    }
    // process unit
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  myFunction();",
        "}",
        "");
    analysisContext.computeLibraryElement(libSource);
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
    SourceCorrectionProposal proposal = (SourceCorrectionProposal) findProposal(CorrectionKind.QF_IMPORT_LIBRARY_PROJECT);
    assertNotNull(proposal);
    // we have "fix", note that preview is for library
    SourceChange appChange = proposal.getChange();
    assertSame(appSource, appChange.getSource());
    assertChangeResult(
        appChange,
        appSource,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library App;",
            "import 'dart:core';",
            "import 'LibA.dart';",
            "part 'Test.dart';",
            ""));
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
   * @return the result of applying {@link SourceCorrectionProposal} to the {@link #testCode}.
   */
  private String applyProposal(SourceCorrectionProposal proposal) {
    SourceChange change = proposal.getChange();
    List<Edit> edits = change.getEdits();
    return CorrectionUtils.applyReplaceEdits(testCode, edits);
  }

  /**
   * Asserts that running proposal with given name produces expected source. Fills
   * {@link #resultProposal} and {@link #resultCode}.
   */
  private void assert_runProcessor(CorrectionKind kind, String expectedSource) throws Exception {
    resultProposal = (SourceCorrectionProposal) findProposal(kind);
    resultCode = applyProposal(resultProposal);
    assertEquals(expectedSource, resultCode);
  }

  private void assertNoFix(CorrectionKind kind) throws Exception {
    CorrectionProposal proposal = findProposal(kind);
    assertNull(proposal);
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
   * @return the {@link SourceRange} of "wordPattern" in {@link #resultCode}.
   */
  private SourceRange getResultRange(String wordPattern) {
    int offset = resultCode.indexOf(wordPattern);
    assertThat(offset).describedAs(wordPattern + " in " + resultCode).isPositive();
    String word = StringUtils.substringBefore(wordPattern, " ");
    return new SourceRange(offset, word.length());
  }

  /**
   * @return the {@link SourceRange}s of "wordPatterns" in {@link #resultCode}.
   */
  private List<SourceRange> getResultRanges(String... wordPatterns) {
    List<SourceRange> ranges = Lists.newArrayList();
    for (String wordPattern : wordPatterns) {
      ranges.add(getResultRange(wordPattern));
    }
    return ranges;
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
