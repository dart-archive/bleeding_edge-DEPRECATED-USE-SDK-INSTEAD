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

package com.google.dart.tools.core.html;

import junit.framework.TestCase;

public class TokenizerTest extends TestCase {

  public void test_tokenize1() {
    tokenize("<html>", new String[] {"<", "html", ">"});
  }

  public void test_tokenize2() {
    tokenize("<html></html>", new String[] {"<", "html", ">", "</", "html", ">"});
  }

  public void test_tokenize3() {
    tokenize("<html><p>one two</p></html>", new String[] {
        "<", "html", ">", "<", "p", ">", "one two", "</", "p", ">", "</", "html", ">"});
  }

  public void test_tokenize4() {
    tokenize("<br/>", new String[] {"<", "br", "/>"});
  }

  public void test_tokenizeAttributes() {
    tokenize("<html bob=\"one two\">", new String[] {"<", "html", "bob", "=", "\"one two\"", ">"});
  }

  public void test_tokenizeComments() {
    tokenize("<!-- foo -->", new String[] {"<!-- foo -->"});
  }

  public void test_tokenizeComments2() {
    tokenize("<!-- foo", new String[] {"<!-- foo"});
  }

  public void test_tokenizeDirectives1() {
    tokenize("<! foo >", new String[] {"<! foo >"});
  }

  public void test_tokenizeDirectives2() {
    tokenize(
        "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>",
        new String[] {"<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"});
  }

  public void test_tokenizeDirectives3() {
    tokenize("<? foo", new String[] {"<? foo"});
  }

  public void test_tokenizeDirectives4() {
    tokenize("<! foo", new String[] {"<! foo"});
  }

  private void tokenize(String input, String[] expectedTokens) {
    Tokenizer t = new Tokenizer(input);

    int count = 0;

    while (t.hasNext()) {
      if (count == expectedTokens.length) {
        assertTrue("too many parsed tokens", false);
      }

      Token token = t.next();

      assertEquals("token " + count, expectedTokens[count], token.getValue());

      count++;
    }

    if (count != expectedTokens.length) {
      assertTrue("not enough parsed tokens", false);
    }
  }

}
