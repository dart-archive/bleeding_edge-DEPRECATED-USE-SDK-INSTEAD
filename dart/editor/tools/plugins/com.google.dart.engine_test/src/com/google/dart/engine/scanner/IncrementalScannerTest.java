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
package com.google.dart.engine.scanner;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.TestSource;
import com.google.dart.engine.utilities.collection.TokenMap;

public class IncrementalScannerTest extends EngineTestCase {
  /**
   * The scanner used to perform incremental scanning, or {@code null} if {@link #scan} has not been
   * invoked.
   */
  private IncrementalScanner incrementalScanner;

  /**
   * The first token from the token stream resulting from parsing the original source, or
   * {@code null} if {@link #scan} has not been invoked.
   */
  private Token originalTokens;

  public void fail_delete_identifier_beginning() {
    // "abs + b;"
    // "s + b;")
    scan("", "ab", "", "s + b;");
    assertTokens("s");
  }

  public void fail_delete_mergeTokens() {
    // "a + b + c;"
    // "ac;")
    scan("a", " + b + ", "", "c;");
    assertTokens("ac");
  }

  public void fail_replace_multiple_partialFirstAndLast() {
    // "aa + bb;"
    // "ab * ab;")
    scan("a", "a + b", "b * a", "b;");
    assertTokens("ab", "*", "ab");
  }

  public void test_delete_identifier_end() {
    // "abs + b;"
    // "a + b;")
    scan("a", "bs", "", " + b;");
    assertTokens("a");
  }

  public void test_delete_identifier_middle() {
    // "abs + b;"
    // "as + b;")
    scan("a", "b", "", "s + b;");
    assertTokens("as");
  }

  public void test_insert_afterIdentifier1() {
    // "a + b;"
    // "abs + b;"
    scan("a", "", "bs", " + b;");
    assertTokens("abs");
    assertReplaced(1, "+");
  }

  public void test_insert_afterIdentifier2() {
    // "a + b;"
    // "a + by;"
    scan("a + b", "", "y", ";");
    assertTokens("by");
  }

  public void test_insert_beforeIdentifier() {
    // "a + b;"
    // "a + xb;")
    scan("a + ", "", "x", "b;");
    assertTokens("xb");
  }

  public void test_insert_beforeIdentifier_firstToken() {
    // "a + b;"
    // "xa + b;"
    scan("", "", "x", "a + b;");
    assertTokens("xa");
  }

  public void test_insert_convertOneFunctionToTwo() {
    // "f() {}"
    // "f() => 0; g() {}"
    scan("f()", "", " => 0; g()", " {}");
    assertTokens("=>", "0", ";", "g", "(", ")");
  }

  public void test_insert_newIdentifier1() {
    // "a;  c;"
    // "a; b c;"
    scan("a; ", "", "b", " c;");
    assertTokens("b");
  }

  public void test_insert_newIdentifier2() {
    // "a;  c;"
    // "a;b  c;"
    scan("a;", "", "b", "  c;");
    assertTokens("b");
    assertReplaced(1, ";");
  }

  public void test_insert_period() {
    // "a + b;"
    // "a + b.;"
    scan("a + b", "", ".", ";");
    assertTokens(".");
  }

  public void test_insert_period_betweenIdentifiers1() {
    // "a b;"
    // "a. b;"
    scan("a", "", ".", " b");
    assertTokens(".");
  }

  public void test_insert_period_betweenIdentifiers2() {
    // "a b;"
    // "a .b;"
    scan("a ", "", ".", "b;");
    assertTokens(".");
  }

  public void test_insert_period_betweenIdentifiers3() {
    // "a  b;"
    // "a . b;"
    scan("a ", "", ".", " b;");
    assertTokens(".");
  }

  public void test_insert_period_insideExistingIdentifier() {
    // "ab;"
    // "a.b;"
    scan("a", "", ".", "b;");
    assertTokens("a", ".", "b");
  }

  public void test_insert_periodAndIdentifier() {
    // "a + b;"
    // "a + b.x;"
    scan("a + b", "", ".x", ";");
    assertTokens(".", "x");
  }

  public void test_insert_whitespace_beginning_beforeToken() {
    // "a + b;"
    // " a + b;"
    scan("", "", " ", "a + b;");
    assertTokens();
  }

  public void test_insert_whitespace_betweenTokens() {
    // "a + b;"
    // "a  + b;"
    scan("a ", "", " ", "+ b;");
    assertTokens();
  }

  public void test_insert_whitespace_end_afterToken() {
    // "a + b;"
    // "a + b; "
    scan("a + b;", "", " ", "");
    assertTokens();
  }

  public void test_insert_whitespace_end_afterWhitespace() {
    // "a + b; "
    // "a + b;  "
    scan("a + b; ", "", " ", "");
    assertTokens();
  }

  public void test_insert_whitespace_withMultipleComments() {
    // "//comment", "//comment2", "a + b;"
    // "//comment", "//comment2", "a  + b;"
    scan(createSource(//
        "//comment",
        "//comment2",
        "a"), "", " ", " + b;");
    assertTokens();
  }

  public void test_replace_identifier_beginning() {
    // "bell + b;"
    // "fell + b;")
    scan("", "b", "f", "ell + b;");
    assertTokens("fell");
  }

