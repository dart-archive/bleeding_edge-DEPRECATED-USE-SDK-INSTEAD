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
package com.google.dart.engine.scanner;

import junit.framework.TestCase;

public abstract class AbstractScannerTest extends TestCase {
  public void test_AbstractScanner_ampersand() throws Exception {
    assertToken(TokenType.AMPERSAND, scan("&"));
  }

  public void test_AbstractScanner_ampersand_ampersand() throws Exception {
    assertToken(TokenType.AMPERSAND_AMPERSAND, scan("&&"));
  }

  public void test_AbstractScanner_ampersand_eq() throws Exception {
    assertToken(TokenType.AMPERSAND_EQ, scan("&="));
  }

  public void test_AbstractScanner_backping() throws Exception {
    assertToken(TokenType.BACKPING, scan("`"));
  }

  public void test_AbstractScanner_backslash() throws Exception {
    assertToken(TokenType.BACKSLASH, scan("\\"));
  }

  public void test_AbstractScanner_bang() throws Exception {
    assertToken(TokenType.BANG, scan("!"));
  }

  public void test_AbstractScanner_bang_eq() throws Exception {
    assertToken(TokenType.BANG_EQ, scan("!="));
  }

  public void test_AbstractScanner_bang_eq_eq() throws Exception {
    assertToken(TokenType.BANG_EQ_EQ, scan("!=="));
  }

  public void test_AbstractScanner_bar() throws Exception {
    assertToken(TokenType.BAR, scan("|"));
  }

  public void test_AbstractScanner_bar_bar() throws Exception {
    assertToken(TokenType.BAR_BAR, scan("||"));
  }

  public void test_AbstractScanner_bar_eq() throws Exception {
    assertToken(TokenType.BAR_EQ, scan("|="));
  }

  public void test_AbstractScanner_caret() throws Exception {
    assertToken(TokenType.CARET, scan("^"));
  }

  public void test_AbstractScanner_caret_eq() throws Exception {
    assertToken(TokenType.CARET_EQ, scan("^="));
  }

  public void test_AbstractScanner_close_curly_bracket() throws Exception {
    assertToken(TokenType.CLOSE_CURLY_BRACKET, scan("}"));
  }

  public void test_AbstractScanner_close_paren() throws Exception {
    assertToken(TokenType.CLOSE_PAREN, scan(")"));
  }

  public void test_AbstractScanner_close_quare_bracket() throws Exception {
    assertToken(TokenType.CLOSE_SQUARE_BRACKET, scan("]"));
  }

  public void test_AbstractScanner_colon() throws Exception {
    assertToken(TokenType.COLON, scan(":"));
  }

  public void test_AbstractScanner_comma() throws Exception {
    assertToken(TokenType.COMMA, scan(","));
  }

  public void test_AbstractScanner_comment_multi() throws Exception {
    assertToken(TokenType.MULTI_LINE_COMMENT, scan("/* comment */"));
  }

  public void test_AbstractScanner_comment_nested() throws Exception {
    assertToken(TokenType.MULTI_LINE_COMMENT, scan("/* comment /* within a */ comment */"));
  }

  public void test_AbstractScanner_comment_single() throws Exception {
    assertToken(TokenType.SINGLE_LINE_COMMENT, scan("// comment"));
  }

  public void test_AbstractScanner_double_both_e() throws Exception {
    assertToken(TokenType.DOUBLE, scan("0.123e4"));
  }

  public void test_AbstractScanner_double_both_E() throws Exception {
    assertToken(TokenType.DOUBLE, scan("0.123E4"));
  }

  public void test_AbstractScanner_double_fraction() throws Exception {
    assertToken(TokenType.DOUBLE, scan(".123"));
  }

  public void test_AbstractScanner_double_fraction_d() throws Exception {
    assertToken(TokenType.DOUBLE, scan(".123d"));
  }

  public void test_AbstractScanner_double_fraction_D() throws Exception {
    assertToken(TokenType.DOUBLE, scan(".123D"));
  }

