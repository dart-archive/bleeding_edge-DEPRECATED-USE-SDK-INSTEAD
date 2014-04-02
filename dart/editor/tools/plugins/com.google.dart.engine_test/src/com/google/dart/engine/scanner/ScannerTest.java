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
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.source.TestSource;
import com.google.dart.engine.utilities.os.OSUtilities;
import com.google.dart.engine.utilities.source.LineInfo;

import junit.framework.TestCase;

public class ScannerTest extends TestCase {
  /**
   * Instances of the class {@code ExpectedLocation} encode information about the expected location
   * of a given offset in source code.
   */
  private static class ExpectedLocation {
    private int offset;
    private int lineNumber;
    private int columnNumber;

    public ExpectedLocation(int offset, int lineNumber, int columnNumber) {
      this.offset = offset;
      this.lineNumber = lineNumber;
      this.columnNumber = columnNumber;
    }
  }

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

  public void test_comment_disabled_multi() throws Exception {
    Scanner scanner = new Scanner(
        null,
        new CharSequenceReader("/* comment */ "),
        AnalysisErrorListener.NULL_LISTENER);
    scanner.setPreserveComments(false);
    Token token = scanner.tokenize();
    assertNotNull(token);
    assertNull(token.getPrecedingComments());
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

  public void test_double_fraction_e() throws Exception {
    assertToken(TokenType.DOUBLE, ".123e4");
  }

  public void test_double_fraction_E() throws Exception {
    assertToken(TokenType.DOUBLE, ".123E4");
  }

  public void test_double_missingDigitInExponent() throws Exception {
    assertError(ScannerErrorCode.MISSING_DIGIT, 1, "1e");
  }

  public void test_double_whole_e() throws Exception {
    assertToken(TokenType.DOUBLE, "12e4");
  }

  public void test_double_whole_E() throws Exception {
    assertToken(TokenType.DOUBLE, "12E4");
  }

  public void test_eq() throws Exception {
    assertToken(TokenType.EQ, "=");
  }

  public void test_eq_eq() throws Exception {
    assertToken(TokenType.EQ_EQ, "==");
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

  public void test_illegalChar_cyrillicLetter_middle() throws Exception {
    assertError(ScannerErrorCode.ILLEGAL_CHARACTER, 0, "Shche\u0433lov");
  }

  public void test_illegalChar_cyrillicLetter_start() throws Exception {
    assertError(ScannerErrorCode.ILLEGAL_CHARACTER, 0, "\u0429");
  }

  public void test_illegalChar_nbsp() throws Exception {
    assertError(ScannerErrorCode.ILLEGAL_CHARACTER, 0, "\u00A0");
  }

  public void test_illegalChar_notLetter() throws Exception {
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

  public void test_keyword_enum() throws Exception {
    assertKeywordToken("enum");
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

  public void test_keyword_rethrow() throws Exception {
    assertKeywordToken("rethrow");
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

  public void test_keyword_with() throws Exception {
    assertKeywordToken("with");
  }

  public void test_lineInfo_multilineComment() throws Exception {
    String source = "/*\r *\r */";
    assertLineInfo(
        source,
        new ExpectedLocation(0, 1, 1),
        new ExpectedLocation(4, 2, 2),
        new ExpectedLocation(source.length() - 1, 3, 3));
  }

  public void test_lineInfo_multilineString() throws Exception {
    String source = "'''a\r\nbc\r\nd'''";
    assertLineInfo(
        source,
        new ExpectedLocation(0, 1, 1),
        new ExpectedLocation(7, 2, 2),
        new ExpectedLocation(source.length() - 1, 3, 4));
  }

  public void test_lineInfo_simpleClass() throws Exception {
    String source = "class Test {\r\n    String s = '...';\r\n    int get x => s.MISSING_GETTER;\r\n}";
    assertLineInfo(
        source,
        new ExpectedLocation(0, 1, 1),
        new ExpectedLocation(source.indexOf("MISSING_GETTER"), 3, 20),
        new ExpectedLocation(source.length() - 1, 4, 1));
  }

  public void test_lineInfo_slashN() throws Exception {
    String source = "class Test {\n}";
    assertLineInfo(source, new ExpectedLocation(0, 1, 1), new ExpectedLocation(
        source.indexOf("}"),
        2,
        1));
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

  public void test_periodAfterNumberNotIncluded_identifier() throws Exception {
    assertTokens(
        "42.isEven()",
        new StringToken(TokenType.INT, "42", 0),
        new Token(TokenType.PERIOD, 2),
        new StringToken(TokenType.IDENTIFIER, "isEven", 3),
        new Token(TokenType.OPEN_PAREN, 9),
        new Token(TokenType.CLOSE_PAREN, 10));
  }

  public void test_periodAfterNumberNotIncluded_period() throws Exception {
    assertTokens(
        "42..isEven()",
        new StringToken(TokenType.INT, "42", 0),
        new Token(TokenType.PERIOD_PERIOD, 2),
        new StringToken(TokenType.IDENTIFIER, "isEven", 4),
        new Token(TokenType.OPEN_PAREN, 10),
        new Token(TokenType.CLOSE_PAREN, 11));
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

  public void test_setSourceStart() throws Exception {
    int offsetDelta = 42;
    GatheringErrorListener listener = new GatheringErrorListener();
    Scanner scanner = new Scanner(null, new SubSequenceReader("a", offsetDelta), listener);
    scanner.setSourceStart(3, 9);
    scanner.tokenize();
    int[] lineStarts = scanner.getLineStarts();
    assertNotNull(lineStarts);
    assertEquals(3, lineStarts.length);
    assertEquals(33, lineStarts[2]);
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
    assertToken(TokenType.STRING, "\"\"\"line1\nline2\"\"\"");
  }

  public void test_string_multi_embeddedQuotes() throws Exception {
    assertToken(TokenType.STRING, "\"\"\"line1\n\"\"\nline2\"\"\"");
  }

  public void test_string_multi_embeddedQuotes_escapedChar() throws Exception {
    assertToken(TokenType.STRING, "\"\"\"a\"\"\\tb\"\"\"");
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

  public void test_string_multi_slashEnter() throws Exception {
    assertToken(TokenType.STRING, "'''\\\n'''");
  }

  public void test_string_multi_unterminated() throws Exception {
    assertError(ScannerErrorCode.UNTERMINATED_STRING_LITERAL, 8, "'''string");
  }

  public void test_string_raw_multi_double() throws Exception {
    assertToken(TokenType.STRING, "r\"\"\"line1\nline2\"\"\"");
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

  public void test_string_simple_interpolation_adjacentIdentifiers() throws Exception {
    assertTokens( //
        "'$a$b'",
        new StringToken(TokenType.STRING, "'", 0),
        new StringToken(TokenType.STRING_INTERPOLATION_IDENTIFIER, "$", 1),
        new StringToken(TokenType.IDENTIFIER, "a", 2),
        new StringToken(TokenType.STRING, "", 3),
        new StringToken(TokenType.STRING_INTERPOLATION_IDENTIFIER, "$", 3),
        new StringToken(TokenType.IDENTIFIER, "b", 4),
        new StringToken(TokenType.STRING, "'", 5));
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

  public void test_string_simple_interpolation_missingIdentifier() throws Exception {
    assertTokens( //
        "'$x$'",
        new StringToken(TokenType.STRING, "'", 0),
        new StringToken(TokenType.STRING_INTERPOLATION_IDENTIFIER, "$", 1),
        new StringToken(TokenType.IDENTIFIER, "x", 2),
        new StringToken(TokenType.STRING, "", 3),
        new StringToken(TokenType.STRING_INTERPOLATION_IDENTIFIER, "$", 3),
        new StringToken(TokenType.STRING, "'", 4));
  }

  public void test_string_simple_interpolation_nonIdentifier() throws Exception {
    assertTokens( //
        "'$1'",
        new StringToken(TokenType.STRING, "'", 0),
        new StringToken(TokenType.STRING_INTERPOLATION_IDENTIFIER, "$", 1),
        new StringToken(TokenType.STRING, "1'", 2));
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
    scanWithListener("'${(}'", listener);
  }

  private void assertComment(TokenType commentType, String source) throws Exception {
    //
    // Test without a trailing end-of-line marker
    //
    Token token = scan(source);
    assertNotNull(token);
    assertEquals(TokenType.EOF, token.getType());

    Token comment = token.getPrecedingComments();
    assertNotNull(comment);
    assertEquals(commentType, comment.getType());
    assertEquals(0, comment.getOffset());
    assertEquals(source.length(), comment.getLength());
    assertEquals(source, comment.getLexeme());
    //
    // Test with a trailing end-of-line marker
    //
    token = scan(source + OSUtilities.LINE_SEPARATOR);
    assertNotNull(token);
    assertEquals(TokenType.EOF, token.getType());

    comment = token.getPrecedingComments();
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
    scanWithListener(source, listener);
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

  private void assertLineInfo(String source, ExpectedLocation... expectedLocations) {
    GatheringErrorListener listener = new GatheringErrorListener();
    scanWithListener(source, listener);
    listener.assertNoErrors();
    LineInfo info = listener.getLineInfo(new TestSource());
    assertNotNull(info);
    for (ExpectedLocation expectedLocation : expectedLocations) {
      LineInfo.Location location = info.getLocation(expectedLocation.offset);
      assertEquals(expectedLocation.lineNumber, location.getLineNumber());
      assertEquals(expectedLocation.columnNumber, location.getColumnNumber());
    }
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
    } else if (expectedType == TokenType.INT || expectedType == TokenType.DOUBLE) {
      Token tokenWithLowerD = scan(source + "d");
      assertNotNull(tokenWithLowerD);
      assertEquals(expectedType, tokenWithLowerD.getType());
      assertEquals(0, tokenWithLowerD.getOffset());
      assertEquals(source.length(), tokenWithLowerD.getLength());
      assertEquals(source, tokenWithLowerD.getLexeme());

      Token tokenWithUpperD = scan(source + "D");
      assertNotNull(tokenWithUpperD);
      assertEquals(expectedType, tokenWithUpperD.getType());
      assertEquals(0, tokenWithUpperD.getOffset());
      assertEquals(source.length(), tokenWithUpperD.getLength());
      assertEquals(source, tokenWithUpperD.getLexeme());
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
    Token token = scanWithListener(source, listener);
    listener.assertNoErrors();
    return token;
  }

  private Token scanWithListener(String source, GatheringErrorListener listener) {
    Scanner scanner = new Scanner(null, new CharSequenceReader(source), listener);
    Token result = scanner.tokenize();
    listener.setLineInfo(new TestSource(), scanner.getLineStarts());
    return result;
  }
}
