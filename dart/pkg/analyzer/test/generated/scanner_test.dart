// This code was auto-generated, is not intended to be edited, and is subject to
// significant change. Please see the README file for more information.
library engine.scanner_test;
import 'package:analyzer/src/generated/java_core.dart';
import 'package:analyzer/src/generated/java_junit.dart';
import 'package:analyzer/src/generated/source.dart';
import 'package:analyzer/src/generated/error.dart';
import 'package:analyzer/src/generated/scanner.dart';
import 'package:analyzer/src/generated/utilities_collection.dart' show TokenMap;
import 'package:unittest/unittest.dart' as _ut;
import 'test_support.dart';
class KeywordStateTest extends JUnitTestCase {
  void test_KeywordState() {
    List<Keyword> keywords = Keyword.values;
    int keywordCount = keywords.length;
    List<String> textToTest = new List<String>(keywordCount * 3);
    for (int i = 0; i < keywordCount; i++) {
      String syntax = keywords[i].syntax;
      textToTest[i] = syntax;
      textToTest[i + keywordCount] = "${syntax}x";
      textToTest[i + keywordCount * 2] = syntax.substring(0, syntax.length - 1);
    }
    KeywordState firstState = KeywordState.KEYWORD_STATE;
    for (int i = 0; i < textToTest.length; i++) {
      String text = textToTest[i];
      int index = 0;
      int length = text.length;
      KeywordState state = firstState;
      while (index < length && state != null) {
        state = state.next(text.codeUnitAt(index));
        index++;
      }
      if (i < keywordCount) {
        JUnitTestCase.assertNotNull(state);
        JUnitTestCase.assertNotNull(state.keyword());
        JUnitTestCase.assertEquals(keywords[i], state.keyword());
      } else if (i < keywordCount * 2) {
        JUnitTestCase.assertNull(state);
      } else {
        JUnitTestCase.assertNotNull(state);
      }
    }
  }
  static dartSuite() {
    _ut.group('KeywordStateTest', () {
      _ut.test('test_KeywordState', () {
        final __test = new KeywordStateTest();
        runJUnitTest(__test, __test.test_KeywordState);
      });
    });
  }
}
class CharSequenceReaderTest extends JUnitTestCase {
  void test_advance() {
    CharSequenceReader reader = new CharSequenceReader(new CharSequence("x"));
    JUnitTestCase.assertEquals(0x78, reader.advance());
    JUnitTestCase.assertEquals(-1, reader.advance());
    JUnitTestCase.assertEquals(-1, reader.advance());
  }
  void test_creation() {
    JUnitTestCase.assertNotNull(new CharSequenceReader(new CharSequence("x")));
  }
  void test_getOffset() {
    CharSequenceReader reader = new CharSequenceReader(new CharSequence("x"));
    JUnitTestCase.assertEquals(-1, reader.offset);
    reader.advance();
    JUnitTestCase.assertEquals(0, reader.offset);
    reader.advance();
    JUnitTestCase.assertEquals(0, reader.offset);
  }
  void test_getString() {
    CharSequenceReader reader = new CharSequenceReader(new CharSequence("xyzzy"));
    reader.offset = 3;
    JUnitTestCase.assertEquals("yzz", reader.getString(1, 0));
    JUnitTestCase.assertEquals("zzy", reader.getString(2, 1));
  }
  void test_peek() {
    CharSequenceReader reader = new CharSequenceReader(new CharSequence("xy"));
    JUnitTestCase.assertEquals(0x78, reader.peek());
    JUnitTestCase.assertEquals(0x78, reader.peek());
    reader.advance();
    JUnitTestCase.assertEquals(0x79, reader.peek());
    JUnitTestCase.assertEquals(0x79, reader.peek());
    reader.advance();
    JUnitTestCase.assertEquals(-1, reader.peek());
    JUnitTestCase.assertEquals(-1, reader.peek());
  }
  void test_setOffset() {
    CharSequenceReader reader = new CharSequenceReader(new CharSequence("xyz"));
    reader.offset = 2;
    JUnitTestCase.assertEquals(2, reader.offset);
  }
  static dartSuite() {
    _ut.group('CharSequenceReaderTest', () {
      _ut.test('test_advance', () {
        final __test = new CharSequenceReaderTest();
        runJUnitTest(__test, __test.test_advance);
      });
      _ut.test('test_creation', () {
        final __test = new CharSequenceReaderTest();
        runJUnitTest(__test, __test.test_creation);
      });
      _ut.test('test_getOffset', () {
        final __test = new CharSequenceReaderTest();
        runJUnitTest(__test, __test.test_getOffset);
      });
      _ut.test('test_getString', () {
        final __test = new CharSequenceReaderTest();
        runJUnitTest(__test, __test.test_getString);
      });
      _ut.test('test_peek', () {
        final __test = new CharSequenceReaderTest();
        runJUnitTest(__test, __test.test_peek);
      });
      _ut.test('test_setOffset', () {
        final __test = new CharSequenceReaderTest();
        runJUnitTest(__test, __test.test_setOffset);
      });
    });
  }
}
class TokenTypeTest extends EngineTestCase {
  void test_isOperator() {
    JUnitTestCase.assertTrue(TokenType.AMPERSAND.isOperator);
    JUnitTestCase.assertTrue(TokenType.AMPERSAND_AMPERSAND.isOperator);
    JUnitTestCase.assertTrue(TokenType.AMPERSAND_EQ.isOperator);
    JUnitTestCase.assertTrue(TokenType.BANG.isOperator);
    JUnitTestCase.assertTrue(TokenType.BANG_EQ.isOperator);
    JUnitTestCase.assertTrue(TokenType.BAR.isOperator);
    JUnitTestCase.assertTrue(TokenType.BAR_BAR.isOperator);
    JUnitTestCase.assertTrue(TokenType.BAR_EQ.isOperator);
    JUnitTestCase.assertTrue(TokenType.CARET.isOperator);
    JUnitTestCase.assertTrue(TokenType.CARET_EQ.isOperator);
    JUnitTestCase.assertTrue(TokenType.EQ.isOperator);
    JUnitTestCase.assertTrue(TokenType.EQ_EQ.isOperator);
    JUnitTestCase.assertTrue(TokenType.GT.isOperator);
    JUnitTestCase.assertTrue(TokenType.GT_EQ.isOperator);
    JUnitTestCase.assertTrue(TokenType.GT_GT.isOperator);
    JUnitTestCase.assertTrue(TokenType.GT_GT_EQ.isOperator);
    JUnitTestCase.assertTrue(TokenType.INDEX.isOperator);
    JUnitTestCase.assertTrue(TokenType.INDEX_EQ.isOperator);
    JUnitTestCase.assertTrue(TokenType.IS.isOperator);
    JUnitTestCase.assertTrue(TokenType.LT.isOperator);
    JUnitTestCase.assertTrue(TokenType.LT_EQ.isOperator);
    JUnitTestCase.assertTrue(TokenType.LT_LT.isOperator);
    JUnitTestCase.assertTrue(TokenType.LT_LT_EQ.isOperator);
    JUnitTestCase.assertTrue(TokenType.MINUS.isOperator);
    JUnitTestCase.assertTrue(TokenType.MINUS_EQ.isOperator);
    JUnitTestCase.assertTrue(TokenType.MINUS_MINUS.isOperator);
    JUnitTestCase.assertTrue(TokenType.PERCENT.isOperator);
    JUnitTestCase.assertTrue(TokenType.PERCENT_EQ.isOperator);
    JUnitTestCase.assertTrue(TokenType.PERIOD_PERIOD.isOperator);
    JUnitTestCase.assertTrue(TokenType.PLUS.isOperator);
    JUnitTestCase.assertTrue(TokenType.PLUS_EQ.isOperator);
    JUnitTestCase.assertTrue(TokenType.PLUS_PLUS.isOperator);
    JUnitTestCase.assertTrue(TokenType.QUESTION.isOperator);
    JUnitTestCase.assertTrue(TokenType.SLASH.isOperator);
    JUnitTestCase.assertTrue(TokenType.SLASH_EQ.isOperator);
    JUnitTestCase.assertTrue(TokenType.STAR.isOperator);
    JUnitTestCase.assertTrue(TokenType.STAR_EQ.isOperator);
    JUnitTestCase.assertTrue(TokenType.TILDE.isOperator);
    JUnitTestCase.assertTrue(TokenType.TILDE_SLASH.isOperator);
    JUnitTestCase.assertTrue(TokenType.TILDE_SLASH_EQ.isOperator);
  }
  void test_isUserDefinableOperator() {
    JUnitTestCase.assertTrue(TokenType.AMPERSAND.isUserDefinableOperator);
    JUnitTestCase.assertTrue(TokenType.BAR.isUserDefinableOperator);
    JUnitTestCase.assertTrue(TokenType.CARET.isUserDefinableOperator);
    JUnitTestCase.assertTrue(TokenType.EQ_EQ.isUserDefinableOperator);
    JUnitTestCase.assertTrue(TokenType.GT.isUserDefinableOperator);
    JUnitTestCase.assertTrue(TokenType.GT_EQ.isUserDefinableOperator);
    JUnitTestCase.assertTrue(TokenType.GT_GT.isUserDefinableOperator);
    JUnitTestCase.assertTrue(TokenType.INDEX.isUserDefinableOperator);
    JUnitTestCase.assertTrue(TokenType.INDEX_EQ.isUserDefinableOperator);
    JUnitTestCase.assertTrue(TokenType.LT.isUserDefinableOperator);
    JUnitTestCase.assertTrue(TokenType.LT_EQ.isUserDefinableOperator);
    JUnitTestCase.assertTrue(TokenType.LT_LT.isUserDefinableOperator);
    JUnitTestCase.assertTrue(TokenType.MINUS.isUserDefinableOperator);
    JUnitTestCase.assertTrue(TokenType.PERCENT.isUserDefinableOperator);
    JUnitTestCase.assertTrue(TokenType.PLUS.isUserDefinableOperator);
    JUnitTestCase.assertTrue(TokenType.SLASH.isUserDefinableOperator);
    JUnitTestCase.assertTrue(TokenType.STAR.isUserDefinableOperator);
    JUnitTestCase.assertTrue(TokenType.TILDE.isUserDefinableOperator);
    JUnitTestCase.assertTrue(TokenType.TILDE_SLASH.isUserDefinableOperator);
  }
  static dartSuite() {
    _ut.group('TokenTypeTest', () {
      _ut.test('test_isOperator', () {
        final __test = new TokenTypeTest();
        runJUnitTest(__test, __test.test_isOperator);
      });
      _ut.test('test_isUserDefinableOperator', () {
        final __test = new TokenTypeTest();
        runJUnitTest(__test, __test.test_isUserDefinableOperator);
      });
    });
  }
}
/**
 * The class `TokenFactory` defines utility methods that can be used to create tokens.
 */
