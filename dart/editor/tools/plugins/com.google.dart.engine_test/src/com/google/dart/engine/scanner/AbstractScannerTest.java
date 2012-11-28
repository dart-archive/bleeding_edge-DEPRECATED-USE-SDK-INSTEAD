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

import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.GatheringErrorListener;

import junit.framework.TestCase;

public abstract class AbstractScannerTest extends TestCase {
  public void test_ampersand() throws Exception {
    assertToken(TokenType.AMPERSAND, "&");
  }

  public void test_ampersand_ampersand() throws Exception {
    assertToken(TokenType.AMPERSAND_AMPERSAND, "&&");
  }

  public void test_ampersand_eq() throws Exception {
    assertToken(TokenType.AMPERSAND_EQ, "&=");
  }

  public void test_at() throws Exception {
    assertToken(TokenType.AT, "@");
  }

  public void test_backping() throws Exception {
    assertToken(TokenType.BACKPING, "`");
  }

  public void test_backslash() throws Exception {
    assertToken(TokenType.BACKSLASH, "\\");
  }

  public void test_bang() throws Exception {
    assertToken(TokenType.BANG, "!");
  }

  public void test_bang_eq() throws Exception {
    assertToken(TokenType.BANG_EQ, "!=");
  }

  public void test_bang_eq_eq() throws Exception {
    assertToken(TokenType.BANG_EQ_EQ, "!==");
  }

  public void test_bar() throws Exception {
    assertToken(TokenType.BAR, "|");
  }

  public void test_bar_bar() throws Exception {
    assertToken(TokenType.BAR_BAR, "||");
  }

  public void test_bar_eq() throws Exception {
    assertToken(TokenType.BAR_EQ, "|=");
  }

  public void test_caret() throws Exception {
    assertToken(TokenType.CARET, "^");
  }

  public void test_caret_eq() throws Exception {
    assertToken(TokenType.CARET_EQ, "^=");
  }

  public void test_close_curly_bracket() throws Exception {
    assertToken(TokenType.CLOSE_CURLY_BRACKET, "}");
  }

  public void test_close_paren() throws Exception {
    assertToken(TokenType.CLOSE_PAREN, ")");
  }

  public void test_close_quare_bracket() throws Exception {
    assertToken(TokenType.CLOSE_SQUARE_BRACKET, "]");
  }

  public void test_colon() throws Exception {
    assertToken(TokenType.COLON, ":");
  }

  public void test_comma() throws Exception {
    assertToken(TokenType.COMMA, ",");
  }

  public void test_comment_multi() throws Exception {
    assertComment(TokenType.MULTI_LINE_COMMENT, "/* comment */");
  }

  public void test_comment_multi_unterminated() throws Exception {
    assertError(ScannerErrorCode.UNTERMINATED_MULTI_LINE_COMMENT, 3, "/* x");
  }

  public void test_comment_nested() throws Exception {
    assertComment(TokenType.MULTI_LINE_COMMENT, "/* comment /* within a */ comment */");
  }

  public void test_comment_single() throws Exception {
    assertComment(TokenType.SINGLE_LINE_COMMENT, "// comment");
  }

  public void test_double_both_e() throws Exception {
    assertToken(TokenType.DOUBLE, "0.123e4");
  }

  public void test_double_both_E() throws Exception {
    assertToken(TokenType.DOUBLE, "0.123E4");
  }

  public void test_double_fraction() throws Exception {
    assertToken(TokenType.DOUBLE, ".123");
  }

  public void test_double_fraction_d() throws Exception {
    assertToken(TokenType.DOUBLE, ".123d");
  }

  public void test_double_fraction_D() throws Exception {
    assertToken(TokenType.DOUBLE, ".123D");
  }

  public void test_double_fraction_e() throws Exception {
    assertToken(TokenType.DOUBLE, ".123e4");
  }

  public void test_double_fraction_E() throws Exception {
    assertToken(TokenType.DOUBLE, ".123E4");
  }

  public void test_double_fraction_ed() throws Exception {
    assertToken(TokenType.DOUBLE, ".123e4d");
  }

  public void test_double_fraction_Ed() throws Exception {
    assertToken(TokenType.DOUBLE, ".123E4d");
  }

  public void test_double_missingDigitInExponent() throws Exception {
    assertError(ScannerErrorCode.MISSING_DIGIT, 1, "1e");
  }

