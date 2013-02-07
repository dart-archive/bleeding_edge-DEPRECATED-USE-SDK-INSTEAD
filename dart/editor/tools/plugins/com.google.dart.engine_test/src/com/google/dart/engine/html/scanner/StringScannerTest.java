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

import junit.framework.TestCase;

public class StringScannerTest extends TestCase {

  public void test_tokenize_attribute() {
    tokenize("<html bob=\"one two\">", new String[] {"<", "html", "bob", "=", "\"one two\"", ">"});
  }

  public void test_tokenize_comment() {
    tokenize("<!-- foo -->", new String[] {"<!-- foo -->"});
  }

  public void test_tokenize_comment_incomplete() {
    tokenize("<!-- foo", new String[] {"<!-- foo"});
  }

  public void test_tokenize_comment_with_gt() {
    tokenize("<!-- foo > -> -->", new String[] {"<!-- foo > -> -->"});
  }

  public void test_tokenize_directive() {
    tokenize("<! foo >", new String[] {"<! foo >"});
  }

  public void test_tokenize_directive_incomplete() {
    tokenize("<? \nfoo", new String[] {"<? \nfoo"}, new int[] {0, 4});
  }

  public void test_tokenize_directive_xml() {
    tokenize(
        "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>",
        new String[] {"<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"});
  }

  public void test_tokenize_directives_incomplete_with_newline() {
    tokenize("<! \nfoo", new String[] {"<! \nfoo"}, new int[] {0, 4});
  }

  public void test_tokenize_empty() {
    tokenize("", new String[] {});
  }

  public void test_tokenize_lt() {
    tokenize("<", new String[] {"<"});
  }

  public void test_tokenize_script_partial() throws Exception {
    tokenize("<script> <p> ", new String[] {"<", "script", ">", " <p> "});
  }

  public void test_tokenize_script_ref() throws Exception {
    tokenize("<script source='some.dart'/> <p>", new String[] {
        "<", "script", "source", "=", "'some.dart'", "/>", " ", "<", "p", ">"});
  }

  public void test_tokenize_script_with_newline() throws Exception {
    tokenize("<script> <p>\n </script>", new String[] {
        "<", "script", ">", " <p>\n ", "</", "script", ">"}, new int[] {0, 13});
  }

  public void test_tokenize_spaces_and_newlines() {
    Token token = tokenize(
        " < html \n bob = 'joe\n' > <\np > one \r\n two <!-- \rfoo --> </ p > </ html > ",
        new String[] {
            " ", "<", "html", "bob", "=", "'joe\n'", ">", " ", "<", "p", ">", " one \r\n two ",
            "<!-- \rfoo -->", " ", "</", "p", ">", " ", "</", "html", ">", " "},
        new int[] {0, 9, 21, 27, 37, 48});
    token = token.getNext();
    assertEquals(1, token.getOffset());
    token = token.getNext();
    assertEquals(3, token.getOffset());
    token = token.getNext();
    assertEquals(10, token.getOffset());
  }

  public void test_tokenize_string() {
    tokenize("<p bob=\"foo\">", new String[] {"<", "p", "bob", "=", "\"foo\"", ">"});
  }

  public void test_tokenize_string_partial() {
    tokenize("<p bob=\"foo", new String[] {"<", "p", "bob", "=", "\"foo"});
  }

  public void test_tokenize_string_single_quote() {
    tokenize("<p bob='foo'>", new String[] {"<", "p", "bob", "=", "'foo'", ">"});
  }

  public void test_tokenize_string_single_quote_partial() {
    tokenize("<p bob='foo", new String[] {"<", "p", "bob", "=", "'foo"});
  }

  public void test_tokenize_tag_begin_end() {
    tokenize("<html></html>", new String[] {"<", "html", ">", "</", "html", ">"});
  }

  public void test_tokenize_tag_begin_only() {
    Token token = tokenize("<html>", new String[] {"<", "html", ">"});
    token = token.getNext();
    assertEquals(1, token.getOffset());
  }

  public void test_tokenize_tag_incomplete_with_special_characters() {
    tokenize("<br-a_b", new String[] {"<", "br-a_b"});
  }

  public void test_tokenize_tag_self_contained() {
    tokenize("<br/>", new String[] {"<", "br", "/>"});
  }

  public void test_tokenize_tags_wellformed() {
    tokenize("<html><p>one two</p></html>", new String[] {
        "<", "html", ">", "<", "p", ">", "one two", "</", "p", ">", "</", "html", ">"});
  }

  private Token tokenize(String input, String[] expectedTokens) {
    return tokenize(input, expectedTokens, new int[] {0});
  }

  private Token tokenize(String input, String[] expectedTokens, int[] expectedLineStarts) {
    StringScanner scanner = new StringScanner(null, input);
    scanner.setPassThroughElements(new String[] {"script", "</"});

    int count = 0;
    Token firstToken = scanner.tokenize();
    Token token = firstToken;

    Token previousToken = token.getPrevious();
    assertTrue(previousToken.isEof());
    assertSame(previousToken, previousToken.getPrevious());
    assertEquals(-1, previousToken.getOffset());
    assertSame(token, previousToken.getNext());

    assertEquals(0, token.getOffset());
    while (!token.isEof()) {
      if (count == expectedTokens.length) {
        fail("too many parsed tokens");
      }
      assertEquals("token " + count, expectedTokens[count], token.getLexeme());
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
