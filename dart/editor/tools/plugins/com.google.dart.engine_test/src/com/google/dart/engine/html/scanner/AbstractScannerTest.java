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
package com.google.dart.engine.html.scanner;

import static com.google.dart.engine.html.scanner.TokenType.COMMENT;
import static com.google.dart.engine.html.scanner.TokenType.DECLARATION;
import static com.google.dart.engine.html.scanner.TokenType.DIRECTIVE;
import static com.google.dart.engine.html.scanner.TokenType.EOF;
import static com.google.dart.engine.html.scanner.TokenType.EQ;
import static com.google.dart.engine.html.scanner.TokenType.GT;
import static com.google.dart.engine.html.scanner.TokenType.LT;
import static com.google.dart.engine.html.scanner.TokenType.LT_SLASH;
import static com.google.dart.engine.html.scanner.TokenType.SLASH_GT;
import static com.google.dart.engine.html.scanner.TokenType.STRING;
import static com.google.dart.engine.html.scanner.TokenType.TAG;
import static com.google.dart.engine.html.scanner.TokenType.TEXT;

import junit.framework.TestCase;

public abstract class AbstractScannerTest extends TestCase {

  public void test_tokenize_attribute() {
    tokenize("<html bob=\"one two\">", new Object[] {LT, "html", "bob", EQ, "\"one two\"", GT});
  }

  public void test_tokenize_comment() {
    tokenize("<!-- foo -->", new Object[] {"<!-- foo -->"});
  }

  public void test_tokenize_comment_incomplete() {
    tokenize("<!-- foo", new Object[] {"<!-- foo"});
  }

  public void test_tokenize_comment_with_gt() {
    tokenize("<!-- foo > -> -->", new Object[] {"<!-- foo > -> -->"});
  }

  public void test_tokenize_declaration() {
    tokenize("<! foo ><html>", new Object[] {"<! foo >", LT, "html", GT});
  }

  public void test_tokenize_declaration_malformed() {
    tokenize("<! foo /><html>", new Object[] {"<! foo />", LT, "html", GT});
  }

  public void test_tokenize_directive_incomplete() {
    tokenize("<? \nfoo", new Object[] {"<? \nfoo"}, new int[] {0, 4});
  }

  public void test_tokenize_directive_xml() {
    tokenize(
        "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>",
        new Object[] {"<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"});
  }

  public void test_tokenize_directives_incomplete_with_newline() {
    tokenize("<! \nfoo", new Object[] {"<! \nfoo"}, new int[] {0, 4});
  }

  public void test_tokenize_empty() {
    tokenize("", new Object[] {});
  }

  public void test_tokenize_lt() {
    tokenize("<", new Object[] {LT});
  }

  public void test_tokenize_script_partial() throws Exception {
    tokenize("<script> <p> ", new Object[] {LT, "script", GT, " <p> "});
  }

  public void test_tokenize_script_ref() throws Exception {
    tokenize("<script source='some.dart'/> <p>", new Object[] {
        LT, "script", "source", EQ, "'some.dart'", SLASH_GT, " ", LT, "p", GT});
  }

  public void test_tokenize_script_with_newline() throws Exception {
    tokenize("<script> <p>\n </script>", new Object[] {
        LT, "script", GT, " <p>\n ", LT_SLASH, "script", GT}, new int[] {0, 13});
  }

  public void test_tokenize_spaces_and_newlines() {
    Token token = tokenize(
        " < html \n bob = 'joe\n' >\n <\np > one \r\n two <!-- \rfoo --> </ p > </ html > ",
        new Object[] {
            " ", LT, "html", "bob", EQ, "'joe\n'", GT, "\n ", LT, "p", GT, " one \r\n two ",
            "<!-- \rfoo -->", " ", LT_SLASH, "p", GT, " ", LT_SLASH, "html", GT, " "},
        new int[] {0, 9, 21, 25, 28, 38, 49});
    token = token.getNext();
    assertEquals(1, token.getOffset());
    token = token.getNext();
    assertEquals(3, token.getOffset());
    token = token.getNext();
    assertEquals(10, token.getOffset());
  }

  public void test_tokenize_string() {
    tokenize("<p bob=\"foo\">", new Object[] {LT, "p", "bob", EQ, "\"foo\"", GT});
  }

  public void test_tokenize_string_partial() {
    tokenize("<p bob=\"foo", new Object[] {LT, "p", "bob", EQ, "\"foo"});
  }

  public void test_tokenize_string_single_quote() {
    tokenize("<p bob='foo'>", new Object[] {LT, "p", "bob", EQ, "'foo'", GT});
  }