  public void test_double_whole_d() throws Exception {
    assertToken(TokenType.DOUBLE, "12d");
  }

  public void test_double_whole_D() throws Exception {
    assertToken(TokenType.DOUBLE, "12D");
  }

  public void test_double_whole_e() throws Exception {
    assertToken(TokenType.DOUBLE, "12e4");
  }

  public void test_double_whole_E() throws Exception {
    assertToken(TokenType.DOUBLE, "12E4");
  }

  public void test_double_whole_ed() throws Exception {
    assertToken(TokenType.DOUBLE, "12e4d");
  }

  public void test_double_whole_Ed() throws Exception {
    assertToken(TokenType.DOUBLE, "12E4d");
  }

  public void test_eq() throws Exception {
    assertToken(TokenType.EQ, "=");
  }

  public void test_eq_eq() throws Exception {
    assertToken(TokenType.EQ_EQ, "==");
  }

  public void test_eq_eq_eq() throws Exception {
    assertToken(TokenType.EQ_EQ_EQ, "===");
  }

  public void test_gt() throws Exception {
    assertToken(TokenType.GT, ">");
  }

  public void test_gt_eq() throws Exception {
    assertToken(TokenType.GT_EQ, ">=");
  }

  public void test_gt_gt() throws Exception {
    assertToken(TokenType.GT_GT, ">>");
  }

  public void test_gt_gt_eq() throws Exception {
    assertToken(TokenType.GT_GT_EQ, ">>=");
  }

  public void test_gt_gt_gt() throws Exception {
    assertToken(TokenType.GT_GT_GT, ">>>");
  }

  public void test_gt_gt_gt_eq() throws Exception {
    assertToken(TokenType.GT_GT_GT_EQ, ">>>=");
  }

  public void test_hash() throws Exception {
    assertToken(TokenType.HASH, "#");
  }

  public void test_hexidecimal() throws Exception {
    assertToken(TokenType.HEXADECIMAL, "0x1A2B3C");
  }

  public void test_hexidecimal_missingDigit() throws Exception {
    assertError(ScannerErrorCode.MISSING_HEX_DIGIT, 1, "0x");
  }

  public void test_identifier() throws Exception {
    assertToken(TokenType.IDENTIFIER, "result");
  }

  public void test_illegalChar() throws Exception {
    assertError(ScannerErrorCode.ILLEGAL_CHARACTER, 0, "\u0312");
  }

  public void test_index() throws Exception {
    assertToken(TokenType.INDEX, "[]");
  }

  public void test_index_eq() throws Exception {
    assertToken(TokenType.INDEX_EQ, "[]=");
  }

  public void test_int() throws Exception {
    assertToken(TokenType.INT, "123");
  }

  public void test_int_initialZero() throws Exception {
    assertToken(TokenType.INT, "0123");
  }

  public void test_keyword_abstract() throws Exception {
    assertKeywordToken("abstract");
  }

  public void test_keyword_as() throws Exception {
    assertKeywordToken("as");
  }

  public void test_keyword_assert() throws Exception {
    assertKeywordToken("assert");
  }

  public void test_keyword_break() throws Exception {
    assertKeywordToken("break");
  }

  public void test_keyword_case() throws Exception {
    assertKeywordToken("case");
  }

  public void test_keyword_catch() throws Exception {
    assertKeywordToken("catch");
  }

  public void test_keyword_class() throws Exception {
    assertKeywordToken("class");
  }

  public void test_keyword_const() throws Exception {
    assertKeywordToken("const");
  }

  public void test_keyword_continue() throws Exception {
    assertKeywordToken("continue");
  }

  public void test_keyword_default() throws Exception {
    assertKeywordToken("default");
  }

  public void test_keyword_do() throws Exception {
    assertKeywordToken("do");
  }

  public void test_keyword_dynamic() throws Exception {
    assertKeywordToken("dynamic");
  }

  public void test_keyword_else() throws Exception {
    assertKeywordToken("else");
  }

  public void test_keyword_export() throws Exception {
    assertKeywordToken("export");
  }

  public void test_keyword_extends() throws Exception {
    assertKeywordToken("extends");
  }

  public void test_keyword_factory() throws Exception {
    assertKeywordToken("factory");
  }

  public void test_keyword_false() throws Exception {
    assertKeywordToken("false");
  }

