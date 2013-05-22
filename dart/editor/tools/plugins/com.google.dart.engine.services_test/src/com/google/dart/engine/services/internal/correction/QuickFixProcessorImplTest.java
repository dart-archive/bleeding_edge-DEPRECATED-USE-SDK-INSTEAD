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

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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
import com.google.dart.engine.services.correction.LinkedPositionProposal;
import com.google.dart.engine.services.correction.QuickFixProcessor;
import com.google.dart.engine.services.correction.SourceCorrectionProposal;
import com.google.dart.engine.services.internal.refactoring.RefactoringImplTest;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.SourceRange;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class QuickFixProcessorImplTest extends RefactoringImplTest {
  private static final QuickFixProcessor PROCESSOR = CorrectionProcessors.getQuickFixProcessor();
  private static CharMatcher NOT_IDENTIFIER_MATCHER = CharMatcher.JAVA_LETTER_OR_DIGIT.negate();

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
        "  print(0)",
        "}");
    assert_runProcessor(
        CorrectionKind.QF_INSERT_SEMICOLON,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "main() {",
            "  print(0);",
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

  public void test_undefinedClass_useSimilar_fromThisLibrary() throws Exception {
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class MyClass {}",
        "main() {",
        "  MyCalss v = null;",
        "}");
    assert_runProcessor(
        CorrectionKind.QF_CHANGE_TO,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "class MyClass {}",
            "main() {",
            "  MyClass v = null;",
            "}"));
  }

  public void test_undefinedClass_useSimilar_String() throws Exception {
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  Stirng s = 'abc';",
        "}");
    assert_runProcessor(
        CorrectionKind.QF_CHANGE_TO,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "main() {",
            "  String s = 'abc';",
            "}"));
  }

  public void test_undefinedFunction_useSimilar_fromImport() throws Exception {
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  pritn(0);",
        "}");
    assert_runProcessor(
        CorrectionKind.QF_CHANGE_TO,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "main() {",
            "  print(0);",
            "}"));
  }

  public void test_undefinedFunction_useSimilar_thisLibrary() throws Exception {
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "myFunction() {}",
        "main() {",
        "  myFuntcion();",
        "}");
    assert_runProcessor(
        CorrectionKind.QF_CHANGE_TO,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "myFunction() {}",
            "main() {",
            "  myFunction();",
            "}"));
  }

  public void test_undefinedMethod_createQualified_fromClass() throws Exception {
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "}",
        "main() {",
        "  A.myUndefinedMethod();",
        "}",
        "");
    assert_runProcessor(
        CorrectionKind.QF_CREATE_METHOD,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "class A {",
            "  static myUndefinedMethod() {",
            "  }",
            "}",
            "main() {",
            "  A.myUndefinedMethod();",
            "}",
            ""));
  }

  public void test_undefinedMethod_createQualified_fromClass_hasOtherMember() throws Exception {
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo() {}",
        "}",
        "main() {",
        "  A.myUndefinedMethod();",
        "}",
        "");
    assert_runProcessor(
        CorrectionKind.QF_CREATE_METHOD,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "class A {",
            "  foo() {}",
            "  ",
            "  static myUndefinedMethod() {",
            "  }",
            "}",
            "main() {",
            "  A.myUndefinedMethod();",
            "}",
            ""));
  }

  public void test_undefinedMethod_createQualified_fromClass_unresolved() throws Exception {
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  NoSuchClass.myUndefinedMethod();",
        "}",
        "");
    assertNoFix(CorrectionKind.QF_CREATE_METHOD);
  }

  public void test_undefinedMethod_createQualified_fromInstance() throws Exception {
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "}",
        "main() {",
        "  A a = new A();",
        "  a.myUndefinedMethod();",
        "}",
        "");
    assert_runProcessor(
        CorrectionKind.QF_CREATE_METHOD,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "class A {",
            "  myUndefinedMethod() {",
            "  }",
            "}",
            "main() {",
            "  A a = new A();",
            "  a.myUndefinedMethod();",
            "}",
            ""));
    // linked positions
    {
      Map<String, List<SourceRange>> expected = Maps.newHashMap();
      expected.put("NAME", getResultRanges("myUndefinedMethod();", "myUndefinedMethod() {"));
      assertEquals(expected, resultProposal.getLinkedPositions());
    }
  }

  public void test_undefinedMethod_createUnqualified_parameters() throws Exception {
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  main() {",
        "    myUndefinedMethod(0, 1.0, '3');",
        "  }",
        "}");
    assert_runProcessor(
        CorrectionKind.QF_CREATE_METHOD,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "class A {",
            "  main() {",
            "    myUndefinedMethod(0, 1.0, '3');",
            "  }",
            "  ",
            "  myUndefinedMethod(int i, double d, String s) {",
            "  }",
            "}"));
    // linked positions
    {
      Map<String, List<SourceRange>> expected = Maps.newHashMap();
      expected.put("NAME", getResultRanges("myUndefinedMethod(0", "myUndefinedMethod(int"));
      expected.put("TYPE0", getResultRanges("int i"));
      expected.put("TYPE1", getResultRanges("double d"));
      expected.put("TYPE2", getResultRanges("String s"));
      expected.put("ARG0", getResultRanges("i,"));
      expected.put("ARG1", getResultRanges("d,"));
      expected.put("ARG2", getResultRanges("s)"));
      assertEquals(expected, resultProposal.getLinkedPositions());
    }
    // linked proposals
    assertLinkedProposals("TYPE0", "int", "num", "Comparable", "Object");
    assertLinkedProposals("TYPE1", "double", "num", "Comparable", "Object");
    assertLinkedProposals("TYPE2", "String", "Comparable", "Object");
  }

  public void test_undefinedMethod_createUnqualified_returnType() throws Exception {
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  main() {",
        "    int v = myUndefinedMethod();",
        "  }",
        "}");
    assert_runProcessor(
        CorrectionKind.QF_CREATE_METHOD,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "class A {",
            "  main() {",
            "    int v = myUndefinedMethod();",
            "  }",
            "  ",
            "  int myUndefinedMethod() {",
            "  }",
            "}"));
    // linked positions
    {
      Map<String, List<SourceRange>> expected = Maps.newHashMap();
      expected.put("NAME", getResultRanges("myUndefinedMethod();", "myUndefinedMethod() {"));
      expected.put("RETURN_TYPE", getResultRanges("int myUndefinedMethod()"));
      assertEquals(expected, resultProposal.getLinkedPositions());
    }
  }

  public void test_undefinedMethod_createUnqualified_staticFromField() throws Exception {
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static var f = myUndefinedMethod();",
        "}");
    assert_runProcessor(
        CorrectionKind.QF_CREATE_METHOD,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "class A {",
            "  static var f = myUndefinedMethod();",
            "  ",
            "  static myUndefinedMethod() {",
            "  }",
            "}"));
  }

  public void test_undefinedMethod_createUnqualified_staticFromMethod() throws Exception {
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static main() {",
        "    myUndefinedMethod();",
        "  }",
        "}");
    assert_runProcessor(
        CorrectionKind.QF_CREATE_METHOD,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "class A {",
            "  static main() {",
            "    myUndefinedMethod();",
            "  }",
            "  ",
            "  static myUndefinedMethod() {",
            "  }",
            "}"));
  }

  public void test_undefinedMethod_useSimilar_qualified() throws Exception {
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  myMethod() {}",
        "}",
        "main() {",
        "  A a = new A();",
        "  a.myMehtod();",
        "}",
        "");
    assert_runProcessor(
        CorrectionKind.QF_CHANGE_TO,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "class A {",
            "  myMethod() {}",
            "}",
            "main() {",
            "  A a = new A();",
            "  a.myMethod();",
            "}",
            ""));
  }

  public void test_undefinedMethod_useSimilar_unqualified_superClass() throws Exception {
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  myMethod() {}",
        "}",
        "class B extends A {",
        "  main() {",
        "    myMehtod();",
        "  }",
        "}");
    assert_runProcessor(
        CorrectionKind.QF_CHANGE_TO,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "class A {",
            "  myMethod() {}",
            "}",
            "class B extends A {",
            "  main() {",
            "    myMethod();",
            "  }",
            "}"));
  }

  public void test_undefinedMethod_useSimilar_unqualified_thisClass() throws Exception {
    prepareProblemWithFix(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  myMethod() {}",
        "  main() {",
        "    myMehtod();",
        "  }",
        "}");
    assert_runProcessor(
        CorrectionKind.QF_CHANGE_TO,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "class A {",
            "  myMethod() {}",
            "  main() {",
            "    myMethod();",
            "  }",
            "}"));
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
    assertNotNull(kind.name(), resultProposal);
    resultCode = applyProposal(resultProposal);
    assertEquals(expectedSource, resultCode);
  }

  private void assertLinkedProposals(String positionName, String... expectedNames) {
    List<LinkedPositionProposal> proposals = resultProposal.getLinkedPositionProposals().get(
        positionName);
    Set<String> actualNames = Sets.newHashSet();
    for (LinkedPositionProposal proposal : proposals) {
      actualNames.add(proposal.getText());
    }
    assertThat(actualNames).contains((Object[]) expectedNames);
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
   * @return the {@link SourceRange} of "identPattern" in {@link #resultCode}.
   */
  private SourceRange getResultRange(String identPattern) {
    int offset = resultCode.indexOf(identPattern);
    assertThat(offset).describedAs(identPattern + " in " + resultCode).isPositive();
    String identifier = identPattern.substring(0, NOT_IDENTIFIER_MATCHER.indexIn(identPattern));
    return new SourceRange(offset, identifier.length());
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