class TokenFactory {
  static Token token(Keyword keyword) => new KeywordToken(keyword, 0);
  static Token token2(String lexeme) => new StringToken(TokenType.STRING, lexeme, 0);
  static Token token3(TokenType type) => new Token(type, 0);
  static Token token4(TokenType type, String lexeme) => new StringToken(type, lexeme, 0);
}
/**
 * Instances of the class `TokenStreamValidator` are used to validate the correct construction
 * of a stream of tokens.
 */
class TokenStreamValidator {

  /**
   * Validate that the stream of tokens that starts with the given token is correct.
   *
   * @param token the first token in the stream of tokens to be validated
   */
  void validate(Token token) {
    JavaStringBuilder builder = new JavaStringBuilder();
    validateStream(builder, token);
    if (builder.length > 0) {
      JUnitTestCase.fail(builder.toString());
    }
  }
  void validateStream(JavaStringBuilder builder, Token token) {
    if (token == null) {
      return;
    }
    Token previousToken = null;
    int previousEnd = -1;
    Token currentToken = token;
    while (currentToken != null && currentToken.type != TokenType.EOF) {
      validateStream(builder, currentToken.precedingComments);
      TokenType type = currentToken.type;
      if (identical(type, TokenType.OPEN_CURLY_BRACKET) || identical(type, TokenType.OPEN_PAREN) || identical(type, TokenType.OPEN_SQUARE_BRACKET) || identical(type, TokenType.STRING_INTERPOLATION_EXPRESSION)) {
        if (currentToken is! BeginToken) {
          builder.append("\r\nExpected BeginToken, found ");
          builder.append(currentToken.runtimeType.toString());
          builder.append(" ");
          writeToken(builder, currentToken);
        }
      }
      int currentStart = currentToken.offset;
      int currentLength = currentToken.length;
      int currentEnd = currentStart + currentLength - 1;
      if (currentStart <= previousEnd) {
        builder.append("\r\nInvalid token sequence: ");
        writeToken(builder, previousToken);
        builder.append(" followed by ");
        writeToken(builder, currentToken);
      }
      previousEnd = currentEnd;
      previousToken = currentToken;
      currentToken = currentToken.next;
    }
  }
  void writeToken(JavaStringBuilder builder, Token token) {
    builder.append("[");
    builder.append(token.type);
    builder.append(", '");
    builder.append(token.lexeme);
    builder.append("', ");
    builder.append(token.offset);
    builder.append(", ");
    builder.append(token.length);
    builder.append("]");
  }
}
class ScannerTest extends JUnitTestCase {
  void test_ampersand() {
    assertToken(TokenType.AMPERSAND, "&");
  }
  void test_ampersand_ampersand() {
    assertToken(TokenType.AMPERSAND_AMPERSAND, "&&");
  }
  void test_ampersand_eq() {
    assertToken(TokenType.AMPERSAND_EQ, "&=");
  }
  void test_at() {
    assertToken(TokenType.AT, "@");
  }
  void test_backping() {
    assertToken(TokenType.BACKPING, "`");
  }
  void test_backslash() {
    assertToken(TokenType.BACKSLASH, "\\");
  }
  void test_bang() {
    assertToken(TokenType.BANG, "!");
  }
  void test_bang_eq() {
    assertToken(TokenType.BANG_EQ, "!=");
  }
  void test_bar() {
    assertToken(TokenType.BAR, "|");
  }
  void test_bar_bar() {
    assertToken(TokenType.BAR_BAR, "||");
  }
  void test_bar_eq() {
    assertToken(TokenType.BAR_EQ, "|=");
  }
  void test_caret() {
    assertToken(TokenType.CARET, "^");
  }
  void test_caret_eq() {
    assertToken(TokenType.CARET_EQ, "^=");
  }
  void test_close_curly_bracket() {
    assertToken(TokenType.CLOSE_CURLY_BRACKET, "}");
  }
  void test_close_paren() {
    assertToken(TokenType.CLOSE_PAREN, ")");
  }
  void test_close_quare_bracket() {
    assertToken(TokenType.CLOSE_SQUARE_BRACKET, "]");
  }
  void test_colon() {
    assertToken(TokenType.COLON, ":");
  }
  void test_comma() {
    assertToken(TokenType.COMMA, ",");
  }
  void test_comment_multi() {
    assertComment(TokenType.MULTI_LINE_COMMENT, "/* comment */");
  }
  void test_comment_multi_unterminated() {
    assertError(ScannerErrorCode.UNTERMINATED_MULTI_LINE_COMMENT, 3, "/* x");
  }
  void test_comment_nested() {
    assertComment(TokenType.MULTI_LINE_COMMENT, "/* comment /* within a */ comment */");
  }
  void test_comment_single() {
    assertComment(TokenType.SINGLE_LINE_COMMENT, "// comment");
  }
  void test_double_both_e() {
    assertToken(TokenType.DOUBLE, "0.123e4");
  }
  void test_double_both_E() {
    assertToken(TokenType.DOUBLE, "0.123E4");
  }
  void test_double_fraction() {
    assertToken(TokenType.DOUBLE, ".123");
  }
  void test_double_fraction_e() {
    assertToken(TokenType.DOUBLE, ".123e4");
  }
  void test_double_fraction_E() {
    assertToken(TokenType.DOUBLE, ".123E4");
  }
  void test_double_missingDigitInExponent() {
    assertError(ScannerErrorCode.MISSING_DIGIT, 1, "1e");
  }
  void test_double_whole_e() {
    assertToken(TokenType.DOUBLE, "12e4");
  }
  void test_double_whole_E() {
    assertToken(TokenType.DOUBLE, "12E4");
  }
  void test_eq() {
    assertToken(TokenType.EQ, "=");
  }
  void test_eq_eq() {
    assertToken(TokenType.EQ_EQ, "==");
  }
  void test_gt() {
    assertToken(TokenType.GT, ">");
  }
  void test_gt_eq() {
    assertToken(TokenType.GT_EQ, ">=");
  }
  void test_gt_gt() {
    assertToken(TokenType.GT_GT, ">>");
  }
  void test_gt_gt_eq() {
    assertToken(TokenType.GT_GT_EQ, ">>=");
  }
  void test_hash() {
    assertToken(TokenType.HASH, "#");
  }
  void test_hexidecimal() {
    assertToken(TokenType.HEXADECIMAL, "0x1A2B3C");
  }
  void test_hexidecimal_missingDigit() {
    assertError(ScannerErrorCode.MISSING_HEX_DIGIT, 1, "0x");
  }
  void test_identifier() {
    assertToken(TokenType.IDENTIFIER, "result");
  }
  void test_illegalChar_cyrillicLetter_middle() {
    assertError(ScannerErrorCode.ILLEGAL_CHARACTER, 0, "Shche\u0433lov");
  }
  void test_illegalChar_cyrillicLetter_start() {
    assertError(ScannerErrorCode.ILLEGAL_CHARACTER, 0, "\u0429");
  }
  void test_illegalChar_nbsp() {
    assertError(ScannerErrorCode.ILLEGAL_CHARACTER, 0, "\u00A0");
  }
  void test_illegalChar_notLetter() {
    assertError(ScannerErrorCode.ILLEGAL_CHARACTER, 0, "\u0312");
  }
  void test_index() {
    assertToken(TokenType.INDEX, "[]");
  }
  void test_index_eq() {
    assertToken(TokenType.INDEX_EQ, "[]=");
  }
  void test_int() {
    assertToken(TokenType.INT, "123");
  }
  void test_int_initialZero() {
    assertToken(TokenType.INT, "0123");
  }
  void test_keyword_abstract() {
    assertKeywordToken("abstract");
  }
  void test_keyword_as() {
    assertKeywordToken("as");
  }
  void test_keyword_assert() {
    assertKeywordToken("assert");
  }
  void test_keyword_break() {
    assertKeywordToken("break");
  }
  void test_keyword_case() {
    assertKeywordToken("case");
  }
  void test_keyword_catch() {
    assertKeywordToken("catch");
  }
  void test_keyword_class() {
    assertKeywordToken("class");
  }
  void test_keyword_const() {
    assertKeywordToken("const");
  }
  void test_keyword_continue() {
    assertKeywordToken("continue");
  }
  void test_keyword_default() {
    assertKeywordToken("default");
  }
  void test_keyword_do() {
    assertKeywordToken("do");
  }
  void test_keyword_dynamic() {
    assertKeywordToken("dynamic");
  }
  void test_keyword_else() {
    assertKeywordToken("else");
  }
  void test_keyword_enum() {
    assertKeywordToken("enum");
  }
  void test_keyword_export() {
    assertKeywordToken("export");
  }
  void test_keyword_extends() {
    assertKeywordToken("extends");
  }
  void test_keyword_factory() {
    assertKeywordToken("factory");
  }
  void test_keyword_false() {
    assertKeywordToken("false");
  }
  void test_keyword_final() {
    assertKeywordToken("final");
  }
  void test_keyword_finally() {
    assertKeywordToken("finally");
  }
  void test_keyword_for() {
    assertKeywordToken("for");
  }
  void test_keyword_get() {
    assertKeywordToken("get");
  }
  void test_keyword_if() {
    assertKeywordToken("if");
  }
  void test_keyword_implements() {
    assertKeywordToken("implements");
  }
  void test_keyword_import() {
    assertKeywordToken("import");
  }
  void test_keyword_in() {
    assertKeywordToken("in");
  }
  void test_keyword_is() {
    assertKeywordToken("is");
  }
  void test_keyword_library() {
    assertKeywordToken("library");
  }
  void test_keyword_new() {
    assertKeywordToken("new");
  }
  void test_keyword_null() {
    assertKeywordToken("null");
  }
  void test_keyword_operator() {
    assertKeywordToken("operator");
  }
  void test_keyword_part() {
    assertKeywordToken("part");
  }
  void test_keyword_rethrow() {
    assertKeywordToken("rethrow");
  }
  void test_keyword_return() {
    assertKeywordToken("return");
  }
  void test_keyword_set() {
    assertKeywordToken("set");
  }
  void test_keyword_static() {
    assertKeywordToken("static");
  }
  void test_keyword_super() {
    assertKeywordToken("super");
  }
  void test_keyword_switch() {
    assertKeywordToken("switch");
  }
  void test_keyword_this() {
    assertKeywordToken("this");
  }
  void test_keyword_throw() {
    assertKeywordToken("throw");
  }
  void test_keyword_true() {
    assertKeywordToken("true");
  }
  void test_keyword_try() {
    assertKeywordToken("try");
  }
  void test_keyword_typedef() {
    assertKeywordToken("typedef");
  }
  void test_keyword_var() {
    assertKeywordToken("var");
  }
  void test_keyword_void() {
    assertKeywordToken("void");
  }
  void test_keyword_while() {
    assertKeywordToken("while");
  }
  void test_keyword_with() {
    assertKeywordToken("with");
  }
  void test_lineInfo_multilineComment() {
    String source = "/*\r *\r */";
    assertLineInfo(source, [
        new ScannerTest_ExpectedLocation(0, 1, 1),
        new ScannerTest_ExpectedLocation(4, 2, 2),
        new ScannerTest_ExpectedLocation(source.length - 1, 3, 3)]);
  }
  void test_lineInfo_multilineString() {
    String source = "'''a\r\nbc\r\nd'''";
    assertLineInfo(source, [
        new ScannerTest_ExpectedLocation(0, 1, 1),
        new ScannerTest_ExpectedLocation(7, 2, 2),
        new ScannerTest_ExpectedLocation(source.length - 1, 3, 4)]);
  }
  void test_lineInfo_simpleClass() {
    String source = "class Test {\r\n    String s = '...';\r\n    int get x => s.MISSING_GETTER;\r\n}";
    assertLineInfo(source, [
        new ScannerTest_ExpectedLocation(0, 1, 1),
        new ScannerTest_ExpectedLocation(source.indexOf("MISSING_GETTER"), 3, 20),
        new ScannerTest_ExpectedLocation(source.length - 1, 4, 1)]);
  }
  void test_lineInfo_slashN() {
    String source = "class Test {\n}";
    assertLineInfo(source, [
        new ScannerTest_ExpectedLocation(0, 1, 1),
        new ScannerTest_ExpectedLocation(source.indexOf("}"), 2, 1)]);
  }
  void test_lt() {
    assertToken(TokenType.LT, "<");
  }
  void test_lt_eq() {
    assertToken(TokenType.LT_EQ, "<=");
  }
  void test_lt_lt() {
    assertToken(TokenType.LT_LT, "<<");
  }
  void test_lt_lt_eq() {
    assertToken(TokenType.LT_LT_EQ, "<<=");
  }
  void test_minus() {
    assertToken(TokenType.MINUS, "-");
  }
  void test_minus_eq() {
    assertToken(TokenType.MINUS_EQ, "-=");
  }
  void test_minus_minus() {
    assertToken(TokenType.MINUS_MINUS, "--");
  }
  void test_open_curly_bracket() {
    assertToken(TokenType.OPEN_CURLY_BRACKET, "{");
  }
  void test_open_paren() {
    assertToken(TokenType.OPEN_PAREN, "(");
  }
  void test_open_square_bracket() {
    assertToken(TokenType.OPEN_SQUARE_BRACKET, "[");
  }
  void test_openSquareBracket() {
    assertToken(TokenType.OPEN_SQUARE_BRACKET, "[");
  }
  void test_percent() {
    assertToken(TokenType.PERCENT, "%");
  }
  void test_percent_eq() {
    assertToken(TokenType.PERCENT_EQ, "%=");
  }
  void test_period() {
    assertToken(TokenType.PERIOD, ".");
  }
  void test_period_period() {
    assertToken(TokenType.PERIOD_PERIOD, "..");
  }
  void test_period_period_period() {
    assertToken(TokenType.PERIOD_PERIOD_PERIOD, "...");
  }
  void test_periodAfterNumberNotIncluded_identifier() {
    assertTokens("42.isEven()", [
        new StringToken(TokenType.INT, "42", 0),
        new Token(TokenType.PERIOD, 2),
        new StringToken(TokenType.IDENTIFIER, "isEven", 3),
        new Token(TokenType.OPEN_PAREN, 9),
        new Token(TokenType.CLOSE_PAREN, 10)]);
  }
  void test_periodAfterNumberNotIncluded_period() {
    assertTokens("42..isEven()", [
        new StringToken(TokenType.INT, "42", 0),
        new Token(TokenType.PERIOD_PERIOD, 2),
        new StringToken(TokenType.IDENTIFIER, "isEven", 4),
        new Token(TokenType.OPEN_PAREN, 10),
        new Token(TokenType.CLOSE_PAREN, 11)]);
  }
  void test_plus() {
    assertToken(TokenType.PLUS, "+");
  }
  void test_plus_eq() {
    assertToken(TokenType.PLUS_EQ, "+=");
  }
  void test_plus_plus() {
    assertToken(TokenType.PLUS_PLUS, "++");
  }
  void test_question() {
    assertToken(TokenType.QUESTION, "?");
  }
  void test_scriptTag_withArgs() {
    assertToken(TokenType.SCRIPT_TAG, "#!/bin/dart -debug");
  }
  void test_scriptTag_withoutSpace() {
    assertToken(TokenType.SCRIPT_TAG, "#!/bin/dart");
  }
  void test_scriptTag_withSpace() {
    assertToken(TokenType.SCRIPT_TAG, "#! /bin/dart");
  }
  void test_semicolon() {
    assertToken(TokenType.SEMICOLON, ";");
  }
  void test_setSourceStart() {
    int offsetDelta = 42;
    GatheringErrorListener listener = new GatheringErrorListener();
    Scanner scanner = new Scanner(null, new SubSequenceReader(new CharSequence("a"), offsetDelta), listener);
    scanner.setSourceStart(3, 9);
    scanner.tokenize();
    List<int> lineStarts = scanner.lineStarts;
    JUnitTestCase.assertNotNull(lineStarts);
    JUnitTestCase.assertEquals(3, lineStarts.length);
    JUnitTestCase.assertEquals(33, lineStarts[2]);
  }
  void test_slash() {
    assertToken(TokenType.SLASH, "/");
  }
  void test_slash_eq() {
    assertToken(TokenType.SLASH_EQ, "/=");
  }
  void test_star() {
    assertToken(TokenType.STAR, "*");
  }
  void test_star_eq() {
    assertToken(TokenType.STAR_EQ, "*=");
  }
  void test_startAndEnd() {
    Token token = scan("a");
    Token previous = token.previous;
    JUnitTestCase.assertEquals(token, previous.next);
    JUnitTestCase.assertEquals(previous, previous.previous);
    Token next = token.next;
    JUnitTestCase.assertEquals(next, next.next);
    JUnitTestCase.assertEquals(token, next.previous);
  }
  void test_string_multi_double() {
    assertToken(TokenType.STRING, "\"\"\"line1\nline2\"\"\"");
  }
  void test_string_multi_embeddedQuotes() {
    assertToken(TokenType.STRING, "\"\"\"line1\n\"\"\nline2\"\"\"");
  }
  void test_string_multi_embeddedQuotes_escapedChar() {
    assertToken(TokenType.STRING, "\"\"\"a\"\"\\tb\"\"\"");
  }
  void test_string_multi_interpolation_block() {
    assertTokens("\"Hello \${name}!\"", [
        new StringToken(TokenType.STRING, "\"Hello ", 0),
        new StringToken(TokenType.STRING_INTERPOLATION_EXPRESSION, "\${", 7),
        new StringToken(TokenType.IDENTIFIER, "name", 9),
        new Token(TokenType.CLOSE_CURLY_BRACKET, 13),
        new StringToken(TokenType.STRING, "!\"", 14)]);
  }
  void test_string_multi_interpolation_identifier() {
    assertTokens("\"Hello \$name!\"", [
        new StringToken(TokenType.STRING, "\"Hello ", 0),
        new StringToken(TokenType.STRING_INTERPOLATION_IDENTIFIER, "\$", 7),
        new StringToken(TokenType.IDENTIFIER, "name", 8),
        new StringToken(TokenType.STRING, "!\"", 12)]);
  }
  void test_string_multi_single() {
    assertToken(TokenType.STRING, "'''string'''");
  }
  void test_string_multi_slashEnter() {
    assertToken(TokenType.STRING, "'''\\\n'''");
  }
  void test_string_multi_unterminated() {
    assertError(ScannerErrorCode.UNTERMINATED_STRING_LITERAL, 8, "'''string");
  }
  void test_string_raw_multi_double() {
    assertToken(TokenType.STRING, "r\"\"\"line1\nline2\"\"\"");
  }
  void test_string_raw_multi_single() {
    assertToken(TokenType.STRING, "r'''string'''");
  }
  void test_string_raw_multi_unterminated() {
    assertError(ScannerErrorCode.UNTERMINATED_STRING_LITERAL, 9, "r'''string");
  }
  void test_string_raw_simple_double() {
    assertToken(TokenType.STRING, "r\"string\"");
  }
  void test_string_raw_simple_single() {
    assertToken(TokenType.STRING, "r'string'");
  }
  void test_string_raw_simple_unterminated_eof() {
    assertError(ScannerErrorCode.UNTERMINATED_STRING_LITERAL, 7, "r'string");
  }
  void test_string_raw_simple_unterminated_eol() {
    assertError(ScannerErrorCode.UNTERMINATED_STRING_LITERAL, 8, "r'string\n");
  }
  void test_string_simple_double() {
    assertToken(TokenType.STRING, "\"string\"");
  }
  void test_string_simple_escapedDollar() {
    assertToken(TokenType.STRING, "'a\\\$b'");
  }
  void test_string_simple_interpolation_adjacentIdentifiers() {
    assertTokens("'\$a\$b'", [
        new StringToken(TokenType.STRING, "'", 0),
        new StringToken(TokenType.STRING_INTERPOLATION_IDENTIFIER, "\$", 1),
        new StringToken(TokenType.IDENTIFIER, "a", 2),
        new StringToken(TokenType.STRING, "", 3),
        new StringToken(TokenType.STRING_INTERPOLATION_IDENTIFIER, "\$", 3),
        new StringToken(TokenType.IDENTIFIER, "b", 4),
        new StringToken(TokenType.STRING, "'", 5)]);
  }
  void test_string_simple_interpolation_block() {
    assertTokens("'Hello \${name}!'", [
        new StringToken(TokenType.STRING, "'Hello ", 0),
        new StringToken(TokenType.STRING_INTERPOLATION_EXPRESSION, "\${", 7),
        new StringToken(TokenType.IDENTIFIER, "name", 9),
        new Token(TokenType.CLOSE_CURLY_BRACKET, 13),
        new StringToken(TokenType.STRING, "!'", 14)]);
  }
  void test_string_simple_interpolation_blockWithNestedMap() {
    assertTokens("'a \${f({'b' : 'c'})} d'", [
        new StringToken(TokenType.STRING, "'a ", 0),
        new StringToken(TokenType.STRING_INTERPOLATION_EXPRESSION, "\${", 3),
        new StringToken(TokenType.IDENTIFIER, "f", 5),
        new Token(TokenType.OPEN_PAREN, 6),
        new Token(TokenType.OPEN_CURLY_BRACKET, 7),
        new StringToken(TokenType.STRING, "'b'", 8),
        new Token(TokenType.COLON, 12),
        new StringToken(TokenType.STRING, "'c'", 14),
        new Token(TokenType.CLOSE_CURLY_BRACKET, 17),
        new Token(TokenType.CLOSE_PAREN, 18),
        new Token(TokenType.CLOSE_CURLY_BRACKET, 19),
        new StringToken(TokenType.STRING, " d'", 20)]);
  }
  void test_string_simple_interpolation_firstAndLast() {
    assertTokens("'\$greeting \$name'", [
        new StringToken(TokenType.STRING, "'", 0),
        new StringToken(TokenType.STRING_INTERPOLATION_IDENTIFIER, "\$", 1),
        new StringToken(TokenType.IDENTIFIER, "greeting", 2),
        new StringToken(TokenType.STRING, " ", 10),
        new StringToken(TokenType.STRING_INTERPOLATION_IDENTIFIER, "\$", 11),
        new StringToken(TokenType.IDENTIFIER, "name", 12),
        new StringToken(TokenType.STRING, "'", 16)]);
  }
  void test_string_simple_interpolation_identifier() {
    assertTokens("'Hello \$name!'", [
        new StringToken(TokenType.STRING, "'Hello ", 0),
        new StringToken(TokenType.STRING_INTERPOLATION_IDENTIFIER, "\$", 7),
        new StringToken(TokenType.IDENTIFIER, "name", 8),
        new StringToken(TokenType.STRING, "!'", 12)]);
  }
  void test_string_simple_interpolation_missingIdentifier() {
    assertTokens("'\$x\$'", [
        new StringToken(TokenType.STRING, "'", 0),
        new StringToken(TokenType.STRING_INTERPOLATION_IDENTIFIER, "\$", 1),
        new StringToken(TokenType.IDENTIFIER, "x", 2),
        new StringToken(TokenType.STRING, "", 3),
        new StringToken(TokenType.STRING_INTERPOLATION_IDENTIFIER, "\$", 3),
        new StringToken(TokenType.STRING, "'", 4)]);
  }
  void test_string_simple_interpolation_nonIdentifier() {
    assertTokens("'\$1'", [
        new StringToken(TokenType.STRING, "'", 0),
        new StringToken(TokenType.STRING_INTERPOLATION_IDENTIFIER, "\$", 1),
        new StringToken(TokenType.STRING, "1'", 2)]);
  }
  void test_string_simple_single() {
    assertToken(TokenType.STRING, "'string'");
  }
  void test_string_simple_unterminated_eof() {
    assertError(ScannerErrorCode.UNTERMINATED_STRING_LITERAL, 6, "'string");
  }
  void test_string_simple_unterminated_eol() {
    assertError(ScannerErrorCode.UNTERMINATED_STRING_LITERAL, 7, "'string\r");
  }
  void test_tilde() {
    assertToken(TokenType.TILDE, "~");
  }
  void test_tilde_slash() {
    assertToken(TokenType.TILDE_SLASH, "~/");
  }
  void test_tilde_slash_eq() {
    assertToken(TokenType.TILDE_SLASH_EQ, "~/=");
  }
  void test_unclosedPairInInterpolation() {
    GatheringErrorListener listener = new GatheringErrorListener();
    scan2("'\${(}'", listener);
  }
  void assertComment(TokenType commentType, String source) {
    Token token = scan(source);
    JUnitTestCase.assertNotNull(token);
    JUnitTestCase.assertEquals(TokenType.EOF, token.type);
    Token comment = token.precedingComments;
    JUnitTestCase.assertNotNull(comment);
    JUnitTestCase.assertEquals(commentType, comment.type);
    JUnitTestCase.assertEquals(0, comment.offset);
    JUnitTestCase.assertEquals(source.length, comment.length);
    JUnitTestCase.assertEquals(source, comment.lexeme);
    token = scan("${source}\n");
    JUnitTestCase.assertNotNull(token);
    JUnitTestCase.assertEquals(TokenType.EOF, token.type);
    comment = token.precedingComments;
    JUnitTestCase.assertNotNull(comment);
    JUnitTestCase.assertEquals(commentType, comment.type);
    JUnitTestCase.assertEquals(0, comment.offset);
    JUnitTestCase.assertEquals(source.length, comment.length);
    JUnitTestCase.assertEquals(source, comment.lexeme);
  }