  public void test_keyword_final() throws Exception {
    assertKeywordToken("final");
  }

  public void test_keyword_finally() throws Exception {
    assertKeywordToken("finally");
  }

  public void test_keyword_for() throws Exception {
    assertKeywordToken("for");
  }

  public void test_keyword_get() throws Exception {
    assertKeywordToken("get");
  }

  public void test_keyword_if() throws Exception {
    assertKeywordToken("if");
  }

  public void test_keyword_implements() throws Exception {
    assertKeywordToken("implements");
  }

  public void test_keyword_import() throws Exception {
    assertKeywordToken("import");
  }

  public void test_keyword_in() throws Exception {
    assertKeywordToken("in");
  }

  public void test_keyword_is() throws Exception {
    assertKeywordToken("is");
  }

  public void test_keyword_library() throws Exception {
    assertKeywordToken("library");
  }

  public void test_keyword_mixin() throws Exception {
    assertKeywordToken("mixin");
  }

  public void test_keyword_new() throws Exception {
    assertKeywordToken("new");
  }

  public void test_keyword_null() throws Exception {
    assertKeywordToken("null");
  }

  public void test_keyword_operator() throws Exception {
    assertKeywordToken("operator");
  }

  public void test_keyword_part() throws Exception {
    assertKeywordToken("part");
  }

  public void test_keyword_return() throws Exception {
    assertKeywordToken("return");
  }

  public void test_keyword_set() throws Exception {
    assertKeywordToken("set");
  }

  public void test_keyword_static() throws Exception {
    assertKeywordToken("static");
  }

  public void test_keyword_super() throws Exception {
    assertKeywordToken("super");
  }

  public void test_keyword_switch() throws Exception {
    assertKeywordToken("switch");
  }

  public void test_keyword_this() throws Exception {
    assertKeywordToken("this");
  }

  public void test_keyword_throw() throws Exception {
    assertKeywordToken("throw");
  }

  public void test_keyword_true() throws Exception {
    assertKeywordToken("true");
  }

  public void test_keyword_try() throws Exception {
    assertKeywordToken("try");
  }

  public void test_keyword_typedef() throws Exception {
    assertKeywordToken("typedef");
  }

  public void test_keyword_var() throws Exception {
    assertKeywordToken("var");
  }

  public void test_keyword_void() throws Exception {
    assertKeywordToken("void");
  }

  public void test_keyword_while() throws Exception {
    assertKeywordToken("while");
  }

  public void test_lt() throws Exception {
    assertToken(TokenType.LT, "<");
  }

  public void test_lt_eq() throws Exception {
    assertToken(TokenType.LT_EQ, "<=");
  }

  public void test_lt_lt() throws Exception {
    assertToken(TokenType.LT_LT, "<<");
  }

  public void test_lt_lt_eq() throws Exception {
    assertToken(TokenType.LT_LT_EQ, "<<=");
  }

  public void test_minus() throws Exception {
    assertToken(TokenType.MINUS, "-");
  }

  public void test_minus_eq() throws Exception {
    assertToken(TokenType.MINUS_EQ, "-=");
  }

  public void test_minus_minus() throws Exception {
    assertToken(TokenType.MINUS_MINUS, "--");
  }

  public void test_open_curly_bracket() throws Exception {
    assertToken(TokenType.OPEN_CURLY_BRACKET, "{");
  }

  public void test_open_paren() throws Exception {
    assertToken(TokenType.OPEN_PAREN, "(");
  }

  public void test_open_square_bracket() throws Exception {
    assertToken(TokenType.OPEN_SQUARE_BRACKET, "[");
  }

  public void test_openSquareBracket() throws Exception {
    assertToken(TokenType.OPEN_SQUARE_BRACKET, "[");
  }

  public void test_percent() throws Exception {
    assertToken(TokenType.PERCENT, "%");
  }

  public void test_percent_eq() throws Exception {
    assertToken(TokenType.PERCENT_EQ, "%=");
  }

  public void test_period() throws Exception {
    assertToken(TokenType.PERIOD, ".");
  }

  public void test_period_period() throws Exception {
    assertToken(TokenType.PERIOD_PERIOD, "..");
  }

  public void test_period_period_period() throws Exception {
    assertToken(TokenType.PERIOD_PERIOD_PERIOD, "...");
  }