  public void test_tokenize_string_single_quote_partial() {
    tokenize("<p bob='foo", new Object[] {LT, "p", "bob", EQ, "'foo"});
  }

  public void test_tokenize_tag_begin_end() {
    tokenize("<html></html>", new Object[] {LT, "html", GT, LT_SLASH, "html", GT});
  }

  public void test_tokenize_tag_begin_only() {
    Token token = tokenize("<html>", new Object[] {LT, "html", GT});
    token = token.getNext();
    assertEquals(1, token.getOffset());
  }

  public void test_tokenize_tag_incomplete_with_special_characters() {
    tokenize("<br-a_b", new Object[] {LT, "br-a_b"});
  }

  public void test_tokenize_tag_self_contained() {
    tokenize("<br/>", new Object[] {LT, "br", SLASH_GT});
  }

  public void test_tokenize_tags_wellformed() {
    tokenize("<html><p>one two</p></html>", new Object[] {
        LT, "html", GT, LT, "p", GT, "one two", LT_SLASH, "p", GT, LT_SLASH, "html", GT});
  }

  protected abstract AbstractScanner newScanner(String input);

  /**
   * Given an object representing an expected token, answer the expected token type.
   * 
   * @param count the token count for error reporting
   * @param expected the object representing an expected token
   * @return the expected token type
   */
  private TokenType getExpectedTokenType(int count, Object expected) {
    if (expected instanceof TokenType) {
      return (TokenType) expected;
    }
    if (expected instanceof String) {
      String lexeme = (String) expected;
      if (lexeme.startsWith("\"") || lexeme.startsWith("'")) {
        return STRING;
      }
      if (lexeme.startsWith("<!--")) {
        return COMMENT;
      }
      if (lexeme.startsWith("<!")) {
        return DECLARATION;
      }
      if (lexeme.startsWith("<?")) {
        return DIRECTIVE;
      }
      if (isTag(lexeme)) {
        return TAG;
      }
      return TEXT;
    }
    fail("Unknown expected token " + count + ": "
        + (expected != null ? expected.getClass() : "null"));
    return null;
  }

  private boolean isTag(String lexeme) {
    if (lexeme.length() == 0 || !Character.isLetter(lexeme.charAt(0))) {
      return false;
    }
    for (int index = 1; index < lexeme.length(); index++) {
      char ch = lexeme.charAt(index);
      if (!Character.isLetterOrDigit(ch) && ch != '-' && ch != '_') {
        return false;
      }
    }
    return true;
  }

  private Token tokenize(String input, Object[] expectedTokens) {
    return tokenize(input, expectedTokens, new int[] {0});
  }

  private Token tokenize(String input, Object[] expectedTokens, int[] expectedLineStarts) {
    AbstractScanner scanner = newScanner(input);
    scanner.setPassThroughElements(new String[] {"script", "</"});

    int count = 0;
    Token firstToken = scanner.tokenize();
    Token token = firstToken;

    Token previousToken = token.getPrevious();
    assertTrue(previousToken.getType() == EOF);
    assertSame(previousToken, previousToken.getPrevious());
    assertEquals(-1, previousToken.getOffset());
    assertSame(token, previousToken.getNext());

    assertEquals(0, token.getOffset());
    while (token.getType() != EOF) {
      if (count == expectedTokens.length) {
        fail("too many parsed tokens");
      }

      Object expected = expectedTokens[count];
      TokenType expectedTokenType = getExpectedTokenType(count, expected);
      assertSame("token " + count, expectedTokenType, token.getType());
      if (expectedTokenType.getLexeme() != null) {
        assertEquals("token " + count, expectedTokenType.getLexeme(), token.getLexeme());
      } else {
        assertEquals("token " + count, expected, token.getLexeme());
      }
      count++;

      previousToken = token;
      token = token.getNext();
      assertSame(previousToken, token.getPrevious());
    }
    assertSame(token, token.getNext());
    assertEquals(input.length(), token.getOffset());

    if (count != expectedTokens.length) {
      assertTrue("not enough parsed tokens", false);
    }

    int[] lineStarts = scanner.getLineStarts();
    boolean success = expectedLineStarts.length == lineStarts.length;
    if (success) {
      for (int i = 0; i < lineStarts.length; i++) {
        if (expectedLineStarts[i] != lineStarts[i]) {
          success = false;
          break;
        }
      }
    }
    if (!success) {
      StringBuilder msg = new StringBuilder();
      msg.append("Expected line starts ");
      for (int start : expectedLineStarts) {
        msg.append(start);
        msg.append(", ");
      }
      msg.append(" but found ");
      for (int start : lineStarts) {
        msg.append(start);
        msg.append(", ");
      }
      fail(msg.toString());
    }

    return firstToken;
  }
}