  public void test_replace_identifier_end() {
    // "bell + b;"
    // "belt + b;")
    scan("bel", "l", "t", " + b;");
    assertTokens("belt");
  }

  public void test_replace_identifier_middle() {
    // "first + b;"
    // "frost + b;")
    scan("f", "ir", "ro", "st + b;");
    assertTokens("frost");
  }

  public void test_replace_operator_oneForMany() {
    // "a + b;"
    // "a * c - b;")
    scan("a ", "+", "* c -", " b;");
    assertTokens("*", "c", "-");
  }

  public void test_replace_operator_oneForOne() {
    // "a + b;"
    // "a * b;")
    scan("a ", "+", "*", " b;");
    assertTokens("*");
  }

  public void test_tokenMap() throws Exception {
    // "main() {a + b;}"
    // "main() { a + b;}"
    scan("main() {", "", " ", "a + b;}");
    TokenMap tokenMap = incrementalScanner.getTokenMap();
    Token oldToken = originalTokens;
    while (oldToken.getType() != TokenType.EOF) {
      Token newToken = tokenMap.get(oldToken);
      assertNotSame(oldToken, newToken);
      assertSame(oldToken.getType(), newToken.getType());
      assertEquals(oldToken.getLexeme(), newToken.getLexeme());
      oldToken = oldToken.getNext();
    }
  }

  private void assertReplaced(int tokenOffset, String lexeme) {
    Token oldToken = originalTokens;
    for (int i = 0; i < tokenOffset; i++) {
      oldToken = oldToken.getNext();
    }
    assertEquals(lexeme, oldToken.getLexeme());
    Token newToken = incrementalScanner.getTokenMap().get(oldToken);
    assertNotNull(newToken);
    assertEquals(lexeme, newToken.getLexeme());
    assertNotSame(oldToken, newToken);
  }

  private void assertTokens(String... lexemes) {
    Token first = incrementalScanner.getFirstToken();
    Token last = incrementalScanner.getLastToken();
    int count = lexemes.length;
    if (count == 0) {
      assertNull(first);
      assertNull(last);
    } else if (count == 1) {
      assertNotNull(first);
      assertEquals(lexemes[0], first.getLexeme());
      assertSame(first, last);
    } else {
      for (String lexeme : lexemes) {
        assertNotNull(first);
        assertEquals(lexeme, first.getLexeme());
        first = first.getNext();
      }
      assertSame(last.getNext(), first);
    }
  }

  /**
   * Given a description of the original and modified contents, perform an incremental scan of the
   * two pieces of text.
   * 
   * @param prefix the unchanged text before the edit region
   * @param removed the text that was removed from the original contents
   * @param added the text that was added to the modified contents
   * @param suffix the unchanged text after the edit region
   */
  private void scan(String prefix, String removed, String added, String suffix) {
    //
    // Compute the information needed to perform the test.
    //
    String originalContents = prefix + removed + suffix;
    String modifiedContents = prefix + added + suffix;
    int replaceStart = prefix.length();
    Source source = new TestSource();
    //
    // Scan the original contents.
    //
    GatheringErrorListener originalListener = new GatheringErrorListener();
    Scanner originalScanner = new Scanner(
        source,
        new CharSequenceReader(originalContents),
        originalListener);
    originalTokens = originalScanner.tokenize();
    assertNotNull(originalTokens);
    //
    // Scan the modified contents.
    //
    GatheringErrorListener modifiedListener = new GatheringErrorListener();
    Scanner modifiedScanner = new Scanner(
        source,
        new CharSequenceReader(modifiedContents),
        modifiedListener);
    Token modifiedTokens = modifiedScanner.tokenize();
    assertNotNull(modifiedTokens);
    //
    // Incrementally scan the modified contents.
    //
    GatheringErrorListener incrementalListener = new GatheringErrorListener();
    incrementalScanner = new IncrementalScanner(
        source,
        new CharSequenceReader(modifiedContents),
        incrementalListener);
    Token incrementalTokens = incrementalScanner.rescan(
        originalTokens,
        replaceStart,
        removed.length(),
        added.length());
    //
    // Validate that the results of the incremental scan are the same as the full scan of the
    // modified source.
    //
    Token incrementalToken = incrementalTokens;
    assertNotNull(incrementalToken);
    while (incrementalToken.getType() != TokenType.EOF && modifiedTokens.getType() != TokenType.EOF) {
      assertSame("Wrong type for token", modifiedTokens.getType(), incrementalToken.getType());
      assertEquals(
          "Wrong offset for token",
          modifiedTokens.getOffset(),
          incrementalToken.getOffset());
      assertEquals(
          "Wrong length for token",
          modifiedTokens.getLength(),
          incrementalToken.getLength());
      assertEquals(
          "Wrong lexeme for token",
          modifiedTokens.getLexeme(),
          incrementalToken.getLexeme());
      incrementalToken = incrementalToken.getNext();
      modifiedTokens = modifiedTokens.getNext();
    }
    assertSame("Too many tokens", TokenType.EOF, incrementalToken.getType());
    assertSame("Not enough tokens", TokenType.EOF, modifiedTokens.getType());
    // TODO(brianwilkerson) Verify that the errors are correct?
  }
}