  public void test_periodAfterNumberNotIncluded() throws Exception {
    assertTokens(
        "42.isEven()",
        new StringToken(TokenType.INT, "42", 0),
        new Token(TokenType.PERIOD, 2),
        new StringToken(TokenType.IDENTIFIER, "isEven", 3),
        new Token(TokenType.OPEN_PAREN, 9),
        new Token(TokenType.CLOSE_PAREN, 10));
  }

  public void test_plus() throws Exception {
    assertToken(TokenType.PLUS, "+");
  }

  public void test_plus_eq() throws Exception {
    assertToken(TokenType.PLUS_EQ, "+=");
  }

  public void test_plus_plus() throws Exception {
    assertToken(TokenType.PLUS_PLUS, "++");
  }

  public void test_question() throws Exception {
    assertToken(TokenType.QUESTION, "?");
  }

  public void test_scriptTag_withArgs() throws Exception {
    assertToken(TokenType.SCRIPT_TAG, "#!/bin/dart -debug");
  }

  public void test_scriptTag_withoutSpace() throws Exception {
    assertToken(TokenType.SCRIPT_TAG, "#!/bin/dart");
  }

  public void test_scriptTag_withSpace() throws Exception {
    assertToken(TokenType.SCRIPT_TAG, "#! /bin/dart");
  }

  public void test_semicolon() throws Exception {
    assertToken(TokenType.SEMICOLON, ";");
  }

  public void test_slash() throws Exception {
    assertToken(TokenType.SLASH, "/");
  }

  public void test_slash_eq() throws Exception {
    assertToken(TokenType.SLASH_EQ, "/=");
  }

  public void test_star() throws Exception {
    assertToken(TokenType.STAR, "*");
  }

  public void test_star_eq() throws Exception {
    assertToken(TokenType.STAR_EQ, "*=");
  }

  public void test_startAndEnd() {
    Token token = scan("a");
    Token previous = token.getPrevious();
    assertEquals(token, previous.getNext());
    assertEquals(previous, previous.getPrevious());
    Token next = token.getNext();
    assertEquals(next, next.getNext());
    assertEquals(token, next.getPrevious());
  }

  public void test_string_multi_double() throws Exception {
    assertToken(TokenType.STRING, "\"\"\"multi-line\nstring\"\"\"");
  }

  public void test_string_multi_interpolation_block() throws Exception {
    assertTokens(
        "\"Hello ${name}!\"",
        new StringToken(TokenType.STRING, "\"Hello ", 0),
        new StringToken(TokenType.STRING_INTERPOLATION_EXPRESSION, "${", 7),
        new StringToken(TokenType.IDENTIFIER, "name", 9),
        new Token(TokenType.CLOSE_CURLY_BRACKET, 13),
        new StringToken(TokenType.STRING, "!\"", 14));
  }

  public void test_string_multi_interpolation_identifier() throws Exception {
    assertTokens( //
        "\"Hello $name!\"",
        new StringToken(TokenType.STRING, "\"Hello ", 0),
        new StringToken(TokenType.STRING_INTERPOLATION_IDENTIFIER, "$", 7),
        new StringToken(TokenType.IDENTIFIER, "name", 8),
        new StringToken(TokenType.STRING, "!\"", 12));
  }

  public void test_string_multi_single() throws Exception {
    assertToken(TokenType.STRING, "'''string'''");
  }

  public void test_string_multi_unterminated() throws Exception {
    assertError(ScannerErrorCode.UNTERMINATED_STRING_LITERAL, 8, "'''string");
  }

  public void test_string_raw_multi_double() throws Exception {
    assertToken(TokenType.STRING, "r\"\"\"string\"\"\"");
  }

  public void test_string_raw_multi_single() throws Exception {
    assertToken(TokenType.STRING, "r'''string'''");
  }

  public void test_string_raw_multi_unterminated() throws Exception {
    assertError(ScannerErrorCode.UNTERMINATED_STRING_LITERAL, 9, "r'''string");
  }

  public void test_string_raw_simple_double() throws Exception {
    assertToken(TokenType.STRING, "r\"string\"");
  }

  public void test_string_raw_simple_single() throws Exception {
    assertToken(TokenType.STRING, "r'string'");
  }

  public void test_string_raw_simple_unterminated_eof() throws Exception {
    assertError(ScannerErrorCode.UNTERMINATED_STRING_LITERAL, 7, "r'string");
  }