  /**
   * Assert that scanning the given source produces an error with the given code.
   *
   * @param illegalCharacter
   * @param i
   * @param source the source to be scanned to produce the error
   */
  void assertError(ScannerErrorCode expectedError, int expectedOffset, String source) {
    GatheringErrorListener listener = new GatheringErrorListener();
    scan2(source, listener);
    listener.assertErrors([new AnalysisError.con2(null, expectedOffset, 1, expectedError, [source.codeUnitAt(expectedOffset) as int])]);
  }

  /**
   * Assert that when scanned the given source contains a single keyword token with the same lexeme
   * as the original source.
   *
   * @param source the source to be scanned
   */
  void assertKeywordToken(String source) {
    Token token = scan(source);
    JUnitTestCase.assertNotNull(token);
    JUnitTestCase.assertEquals(TokenType.KEYWORD, token.type);
    JUnitTestCase.assertEquals(0, token.offset);
    JUnitTestCase.assertEquals(source.length, token.length);
    JUnitTestCase.assertEquals(source, token.lexeme);
    Object value = token.value();
    JUnitTestCase.assertTrue(value is Keyword);
    JUnitTestCase.assertEquals(source, ((value as Keyword)).syntax);
    token = scan(" ${source} ");
    JUnitTestCase.assertNotNull(token);
    JUnitTestCase.assertEquals(TokenType.KEYWORD, token.type);
    JUnitTestCase.assertEquals(1, token.offset);
    JUnitTestCase.assertEquals(source.length, token.length);
    JUnitTestCase.assertEquals(source, token.lexeme);
    value = token.value();
    JUnitTestCase.assertTrue(value is Keyword);
    JUnitTestCase.assertEquals(source, ((value as Keyword)).syntax);
    JUnitTestCase.assertEquals(TokenType.EOF, token.next.type);
  }
  void assertLineInfo(String source, List<ScannerTest_ExpectedLocation> expectedLocations) {
    GatheringErrorListener listener = new GatheringErrorListener();
    scan2(source, listener);
    listener.assertNoErrors();
    LineInfo info = listener.getLineInfo(new TestSource());
    JUnitTestCase.assertNotNull(info);
    for (ScannerTest_ExpectedLocation expectedLocation in expectedLocations) {
      LineInfo_Location location = info.getLocation(expectedLocation._offset);
      JUnitTestCase.assertEquals(expectedLocation._lineNumber, location.lineNumber);
      JUnitTestCase.assertEquals(expectedLocation._columnNumber, location.columnNumber);
    }
  }

