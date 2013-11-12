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
   * The first token from the token stream resulting from parsing the original source, or
   * {@code null} if {@link #scan} has not been invoked.
   */
  private Token originalTokens;

  /**
   * The scanner used to perform incremental scanning, or {@code null} if {@link #scan} has not been
   * invoked.
   */
  private IncrementalScanner incrementalScanner;

  /**
   * The first token from the token stream resulting from performing an incremental scan, or
   * {@code null} if {@link #scan} has not been invoked.
   */
  private Token incrementalTokens;

  public void fail_delete_identifier_beginning() {
    // "abs + b;"
    // "s + b;")
    scan("", "ab", "", "s + b;");
    assertTokens(0, 0, "s", "+", "b", ";");
  }

  public void fail_delete_mergeTokens() {
    // "a + b + c;"
    // "ac;")
    scan("a", " + b + ", "", "c;");
    assertTokens(0, 0, "ac", ";");
  }

  public void fail_replace_multiple_partialFirstAndLast() {
    // "aa + bb;"
    // "ab * ab;")
    scan("a", "a + b", "b * a", "b;");
    assertTokens(0, 2, "ab", "*", "ab", ";");
  }

  public void test_delete_identifier_end() {
    // "abs + b;"
    // "a + b;")
    scan("a", "bs", "", " + b;");
    assertTokens(0, 0, "a", "+", "b", ";");
  }

  public void test_delete_identifier_middle() {
    // "abs + b;"
    // "as + b;")
    scan("a", "b", "", "s + b;");
    assertTokens(0, 0, "as", "+", "b", ";");
  }

  public void test_insert_afterIdentifier1() {
    // "a + b;"
    // "abs + b;"
    scan("a", "", "bs", " + b;");
    assertTokens(0, 0, "abs", "+", "b", ";");
    assertReplaced(1, "+");
  }

  public void test_insert_afterIdentifier2() {
    // "a + b;"
    // "a + by;"
    scan("a + b", "", "y", ";");
    assertTokens(2, 2, "a", "+", "by", ";");
  }

  public void test_insert_beforeIdentifier() {
    // "a + b;"
    // "a + xb;")
    scan("a + ", "", "x", "b;");
    assertTokens(2, 2, "a", "+", "xb", ";");
  }

  public void test_insert_beforeIdentifier_firstToken() {
    // "a + b;"
    // "xa + b;"
    scan("", "", "x", "a + b;");
    assertTokens(0, 0, "xa", "+", "b", ";");
  }

  public void test_insert_convertOneFunctionToTwo() {
    // "f() {}"
    // "f() => 0; g() {}"
    scan("f()", "", " => 0; g()", " {}");
    assertTokens(3, 8, "f", "(", ")", "=>", "0", ";", "g", "(", ")", "{", "}");
  }

  public void test_insert_newIdentifier1() {
    // "a;  c;"
    // "a; b c;"
    scan("a; ", "", "b", " c;");
    assertTokens(2, 2, "a", ";", "b", "c", ";");
  }

  public void test_insert_newIdentifier2() {
    // "a;  c;"
    // "a;b  c;"
    scan("a;", "", "b", "  c;");
    assertTokens(2, 2, "a", ";", "b", "c", ";");
    assertReplaced(1, ";");
  }

  public void test_insert_period() {
    // "a + b;"
    // "a + b.;"
    scan("a + b", "", ".", ";");
    assertTokens(3, 3, "a", "+", "b", ".", ";");
  }

  public void test_insert_period_betweenIdentifiers1() {
    // "a b;"
    // "a. b;"
    scan("a", "", ".", " b;");
    assertTokens(1, 1, "a", ".", "b", ";");
  }

  public void test_insert_period_betweenIdentifiers2() {
    // "a b;"
    // "a .b;"
    scan("a ", "", ".", "b;");
    assertTokens(1, 1, "a", ".", "b", ";");
  }

  public void test_insert_period_betweenIdentifiers3() {
    // "a  b;"
    // "a . b;"
    scan("a ", "", ".", " b;");
    assertTokens(1, 1, "a", ".", "b", ";");
  }

  public void test_insert_period_insideExistingIdentifier() {
    // "ab;"
    // "a.b;"
    scan("a", "", ".", "b;");
    assertTokens(0, 2, "a", ".", "b", ";");
  }

  public void test_insert_periodAndIdentifier() {
    // "a + b;"
    // "a + b.x;"
    scan("a + b", "", ".x", ";");
    assertTokens(3, 4, "a", "+", "b", ".", "x", ";");
  }

  public void test_insert_whitespace_beginning_beforeToken() {
    // "a + b;"
    // " a + b;"
    scan("", "", " ", "a + b;");
    assertTokens(-1, -1, "a", "+", "b", ";");
  }

  public void test_insert_whitespace_betweenTokens() {
    // "a + b;"
    // "a  + b;"
    scan("a ", "", " ", "+ b;");
    assertTokens(-1, -1, "a", "+", "b", ";");
  }

  public void test_insert_whitespace_end_afterToken() {
    // "a + b;"
    // "a + b; "
    scan("a + b;", "", " ", "");
    assertTokens(-1, -1, "a", "+", "b", ";");
  }

  public void test_insert_whitespace_end_afterWhitespace() {
    // "a + b; "
    // "a + b;  "
    scan("a + b; ", "", " ", "");
    assertTokens(-1, -1, "a", "+", "b", ";");
  }

  public void test_insert_whitespace_withMultipleComments() {
    // "//comment", "//comment2", "a + b;"
    // "//comment", "//comment2", "a  + b;"
    scan(createSource(//
        "//comment",
        "//comment2",
        "a"), "", " ", " + b;");
    assertTokens(-1, -1, "a", "+", "b", ";");
  }

  public void test_replace_identifier_beginning() {
    // "bell + b;"
    // "fell + b;")
    scan("", "b", "f", "ell + b;");
    assertTokens(0, 0, "fell", "+", "b", ";");
  }

  public void test_replace_identifier_end() {
    // "bell + b;"
    // "belt + b;")
    scan("bel", "l", "t", " + b;");
    assertTokens(0, 0, "belt", "+", "b", ";");
  }

  public void test_replace_identifier_middle() {
    // "first + b;"
    // "frost + b;")
    scan("f", "ir", "ro", "st + b;");
    assertTokens(0, 0, "frost", "+", "b", ";");
  }

  public void test_replace_operator_oneForMany() {
    // "a + b;"
    // "a * c - b;")
    scan("a ", "+", "* c -", " b;");
    assertTokens(1, 3, "a", "*", "c", "-", "b", ";");
  }

  public void test_replace_operator_oneForOne() {
    // "a + b;"
    // "a * b;")
    scan("a ", "+", "*", " b;");
    assertTokens(1, 1, "a", "*", "b", ";");
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

  private void assertTokens(int firstIndex, int lastIndex, String... lexemes) {
    Token firstToken = null;
    Token lastToken = null;
    int count = lexemes.length;
    Token token = incrementalTokens;
    for (int i = 0; i < count; i++) {
      assertEquals(lexemes[i], token.getLexeme());
      if (i == firstIndex) {
        firstToken = token;
      }
      if (i == lastIndex) {
        lastToken = token;
      }
      token = token.getNext();
    }
    assertSame("Too many tokens", TokenType.EOF, token.getType());
    if (firstIndex >= 0) {
      assertNotNull(firstToken);
    }
    assertSame(firstToken, incrementalScanner.getFirstToken());
    if (lastIndex >= 0) {
      assertNotNull(lastToken);
    }
    assertSame(lastToken, incrementalScanner.getLastToken());
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
    incrementalTokens = incrementalScanner.rescan(
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
