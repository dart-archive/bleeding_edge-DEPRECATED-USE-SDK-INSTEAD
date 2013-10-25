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

import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.TestSource;

import junit.framework.TestCase;

public class IncrementalScannerTest extends TestCase {
  public void test_rescan_addedToIdentifier() {
    assertTokens(//
        "a + b;",
        "abs + b;");
  }

  public void test_rescan_insertedPeriod() {
    assertTokens(//
        "a + b;",
        "a + b.;");
  }

  public void test_rescan_oneFunctionToTwo() {
    assertTokens(//
        "f() {}",
        "f() => 0; g() {}");
  }

  private void assertTokens(String originalContents, String modifiedContents) {
    //
    // Compute the location of the deleted and inserted text.
    //
    int originalLength = originalContents.length();
    int modifiedLength = modifiedContents.length();
    int replaceStart = 0;
    while (replaceStart < originalLength && replaceStart < modifiedLength
        && originalContents.charAt(replaceStart) == modifiedContents.charAt(replaceStart)) {
      replaceStart++;
    }
    int originalEnd = originalLength - 1;
    int modifiedEnd = modifiedLength - 1;
    while (originalEnd >= replaceStart && modifiedEnd >= replaceStart
        && originalContents.charAt(originalEnd) == modifiedContents.charAt(modifiedEnd)) {
      originalEnd--;
      modifiedEnd--;
    }
    Source source = new TestSource();
    //
    // Scan the original contents.
    //
    GatheringErrorListener originalListener = new GatheringErrorListener();
    Scanner originalScanner = new Scanner(
        source,
        new CharSequenceReader(originalContents),
        originalListener);
    Token originalToken = originalScanner.tokenize();
    assertNotNull(originalToken);
    //
    // Scan the modified contents.
    //
    GatheringErrorListener modifiedListener = new GatheringErrorListener();
    Scanner modifiedScanner = new Scanner(
        source,
        new CharSequenceReader(modifiedContents),
        modifiedListener);
    Token modifiedToken = modifiedScanner.tokenize();
    assertNotNull(modifiedToken);
    //
    // Incrementally scan the modified contents.
    //
    GatheringErrorListener incrementalListener = new GatheringErrorListener();
    IncrementalScanner incrementalScanner = new IncrementalScanner(source, new CharSequenceReader(
        modifiedContents), incrementalListener);
    Token incrementalToken = incrementalScanner.rescan(originalToken, replaceStart, originalEnd
        - replaceStart + 1, modifiedEnd - replaceStart + 1);
    //
    // Validate that the results of the incremental scan are the same as the full scan of the
    // modified source.
    //
    assertNotNull(incrementalToken);
    while (incrementalToken.getType() != TokenType.EOF && modifiedToken.getType() != TokenType.EOF) {
      assertSame("Wrong type for token", modifiedToken.getType(), incrementalToken.getType());
      assertEquals(
          "Wrong offset for token",
          modifiedToken.getOffset(),
          incrementalToken.getOffset());
      assertEquals(
          "Wrong length for token",
          modifiedToken.getLength(),
          incrementalToken.getLength());
      assertEquals(
          "Wrong lexeme for token",
          modifiedToken.getLexeme(),
          incrementalToken.getLexeme());
      incrementalToken = incrementalToken.getNext();
      modifiedToken = modifiedToken.getNext();
    }
    assertSame("Too many tokens", TokenType.EOF, incrementalToken.getType());
    assertSame("Not enough tokens", TokenType.EOF, modifiedToken.getType());
    // TODO(brianwilkerson) Verify that the errors are correct?
  }
}