  /**
   * Assert that the token scanned from the given source has the expected type.
   *
   * @param expectedType the expected type of the token
   * @param source the source to be scanned to produce the actual token
   */
  Token assertToken(TokenType expectedType, String source) {
    Token originalToken = scan(source);
    JUnitTestCase.assertNotNull(originalToken);
    JUnitTestCase.assertEquals(expectedType, originalToken.type);
    JUnitTestCase.assertEquals(0, originalToken.offset);
    JUnitTestCase.assertEquals(source.length, originalToken.length);
    JUnitTestCase.assertEquals(source, originalToken.lexeme);
    if (identical(expectedType, TokenType.SCRIPT_TAG)) {
      return originalToken;
    } else if (identical(expectedType, TokenType.SINGLE_LINE_COMMENT)) {
      Token tokenWithSpaces = scan(" ${source}");
      JUnitTestCase.assertNotNull(tokenWithSpaces);
      JUnitTestCase.assertEquals(expectedType, tokenWithSpaces.type);
      JUnitTestCase.assertEquals(1, tokenWithSpaces.offset);
      JUnitTestCase.assertEquals(source.length, tokenWithSpaces.length);
      JUnitTestCase.assertEquals(source, tokenWithSpaces.lexeme);
      return originalToken;
    } else if (identical(expectedType, TokenType.INT) || identical(expectedType, TokenType.DOUBLE)) {
      Token tokenWithLowerD = scan("${source}d");
      JUnitTestCase.assertNotNull(tokenWithLowerD);
      JUnitTestCase.assertEquals(expectedType, tokenWithLowerD.type);
      JUnitTestCase.assertEquals(0, tokenWithLowerD.offset);
      JUnitTestCase.assertEquals(source.length, tokenWithLowerD.length);
      JUnitTestCase.assertEquals(source, tokenWithLowerD.lexeme);
      Token tokenWithUpperD = scan("${source}D");
      JUnitTestCase.assertNotNull(tokenWithUpperD);
      JUnitTestCase.assertEquals(expectedType, tokenWithUpperD.type);
      JUnitTestCase.assertEquals(0, tokenWithUpperD.offset);
      JUnitTestCase.assertEquals(source.length, tokenWithUpperD.length);
      JUnitTestCase.assertEquals(source, tokenWithUpperD.lexeme);
    }
    Token tokenWithSpaces = scan(" ${source} ");
    JUnitTestCase.assertNotNull(tokenWithSpaces);
    JUnitTestCase.assertEquals(expectedType, tokenWithSpaces.type);
    JUnitTestCase.assertEquals(1, tokenWithSpaces.offset);
    JUnitTestCase.assertEquals(source.length, tokenWithSpaces.length);
    JUnitTestCase.assertEquals(source, tokenWithSpaces.lexeme);
    JUnitTestCase.assertEquals(TokenType.EOF, originalToken.next.type);
    return originalToken;
  }