  public void test_string_raw_simple_unterminated_eol() throws Exception {
    assertError(ScannerErrorCode.UNTERMINATED_STRING_LITERAL, 8, "r'string\n");
  }

  public void test_string_simple_double() throws Exception {
    assertToken(TokenType.STRING, "\"string\"");
  }

  public void test_string_simple_escapedDollar() throws Exception {
    assertToken(TokenType.STRING, "'a\\$b'");
  }

  public void test_string_simple_interpolation_block() throws Exception {
    assertTokens(
        "'Hello ${name}!'",
        new StringToken(TokenType.STRING, "'Hello ", 0),
        new StringToken(TokenType.STRING_INTERPOLATION_EXPRESSION, "${", 7),
        new StringToken(TokenType.IDENTIFIER, "name", 9),
        new Token(TokenType.CLOSE_CURLY_BRACKET, 13),
        new StringToken(TokenType.STRING, "!'", 14));
  }

  public void test_string_simple_interpolation_blockWithNestedMap() throws Exception {
    assertTokens(
        "'a ${f({'b' : 'c'})} d'",
        new StringToken(TokenType.STRING, "'a ", 0),
        new StringToken(TokenType.STRING_INTERPOLATION_EXPRESSION, "${", 3),
        new StringToken(TokenType.IDENTIFIER, "f", 5),
        new Token(TokenType.OPEN_PAREN, 6),
        new Token(TokenType.OPEN_CURLY_BRACKET, 7),
        new StringToken(TokenType.STRING, "'b'", 8),
        new Token(TokenType.COLON, 12),
        new StringToken(TokenType.STRING, "'c'", 14),
        new Token(TokenType.CLOSE_CURLY_BRACKET, 17),
        new Token(TokenType.CLOSE_PAREN, 18),
        new Token(TokenType.CLOSE_CURLY_BRACKET, 19),
        new StringToken(TokenType.STRING, " d'", 20));
  }

  public void test_string_simple_interpolation_firstAndLast() throws Exception {
    assertTokens( //
        "'$greeting $name'",
        new StringToken(TokenType.STRING, "'", 0),
        new StringToken(TokenType.STRING_INTERPOLATION_IDENTIFIER, "$", 1),
        new StringToken(TokenType.IDENTIFIER, "greeting", 2),
        new StringToken(TokenType.STRING, " ", 10),
        new StringToken(TokenType.STRING_INTERPOLATION_IDENTIFIER, "$", 11),
        new StringToken(TokenType.IDENTIFIER, "name", 12),
        new StringToken(TokenType.STRING, "'", 16));
  }

  public void test_string_simple_interpolation_identifier() throws Exception {
    assertTokens( //
        "'Hello $name!'",
        new StringToken(TokenType.STRING, "'Hello ", 0),
        new StringToken(TokenType.STRING_INTERPOLATION_IDENTIFIER, "$", 7),
        new StringToken(TokenType.IDENTIFIER, "name", 8),
        new StringToken(TokenType.STRING, "!'", 12));
  }

  public void test_string_simple_single() throws Exception {
    assertToken(TokenType.STRING, "'string'");
  }

  public void test_string_simple_unterminated_eof() throws Exception {
    assertError(ScannerErrorCode.UNTERMINATED_STRING_LITERAL, 6, "'string");
  }

  public void test_string_simple_unterminated_eol() throws Exception {
    assertError(ScannerErrorCode.UNTERMINATED_STRING_LITERAL, 7, "'string\r");
  }

  public void test_tilde() throws Exception {
    assertToken(TokenType.TILDE, "~");
  }

  public void test_tilde_slash() throws Exception {
    assertToken(TokenType.TILDE_SLASH, "~/");
  }

  public void test_tilde_slash_eq() throws Exception {
    assertToken(TokenType.TILDE_SLASH_EQ, "~/=");
  }

  public void test_unclosedPairInInterpolation() throws Exception {
    GatheringErrorListener listener = new GatheringErrorListener();
    scan("'${(}'", listener);
  }

  protected abstract Token scan(String source, GatheringErrorListener listener);