  public void test_AbstractScanner_double_fraction_e() throws Exception {
    assertToken(TokenType.DOUBLE, scan(".123e4"));
  }

  public void test_AbstractScanner_double_fraction_E() throws Exception {
    assertToken(TokenType.DOUBLE, scan(".123E4"));
  }

  public void test_AbstractScanner_double_fraction_ed() throws Exception {
    assertToken(TokenType.DOUBLE, scan(".123e4d"));
  }

  public void test_AbstractScanner_double_fraction_Ed() throws Exception {
    assertToken(TokenType.DOUBLE, scan(".123E4d"));
  }

  public void test_AbstractScanner_double_whole_d() throws Exception {
    assertToken(TokenType.DOUBLE, scan("12d"));
  }

  public void test_AbstractScanner_double_whole_D() throws Exception {
    assertToken(TokenType.DOUBLE, scan("12D"));
  }

  public void test_AbstractScanner_double_whole_e() throws Exception {
    assertToken(TokenType.DOUBLE, scan("12e4"));
  }

  public void test_AbstractScanner_double_whole_E() throws Exception {
    assertToken(TokenType.DOUBLE, scan("12E4"));
  }

  public void test_AbstractScanner_double_whole_ed() throws Exception {
    assertToken(TokenType.DOUBLE, scan("12e4d"));
  }

  public void test_AbstractScanner_double_whole_Ed() throws Exception {
    assertToken(TokenType.DOUBLE, scan("12E4d"));
  }

  public void test_AbstractScanner_eq() throws Exception {
    assertToken(TokenType.EQ, scan("="));
  }

  public void test_AbstractScanner_eq_eq() throws Exception {
    assertToken(TokenType.EQ_EQ, scan("=="));
  }

  public void test_AbstractScanner_eq_eq_eq() throws Exception {
    assertToken(TokenType.EQ_EQ_EQ, scan("==="));
  }

  public void test_AbstractScanner_gt() throws Exception {
    assertToken(TokenType.GT, scan(">"));
  }

  public void test_AbstractScanner_gt_eq() throws Exception {
    assertToken(TokenType.GT_EQ, scan(">="));
  }

  public void test_AbstractScanner_gt_gt() throws Exception {
    assertToken(TokenType.GT_GT, scan(">>"));
  }

  public void test_AbstractScanner_gt_gt_eq() throws Exception {
    assertToken(TokenType.GT_GT_EQ, scan(">>="));
  }

  public void test_AbstractScanner_gt_gt_gt() throws Exception {
    assertToken(TokenType.GT_GT_GT, scan(">>>"));
  }

  public void test_AbstractScanner_gt_gt_gt_eq() throws Exception {
    assertToken(TokenType.GT_GT_GT_EQ, scan(">>>="));
  }

  public void test_AbstractScanner_hash() throws Exception {
    assertToken(TokenType.HASH, scan("#"));
  }

  public void test_AbstractScanner_hexidecimal() throws Exception {
    assertToken(TokenType.HEXADECIMAL, scan("0x1A2B3C"));
  }

  public void test_AbstractScanner_identifier() throws Exception {
    assertToken(TokenType.IDENTIFIER, scan("identifier"));
  }

  public void test_AbstractScanner_index() throws Exception {
    assertToken(TokenType.INDEX, scan("[]"));
  }

  public void test_AbstractScanner_index_eq() throws Exception {
    assertToken(TokenType.INDEX_EQ, scan("[]="));
  }

  public void test_AbstractScanner_int() throws Exception {
    assertToken(TokenType.INT, scan("123"));
  }

  public void test_AbstractScanner_int_initialZero() throws Exception {
    assertToken(TokenType.INT, scan("0123"));
  }

  public void test_AbstractScanner_keyword_abstract() throws Exception {
    assertKeywordToken("abstract");
  }

  public void test_AbstractScanner_keyword_assert() throws Exception {
    assertKeywordToken("assert");
  }