  /**
   * Assert that when scanned the given source contains a sequence of tokens identical to the given
   * tokens.
   *
   * @param source the source to be scanned
   * @param expectedTokens the tokens that are expected to be in the source
   */
  void assertTokens(String source, List<Token> expectedTokens) {
    Token token = scan(source);
    JUnitTestCase.assertNotNull(token);
    for (int i = 0; i < expectedTokens.length; i++) {
      Token expectedToken = expectedTokens[i];
      JUnitTestCase.assertEqualsMsg("Wrong type for token ${i}", expectedToken.type, token.type);
      JUnitTestCase.assertEqualsMsg("Wrong offset for token ${i}", expectedToken.offset, token.offset);
      JUnitTestCase.assertEqualsMsg("Wrong length for token ${i}", expectedToken.length, token.length);
      JUnitTestCase.assertEqualsMsg("Wrong lexeme for token ${i}", expectedToken.lexeme, token.lexeme);
      token = token.next;
      JUnitTestCase.assertNotNull(token);
    }
    JUnitTestCase.assertEquals(TokenType.EOF, token.type);
  }
  Token scan(String source) {
    GatheringErrorListener listener = new GatheringErrorListener();
    Token token = scan2(source, listener);
    listener.assertNoErrors();
    return token;
  }
  Token scan2(String source, GatheringErrorListener listener) {
    Scanner scanner = new Scanner(null, new CharSequenceReader(new CharSequence(source)), listener);
    Token result = scanner.tokenize();
    listener.setLineInfo(new TestSource(), scanner.lineStarts);
    return result;
  }
  static dartSuite() {
    _ut.group('ScannerTest', () {
      _ut.test('test_ampersand', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_ampersand);
      });
      _ut.test('test_ampersand_ampersand', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_ampersand_ampersand);
      });
      _ut.test('test_ampersand_eq', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_ampersand_eq);
      });
      _ut.test('test_at', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_at);
      });
      _ut.test('test_backping', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_backping);
      });
      _ut.test('test_backslash', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_backslash);
      });
      _ut.test('test_bang', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_bang);
      });
      _ut.test('test_bang_eq', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_bang_eq);
      });
      _ut.test('test_bar', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_bar);
      });
      _ut.test('test_bar_bar', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_bar_bar);
      });
      _ut.test('test_bar_eq', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_bar_eq);
      });
      _ut.test('test_caret', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_caret);
      });
      _ut.test('test_caret_eq', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_caret_eq);
      });
      _ut.test('test_close_curly_bracket', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_close_curly_bracket);
      });
      _ut.test('test_close_paren', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_close_paren);
      });
      _ut.test('test_close_quare_bracket', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_close_quare_bracket);
      });
      _ut.test('test_colon', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_colon);
      });
      _ut.test('test_comma', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_comma);
      });
      _ut.test('test_comment_multi', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_comment_multi);
      });
      _ut.test('test_comment_multi_unterminated', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_comment_multi_unterminated);
      });
      _ut.test('test_comment_nested', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_comment_nested);
      });
      _ut.test('test_comment_single', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_comment_single);
      });
      _ut.test('test_double_both_E', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_double_both_E);
      });
      _ut.test('test_double_both_e', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_double_both_e);
      });
      _ut.test('test_double_fraction', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_double_fraction);
      });
      _ut.test('test_double_fraction_E', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_double_fraction_E);
      });
      _ut.test('test_double_fraction_e', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_double_fraction_e);
      });
      _ut.test('test_double_missingDigitInExponent', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_double_missingDigitInExponent);
      });
      _ut.test('test_double_whole_E', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_double_whole_E);
      });
      _ut.test('test_double_whole_e', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_double_whole_e);
      });
      _ut.test('test_eq', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_eq);
      });
      _ut.test('test_eq_eq', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_eq_eq);
      });
      _ut.test('test_gt', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_gt);
      });
      _ut.test('test_gt_eq', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_gt_eq);
      });
      _ut.test('test_gt_gt', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_gt_gt);
      });
      _ut.test('test_gt_gt_eq', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_gt_gt_eq);
      });
      _ut.test('test_hash', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_hash);
      });
      _ut.test('test_hexidecimal', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_hexidecimal);
      });
      _ut.test('test_hexidecimal_missingDigit', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_hexidecimal_missingDigit);
      });
      _ut.test('test_identifier', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_identifier);
      });
      _ut.test('test_illegalChar_cyrillicLetter_middle', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_illegalChar_cyrillicLetter_middle);
      });
      _ut.test('test_illegalChar_cyrillicLetter_start', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_illegalChar_cyrillicLetter_start);
      });
      _ut.test('test_illegalChar_nbsp', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_illegalChar_nbsp);
      });
      _ut.test('test_illegalChar_notLetter', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_illegalChar_notLetter);
      });
      _ut.test('test_index', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_index);
      });
      _ut.test('test_index_eq', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_index_eq);
      });
      _ut.test('test_int', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_int);
      });
      _ut.test('test_int_initialZero', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_int_initialZero);
      });
      _ut.test('test_keyword_abstract', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_abstract);
      });
      _ut.test('test_keyword_as', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_as);
      });
      _ut.test('test_keyword_assert', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_assert);
      });
      _ut.test('test_keyword_break', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_break);
      });
      _ut.test('test_keyword_case', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_case);
      });
      _ut.test('test_keyword_catch', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_catch);
      });
      _ut.test('test_keyword_class', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_class);
      });
      _ut.test('test_keyword_const', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_const);
      });
      _ut.test('test_keyword_continue', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_continue);
      });
      _ut.test('test_keyword_default', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_default);
      });
      _ut.test('test_keyword_do', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_do);
      });
      _ut.test('test_keyword_dynamic', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_dynamic);
      });
      _ut.test('test_keyword_else', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_else);
      });
      _ut.test('test_keyword_enum', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_enum);
      });
      _ut.test('test_keyword_export', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_export);
      });
      _ut.test('test_keyword_extends', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_extends);
      });
      _ut.test('test_keyword_factory', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_factory);
      });
      _ut.test('test_keyword_false', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_false);
      });
      _ut.test('test_keyword_final', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_final);
      });
      _ut.test('test_keyword_finally', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_finally);
      });
      _ut.test('test_keyword_for', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_for);
      });
      _ut.test('test_keyword_get', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_get);
      });
      _ut.test('test_keyword_if', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_if);
      });
      _ut.test('test_keyword_implements', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_implements);
      });
      _ut.test('test_keyword_import', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_import);
      });
      _ut.test('test_keyword_in', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_in);
      });
      _ut.test('test_keyword_is', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_is);
      });
      _ut.test('test_keyword_library', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_library);
      });
      _ut.test('test_keyword_new', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_new);
      });
      _ut.test('test_keyword_null', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_null);
      });
      _ut.test('test_keyword_operator', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_operator);
      });
      _ut.test('test_keyword_part', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_part);
      });
      _ut.test('test_keyword_rethrow', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_rethrow);
      });
      _ut.test('test_keyword_return', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_return);
      });
      _ut.test('test_keyword_set', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_set);
      });
      _ut.test('test_keyword_static', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_static);
      });
      _ut.test('test_keyword_super', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_super);
      });
      _ut.test('test_keyword_switch', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_switch);
      });
      _ut.test('test_keyword_this', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_this);
      });
      _ut.test('test_keyword_throw', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_throw);
      });
      _ut.test('test_keyword_true', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_true);
      });
      _ut.test('test_keyword_try', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_try);
      });
      _ut.test('test_keyword_typedef', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_typedef);
      });
      _ut.test('test_keyword_var', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_var);
      });
      _ut.test('test_keyword_void', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_void);
      });
      _ut.test('test_keyword_while', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_while);
      });
      _ut.test('test_keyword_with', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_keyword_with);
      });
      _ut.test('test_lineInfo_multilineComment', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_lineInfo_multilineComment);
      });
      _ut.test('test_lineInfo_multilineString', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_lineInfo_multilineString);
      });
      _ut.test('test_lineInfo_simpleClass', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_lineInfo_simpleClass);
      });
      _ut.test('test_lineInfo_slashN', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_lineInfo_slashN);
      });
      _ut.test('test_lt', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_lt);
      });
      _ut.test('test_lt_eq', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_lt_eq);
      });
      _ut.test('test_lt_lt', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_lt_lt);
      });
      _ut.test('test_lt_lt_eq', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_lt_lt_eq);
      });
      _ut.test('test_minus', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_minus);
      });
      _ut.test('test_minus_eq', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_minus_eq);
      });
      _ut.test('test_minus_minus', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_minus_minus);
      });
      _ut.test('test_openSquareBracket', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_openSquareBracket);
      });
      _ut.test('test_open_curly_bracket', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_open_curly_bracket);
      });
      _ut.test('test_open_paren', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_open_paren);
      });
      _ut.test('test_open_square_bracket', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_open_square_bracket);
      });
      _ut.test('test_percent', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_percent);
      });
      _ut.test('test_percent_eq', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_percent_eq);
      });
      _ut.test('test_period', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_period);
      });
      _ut.test('test_periodAfterNumberNotIncluded_identifier', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_periodAfterNumberNotIncluded_identifier);
      });
      _ut.test('test_periodAfterNumberNotIncluded_period', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_periodAfterNumberNotIncluded_period);
      });
      _ut.test('test_period_period', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_period_period);
      });
      _ut.test('test_period_period_period', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_period_period_period);
      });
      _ut.test('test_plus', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_plus);
      });
      _ut.test('test_plus_eq', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_plus_eq);
      });
      _ut.test('test_plus_plus', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_plus_plus);
      });
      _ut.test('test_question', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_question);
      });
      _ut.test('test_scriptTag_withArgs', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_scriptTag_withArgs);
      });
      _ut.test('test_scriptTag_withSpace', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_scriptTag_withSpace);
      });
      _ut.test('test_scriptTag_withoutSpace', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_scriptTag_withoutSpace);
      });
      _ut.test('test_semicolon', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_semicolon);
      });
      _ut.test('test_setSourceStart', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_setSourceStart);
      });
      _ut.test('test_slash', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_slash);
      });
      _ut.test('test_slash_eq', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_slash_eq);
      });
      _ut.test('test_star', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_star);
      });
      _ut.test('test_star_eq', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_star_eq);
      });
      _ut.test('test_startAndEnd', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_startAndEnd);
      });
      _ut.test('test_string_multi_double', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_string_multi_double);
      });
      _ut.test('test_string_multi_embeddedQuotes', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_string_multi_embeddedQuotes);
      });
      _ut.test('test_string_multi_embeddedQuotes_escapedChar', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_string_multi_embeddedQuotes_escapedChar);
      });
      _ut.test('test_string_multi_interpolation_block', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_string_multi_interpolation_block);
      });
      _ut.test('test_string_multi_interpolation_identifier', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_string_multi_interpolation_identifier);
      });
      _ut.test('test_string_multi_single', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_string_multi_single);
      });
      _ut.test('test_string_multi_slashEnter', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_string_multi_slashEnter);
      });
      _ut.test('test_string_multi_unterminated', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_string_multi_unterminated);
      });
      _ut.test('test_string_raw_multi_double', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_string_raw_multi_double);
      });
      _ut.test('test_string_raw_multi_single', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_string_raw_multi_single);
      });
      _ut.test('test_string_raw_multi_unterminated', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_string_raw_multi_unterminated);
      });
      _ut.test('test_string_raw_simple_double', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_string_raw_simple_double);
      });
      _ut.test('test_string_raw_simple_single', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_string_raw_simple_single);
      });
      _ut.test('test_string_raw_simple_unterminated_eof', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_string_raw_simple_unterminated_eof);
      });
      _ut.test('test_string_raw_simple_unterminated_eol', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_string_raw_simple_unterminated_eol);
      });
      _ut.test('test_string_simple_double', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_string_simple_double);
      });
      _ut.test('test_string_simple_escapedDollar', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_string_simple_escapedDollar);
      });
      _ut.test('test_string_simple_interpolation_adjacentIdentifiers', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_string_simple_interpolation_adjacentIdentifiers);
      });
      _ut.test('test_string_simple_interpolation_block', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_string_simple_interpolation_block);
      });
      _ut.test('test_string_simple_interpolation_blockWithNestedMap', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_string_simple_interpolation_blockWithNestedMap);
      });
      _ut.test('test_string_simple_interpolation_firstAndLast', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_string_simple_interpolation_firstAndLast);
      });
      _ut.test('test_string_simple_interpolation_identifier', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_string_simple_interpolation_identifier);
      });
      _ut.test('test_string_simple_interpolation_missingIdentifier', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_string_simple_interpolation_missingIdentifier);
      });
      _ut.test('test_string_simple_interpolation_nonIdentifier', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_string_simple_interpolation_nonIdentifier);
      });
      _ut.test('test_string_simple_single', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_string_simple_single);
      });
      _ut.test('test_string_simple_unterminated_eof', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_string_simple_unterminated_eof);
      });
      _ut.test('test_string_simple_unterminated_eol', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_string_simple_unterminated_eol);
      });
      _ut.test('test_tilde', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_tilde);
      });
      _ut.test('test_tilde_slash', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_tilde_slash);
      });
      _ut.test('test_tilde_slash_eq', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_tilde_slash_eq);
      });
      _ut.test('test_unclosedPairInInterpolation', () {
        final __test = new ScannerTest();
        runJUnitTest(__test, __test.test_unclosedPairInInterpolation);
      });
    });
  }
}
/**
 * Instances of the class `ExpectedLocation` encode information about the expected location
 * of a given offset in source code.
 */