  private void assertComment(TokenType commentType, String source) throws Exception {
    Token token = scan(source);
    assertNotNull(token);
    assertEquals(TokenType.EOF, token.getType());

    Token comment = token.getPrecedingComments();
    assertNotNull(comment);
    assertEquals(commentType, comment.getType());
    assertEquals(0, comment.getOffset());
    assertEquals(source.length(), comment.getLength());
    assertEquals(source, comment.getLexeme());
  }

  /**
   * Assert that scanning the given source produces an error with the given code.
   * 
   * @param illegalCharacter
   * @param i
   * @param source the source to be scanned to produce the error
   */
  private void assertError(ScannerErrorCode expectedError, int expectedOffset, String source) {
    GatheringErrorListener listener = new GatheringErrorListener();
    scan(source, listener);
    listener.assertErrors(new AnalysisError(
        null,
        expectedOffset,
        1,
        expectedError,
        (int) source.charAt(expectedOffset)));
  }

  /**
   * Assert that when scanned the given source contains a single keyword token with the same lexeme
   * as the original source.
   * 
   * @param source the source to be scanned
   */
  private void assertKeywordToken(String source) {
    Token token = scan(source);
    assertNotNull(token);
    assertEquals(TokenType.KEYWORD, token.getType());
    assertEquals(0, token.getOffset());
    assertEquals(source.length(), token.getLength());
    assertEquals(source, token.getLexeme());
    Object value = token.value();
    assertTrue(value instanceof Keyword);
    assertEquals(source, ((Keyword) value).getSyntax());

    token = scan(" " + source + " ");
    assertNotNull(token);
    assertEquals(TokenType.KEYWORD, token.getType());
    assertEquals(1, token.getOffset());
    assertEquals(source.length(), token.getLength());
    assertEquals(source, token.getLexeme());
    value = token.value();
    assertTrue(value instanceof Keyword);
    assertEquals(source, ((Keyword) value).getSyntax());

    assertEquals(TokenType.EOF, token.getNext().getType());
  }

  /**
   * Assert that the token scanned from the given source has the expected type.
   * 
   * @param expectedType the expected type of the token
   * @param source the source to be scanned to produce the actual token
   */
  private Token assertToken(TokenType expectedType, String source) {
    Token originalToken = scan(source);
    assertNotNull(originalToken);
    assertEquals(expectedType, originalToken.getType());
    assertEquals(0, originalToken.getOffset());
    assertEquals(source.length(), originalToken.getLength());
    assertEquals(source, originalToken.getLexeme());

    if (expectedType == TokenType.SCRIPT_TAG) {
      // Adding space before the script tag is not allowed, and adding text at the end changes nothing.
      return originalToken;
    } else if (expectedType == TokenType.SINGLE_LINE_COMMENT) {
      // Adding space to an end-of-line comment changes the comment.
      Token tokenWithSpaces = scan(" " + source);
      assertNotNull(tokenWithSpaces);
      assertEquals(expectedType, tokenWithSpaces.getType());
      assertEquals(1, tokenWithSpaces.getOffset());
      assertEquals(source.length(), tokenWithSpaces.getLength());
      assertEquals(source, tokenWithSpaces.getLexeme());
      return originalToken;
    }

    Token tokenWithSpaces = scan(" " + source + " ");
    assertNotNull(tokenWithSpaces);
    assertEquals(expectedType, tokenWithSpaces.getType());
    assertEquals(1, tokenWithSpaces.getOffset());
    assertEquals(source.length(), tokenWithSpaces.getLength());
    assertEquals(source, tokenWithSpaces.getLexeme());

    assertEquals(TokenType.EOF, originalToken.getNext().getType());

    return originalToken;
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
    for (int i = 0; i < expectedTokens.length; i++) {
      Token expectedToken = expectedTokens[i];
      assertEquals("Wrong type for token " + i, expectedToken.getType(), token.getType());
      assertEquals("Wrong offset for token " + i, expectedToken.getOffset(), token.getOffset());
      assertEquals("Wrong length for token " + i, expectedToken.getLength(), token.getLength());
      assertEquals("Wrong lexeme for token " + i, expectedToken.getLexeme(), token.getLexeme());
      token = token.getNext();
      assertNotNull(token);
    }
    assertEquals(TokenType.EOF, token.getType());
  }

  private Token scan(String source) {
    GatheringErrorListener listener = new GatheringErrorListener();
    Token token = scan(source, listener);
    listener.assertNoErrors();
    return token;
  }
}