  public void test_AbstractScanner_keyword_break() throws Exception {
    assertKeywordToken("break");
  }

  public void test_AbstractScanner_keyword_case() throws Exception {
    assertKeywordToken("case");
  }

  public void test_AbstractScanner_keyword_catch() throws Exception {
    assertKeywordToken("catch");
  }

  public void test_AbstractScanner_keyword_class() throws Exception {
    assertKeywordToken("class");
  }

  public void test_AbstractScanner_keyword_const() throws Exception {
    assertKeywordToken("const");
  }

  public void test_AbstractScanner_keyword_continue() throws Exception {
    assertKeywordToken("continue");
  }

  public void test_AbstractScanner_keyword_default() throws Exception {
    assertKeywordToken("default");
  }

  public void test_AbstractScanner_keyword_do() throws Exception {
    assertKeywordToken("do");
  }

  public void test_AbstractScanner_keyword_else() throws Exception {
    assertKeywordToken("else");
  }

  public void test_AbstractScanner_keyword_extends() throws Exception {
    assertKeywordToken("extends");
  }

  public void test_AbstractScanner_keyword_factory() throws Exception {
    assertKeywordToken("factory");
  }

  public void test_AbstractScanner_keyword_false() throws Exception {
    assertKeywordToken("false");
  }

  public void test_AbstractScanner_keyword_final() throws Exception {
    assertKeywordToken("final");
  }

  public void test_AbstractScanner_keyword_finally() throws Exception {
    assertKeywordToken("finally");
  }

  public void test_AbstractScanner_keyword_for() throws Exception {
    assertKeywordToken("for");
  }

  public void test_AbstractScanner_keyword_get() throws Exception {
    assertKeywordToken("get");
  }

  public void test_AbstractScanner_keyword_if() throws Exception {
    assertKeywordToken("if");
  }

  public void test_AbstractScanner_keyword_implements() throws Exception {
    assertKeywordToken("implements");
  }

  public void test_AbstractScanner_keyword_import() throws Exception {
    assertKeywordToken("import");
  }

  public void test_AbstractScanner_keyword_in() throws Exception {
    assertKeywordToken("in");
  }

  public void test_AbstractScanner_keyword_interface() throws Exception {
    assertKeywordToken("interface");
  }

  public void test_AbstractScanner_keyword_is() throws Exception {
    assertKeywordToken("is");
  }

  public void test_AbstractScanner_keyword_library() throws Exception {
    assertKeywordToken("library");
  }

  public void test_AbstractScanner_keyword_native() throws Exception {
    assertKeywordToken("native");
  }

  public void test_AbstractScanner_keyword_negate() throws Exception {
    assertKeywordToken("negate");
  }

  public void test_AbstractScanner_keyword_new() throws Exception {
    assertKeywordToken("new");
  }

  public void test_AbstractScanner_keyword_null() throws Exception {
    assertKeywordToken("null");
  }

  public void test_AbstractScanner_keyword_operator() throws Exception {
    assertKeywordToken("operator");
  }

  public void test_AbstractScanner_keyword_return() throws Exception {
    assertKeywordToken("return");
  }

  public void test_AbstractScanner_keyword_set() throws Exception {
    assertKeywordToken("set");
  }

  public void test_AbstractScanner_keyword_source() throws Exception {
    assertKeywordToken("source");
  }

  public void test_AbstractScanner_keyword_static() throws Exception {
    assertKeywordToken("static");
  }

  public void test_AbstractScanner_keyword_super() throws Exception {
    assertKeywordToken("super");
  }

  public void test_AbstractScanner_keyword_switch() throws Exception {
    assertKeywordToken("switch");
  }

  public void test_AbstractScanner_keyword_this() throws Exception {
    assertKeywordToken("this");
  }

  public void test_AbstractScanner_keyword_throw() throws Exception {
    assertKeywordToken("throw");
  }

  public void test_AbstractScanner_keyword_true() throws Exception {
    assertKeywordToken("true");
  }

