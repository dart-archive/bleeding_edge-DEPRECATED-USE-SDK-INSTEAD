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
package com.google.dart.engine.parser;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.ast.ASTComparator;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.scanner.CharSequenceReader;
import com.google.dart.engine.scanner.IncrementalScanner;
import com.google.dart.engine.scanner.Scanner;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.TestSource;

public class IncrementalParserTest extends EngineTestCase {
  public void fail_reparse_oneFunctionToTwo() {
    // We are not currently able to handle the case where one node must be replaced by more than one
    // node.
    assertParse(//
        "f() {}",
        "f() => 0; g() {}");
  }

  public void test_reparse_addedAfterIdentifier1() {
    assertParse(//
        "f() => a + b;",
        "f() => abs + b;");
  }

  public void test_reparse_addedAfterIdentifier2() {
    assertParse(//
        "f() => a + b;",
        "f() => a + bar;");
  }

  public void test_resparse_addedBeforeIdentifier1() {
    assertParse(//
        "f() => a + b;",
        "f() => xa + b;");
  }

  public void test_resparse_addedBeforeIdentifier2() {
    assertParse(//
        "f() => a + b;",
        "f() => a + xb;");
  }

  public void test_resparse_addedNewIdentifier1() {
    assertParse(//
        "a; c;",
        "a; b c;");
  }

  public void test_resparse_addedNewIdentifier2() {
    assertParse(//
        "a;  c;",
        "a;b  c;");
  }

  public void fail_resparse_appendWhitespace1() {
    assertParse(//
        "f() => a + b;",
        "f() => a + b; ");
  }

  public void fail_resparse_appendWhitespace2() {
    assertParse(//
        "f() => a + b;",
        "f() => a + b;  ");
  }

  public void test_resparse_insertedPeriod() {
    assertParse(//
        "f() => a + b;",
        "f() => a + b.;");
  }

  public void fail_resparse_insertWhitespace() {
    assertParse(//
        "f() => a + b;",
        "f() => a  + b;");
  }

  private void assertParse(String originalContents, String modifiedContents) {
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
    int lengthDelta = modifiedLength - originalLength;
    int originalEnd = originalLength - 1;
    int modifiedEnd = modifiedLength - 1;
    while (originalEnd >= replaceStart && modifiedEnd >= (replaceStart + lengthDelta)
        && originalContents.charAt(originalEnd) == modifiedContents.charAt(modifiedEnd)) {
      originalEnd--;
      modifiedEnd--;
    }
    Source source = new TestSource();
    //
    // Parse the original contents.
    //
    GatheringErrorListener originalListener = new GatheringErrorListener();
    Scanner originalScanner = new Scanner(
        source,
        new CharSequenceReader(originalContents),
        originalListener);
    Token originalToken = originalScanner.tokenize();
    assertNotNull(originalToken);
    Parser originalParser = new Parser(source, originalListener);
    CompilationUnit originalUnit = originalParser.parseCompilationUnit(originalToken);
    assertNotNull(originalUnit);
    //
    // Parse the modified contents.
    //
    GatheringErrorListener modifiedListener = new GatheringErrorListener();
    Scanner modifiedScanner = new Scanner(
        source,
        new CharSequenceReader(modifiedContents),
        modifiedListener);
    Token modifiedToken = modifiedScanner.tokenize();
    assertNotNull(modifiedToken);
    Parser modifiedParser = new Parser(source, modifiedListener);
    CompilationUnit modifiedUnit = modifiedParser.parseCompilationUnit(modifiedToken);
    assertNotNull(modifiedUnit);
    //
    // Incrementally parse the modified contents.
    //
    GatheringErrorListener incrementalListener = new GatheringErrorListener();
    IncrementalScanner incrementalScanner = new IncrementalScanner(source, new CharSequenceReader(
        modifiedContents), incrementalListener);
    Token incrementalToken = incrementalScanner.rescan(originalToken, replaceStart, originalEnd
        - replaceStart + 1, modifiedEnd - replaceStart + 1);
    assertNotNull(incrementalToken);
    IncrementalParser incrementalParser = new IncrementalParser(
        source,
        incrementalScanner.getTokenMap(),
        incrementalListener);
    CompilationUnit incrementalUnit = incrementalParser.reparse(
        originalUnit,
        incrementalScanner.getFirstToken(),
        incrementalScanner.getLastToken(),
        replaceStart,
        originalEnd);
    assertNotNull(incrementalUnit);
    //
    // Validate that the results of the incremental parse are the same as the full parse of the
    // modified source.
    //
    assertTrue(ASTComparator.equals(modifiedUnit, incrementalUnit));
    // TODO(brianwilkerson) Verify that the errors are correct?
  }
}
