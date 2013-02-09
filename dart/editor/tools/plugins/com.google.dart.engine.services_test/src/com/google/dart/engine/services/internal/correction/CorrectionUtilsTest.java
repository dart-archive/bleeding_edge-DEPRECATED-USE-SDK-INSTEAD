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

import com.google.common.collect.ImmutableList;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.VariableDeclarationStatement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.source.SourceRange;

import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartLength;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.CharBuffer;

public class CorrectionUtilsTest extends AbstractDartTest {

  public void test_applyReplaceEdits() throws Exception {
    String s = "0123456789";
    String result = CorrectionUtils.applyReplaceEdits(
        s,
        ImmutableList.of(new Edit(7, 1, "A"), new Edit(1, 2, "BBB"), new Edit(4, 1, "C")));
    assertEquals("0BBB3C56A89", result);
  }

  /**
   * Test for {@link CorrectionUtils#covers(SourceRange, DartNode)}.
   */
  public void test_covers_SourceRange_DartNode() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "}",
        "");
    assertFalse(CorrectionUtils.covers(rangeStartLength(0, 1), testUnit));
    assertTrue(CorrectionUtils.covers(rangeStartLength(0, 1000), testUnit));
  }

  public void test_getDeltaOffset() throws Exception {
    assertEquals(1, CorrectionUtils.getDeltaOffset(new Edit(0, 5, "123456")));
    assertEquals(-2, CorrectionUtils.getDeltaOffset(new Edit(0, 5, "123")));
  }

  /**
   * Test for {@link CorrectionUtils#getEndOfLine()}.
   */
  public void test_getEndOfLine_default() throws Exception {
    CompilationUnit unit = parseUnit("");
    CorrectionUtils utils = new CorrectionUtils(unit);
    assertEquals(CorrectionUtils.DEFAULT_END_OF_LINE, utils.getEndOfLine());
  }

  /**
   * Test for {@link CorrectionUtils#getEndOfLine()}.
   */
  public void test_getEndOfLine_unix() throws Exception {
    CompilationUnit unit = parseUnit("// aaa\n// bbb\n// ccc");
    CorrectionUtils utils = new CorrectionUtils(unit);
    assertEquals("\n", utils.getEndOfLine());
  }

  /**
   * Test for {@link CorrectionUtils#getEndOfLine()}.
   */
  public void test_getEndOfLine_windows() throws Exception {
    CompilationUnit unit = parseUnit("// aaa\r\n// bbb\r\n// ccc");
    CorrectionUtils utils = new CorrectionUtils(unit);
    assertEquals("\r\n", utils.getEndOfLine());
  }

  public void test_getIndent() throws Exception {
    parseTestUnit();
    CorrectionUtils utils = getTestCorrectionUtils();
    assertEquals("", utils.getIndent(0));
    assertEquals("  ", utils.getIndent(1));
    assertEquals("    ", utils.getIndent(2));
  }

  public void test_getLineNext() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "// 1",
        "// 2\r",
        "// 3");
    CorrectionUtils utils = getTestCorrectionUtils();
    assertEquals(findOffset("// 1"), utils.getLineNext(findOffset(" filler")));
    assertEquals(findOffset("// 2"), utils.getLineNext(findOffset("/ 1")));
    assertEquals(findOffset("// 3"), utils.getLineNext(findOffset("/ 2")));
  }

  /**
   * Test for {@link CorrectionUtils#getLinePrefix(int)}.
   */
  public void test_getLinePrefix() throws Exception {
    CompilationUnit unit = parseUnit("//000\n  //111\n   \n  ");
    CorrectionUtils utils = new CorrectionUtils(unit);
    // 0
    assertEquals("", utils.getLinePrefix(0));
    assertEquals("", utils.getLinePrefix(1));
    assertEquals("", utils.getLinePrefix(2));
    // 1
    assertEquals("  ", utils.getLinePrefix(6));
    assertEquals("  ", utils.getLinePrefix(7));
    assertEquals("  ", utils.getLinePrefix(8));
    assertEquals("  ", utils.getLinePrefix(9));
    assertEquals("  ", utils.getLinePrefix(10));
    // 2
    assertEquals("   ", utils.getLinePrefix(14));
    assertEquals("   ", utils.getLinePrefix(15));
    assertEquals("   ", utils.getLinePrefix(16));
    // 3
    assertEquals("  ", utils.getLinePrefix(19));
  }

  // TODO(scheglov) finish this test
  public void test_getLinesRange_SourceRange() throws Exception {
//    parseTestUnit(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "// 1",
//        "// 2",
//        "// 3");
//    CorrectionUtils utils = getTestCorrectionUtils();
//    // use lines start/end already
//    {
//      int rs = findOffset("// filler");
//      int re = findOffset("// 2");
//      assertEquals(rangeStartEnd(rs, re), utils.getLinesRange(rangeStartEnd(rs, re)));
//    }
//    // use start/end with offsets from lines
//    {
//      int as = findOffset("// filler") + 1;
//      int ae = findOffset("// 2") + 2;
//      int rs = findOffset("// filler");
//      int re = findOffset("// 2");
//      assertEquals(rangeStartEnd(rs, re), utils.getLinesRange(rangeStartEnd(as, ae)));
//    }
  }

  /**
   * Test for {@link CorrectionUtils#getLineThis(int)}.
   */
  public void test_getLineThis() throws Exception {
    CompilationUnit unit = parseUnit("//aaa\r\n//bbbb\r\nccccc");
    CorrectionUtils utils = new CorrectionUtils(unit);
    // 0
    assertEquals(0, utils.getLineThis(0));
    assertEquals(0, utils.getLineThis(1));
    assertEquals(0, utils.getLineThis(2));
    // 5
    assertEquals(7, utils.getLineThis(7));
    assertEquals(7, utils.getLineThis(8));
    assertEquals(7, utils.getLineThis(9));
    assertEquals(7, utils.getLineThis(10));
    // 11
    assertEquals(15, utils.getLineThis(15));
    assertEquals(15, utils.getLineThis(16));
    assertEquals(15, utils.getLineThis(17));
    assertEquals(15, utils.getLineThis(18));
    assertEquals(15, utils.getLineThis(19));
  }

  /**
   * Test for {@link CorrectionUtils#getNodePrefix(DartNode)}.
   */
  public void test_getNodePrefix_block_noPrefix() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "var a;",
        "}",
        "");
    assert_getNodePrefix("var a;", "");
  }

  /**
   * Test for {@link CorrectionUtils#getNodePrefix(DartNode)}.
   */
  public void test_getNodePrefix_block_withPrefix() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var a;",
        "}",
        "");
    assert_getNodePrefix("var a;", "  ");
  }

  /**
   * Test for {@link CorrectionUtils#getNodePrefix(DartNode)}.
   */
  public void test_getNodePrefix_FunctionExpression() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  foo(() => true);",
        "}",
        "");
    // find node
    FunctionExpression node = findTestNode("() => true", FunctionExpression.class);
    assertNotNull(node);
    // assert prefix
    CorrectionUtils utils = getTestCorrectionUtils();
    assertEquals("  ", utils.getNodePrefix(node));
  }

  /**
   * Test for {@link CorrectionUtils#getNodePrefix(DartNode)}.
   */
  public void test_getNodePrefix_noPrefix() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  var b;var a;",
        "}",
        "");
    assert_getNodePrefix("var a;", "");
  }

  public void test_getNonWhitespaceForward() throws Exception {
    String code = " \t//0123456789  ";
    parseTestUnit(code);
    CorrectionUtils utils = new CorrectionUtils(testUnit);
    assertEquals(2, utils.getNonWhitespaceForward(0));
    assertEquals(2, utils.getNonWhitespaceForward(1));
    assertEquals(2, utils.getNonWhitespaceForward(2));
    assertEquals(3, utils.getNonWhitespaceForward(3));
    assertEquals(code.length(), utils.getNonWhitespaceForward(code.indexOf("9") + 1));
  }

  public void test_getStringPrefix() throws Exception {
    assertEquals("", CorrectionUtils.getStringPrefix(""));
    assertEquals("", CorrectionUtils.getStringPrefix("01234"));
    assertEquals("", CorrectionUtils.getStringPrefix("0 1234"));
    assertEquals(" ", CorrectionUtils.getStringPrefix(" 01234"));
    assertEquals("  ", CorrectionUtils.getStringPrefix("  01234"));
  }

  /**
   * Test for {@link CorrectionUtils#getText()}.
   */
  public void test_getText() throws Exception {
    String code = "// 0123456789";
    parseTestUnit(code);
    CorrectionUtils utils = new CorrectionUtils(testUnit);
    assertEquals(code, utils.getText());
  }

  /**
   * Test for {@link CorrectionUtils#getText(ASTNode)}.
   */
  public void test_getText_ASTNode() throws Exception {
    parseTestUnit("class AAA {}");
    ASTNode node = findTestNode("AAA {", Identifier.class);
    CorrectionUtils utils = new CorrectionUtils(testUnit);
    assertEquals("AAA", utils.getText(node));
  }

  /**
   * Test for {@link CorrectionUtils#getText(int, int)}.
   */
  public void test_getText_ints() throws Exception {
    parseTestUnit("// 0123456789");
    CorrectionUtils utils = new CorrectionUtils(testUnit);
    assertEquals("0123", utils.getText(3, 4));
  }

  /**
   * Test for {@link CorrectionUtils#getText(SourceRange)}.
   */
  public void test_getText_SourceRange() throws Exception {
    parseTestUnit("// 0123456789");
    CorrectionUtils utils = new CorrectionUtils(testUnit);
    assertEquals("0123", utils.getText(rangeStartLength(3, 4)));
  }

  public void test_getTypeSource_Expression() throws Exception {
    Type type = mock(Type.class);
    Expression expression = mock(Expression.class);
    when(expression.getStaticType()).thenReturn(type);
    // null
    assertEquals(null, CorrectionUtils.getTypeSource((Expression) null));
    // int
    when(type.toString()).thenReturn("int");
    assertEquals("int", CorrectionUtils.getTypeSource(expression));
    // dynamic
    when(type.toString()).thenReturn("dynamic");
    assertEquals(null, CorrectionUtils.getTypeSource(expression));
    // List<dynamic>
    when(type.toString()).thenReturn("List<dynamic>");
    assertEquals("List", CorrectionUtils.getTypeSource(expression));
    // Map<dynamic, dynamic>
    when(type.toString()).thenReturn("Map<dynamic, dynamic>");
    assertEquals("Map", CorrectionUtils.getTypeSource(expression));
  }

  public void test_getTypeSource_Type() throws Exception {
    Type type = mock(Type.class);
    // int
    when(type.toString()).thenReturn("int");
    assertEquals("int", CorrectionUtils.getTypeSource(type));
    // dynamic
    when(type.toString()).thenReturn("dynamic");
    assertEquals("dynamic", CorrectionUtils.getTypeSource(type));
    // List<dynamic>
    when(type.toString()).thenReturn("List<dynamic>");
    assertEquals("List", CorrectionUtils.getTypeSource(type));
    // Map<dynamic, dynamic>
    when(type.toString()).thenReturn("Map<dynamic, dynamic>");
    assertEquals("Map", CorrectionUtils.getTypeSource(type));
  }

  public void test_getUnit() throws Exception {
    parseTestUnit("");
    assertSame(testUnit, getTestCorrectionUtils().getUnit());
  }

  public void test_isJustWhitespace() throws Exception {
    parseTestUnit("//  0123");
    CorrectionUtils utils = getTestCorrectionUtils();
    assertTrue(utils.isJustWhitespace(rangeStartLength(2, 1)));
    assertTrue(utils.isJustWhitespace(rangeStartLength(2, 2)));
    assertFalse(utils.isJustWhitespace(rangeStartLength(0, 1)));
    assertFalse(utils.isJustWhitespace(rangeStartLength(2, 3)));
  }

  public void test_isJustWhitespaceOrComment() throws Exception {
    parseTestUnit("var a;  // 0123");
    CorrectionUtils utils = getTestCorrectionUtils();
    // whitespace
    assertTrue(utils.isJustWhitespaceOrComment(rangeStartLength(6, 1)));
    assertTrue(utils.isJustWhitespaceOrComment(rangeStartLength(6, 2)));
    // whitespace + comment
    assertTrue(utils.isJustWhitespaceOrComment(rangeStartLength(6, 4)));
    assertTrue(utils.isJustWhitespaceOrComment(rangeStartLength(6, 5)));
    // not whitespace
    assertFalse(utils.isJustWhitespaceOrComment(rangeStartLength(0, 1)));
    assertFalse(utils.isJustWhitespaceOrComment(rangeStartLength(3, 2)));
  }

  public void test_new_withCharBuffer() throws Exception {
    Source source = mock(Source.class);
    CompilationUnit unit = mock(CompilationUnit.class);
    CompilationUnitElement unitElement = mock(CompilationUnitElement.class);
    when(unit.getElement()).thenReturn(unitElement);
    when(unitElement.getSource()).thenReturn(source);
    // mock content
    final CharBuffer charBuffer = mock(CharBuffer.class);
    when(charBuffer.toString()).thenReturn("// 0123");
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        ((Source.ContentReceiver) invocation.getArguments()[0]).accept(charBuffer);
        return null;
      }
    }).when(source).getContents(any(Source.ContentReceiver.class));
    // create CorrectionUtils, ask content
    CorrectionUtils utils = new CorrectionUtils(unit);
    // verify that content was requested
    verify(source).getContents(any(Source.ContentReceiver.class));
    assertEquals("// 0123", utils.getText());
  }

  /**
   * Asserts that {@link ExtractUtils#getNodePrefix(DartNode)} in {@link #testUnit} has expected
   * prefix.
   */
  private void assert_getNodePrefix(String nodePattern, String expectedPrefix) throws Exception {
    // find node
    VariableDeclarationStatement node = findTestNode(
        nodePattern,
        VariableDeclarationStatement.class);
    assertNotNull(node);
    // assert prefix
    CorrectionUtils utils = getTestCorrectionUtils();
    assertEquals(expectedPrefix, utils.getNodePrefix(node));
  }
}