  public void test_AbstractScanner_keyword_try() throws Exception {
    assertKeywordToken("try");
  }

  public void test_AbstractScanner_keyword_typedef() throws Exception {
    assertKeywordToken("typedef");
  }

  public void test_AbstractScanner_keyword_var() throws Exception {
    assertKeywordToken("var");
  }

  public void test_AbstractScanner_keyword_void() throws Exception {
    assertKeywordToken("void");
  }

  public void test_AbstractScanner_keyword_while() throws Exception {
    assertKeywordToken("while");
  }

  public void test_AbstractScanner_lt() throws Exception {
    assertToken(TokenType.LT, scan("<"));
  }

  public void test_AbstractScanner_lt_eq() throws Exception {
    assertToken(TokenType.LT_EQ, scan("<="));
  }

  public void test_AbstractScanner_lt_lt() throws Exception {
    assertToken(TokenType.LT_LT, scan("<<"));
  }

  public void test_AbstractScanner_lt_lt_eq() throws Exception {
    assertToken(TokenType.LT_LT_EQ, scan("<<="));
  }

  public void test_AbstractScanner_minus() throws Exception {
    assertToken(TokenType.MINUS, scan("-"));
  }

  public void test_AbstractScanner_minus_eq() throws Exception {
    assertToken(TokenType.MINUS_EQ, scan("-="));
  }

  public void test_AbstractScanner_minus_minus() throws Exception {
    assertToken(TokenType.MINUS_MINUS, scan("--"));
  }

  public void test_AbstractScanner_open_curly_bracket() throws Exception {
    assertToken(TokenType.OPEN_CURLY_BRACKET, scan("{"));
  }

  public void test_AbstractScanner_open_paren() throws Exception {
    assertToken(TokenType.OPEN_PAREN, scan("("));
  }

  public void test_AbstractScanner_open_square_bracket() throws Exception {
    assertToken(TokenType.OPEN_SQUARE_BRACKET, scan("["));
  }

  public void test_AbstractScanner_openSquareBracket() throws Exception {
    assertToken(TokenType.OPEN_SQUARE_BRACKET, scan("["));
  }

  public void test_AbstractScanner_percent() throws Exception {
    assertToken(TokenType.PERCENT, scan("%"));
  }

  public void test_AbstractScanner_percent_eq() throws Exception {
    assertToken(TokenType.PERCENT_EQ, scan("%="));
  }

  public void test_AbstractScanner_period() throws Exception {
    assertToken(TokenType.PERIOD, scan("."));
  }

  public void test_AbstractScanner_period_period() throws Exception {
    assertToken(TokenType.PERIOD_PERIOD, scan(".."));
  }

  public void test_AbstractScanner_period_period_period() throws Exception {
    assertToken(TokenType.PERIOD_PERIOD_PERIOD, scan("..."));
  }

  public void test_AbstractScanner_periodAfterNumberNotIncluded() throws Exception {
    assertTokens(
        "42.isEven()",
        new Token(TokenType.INT, 0),
        new Token(TokenType.PERIOD, 2),
        new Token(TokenType.IDENTIFIER, 3),
        new Token(TokenType.OPEN_PAREN, 9),
        new Token(TokenType.CLOSE_PAREN, 10));
  }

  public void test_AbstractScanner_plus() throws Exception {
    assertToken(TokenType.PLUS, scan("+"));
  }

  public void test_AbstractScanner_plus_eq() throws Exception {
    assertToken(TokenType.PLUS_EQ, scan("+="));
  }

  public void test_AbstractScanner_plus_plus() throws Exception {
    assertToken(TokenType.PLUS_PLUS, scan("++"));
  }

  public void test_AbstractScanner_question() throws Exception {
    assertToken(TokenType.QUESTION, scan("?"));
  }

  public void test_AbstractScanner_semicolon() throws Exception {
    assertToken(TokenType.SEMICOLON, scan(";"));
  }

  public void test_AbstractScanner_slash() throws Exception {
    assertToken(TokenType.SLASH, scan("/"));
  }