class ScannerTest_ExpectedLocation {
  int _offset = 0;
  int _lineNumber = 0;
  int _columnNumber = 0;
  ScannerTest_ExpectedLocation(int offset, int lineNumber, int columnNumber) {
    this._offset = offset;
    this._lineNumber = lineNumber;
    this._columnNumber = columnNumber;
  }
}
class IncrementalScannerTest extends EngineTestCase {
  Token _originalTokens;
  void test_rescan_addedBeforeIdentifier1() {
    IncrementalScanner scanner = assertTokens("a + b;", "xa + b;");
    JUnitTestCase.assertEquals("xa", scanner.firstToken.lexeme);
    JUnitTestCase.assertEquals("xa", scanner.lastToken.lexeme);
  }
  void test_rescan_addedBeforeIdentifier2() {
    IncrementalScanner scanner = assertTokens("a + b;", "a + xb;");
    JUnitTestCase.assertEquals("xb", scanner.firstToken.lexeme);
    JUnitTestCase.assertEquals("xb", scanner.lastToken.lexeme);
  }
  void test_rescan_addedNewIdentifier1() {
    IncrementalScanner scanner = assertTokens("a;  c;", "a; b c;");
    JUnitTestCase.assertEquals("b", scanner.firstToken.lexeme);
    JUnitTestCase.assertEquals("b", scanner.lastToken.lexeme);
  }
  void test_rescan_addedNewIdentifier2() {
    IncrementalScanner scanner = assertTokens("a;  c;", "a;b  c;");
    JUnitTestCase.assertEquals("b", scanner.firstToken.lexeme);
    JUnitTestCase.assertEquals("b", scanner.lastToken.lexeme);
    Token oldToken = _originalTokens.next;
    JUnitTestCase.assertSame(TokenType.SEMICOLON, oldToken.type);
    Token newToken = scanner.tokenMap.get(oldToken);
    JUnitTestCase.assertNotNull(newToken);
    JUnitTestCase.assertEquals(TokenType.SEMICOLON, newToken.type);
    JUnitTestCase.assertNotSame(oldToken, newToken);
  }
  void test_rescan_addedToIdentifier1() {
    IncrementalScanner scanner = assertTokens("a + b;", "abs + b;");
    JUnitTestCase.assertEquals("abs", scanner.firstToken.lexeme);
    JUnitTestCase.assertEquals("abs", scanner.lastToken.lexeme);
    Token oldToken = _originalTokens.next;
    JUnitTestCase.assertEquals(TokenType.PLUS, oldToken.type);
    Token newToken = scanner.tokenMap.get(oldToken);
    JUnitTestCase.assertNotNull(newToken);
    JUnitTestCase.assertEquals(TokenType.PLUS, newToken.type);
    JUnitTestCase.assertNotSame(oldToken, newToken);
  }
  void test_rescan_addedToIdentifier2() {
    IncrementalScanner scanner = assertTokens("a + b;", "a + by;");
    JUnitTestCase.assertEquals("by", scanner.firstToken.lexeme);
    JUnitTestCase.assertEquals("by", scanner.lastToken.lexeme);
  }
  void test_rescan_appendWhitespace1() {
    IncrementalScanner scanner = assertTokens("a + b;", "a + b; ");
    JUnitTestCase.assertNull(scanner.firstToken);
    JUnitTestCase.assertNull(scanner.lastToken);
  }
  void test_rescan_appendWhitespace2() {
    IncrementalScanner scanner = assertTokens("a + b; ", "a + b;  ");
    JUnitTestCase.assertNull(scanner.firstToken);
    JUnitTestCase.assertNull(scanner.lastToken);
  }
  void test_rescan_insertedPeriod() {
    IncrementalScanner scanner = assertTokens("a + b;", "a + b.;");
    JUnitTestCase.assertEquals(".", scanner.firstToken.lexeme);
    JUnitTestCase.assertEquals(".", scanner.lastToken.lexeme);
  }
  void test_rescan_insertedPeriodBetweenIdentifiers1() {
    IncrementalScanner scanner = assertTokens("a b;", "a. b;");
    JUnitTestCase.assertEquals(".", scanner.firstToken.lexeme);
    JUnitTestCase.assertEquals(".", scanner.lastToken.lexeme);
  }
  void test_rescan_insertedPeriodBetweenIdentifiers2() {
    IncrementalScanner scanner = assertTokens("a b;", "a .b;");
    JUnitTestCase.assertEquals(".", scanner.firstToken.lexeme);
    JUnitTestCase.assertEquals(".", scanner.lastToken.lexeme);
  }
  void test_rescan_insertedPeriodBetweenIdentifiers3() {
    IncrementalScanner scanner = assertTokens("a  b;", "a . b;");
    JUnitTestCase.assertEquals(".", scanner.firstToken.lexeme);
    JUnitTestCase.assertEquals(".", scanner.lastToken.lexeme);
  }
  void test_rescan_insertedPeriodIdentifier() {
    IncrementalScanner scanner = assertTokens("a + b;", "a + b.x;");
    JUnitTestCase.assertEquals(".", scanner.firstToken.lexeme);
    JUnitTestCase.assertEquals("x", scanner.lastToken.lexeme);
  }
  void test_rescan_insertedPeriodInsideExistingIdentifier() {
    IncrementalScanner scanner = assertTokens("ab;", "a.b;");
    JUnitTestCase.assertEquals("a", scanner.firstToken.lexeme);
    JUnitTestCase.assertEquals("b", scanner.lastToken.lexeme);
  }
  void test_rescan_insertLeadingWhitespace() {
    IncrementalScanner scanner = assertTokens("a + b;", " a + b;");
    JUnitTestCase.assertNull(scanner.firstToken);
    JUnitTestCase.assertNull(scanner.lastToken);
  }
  void test_rescan_insertWhitespace() {
    IncrementalScanner scanner = assertTokens("a + b;", "a  + b;");
    JUnitTestCase.assertNull(scanner.firstToken);
    JUnitTestCase.assertNull(scanner.lastToken);
  }
  void test_rescan_insertWhitespaceWithMultipleComments() {
    IncrementalScanner scanner = assertTokens(EngineTestCase.createSource(["//comment", "//comment2", "a + b;"]), EngineTestCase.createSource(["//comment", "//comment2", "a  + b;"]));
    JUnitTestCase.assertNull(scanner.firstToken);
    JUnitTestCase.assertNull(scanner.lastToken);
  }
  void test_rescan_oneFunctionToTwo() {
    IncrementalScanner scanner = assertTokens("f() {}", "f() => 0; g() {}");
    JUnitTestCase.assertEquals("=>", scanner.firstToken.lexeme);
    JUnitTestCase.assertEquals(")", scanner.lastToken.lexeme);
  }
  void test_tokenMap() {
    IncrementalScanner scanner = assertTokens("main() {a + b;}", "main() { a + b;}");
    TokenMap tokenMap = scanner.tokenMap;
    Token oldToken = _originalTokens;
    while (oldToken.type != TokenType.EOF) {
      Token newToken = tokenMap.get(oldToken);
      JUnitTestCase.assertNotSame(oldToken, newToken);
      JUnitTestCase.assertSame(oldToken.type, newToken.type);
      JUnitTestCase.assertEquals(oldToken.lexeme, newToken.lexeme);
      oldToken = oldToken.next;
    }
  }
  IncrementalScanner assertTokens(String originalContents, String modifiedContents) {
    int originalLength = originalContents.length;
    int modifiedLength = modifiedContents.length;
    int replaceStart = 0;
    while (replaceStart < originalLength && replaceStart < modifiedLength && originalContents.codeUnitAt(replaceStart) == modifiedContents.codeUnitAt(replaceStart)) {
      replaceStart++;
    }
    int originalEnd = originalLength - 1;
    int modifiedEnd = modifiedLength - 1;
    while (originalEnd >= replaceStart && modifiedEnd >= replaceStart && originalContents.codeUnitAt(originalEnd) == modifiedContents.codeUnitAt(modifiedEnd)) {
      originalEnd--;
      modifiedEnd--;
    }
    Source source = new TestSource();
    GatheringErrorListener originalListener = new GatheringErrorListener();
    Scanner originalScanner = new Scanner(source, new CharSequenceReader(new CharSequence(originalContents)), originalListener);
    _originalTokens = originalScanner.tokenize();
    JUnitTestCase.assertNotNull(_originalTokens);
    GatheringErrorListener modifiedListener = new GatheringErrorListener();
    Scanner modifiedScanner = new Scanner(source, new CharSequenceReader(new CharSequence(modifiedContents)), modifiedListener);
    Token modifiedTokens = modifiedScanner.tokenize();
    JUnitTestCase.assertNotNull(modifiedTokens);
    GatheringErrorListener incrementalListener = new GatheringErrorListener();
    IncrementalScanner incrementalScanner = new IncrementalScanner(source, new CharSequenceReader(new CharSequence(modifiedContents)), incrementalListener);
    Token incrementalTokens = incrementalScanner.rescan(_originalTokens, replaceStart, originalEnd - replaceStart + 1, modifiedEnd - replaceStart + 1);
    Token incrementalToken = incrementalTokens;
    JUnitTestCase.assertNotNull(incrementalToken);
    while (incrementalToken.type != TokenType.EOF && modifiedTokens.type != TokenType.EOF) {
      JUnitTestCase.assertSameMsg("Wrong type for token", modifiedTokens.type, incrementalToken.type);
      JUnitTestCase.assertEqualsMsg("Wrong offset for token", modifiedTokens.offset, incrementalToken.offset);
      JUnitTestCase.assertEqualsMsg("Wrong length for token", modifiedTokens.length, incrementalToken.length);
      JUnitTestCase.assertEqualsMsg("Wrong lexeme for token", modifiedTokens.lexeme, incrementalToken.lexeme);
      incrementalToken = incrementalToken.next;
      modifiedTokens = modifiedTokens.next;
    }
    JUnitTestCase.assertSameMsg("Too many tokens", TokenType.EOF, incrementalToken.type);
    JUnitTestCase.assertSameMsg("Not enough tokens", TokenType.EOF, modifiedTokens.type);
    return incrementalScanner;
  }
  static dartSuite() {
    _ut.group('IncrementalScannerTest', () {
      _ut.test('test_rescan_addedBeforeIdentifier1', () {
        final __test = new IncrementalScannerTest();
        runJUnitTest(__test, __test.test_rescan_addedBeforeIdentifier1);
      });
      _ut.test('test_rescan_addedBeforeIdentifier2', () {
        final __test = new IncrementalScannerTest();
        runJUnitTest(__test, __test.test_rescan_addedBeforeIdentifier2);
      });
      _ut.test('test_rescan_addedNewIdentifier1', () {
        final __test = new IncrementalScannerTest();
        runJUnitTest(__test, __test.test_rescan_addedNewIdentifier1);
      });
      _ut.test('test_rescan_addedNewIdentifier2', () {
        final __test = new IncrementalScannerTest();
        runJUnitTest(__test, __test.test_rescan_addedNewIdentifier2);
      });
      _ut.test('test_rescan_addedToIdentifier1', () {
        final __test = new IncrementalScannerTest();
        runJUnitTest(__test, __test.test_rescan_addedToIdentifier1);
      });
      _ut.test('test_rescan_addedToIdentifier2', () {
        final __test = new IncrementalScannerTest();
        runJUnitTest(__test, __test.test_rescan_addedToIdentifier2);
      });
      _ut.test('test_rescan_appendWhitespace1', () {
        final __test = new IncrementalScannerTest();
        runJUnitTest(__test, __test.test_rescan_appendWhitespace1);
      });
      _ut.test('test_rescan_appendWhitespace2', () {
        final __test = new IncrementalScannerTest();
        runJUnitTest(__test, __test.test_rescan_appendWhitespace2);
      });
      _ut.test('test_rescan_insertLeadingWhitespace', () {
        final __test = new IncrementalScannerTest();
        runJUnitTest(__test, __test.test_rescan_insertLeadingWhitespace);
      });
      _ut.test('test_rescan_insertWhitespace', () {
        final __test = new IncrementalScannerTest();
        runJUnitTest(__test, __test.test_rescan_insertWhitespace);
      });
      _ut.test('test_rescan_insertWhitespaceWithMultipleComments', () {
        final __test = new IncrementalScannerTest();
        runJUnitTest(__test, __test.test_rescan_insertWhitespaceWithMultipleComments);
      });
      _ut.test('test_rescan_insertedPeriod', () {
        final __test = new IncrementalScannerTest();
        runJUnitTest(__test, __test.test_rescan_insertedPeriod);
      });
      _ut.test('test_rescan_insertedPeriodBetweenIdentifiers1', () {
        final __test = new IncrementalScannerTest();
        runJUnitTest(__test, __test.test_rescan_insertedPeriodBetweenIdentifiers1);
      });
      _ut.test('test_rescan_insertedPeriodBetweenIdentifiers2', () {
        final __test = new IncrementalScannerTest();
        runJUnitTest(__test, __test.test_rescan_insertedPeriodBetweenIdentifiers2);
      });
      _ut.test('test_rescan_insertedPeriodBetweenIdentifiers3', () {
        final __test = new IncrementalScannerTest();
        runJUnitTest(__test, __test.test_rescan_insertedPeriodBetweenIdentifiers3);
      });
      _ut.test('test_rescan_insertedPeriodIdentifier', () {
        final __test = new IncrementalScannerTest();
        runJUnitTest(__test, __test.test_rescan_insertedPeriodIdentifier);
      });
      _ut.test('test_rescan_insertedPeriodInsideExistingIdentifier', () {
        final __test = new IncrementalScannerTest();
        runJUnitTest(__test, __test.test_rescan_insertedPeriodInsideExistingIdentifier);
      });
      _ut.test('test_rescan_oneFunctionToTwo', () {
        final __test = new IncrementalScannerTest();
        runJUnitTest(__test, __test.test_rescan_oneFunctionToTwo);
      });
      _ut.test('test_tokenMap', () {
        final __test = new IncrementalScannerTest();
        runJUnitTest(__test, __test.test_tokenMap);
      });
    });
  }
}
main() {
  CharSequenceReaderTest.dartSuite();
  IncrementalScannerTest.dartSuite();
  KeywordStateTest.dartSuite();
  ScannerTest.dartSuite();
  TokenTypeTest.dartSuite();
}