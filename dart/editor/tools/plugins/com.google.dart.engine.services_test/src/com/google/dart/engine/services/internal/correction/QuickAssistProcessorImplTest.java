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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.change.CreateFileChange;
import com.google.dart.engine.services.change.Edit;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.correction.ChangeCorrectionProposal;
import com.google.dart.engine.services.correction.CorrectionKind;
import com.google.dart.engine.services.correction.CorrectionProcessors;
import com.google.dart.engine.services.correction.CorrectionProposal;
import com.google.dart.engine.services.correction.LinkedPositionProposal;
import com.google.dart.engine.services.correction.QuickAssistProcessor;
import com.google.dart.engine.services.correction.SourceCorrectionProposal;
import com.google.dart.engine.services.internal.refactoring.RefactoringImplTest;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.SourceRange;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class QuickAssistProcessorImplTest extends RefactoringImplTest {
  private static final QuickAssistProcessor PROCESSOR = CorrectionProcessors.getQuickAssistProcessor();
  private static CharMatcher NOT_IDENTIFIER_MATCHER = CharMatcher.JAVA_LETTER_OR_DIGIT.negate();

  /**
   * @return the result of applying {@link SourceCorrectionProposal} to the {@link #testCode}.
   */
  private static String applyProposal(String code, SourceCorrectionProposal proposal) {
    SourceChange change = proposal.getChange();
    List<Edit> edits = change.getEdits();
    return CorrectionUtils.applyReplaceEdits(code, edits);
  }

  /**
   * @return the {@link CorrectionProposal} with the given kind.
   */
  private static CorrectionProposal findProposal(CorrectionProposal[] proposals, CorrectionKind kind) {
    for (CorrectionProposal proposal : proposals) {
      if (proposal.getKind() == kind) {
        return proposal;
      }
    }
    return null;
  }

  private int selectionOffset = 0;
  private int selectionLength = 0;

  private SourceCorrectionProposal resultProposal;
  private String resultSource;

  public void test_addPartDirective() throws Exception {
    String libCode = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library app;");
    String testCode = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "part of app;");
    // add sources
    addSource("/my_app.dart", libCode);
    Source testSource = addSource("/test.dart", testCode);
    // build LibrarySource(s)
    while (analysisContext.performAnalysisTask().getChangeNotices() != null) {
    }
    // use parsed, but not resolved test.dart
    CompilationUnit testUnit = analysisContext.parseCompilationUnit(testSource);
    CorrectionProposal[] proposals = CorrectionProcessors.getQuickAssistProcessor().getProposals(
        analysisContext,
        testSource,
        testUnit,
        testCode.indexOf("part of"));
    assert_runProcessor(
        libCode,
        proposals,
        CorrectionKind.QA_ADD_PART_DIRECTIVE,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library app;",
            "",
            "part 'test.dart';"));
  }

  public void test_addTypeAnnotation_classField_OK_final() throws Exception {
    assert_addTypeAnnotation_classField("final v = 1;", " = 1", "final int v = 1;");
  }

  public void test_addTypeAnnotation_classField_OK_int() throws Exception {
    assert_addTypeAnnotation_classField("var v = 1;", " = 1", "int v = 1;");
  }

  public void test_addTypeAnnotation_local_OK_Function() throws Exception {
    assert_addTypeAnnotation_localVariable("var v = () => 1;", " = ()", "Function v = () => 1;");
  }

  public void test_addTypeAnnotation_local_OK_int() throws Exception {
    assert_addTypeAnnotation_localVariable("var v = 1;", " = 1", "int v = 1;");
  }

  public void test_addTypeAnnotation_local_OK_List() throws Exception {
    assert_addTypeAnnotation_localVariable(
        "var v = new List<String>();",
        " = new",
        "List<String> v = new List<String>();");
  }

  public void test_addTypeAnnotation_local_OK_onInitializer() throws Exception {
    assert_addTypeAnnotation_localVariable("var v = 123;", "23;", "int v = 123;");
  }

  public void test_addTypeAnnotation_local_OK_onName() throws Exception {
    assert_addTypeAnnotation_localVariable("var abc = 1;", "bc ", "int abc = 1;");
  }

  public void test_addTypeAnnotation_local_OK_onVar() throws Exception {
    assert_addTypeAnnotation_localVariable("var v = 1;", "var ", "int v = 1;");
  }

  public void test_addTypeAnnotation_local_wrong_hasTypeAnnotation() throws Exception {
    String source = "int v = 42;";
    assert_addTypeAnnotation_localVariable(source, " = 42", source);
  }

  public void test_addTypeAnnotation_local_wrong_multiple() throws Exception {
    String source = "var a = 1, b = '';";
    assert_addTypeAnnotation_localVariable(source, "var ", source);
  }

  public void test_addTypeAnnotation_local_wrong_null() throws Exception {
    String source = "var v = null;";
    assert_addTypeAnnotation_localVariable(source, " = null", source);
  }

  public void test_addTypeAnnotation_local_wrong_unknown() throws Exception {
    verifyNoTestUnitErrors = false;
    String source = "var v = unknownVar;";
    assert_addTypeAnnotation_localVariable(source, " = unknown", source);
  }

  public void test_addTypeAnnotation_topLevelField_OK_int() throws Exception {
    assert_addTypeAnnotation_topLevelField("var v = 1;", " = 1", "int v = 1;");
  }

  public void test_addTypeAnnotation_topLevelField_OK_onVar() throws Exception {
    assert_addTypeAnnotation_topLevelField("var v = 1;", "var", "int v = 1;");
  }

  public void test_addTypeAnnotation_topLevelField_wrong_multiple() throws Exception {
    String source = "var a = 1, b = '';";
    assert_addTypeAnnotation_topLevelField(source, "var ", source);
  }

  public void test_addTypeAnnotation_topLevelField_wrong_noValue() throws Exception {
    String source = "var v;";
    assert_addTypeAnnotation_topLevelField(source, "var ", source);
  }

  public void test_assignToLocalVariable() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "List<int> readBytes() => <int> [];",
        "main() {",
        "  List<int> bytes;",
        "  readBytes();",
        "}",
        "");
    assert_assignToLocalVariable(
        initial,
        "readBytes();",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "List<int> readBytes() => <int> [];",
            "main() {",
            "  List<int> bytes;",
            "  var readBytes = readBytes();",
            "}",
            ""));
    // linked positions
    {
      Map<String, List<SourceRange>> expected = Maps.newHashMap();
      expected.put("NAME", getResultRanges("readBytes ="));
      assertEquals(expected, resultProposal.getLinkedPositions());
    }
    // linked proposals
    assertLinkedProposals("NAME", "list", "bytes2", "readBytes");
  }

  public void test_assignToLocalVariable_alreadyAssignment() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "List<int> readBytes() => <int> [];",
        "main() {",
        "  List<int> bytes;",
        "  bytes = readBytes();",
        "}",
        "");
    assert_assignToLocalVariable(initial, "readBytes();", initial);
  }

  public void test_assignToLocalVariable_notExistingWithPrefix() throws Exception {
    verifyNoTestUnitErrors = false;
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:math' as p;",
        "main() {",
        "  new p.NoSuchClass();",
        "}",
        "");
    assert_assignToLocalVariable(
        initial,
        "new p.NoSuchClass()",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "import 'dart:math' as p;",
            "main() {",
            "  var noSuchClass = new p.NoSuchClass();",
            "}",
            ""));
    // linked positions
    {
      Map<String, List<SourceRange>> expected = Maps.newHashMap();
      expected.put("NAME", getResultRanges("noSuchClass ="));
      assertEquals(expected, resultProposal.getLinkedPositions());
    }
    // linked proposals
    assertLinkedProposals("NAME", "noSuchClass", "suchClass", "class");
  }

  public void test_assignToLocalVariable_throw() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  throw 42;",
        "}",
        "");
    assert_assignToLocalVariable(initial, "throw 42;", initial);
  }

  public void test_convertToBlockBody_OK_closure() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "setup(x) {}",
        "main() {",
        "  setup(() => print('done'));",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "setup(x) {}",
        "main() {",
        "  setup(() {",
        "    return print('done');",
        "  });",
        "}",
        "");
    assert_convertToBlockBody(initial, "() => print", expected);
  }

  public void test_convertToBlockBody_OK_method() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "fff() => 123;");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "fff() {",
        "  return 123;",
        "}");
    assert_convertToBlockBody(initial, "fff() ", expected);
  }

  public void test_convertToBlockBody_OK_onName() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  fff() => 123;",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  fff() {",
        "    return 123;",
        "  }",
        "}");
    assert_convertToBlockBody(initial, "fff() ", expected);
  }

  public void test_convertToBlockBody_OK_onValue() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() => 123;");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  return 123;",
        "}");
    assert_convertToBlockBody(initial, "23;", expected);
  }

  public void test_convertToBlockBody_wrong_noEnclosingFunction() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var v = 123;");
    assert_convertToBlockBody_wrong(initial, "v = 123");
  }

  public void test_convertToBlockBody_wrong_notExpressionBlock() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  return 123;",
        "}");
    assert_convertToBlockBody_wrong(initial, "return 123;");
  }

  public void test_convertToExpressionBody_OK_closure() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "setup(x) {}",
        "main() {",
        "  setup(() {",
        "    return 42;",
        "  });",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "setup(x) {}",
        "main() {",
        "  setup(() => 42);",
        "}",
        "");
    assert_convertToExpressionBody(initial, "42;", expected);
  }

  public void test_convertToExpressionBody_OK_method_onBlock() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  f() { // marker",
        "    return 0;",
        "  }",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  f() => 0;",
        "}");
    assert_convertToExpressionBody(initial, "{ // marker", expected);
  }

  public void test_convertToExpressionBody_OK_topFunction_onBlock() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() { // marker",
        "  return 0;",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() => 0;");
    assert_convertToExpressionBody(initial, "{ // marker", expected);
  }

  public void test_convertToExpressionBody_OK_topFunction_onName() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "fff() {",
        "  return 0;",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "fff() => 0;");
    assert_convertToExpressionBody(initial, "ff() {", expected);
  }

  public void test_convertToExpressionBody_OK_topFunction_onReturnStatement() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  return 0;",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() => 0;");
    assert_convertToExpressionBody(initial, "return 0;", expected);
  }

  public void test_convertToExpressionBody_wrong_already() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() => 0;");
    assert_convertToExpressionBody_wrong(initial, "f()");