  public void test_AbstractScanner_slash_eq() throws Exception {
    assertToken(TokenType.SLASH_EQ, scan("/="));
  }

  public void test_AbstractScanner_star() throws Exception {
    assertToken(TokenType.STAR, scan("*"));
  }

  public void test_AbstractScanner_star_eq() throws Exception {
    assertToken(TokenType.STAR_EQ, scan("*="));
  }

  public void test_AbstractScanner_string_multi_double() throws Exception {
    assertToken(TokenType.STRING, scan("\"\"\"string\"\"\""));
  }

  public void test_AbstractScanner_string_multi_single() throws Exception {
    assertToken(TokenType.STRING, scan("'''string'''"));
  }

  public void test_AbstractScanner_string_raw_double() throws Exception {
    assertToken(TokenType.STRING, scan("@\"string\""));
  }

  public void test_AbstractScanner_string_raw_single() throws Exception {
    assertToken(TokenType.STRING, scan("@'string'"));
  }

  public void test_AbstractScanner_string_simple_double() throws Exception {
    assertToken(TokenType.STRING, scan("\"string\""));
  }

  public void test_AbstractScanner_string_simple_interpolation_block() throws Exception {
    assertTokens(
        "\"Hello ${name}!\"",
        new Token(TokenType.STRING, 0),
        new Token(TokenType.STRING_INTERPOLATION, 7),
        new Token(TokenType.IDENTIFIER, 9),
        new Token(TokenType.CLOSE_CURLY_BRACKET, 13),
        new Token(TokenType.STRING, 14));
  }

  public void test_AbstractScanner_string_simple_interpolation_identifier() throws Exception {
    assertTokens( //
        "\"Hello $name!\"",
        new Token(TokenType.STRING, 0),
        new Token(TokenType.STRING_INTERPOLATION, 7),
        new Token(TokenType.IDENTIFIER, 8),
        new Token(TokenType.STRING, 12));
  }

  public void test_AbstractScanner_string_simple_single() throws Exception {
    assertToken(TokenType.STRING, scan("'string'"));
  }

  public void test_AbstractScanner_tilde() throws Exception {
    assertToken(TokenType.TILDE, scan("~"));
  }

  public void test_AbstractScanner_tilde_slash() throws Exception {
    assertToken(TokenType.TILDE_SLASH, scan("~/"));
  }

  public void test_AbstractScanner_tilde_slash_eq() throws Exception {
    assertToken(TokenType.TILDE_SLASH_EQ, scan("~/="));
  }

  protected abstract Token scan(String source);

  /**
   * Assert that when scanned the given source contains a single keyword token with the same lexeme
   * as the original source.
   * 
   * @param source the source to be scanned
   */
  private void assertKeywordToken(String source) {
    Token token = scan(source);
    assertToken(TokenType.KEYWORD, token);
    Object value = token.value();
    assertTrue(value instanceof Keyword);
    assertEquals(source, ((Keyword) value).getSyntax());
  }

  /**
   * Assert that the token that is being tested has the expected type.
   * 
   * @param expectedType the expected type of the token
   * @param actualToken the token being tested
   */
  private void assertToken(TokenType expectedType, Token actualToken) {
    assertNotNull(actualToken);
    assertEquals(expectedType, actualToken.getType());
    assertEquals(0, actualToken.getOffset());
  }

  /**
   * Assert that when scanned the given source contains a sequence of tokens identical to the given
   * tokens.
   * 
   * @param source the source to be scanned
   * @param expectedTokens the tokens that are expected to be in the source
   */
  private void assertTokens(String source, Token... expectedTokens) {
    Token token = scan(source);
    assertNotNull(token);
    for (Token expectedToken : expectedTokens) {
      assertEquals(expectedToken.getType(), token.getType());
      assertEquals(expectedToken.getOffset(), token.getOffset());
      token = token.getNext();
      assertNotNull(token);
    }
    assertEquals(TokenType.EOF, token.getType());
  }
}