//    assertNoProposal(
//        initial,
//        "f()",
//        "Convert into using function with expression body");
  }

  public void test_convertToExpressionBody_wrong_moreThanOneStatement() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var v1 = 1;",
        "  var v2 = 2;",
        "}");
    assert_convertToExpressionBody_wrong(initial, "v1 = 1");
  }

  public void test_convertToExpressionBody_wrong_noEnclosingFunction() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var v = 0;");
    assert_convertToExpressionBody_wrong(initial, "v = 0");
  }

  public void test_convertToExpressionBody_wrong_noReturn() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var v = 0;",
        "}");
    assert_convertToExpressionBody_wrong(initial, "v = 0");
  }

  public void test_convertToExpressionBody_wrong_noReturnValue() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  return;",
        "}");
    assert_convertToExpressionBody_wrong(initial, "return;");
  }

  public void test_convertToIsNot_OK_childOfIs_left() throws Exception {
    String initial = "!(v is String)";
    String expected = "v is! String";
    assert_convertToIsNot(initial, "v is", expected);
  }

  public void test_convertToIsNot_OK_childOfIs_right() throws Exception {
    String initial = "!(v is String)";
    String expected = "v is! String";
    assert_convertToIsNot(initial, "String)", expected);
  }

  public void test_convertToIsNot_OK_is() throws Exception {
    String initial = "!(v is String)";
    String expected = "v is! String";
    assert_convertToIsNot(initial, "is String", expected);
  }

  public void test_convertToIsNot_OK_is_higherPrecedencePrefix() throws Exception {
    String initial = "!!(v is String)";
    String expected = "!(v is! String)";
    assert_convertToIsNot(initial, "is String", expected);
  }

  public void test_convertToIsNot_OK_is_not_higherPrecedencePrefix() throws Exception {
    String initial = "!!(v is String)";
    String expected = "!(v is! String)";
    assert_convertToIsNot(initial, "!(", expected);
  }

  public void test_convertToIsNot_OK_not() throws Exception {
    String initial = "!(v is String)";
    String expected = "v is! String";
    assert_convertToIsNot(initial, "!", expected);
  }

  public void test_convertToIsNot_OK_parentheses() throws Exception {
    String initial = "!(v is String)";
    String expected = "v is! String";
    assert_convertToIsNot(initial, "(v", expected);
  }

  public void test_convertToIsNot_wrong_is_alreadyIsNot() throws Exception {
    String initial = "v is! String";
    assert_convertToIsNot(initial, "is! String", initial);
  }

  public void test_convertToIsNot_wrong_is_noEnclosingParenthesis() throws Exception {
    String initial = "v is String";
    assert_convertToIsNot(initial, "is String", initial);
  }

  public void test_convertToIsNot_wrong_is_noPrefix() throws Exception {
    String initial = "(v is String)";
    assert_convertToIsNot(initial, "is String", initial);
  }

  public void test_convertToIsNot_wrong_is_notIsExpression() throws Exception {
    String initial = "123 + 456";
    assert_convertToIsNot(initial, "123 +", initial);
  }

  public void test_convertToIsNot_wrong_is_notTheNotOperator() throws Exception {
    verifyNoTestUnitErrors = false;
    String initial = "++(v is String)";
    assert_convertToIsNot(initial, "is String", initial);
  }

  public void test_convertToIsNot_wrong_not_alreadyIsNot() throws Exception {
    String initial = "!(v is! String)";
    assert_convertToIsNot(initial, "!(", initial);
  }

  public void test_convertToIsNot_wrong_not_noEnclosingParenthesis() throws Exception {
    String initial = "!v";
    assert_convertToIsNot(initial, "!v", initial);
  }

  public void test_convertToIsNot_wrong_not_notIsExpression() throws Exception {
    String initial = "!(v == 0)";
    assert_convertToIsNot(initial, "!(", initial);
  }

  public void test_convertToIsNot_wrong_not_notTheNotOperator() throws Exception {
    verifyNoTestUnitErrors = false;
    String initial = "++(v is String)";
    assert_convertToIsNot(initial, "++(", initial);
  }

  public void test_convertToIsNotEmpty_BAD_noIsNotEmpty() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  bool get isEmpty => false;",
        "}",
        "f(A a) {",
        "  !a.isEmpty;",
        "}");
    assert_convertToIsNotEmpty2(initial, "isEmpty;", initial);
  }

  public void test_convertToIsNotEmpty_BAD_notInPrefixExpression() throws Exception {
    assert_convertToIsNotEmpty("str.isEmpty", "isEmpty", "str.isEmpty");
  }

  public void test_convertToIsNotEmpty_BAD_notIsEmpty() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f(int v) {",
        "  !v.isEven;",
        "}");
    assert_convertToIsNotEmpty2(initial, "isEven", initial);
  }

  public void test_convertToIsNotEmpty_BAD_notNegation() throws Exception {
    verifyNoTestUnitErrors = false;
    assert_convertToIsNotEmpty("-str.isEmpty", "isEmpty", "-str.isEmpty");
  }

  public void test_convertToIsNotEmpty_OK_on_isEmpty() throws Exception {
    assert_convertToIsNotEmpty("!str.isEmpty", "isEmpty", "str.isNotEmpty");
  }

  public void test_convertToIsNotEmpty_OK_on_str() throws Exception {
    assert_convertToIsNotEmpty("!str.isEmpty", "tr.", "str.isNotEmpty");
  }

  public void test_convertToIsNotEmpty_OK_propertyAccess() throws Exception {
    assert_convertToIsNotEmpty("!'text'.isEmpty", "isEmpty", "'text'.isNotEmpty");
  }

  /**
   * We should go up only until we have same operator.
   */
  public void test_exchangeBinaryExpressionArguments_OK_extended_mixOperator_1() throws Exception {
    assert_exchangeBinaryExpressionArguments_success("1 * 2 * 3 + 4", "* 2", "2 * 3 * 1 + 4");
  }

  /**
   * We should go up only until we have same operator.
   */
  public void test_exchangeBinaryExpressionArguments_OK_extended_mixOperator_2() throws Exception {
    assert_exchangeBinaryExpressionArguments_success("1 + 2 - 3 + 4", "+ 2", "2 + 1 - 3 + 4");
  }

  /**
   * Even if as AST level we have tree of "+" expressions, for user this is single expression. So,
   * exchange should happen correctly (from user POV) at any point.
   */
  public void test_exchangeBinaryExpressionArguments_OK_extended_sameOperator_afterFirst()
      throws Exception {
    assert_exchangeBinaryExpressionArguments_success("1 + 2 + 3", "+ 2", "2 + 3 + 1");
  }

  /**
   * Even if as AST level we have tree of "+" expressions, for user this is single expression. So,
   * exchange should happen correctly (from user POV) at any point.
   */
  public void test_exchangeBinaryExpressionArguments_OK_extended_sameOperator_afterSecond()
      throws Exception {
    assert_exchangeBinaryExpressionArguments_success("1 + 2 + 3", "+ 3", "3 + 1 + 2");
  }

  public void test_exchangeBinaryExpressionArguments_OK_simple_afterOperator() throws Exception {
    assert_exchangeBinaryExpressionArguments_success("1 + 2", " 2", "2 + 1");
  }

  public void test_exchangeBinaryExpressionArguments_OK_simple_beforeOperator() throws Exception {
    assert_exchangeBinaryExpressionArguments_success("1 + 2", "+ 2", "2 + 1");
  }

  public void test_exchangeBinaryExpressionArguments_OK_simple_fullSelection() throws Exception {
    selectionLength = 5;
    assert_exchangeBinaryExpressionArguments_success("1 + 2", "1 + 2", "2 + 1");
  }

  public void test_exchangeBinaryExpressionArguments_OK_simple_withLength() throws Exception {
    selectionLength = 2;
    assert_exchangeBinaryExpressionArguments_success("1 + 2", "+ 2", "2 + 1");
  }

  public void test_exchangeBinaryExpressionArguments_wrong_extraLength() throws Exception {
    selectionLength = 3;
    assert_exchangeBinaryExpressionArguments_wrong("111 + 222", "+ 222");
  }

  public void test_exchangeBinaryExpressionArguments_wrong_onOperand() throws Exception {
    assert_exchangeBinaryExpressionArguments_wrong("111 + 222", "11 +");
  }

  public void test_exchangeBinaryExpressionArguments_wrong_selectionWithBinary() throws Exception {
    selectionLength = 9;
    assert_exchangeBinaryExpressionArguments_wrong("1 + 2 + 3", "1 + 2 + 3");
  }

  public void test_extractClassIntoPart() throws Exception {
    String partInitial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "part of app;",
        "",
        "int varBefore;",
        "",
        "class MySuperClass {",
        "}",
        "",
        "int varAfter;");
    String libInitial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library app;",
        "",
        "part 'test.dart';");
    // prepare Source(s)
    Source libSource = addSource("/my_app.dart", libInitial);
    Source testSource = addSource("/test.dart", partInitial);
    // initialize "test" fields
    parseTestUnit(libSource, testSource);
    selectionOffset = findOffset("MySuperClass {");
    // prepare change
    Change proposalChange;
    {
      CorrectionProposal[] proposals = getProposals();
      CorrectionProposal proposal = findProposal(proposals, CorrectionKind.QA_EXTRACT_CLASS);
      assertThat(proposal).isInstanceOf(ChangeCorrectionProposal.class);
      proposalChange = ((ChangeCorrectionProposal) proposal).getChange();
    }
    // check Source(s)
    RefactoringImplTest.assertChangeResult(
        getAnalysisContext(),
        proposalChange,
        testSource,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "part of app;",
            "",
            "int varBefore;",
            "",
            "",
            "int varAfter;"));
    RefactoringImplTest.assertChangeResult(
        getAnalysisContext(),
        proposalChange,
        libSource,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library app;",
            "",
            "part 'test.dart';",
            "part 'my_super_class.dart';"));
    {
      CreateFileChange fileChange = RefactoringImplTest.findCreateFileChange(
          proposalChange,
          "my_super_class.dart");
      assertNotNull(fileChange);
      assertEquals(makeSource(//
          "part of app;",
          "",
          "class MySuperClass {",
          "}",
          ""), fileChange.getContent());
    }
  }

  public void test_importAddShow_BAD_hasShow() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:math' show PI;",
        "main() {",
        "  PI;",
        "}");
    assert_importAddShow_BAD(initial, "import 'dart:math");
  }

  public void test_importAddShow_BAD_unused() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:math';",
        "");
    assert_importAddShow_BAD(initial, "import 'dart:math");
  }

  public void test_importAddShow_OK_onDirective() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:math';",
        "main() {",
        "  PI;",
        "  E;",
        "  max(1, 2);",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:math' show E, PI, max;",
        "main() {",
        "  PI;",
        "  E;",
        "  max(1, 2);",
        "}");
    assert_importAddShow(initial, "art:math", expected);
  }

  public void test_importAddShow_OK_onUri() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:math';",
        "main() {",
        "  PI;",
        "  E;",
        "  max(1, 2);",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:math' show E, PI, max;",
        "main() {",
        "  PI;",
        "  E;",
        "  max(1, 2);",
        "}");
    assert_importAddShow(initial, "import 'dart:math", expected);
  }

  public void test_invertIfStatement_blocks() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "print(x) {}",
        "main() {",
        "  if (true) {",
        "    0;",
        "  } else {",
        "    1;",
        "  }",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "print(x) {}",
        "main() {",
        "  if (false) {",
        "    1;",
        "  } else {",
        "    0;",
        "  }",
        "}",
        "");
    assert_invertIfStatement(initial, "if ", expected);
  }

  public void test_invertIfStatement_statements() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "print(x) {}",
        "main() {",
        "  if (true)",
        "    0;",
        "  else",
        "    1;",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "print(x) {}",
        "main() {",
        "  if (false)",
        "    1;",
        "  else",
        "    0;",
        "}",
        "");
    assert_invertIfStatement(initial, "if ", expected);
  }

  public void test_joinIfStatementInner_OK_conditionAndOr() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1) {",
        "    if (2 == 2 || 3 == 3) {",
        "      print(0);",
        "    }",
        "  }",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1 && (2 == 2 || 3 == 3)) {",
        "    print(0);",
        "  }",
        "}");
    assert_joinIfStatementInner(initial, "if (1 ==", expected);
  }

  public void test_joinIfStatementInner_OK_conditionInvocation() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (isCheck()) {",
        "    if (2 == 2) {",
        "      print(0);",
        "    }",
        "  }",
        "}",
        "bool isCheck() => false;");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (isCheck() && 2 == 2) {",
        "    print(0);",
        "  }",
        "}",
        "bool isCheck() => false;");
    assert_joinIfStatementInner(initial, "if (isCheck", expected);
  }

  public void test_joinIfStatementInner_OK_conditionOrAnd() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1 || 2 == 2) {",
        "    if (3 == 3) {",
        "      print(0);",
        "    }",
        "  }",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if ((1 == 1 || 2 == 2) && 3 == 3) {",
        "    print(0);",
        "  }",
        "}");
    assert_joinIfStatementInner(initial, "if (1 ==", expected);
  }

  public void test_joinIfStatementInner_OK_onCondition() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1) {",
        "    if (2 == 2) {",
        "      print(0);",
        "    }",
        "  }",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1 && 2 == 2) {",
        "    print(0);",
        "  }",
        "}");
    assert_joinIfStatementInner(initial, "1 ==", expected);
  }

  public void test_joinIfStatementInner_OK_simpleConditions_block_block() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1) {",
        "    if (2 == 2) {",
        "      print(0);",
        "    }",
        "  }",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1 && 2 == 2) {",
        "    print(0);",
        "  }",
        "}");
    assert_joinIfStatementInner(initial, "if (1 ==", expected);
  }

  public void test_joinIfStatementInner_OK_simpleConditions_block_single() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1) {",
        "    if (2 == 2)",
        "      print(0);",
        "  }",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1 && 2 == 2) {",
        "    print(0);",
        "  }",
        "}");
    assert_joinIfStatementInner(initial, "if (1 ==", expected);
  }

  public void test_joinIfStatementInner_OK_simpleConditions_single_blockMulti() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1)",
        "    if (2 == 2) {",
        "      print(1);",
        "      print(2);",
        "      print(3);",
        "    }",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1 && 2 == 2) {",
        "    print(1);",
        "    print(2);",
        "    print(3);",
        "  }",
        "}");
    assert_joinIfStatementInner(initial, "if (1 ==", expected);
  }

  public void test_joinIfStatementInner_OK_simpleConditions_single_blockOne() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1)",
        "    if (2 == 2) {",
        "      print(0);",
        "    }",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1 && 2 == 2) {",
        "    print(0);",
        "  }",
        "}");
    assert_joinIfStatementInner(initial, "if (1 ==", expected);
  }

  public void test_joinIfStatementInner_wrong_innerNotIf() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1) {",
        "    print(0);",
        "  }",
        "}");
    assert_joinIfStatementInner_wrong(initial, "if (1 ==");
  }

  public void test_joinIfStatementInner_wrong_innerWithElse() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1) {",
        "    if (2 == 2 ) {",
        "      print(0);",
        "    } else {",
        "      print(1);",
        "    }",
        "  }",
        "}");
    assert_joinIfStatementInner_wrong(initial, "if (1 ==");
  }

  public void test_joinIfStatementInner_wrong_targetNotIf() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(0);",
        "}");
    assert_joinIfStatementInner_wrong(initial, "print(0");
  }

  public void test_joinIfStatementInner_wrong_targetWithElse() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1) {",
        "    if (2 == 2 ) {",
        "      print(0);",
        "    }",
        "  } else {",
        "    print(1);",
        "  }",
        "}");
    assert_joinIfStatementInner_wrong(initial, "if (1 ==");
  }

  public void test_joinIfStatementOuter_OK_conditionAndOr() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1) {",
        "    if (2 == 2 || 3 == 3) {",
        "      print(0);",
        "    }",
        "  }",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1 && (2 == 2 || 3 == 3)) {",
        "    print(0);",
        "  }",
        "}");
    assert_joinIfStatementOuter(initial, "if (2 ==", expected);
  }

  public void test_joinIfStatementOuter_OK_conditionInvocation() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1) {",
        "    if (isCheck()) {",
        "      print(0);",
        "    }",
        "  }",
        "}",
        "bool isCheck() => false;");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1 && isCheck()) {",
        "    print(0);",
        "  }",
        "}",
        "bool isCheck() => false;");
    assert_joinIfStatementOuter(initial, "if (isCheck", expected);
  }

  public void test_joinIfStatementOuter_OK_conditionOrAnd() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1 || 2 == 2) {",
        "    if (3 == 3) {",
        "      print(0);",
        "    }",
        "  }",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if ((1 == 1 || 2 == 2) && 3 == 3) {",
        "    print(0);",
        "  }",
        "}");
    assert_joinIfStatementOuter(initial, "if (3 ==", expected);
  }

  public void test_joinIfStatementOuter_OK_onCondition() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1) {",
        "    if (2 == 2) {",
        "      print(0);",
        "    }",
        "  }",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1 && 2 == 2) {",
        "    print(0);",
        "  }",
        "}");
    assert_joinIfStatementOuter(initial, "2 ==", expected);
  }

  public void test_joinIfStatementOuter_OK_simpleConditions_block_block() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1) {",
        "    if (2 == 2) {",
        "      print(0);",
        "    }",
        "  }",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1 && 2 == 2) {",
        "    print(0);",
        "  }",
        "}");
    assert_joinIfStatementOuter(initial, "if (2 ==", expected);
  }

  public void test_joinIfStatementOuter_OK_simpleConditions_block_single() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1) {",
        "    if (2 == 2)",
        "      print(0);",
        "  }",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1 && 2 == 2) {",
        "    print(0);",
        "  }",
        "}");
    assert_joinIfStatementOuter(initial, "if (2 ==", expected);
  }

  public void test_joinIfStatementOuter_OK_simpleConditions_single_blockMulti() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1)",
        "    if (2 == 2) {",
        "      print(1);",
        "      print(2);",
        "      print(3);",
        "    }",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1 && 2 == 2) {",
        "    print(1);",
        "    print(2);",
        "    print(3);",
        "  }",
        "}");
    assert_joinIfStatementOuter(initial, "if (2 ==", expected);
  }

  public void test_joinIfStatementOuter_OK_simpleConditions_single_blockOne() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1)",
        "    if (2 == 2) {",
        "      print(0);",
        "    }",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1 && 2 == 2) {",
        "    print(0);",
        "  }",
        "}");
    assert_joinIfStatementOuter(initial, "if (2 ==", expected);
  }

  public void test_joinIfStatementOuter_wrong_OuterNotIf() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1) {",
        "    print(0);",
        "  }",
        "}");
    assert_joinIfStatementOuter_wrong(initial, "if (1 ==");
  }

  public void test_joinIfStatementOuter_wrong_OuterWithElse() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1) {",
        "    if (2 == 2 ) {",
        "      print(0);",
        "    }",
        "  } else {",
        "    print(1);",
        "  }",
        "}");
    assert_joinIfStatementOuter_wrong(initial, "if (2 ==");
  }

  public void test_joinIfStatementOuter_wrong_targetNotIf() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(0);",
        "}");
    assert_joinIfStatementOuter_wrong(initial, "print(0");
  }

  public void test_joinIfStatementOuter_wrong_targetWithElse() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1) {",
        "    if (2 == 2 ) {",
        "      print(0);",
        "    } else {",
        "      print(1);",
        "    }",
        "  }",
        "}");
    assert_joinIfStatementOuter_wrong(initial, "if (2 ==");
  }

  public void test_joinVariableDeclaration_onAssignment_OK() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var v;",
        "  v = 1;",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var v = 1;",
        "}");
    assert_joinVariableDeclaration(initial, "v ", expected);
  }

  public void test_joinVariableDeclaration_onAssignment_wrong_hasInitializer() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var v = 1;",
        "  v = 2;",
        "}");
    assert_joinVariableDeclaration_wrong(initial, "v = 2");
  }

  public void test_joinVariableDeclaration_onAssignment_wrong_notAdjacent() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var v;",
        "  var bar;",
        "  v = 1;",
        "}");
    assert_joinVariableDeclaration_wrong(initial, "v =");
  }

  public void test_joinVariableDeclaration_onAssignment_wrong_notAssignment() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var v;",
        "  v += 1;",
        "}");
    assert_joinVariableDeclaration_wrong(initial, "v +=");
  }

  public void test_joinVariableDeclaration_onAssignment_wrong_notDeclaration() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f(var v) {",
        "  v = 1;",
        "}");
    assert_joinVariableDeclaration_wrong(initial, "v =");
  }

  public void test_joinVariableDeclaration_onAssignment_wrong_notLeftArgument() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var v;",
        "  1 + v; // marker",
        "}");
    assert_joinVariableDeclaration_wrong(initial, "v; // marker");
  }

  public void test_joinVariableDeclaration_onAssignment_wrong_notOneVariable() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var v, v2;",
        "  v = 1;",
        "}");
    assert_joinVariableDeclaration_wrong(initial, "v =");
  }

  public void test_joinVariableDeclaration_onAssignment_wrong_notResolved() throws Exception {
    verifyNoTestUnitErrors = false;
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var v;",
        "  x = 1;",
        "}");
    assert_joinVariableDeclaration_wrong(initial, "x = 1");
  }

  public void test_joinVariableDeclaration_onAssignment_wrong_notSameBlock() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var v;",
        "  {",
        "    v = 1;",
        "  }",
        "}");
    assert_joinVariableDeclaration_wrong(initial, "v =");
  }

  public void test_joinVariableDeclaration_onDeclaration_OK_onName() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v;",
        "  v = 1;",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v = 1;",
        "}");
    assert_joinVariableDeclaration(initial, "v;", expected);
  }

  public void test_joinVariableDeclaration_onDeclaration_OK_onType() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v;",
        "  v = 1;",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v = 1;",
        "}");
    assert_joinVariableDeclaration(initial, "int v;", expected);
  }

  public void test_joinVariableDeclaration_onDeclaration_OK_onVar() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v;",
        "  v = 1;",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v = 1;",
        "}");
    assert_joinVariableDeclaration(initial, "var v;", expected);
  }

  public void test_joinVariableDeclaration_onDeclaration_wrong_hasInitializer() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v = 1;",
        "  v = 2;",
        "}");
    assert_joinVariableDeclaration_wrong(initial, "v = 1;");
  }

  public void test_joinVariableDeclaration_onDeclaration_wrong_lastStatement() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true)",
        "    var v;",
        "}");
    assert_joinVariableDeclaration_wrong(initial, "v;");
  }

  public void test_joinVariableDeclaration_onDeclaration_wrong_nextNotAssignmentExpression()
      throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v;",
        "  print(0);",
        "}");
    assert_joinVariableDeclaration_wrong(initial, "v;");
  }

  public void test_joinVariableDeclaration_onDeclaration_wrong_nextNotExpressionStatement()
      throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v;",
        "  if (true) return;",
        "}");
    assert_joinVariableDeclaration_wrong(initial, "v;");
  }

  public void test_joinVariableDeclaration_onDeclaration_wrong_nextNotPureAssignment()
      throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v;",
        "  v += 0;",
        "}");
    assert_joinVariableDeclaration_wrong(initial, "v;");
  }

  public void test_joinVariableDeclaration_onDeclaration_wrong_notInBlock() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v;",
        "}");
    assert_joinVariableDeclaration_wrong(initial, "v;");
  }

  public void test_joinVariableDeclaration_onDeclaration_wrong_notOneVariable() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v1, v2;",
        "  v2 = 0;",
        "}");
    assert_joinVariableDeclaration_wrong(initial, "v2;");
  }

  public void test_nullContext() throws Exception {
    CorrectionProposal[] proposals = PROCESSOR.getProposals(null);
    assertThat(proposals).isEmpty();
  }

  public void test_removeTypeAnnotation_classField_OK() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int v = 1;",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var v = 1;",
        "}");
    assert_removeTypeAnnotation(initial, "int ", expected);
  }

  public void test_removeTypeAnnotation_localVariable_OK() throws Exception {
    String initialSource = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int a = 1, b = 2;",
        "}",
        "");
    String expectedSource = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var a = 1, b = 2;",
        "}",
        "");
    assert_removeTypeAnnotation(initialSource, "int a", expectedSource);
  }

  public void test_removeTypeAnnotation_topLevelVariable_OK() throws Exception {
    assert_removeTypeAnnotation("int v = 1;", "int ", "var v = 1;");
  }

  public void test_replaceConditionalWithIfElse_OK_assignment() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int vvv;",
        "  vvv = true ? 111 : 222;",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int vvv;",
        "  if (true) {",
        "    vvv = 111;",
        "  } else {",
        "    vvv = 222;",
        "  }",
        "}");
    // on conditional
    assert_replaceConditionalWithIfElse(initial, "11 :", expected);
    // on variable
    assert_replaceConditionalWithIfElse(initial, "vv =", expected);
  }

  public void test_replaceConditionalWithIfElse_OK_return() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  return true ? 111 : 222;",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  if (true) {",
        "    return 111;",
        "  } else {",
        "    return 222;",
        "  }",
        "}");
    // on conditional
    assert_replaceConditionalWithIfElse(initial, "11 :", expected);
    // on statement
    assert_replaceConditionalWithIfElse(initial, "return ", expected);
  }

  public void test_replaceConditionalWithIfElse_OK_variableDeclaration() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1, vvv = true ? 111 : 222, b = 2;",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1, vvv, b = 2;",
        "  if (true) {",
        "    vvv = 111;",
        "  } else {",
        "    vvv = 222;",
        "  }",
        "}");
    // on conditional
    assert_replaceConditionalWithIfElse(initial, "11 :", expected);
    // on variable
    assert_replaceConditionalWithIfElse(initial, "vv =", expected);
    // on statement
    assert_replaceConditionalWithIfElse(initial, "int ", expected);
  }

  public void test_replaceConditionalWithIfElse_wrong_noEnclosingStatement() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var v = true ? 111 : 222;");
    assert_replaceConditionalWithIfElse_wrong(initial, "? 111");
  }

  public void test_replaceIfElseWithConditional_OK_assignment() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int vvv;",
        "  if (true) {",
        "    vvv = 111;",
        "  } else {",
        "    vvv = 222;",
        "  }",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int vvv;",
        "  vvv = true ? 111 : 222;",
        "}");
    assert_replaceIfElseWithConditional(initial, "if (true)", expected);
  }

  public void test_replaceIfElseWithConditional_OK_return() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  if (true) {",
        "    return 111;",
        "  } else {",
        "    return 222;",
        "  }",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  return true ? 111 : 222;",
        "}");
    assert_replaceIfElseWithConditional(initial, "if (true)", expected);
  }

  public void test_replaceIfElseWithConditional_wrong_notIfStatement() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  print(0);",
        "}");
    assert_replaceIfElseWithConditional_wrong(initial, "print(0)");
  }

  public void test_replaceIfElseWithConditional_wrong_notSingleStatememt() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int vvv;",
        "  if (true) {",
        "    print(0);",
        "    vvv = 111;",
        "  } else {",
        "    print(0);",
        "    vvv = 222;",
        "  }",
        "}");
    assert_replaceIfElseWithConditional_wrong(initial, "if (true)");
  }

  public void test_splitAndCondition_OK_innerAndExpression() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1 && 2 == 2 && 3 == 3) {",
        "    print(0);",
        "  }",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1) {",
        "    if (2 == 2 && 3 == 3) {",
        "      print(0);",
        "    }",
        "  }",
        "}");
    assert_splitAndCondition(initial, "&& 2 == 2", expected);
  }

  public void test_splitAndCondition_OK_thenBlock() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true && false) {",
        "    print(0);",
        "    if (3 == 3) {",
        "      print(1);",
        "    }",
        "  }",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true) {",
        "    if (false) {",
        "      print(0);",
        "      if (3 == 3) {",
        "        print(1);",
        "      }",
        "    }",
        "  }",
        "}");
    assert_splitAndCondition(initial, "&& false)", expected);
  }

  public void test_splitAndCondition_OK_thenBlock_elseBlock() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true && false) {",
        "    print(0);",
        "  } else {",
        "    print(1);",
        "    if (2 == 2) {",
        "      print(2);",
        "    }",
        "  }",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true) {",
        "    if (false) {",
        "      print(0);",
        "    } else {",
        "      print(1);",
        "      if (2 == 2) {",
        "        print(2);",
        "      }",
        "    }",
        "  }",
        "}");
    assert_splitAndCondition(initial, "&& false)", expected);
  }

  public void test_splitAndCondition_OK_thenStatement() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true && false)",
        "    print(0);",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true)",
        "    if (false)",
        "      print(0);",
        "}");
    assert_splitAndCondition(initial, "&& false)", expected);
  }

  public void test_splitAndCondition_OK_thenStatement_elseStatement() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true && false)",
        "    print(0);",
        "  else",
        "    print(1);",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true)",
        "    if (false)",
        "      print(0);",
        "    else",
        "      print(1);",
        "}");
    assert_splitAndCondition(initial, "&& false)", expected);
  }

  public void test_splitAndCondition_wrong() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1 && 2 == 2) {",
        "    print(0);",
        "  }",
        "  print(3 == 3 && 4 == 4);",
        "}");
    // not binary expression
    assert_splitAndCondition_wrong(initial, "main() {");
    // selection is not empty and includes more than just operator
    {
      selectionLength = 5;
      assert_splitAndCondition_wrong(initial, "&& 2 == 2");
      selectionLength = 0;
    }
  }

  public void test_splitAndCondition_wrong_notAnd() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (1 == 1 || 2 == 2) {",
        "    print(0);",
        "  }",
        "}");
    // not &&
    assert_splitAndCondition_wrong(initial, "|| 2 == 2");
  }

  public void test_splitAndCondition_wrong_notPartOfIf() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(1 == 1 && 2 == 2);",
        "}");
    assert_splitAndCondition_wrong(initial, "&& 2");
  }

  public void test_splitAndCondition_wrong_notTopLevelAnd() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true || (1 == 1 && 2 == 2)) {",
        "    print(0);",
        "  }",
        "  if (true && (3 == 3 && 4 == 4)) {",
        "    print(0);",
        "  }",
        "}");
    assert_splitAndCondition_wrong(initial, "&& 2");
    assert_splitAndCondition_wrong(initial, "&& 4");
  }

  public void test_splitVariableDeclaration_OK_onName() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v = 1;",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v;",
        "  v = 1;",
        "}");
    assert_splitVariableDeclaration(initial, "v ", expected);
  }

  public void test_splitVariableDeclaration_OK_onType() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v = 1;",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v;",
        "  v = 1;",
        "}");
    assert_splitVariableDeclaration(initial, "int v", expected);
  }

  public void test_splitVariableDeclaration_OK_onVar() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v = 1;",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v;",
        "  v = 1;",
        "}");
    assert_splitVariableDeclaration(initial, "var v", expected);
  }

  public void test_splitVariableDeclaration_wrong_notOneVariable() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v = 1, v2;",
        "}");
    assert_splitVariableDeclaration_wrong(initial, "v = 1");
  }

  public void test_surroundWith_block() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  print(0);",
        "  print(1);",
        "// end",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  {",
        "    print(0);",
        "    print(1);",
        "  }",
        "// end",
        "}");
    assert_surroundsWith(initial, CorrectionKind.QA_SURROUND_WITH_BLOCK, expected);
  }

  public void test_surroundWith_doWhile() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  print(0);",
        "  print(1);",
        "// end",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  do {",
        "    print(0);",
        "    print(1);",
        "  } while (condition);",
        "// end",
        "}");
    assert_surroundsWith(initial, CorrectionKind.QA_SURROUND_WITH_DO_WHILE, expected);
  }

  public void test_surroundWith_for() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  print(0);",
        "  print(1);",
        "// end",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  for (var v = init; condition; increment) {",
        "    print(0);",
        "    print(1);",
        "  }",
        "// end",
        "}");
    assert_surroundsWith(initial, CorrectionKind.QA_SURROUND_WITH_FOR, expected);
  }

  public void test_surroundWith_forIn() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  print(0);",
        "  print(1);",
        "// end",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  for (var item in iterable) {",
        "    print(0);",
        "    print(1);",
        "  }",
        "// end",
        "}");
    assert_surroundsWith(initial, CorrectionKind.QA_SURROUND_WITH_FOR_IN, expected);
  }

  public void test_surroundWith_if() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  print(0);",
        "  print(1);",
        "// end",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  if (condition) {",
        "    print(0);",
        "    print(1);",
        "  }",
        "// end",
        "}");
    assert_surroundsWith(initial, CorrectionKind.QA_SURROUND_WITH_IF, expected);
  }

  public void test_surroundWith_tryCatch() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  print(0);",
        "  print(1);",
        "// end",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  try {",
        "    print(0);",
        "    print(1);",
        "  } on Exception catch (e) {",
        "    // TODO",
        "  }",
        "// end",
        "}");
    assert_surroundsWith(initial, CorrectionKind.QA_SURROUND_WITH_TRY_CATCH, expected);
  }

  public void test_surroundWith_tryFinally() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  print(0);",
        "  print(1);",
        "// end",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  try {",
        "    print(0);",
        "    print(1);",
        "  } finally {",
        "    // TODO",
        "  }",
        "// end",
        "}");
    assert_surroundsWith(initial, CorrectionKind.QA_SURROUND_WITH_TRY_FINALLY, expected);
  }

  public void test_surroundWith_while() throws Exception {
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  print(0);",
        "  print(1);",
        "// end",
        "}");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  while (condition) {",
        "    print(0);",
        "    print(1);",
        "  }",
        "// end",
        "}");
    assert_surroundsWith(initial, CorrectionKind.QA_SURROUND_WITH_WHILE, expected);
  }

  private void assert_addTypeAnnotation(String initialSource, String offsetPattern,
      String expectedSource) throws Exception {
    assert_runProcessor(
        CorrectionKind.QA_ADD_TYPE_ANNOTATION,
        initialSource,
        offsetPattern,
        expectedSource);
  }

  private void assert_addTypeAnnotation_classField(String initialDeclaration, String offsetPattern,
      String expectedDeclaration) throws Exception {
    String initialSource = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  " + initialDeclaration,
        "}",
        "");
    String expectedSource = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  " + expectedDeclaration,
        "}",
        "");
    assert_addTypeAnnotation(initialSource, offsetPattern, expectedSource);
  }

  private void assert_addTypeAnnotation_localVariable(String initialStatement,
      String offsetPattern, String expectedStatement) throws Exception {
    String initialSource = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  " + initialStatement,
        "}",
        "");
    String expectedSource = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  " + expectedStatement,
        "}",
        "");
    assert_addTypeAnnotation(initialSource, offsetPattern, expectedSource);
  }

  private void assert_addTypeAnnotation_topLevelField(String initialDeclaration,
      String offsetPattern, String expectedDeclaration) throws Exception {
    String initialSource = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        initialDeclaration,
        "");
    String expectedSource = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        expectedDeclaration,
        "");
    assert_addTypeAnnotation(initialSource, offsetPattern, expectedSource);
  }

  private void assert_assignToLocalVariable(String initialSource, String offsetPattern,
      String expectedSource) throws Exception {
    assert_runProcessor(
        CorrectionKind.QA_ASSIGN_TO_LOCAL_VARIABLE,
        initialSource,
        offsetPattern,
        expectedSource);
  }

  private void assert_convertToBlockBody(String initialSource, String offsetPattern,
      String expectedSource) throws Exception {
    assert_runProcessor(
        CorrectionKind.QA_CONVERT_INTO_BLOCK_BODY,
        initialSource,
        offsetPattern,
        expectedSource);
  }

  private void assert_convertToBlockBody_wrong(String initialSource, String offsetPattern)
      throws Exception {
    assert_convertToBlockBody(initialSource, offsetPattern, initialSource);
  }

  private void assert_convertToExpressionBody(String initialSource, String offsetPattern,
      String expectedSource) throws Exception {
    assert_runProcessor(
        CorrectionKind.QA_CONVERT_INTO_EXPRESSION_BODY,
        initialSource,
        offsetPattern,
        expectedSource);
  }

  private void assert_convertToExpressionBody_wrong(String initialSource, String offsetPattern)
      throws Exception {
    assert_convertToExpressionBody(initialSource, offsetPattern, initialSource);
  }

  private void assert_convertToIsNot(String initialSource, String offsetPattern,
      String expectedSource) throws Exception {
    initialSource = "var v;\nvar v2 = " + initialSource + ";";
    expectedSource = "var v;\nvar v2 = " + expectedSource + ";";
    assert_runProcessor(
        CorrectionKind.QA_CONVERT_INTO_IS_NOT,
        initialSource,
        offsetPattern,
        expectedSource);
  }

  private void assert_convertToIsNotEmpty(String initial, String offsetPattern, String expected)
      throws Exception {
    String initialSource = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main(String str) {",
        "  " + initial + ";",
        "}");
    String expectedSource = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main(String str) {",
        "  " + expected + ";",
        "}");
    assert_convertToIsNotEmpty2(initialSource, offsetPattern, expectedSource);
  }

  private void assert_convertToIsNotEmpty2(String initialSource, String offsetPattern,
      String expectedSource) throws Exception {
    assert_runProcessor(
        CorrectionKind.QA_CONVERT_INTO_IS_NOT_EMPTY,
        initialSource,
        offsetPattern,
        expectedSource);
  }

  private void assert_exchangeBinaryExpressionArguments_success(String initialExpression,
      String offsetPattern, String expectedExpression) throws Exception {
    String initialSource = "var v = " + initialExpression + ";";
    String expectedSource = "var v = " + expectedExpression + ";";
    assert_runProcessor(
        CorrectionKind.QA_EXCHANGE_OPERANDS,
        initialSource,
        offsetPattern,
        expectedSource);
  }

  private void assert_exchangeBinaryExpressionArguments_wrong(String expression,
      String offsetPattern) throws Exception {
    assert_exchangeBinaryExpressionArguments_success(expression, offsetPattern, expression);
  }

  private void assert_importAddShow(String initialSource, String offsetPattern,
      String expectedSource) throws Exception {
    assert_runProcessor(
        CorrectionKind.QA_IMPORT_ADD_SHOW,
        initialSource,
        offsetPattern,
        expectedSource);
  }

  private void assert_importAddShow_BAD(String initialSource, String offsetPattern)
      throws Exception {
    assert_importAddShow(initialSource, offsetPattern, initialSource);
  }

  private void assert_invertIfStatement(String initialSource, String offsetPattern,
      String expectedSource) throws Exception {
    assert_runProcessor(
        CorrectionKind.QA_INVERT_IF_STATEMENT,
        initialSource,
        offsetPattern,
        expectedSource);
  }

  private void assert_joinIfStatementInner(String initialSource, String offsetPattern,
      String expectedSource) throws Exception {
    assert_runProcessor(
        CorrectionKind.QA_JOIN_IF_WITH_INNER,
        initialSource,
        offsetPattern,
        expectedSource);
  }

  private void assert_joinIfStatementInner_wrong(String initialSource, String offsetPattern)
      throws Exception {
    assert_joinIfStatementInner(initialSource, offsetPattern, initialSource);
  }

  private void assert_joinIfStatementOuter(String initialSource, String offsetPattern,
      String expectedSource) throws Exception {
    assert_runProcessor(
        CorrectionKind.QA_JOIN_IF_WITH_OUTER,
        initialSource,
        offsetPattern,
        expectedSource);
  }

  private void assert_joinIfStatementOuter_wrong(String initialSource, String offsetPattern)
      throws Exception {
    assert_joinIfStatementOuter(initialSource, offsetPattern, initialSource);
  }

  private void assert_joinVariableDeclaration(String initialSource, String offsetPattern,
      String expectedSource) throws Exception {
    assert_runProcessor(
        CorrectionKind.QA_JOIN_VARIABLE_DECLARATION,
        initialSource,
        offsetPattern,
        expectedSource);
  }

  private void assert_joinVariableDeclaration_wrong(String expression, String offsetPattern)
      throws Exception {
    assert_joinVariableDeclaration(expression, offsetPattern, expression);
  }

  private void assert_removeTypeAnnotation(String initialSource, String offsetPattern,
      String expectedSource) throws Exception {
    assert_runProcessor(
        CorrectionKind.QA_REMOVE_TYPE_ANNOTATION,
        initialSource,
        offsetPattern,
        expectedSource);
  }

  private void assert_replaceConditionalWithIfElse(String initialSource, String offsetPattern,
      String expectedSource) throws Exception {
    assert_runProcessor(
        CorrectionKind.QA_REPLACE_CONDITIONAL_WITH_IF_ELSE,
        initialSource,
        offsetPattern,
        expectedSource);
  }

  private void assert_replaceConditionalWithIfElse_wrong(String initialSource, String offsetPattern)
      throws Exception {
    assert_replaceConditionalWithIfElse(initialSource, offsetPattern, initialSource);
  }

  private void assert_replaceIfElseWithConditional(String initialSource, String offsetPattern,
      String expectedSource) throws Exception {
    assert_runProcessor(
        CorrectionKind.QA_REPLACE_IF_ELSE_WITH_CONDITIONAL,
        initialSource,
        offsetPattern,
        expectedSource);
  }

  private void assert_replaceIfElseWithConditional_wrong(String initialSource, String offsetPattern)
      throws Exception {
    assert_replaceIfElseWithConditional(initialSource, offsetPattern, initialSource);
  }

  /**
   * Asserts that running proposal of the given kind produces expected source.
   */
  private void assert_runProcessor(CorrectionKind kind, String expectedSource) throws Exception {
    // XXX used to see coverage of only one quick assist
//    if (kind != CorrectionKind.QA_CONVERT_INTO_IS_NOT) {
//      return;
//    }
    CorrectionProposal[] proposals = getProposals();
    assert_runProcessor(testCode, proposals, kind, expectedSource);
  }

  /**
   * Asserts that running proposal with given name produces expected source.
   */
  private void assert_runProcessor(CorrectionKind kind, String initialSource, String offsetPattern,
      String expectedSource) throws Exception {
    indexTestUnit(initialSource);
    selectionOffset = findOffset(offsetPattern);
    assert_runProcessor(kind, expectedSource);
  }

  /**
   * Asserts that running proposal of the given kind produces expected source.
   */
  private void assert_runProcessor(String initialSource, CorrectionProposal[] proposals,
      CorrectionKind kind, String expectedSource) {
    resultSource = initialSource;
    // apply SourceCorrectionProposal 
    CorrectionProposal proposal = findProposal(proposals, kind);
    if (proposal != null) {
      assertThat(proposal).isInstanceOf(SourceCorrectionProposal.class);
      resultProposal = (SourceCorrectionProposal) proposal;
      resultSource = applyProposal(initialSource, resultProposal);
    }
    // assert result
    assertEquals(expectedSource, resultSource);
  }

  private void assert_splitAndCondition(String initialSource, String offsetPattern,
      String expectedSource) throws Exception {
    assert_runProcessor(
        CorrectionKind.QA_SPLIT_AND_CONDITION,
        initialSource,
        offsetPattern,
        expectedSource);
  }

  private void assert_splitAndCondition_wrong(String initialSource, String offsetPattern)
      throws Exception {
    assert_splitAndCondition(initialSource, offsetPattern, initialSource);
  }

  private void assert_splitVariableDeclaration(String initialSource, String offsetPattern,
      String expectedSource) throws Exception {
    assert_runProcessor(
        CorrectionKind.QA_SPLIT_VARIABLE_DECLARATION,
        initialSource,
        offsetPattern,
        expectedSource);
  }

  private void assert_splitVariableDeclaration_wrong(String initialSource, String offsetPattern)
      throws Exception {
    assert_splitVariableDeclaration(initialSource, offsetPattern, initialSource);
  }

  private void assert_surroundsWith(String initialSource, CorrectionKind kind, String expectedSource)
      throws Exception {
    parseTestUnit(initialSource);
    setSelectionFromStartEndComments();
    assert_runProcessor(kind, expectedSource);
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

  private CorrectionProposal[] getProposals() throws Exception {
    AssistContext context = new AssistContext(
        searchEngine,
        analysisContext,
        null,
        testSource,
        testUnit,
        selectionOffset,
        selectionLength);
    return PROCESSOR.getProposals(context);
  }

  /**
   * @return the {@link SourceRange} of "identPattern" in {@link #resultCode}.
   */
  private SourceRange getResultRange(String identPattern) {
    int offset = resultSource.indexOf(identPattern);
    assertThat(offset).describedAs(identPattern + " in " + resultSource).isPositive();
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

  private void setSelectionFromStartEndComments() throws Exception {
    selectionOffset = findEnd("// start") + EOL.length();
    selectionLength = findOffset("// end") - selectionOffset;
  }
}
