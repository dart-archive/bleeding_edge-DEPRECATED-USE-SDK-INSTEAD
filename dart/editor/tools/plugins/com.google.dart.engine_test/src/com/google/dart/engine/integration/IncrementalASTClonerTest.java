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
package com.google.dart.engine.integration;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.visitor.ToSourceVisitor;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.parser.Parser;
import com.google.dart.engine.scanner.CharSequenceReader;
import com.google.dart.engine.scanner.Scanner;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.ContentCache;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.ast.IncrementalASTCloner;
import com.google.dart.engine.utilities.collection.TokenMap;
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.engine.utilities.source.LineInfo;

import junit.framework.TestCase;

import java.io.File;
import java.nio.CharBuffer;

public class IncrementalASTClonerTest extends TestCase {

  private static final ContentCache contentCache = new ContentCache();
  private static int fileCount;
  private static int scanCount;

  public static void main(String[] args) {
    if (args.length > 0) {
      for (String path : args) {
        File file = new File(path);
        if (file.exists()) {
          traverse(file);
        } else {
          System.out.println("File does not exist: " + file);
        }
      }
      System.out.println("" + scanCount + " of " + fileCount + " files scanned");
    } else {
      System.out.println("Missing file or directory name to traverse");
    }
  }

  private static void scan(File dartFile) {
    fileCount++;
    final FileBasedSource source = new FileBasedSource(contentCache, dartFile);

    final Token[] tokens = new Token[1];
    final LineInfo[] lineInfo = new LineInfo[1];
    Source.ContentReceiver receiver = new Source.ContentReceiver() {
      @Override
      public void accept(CharBuffer contents, long modificationTime) {
        Scanner scanner = new Scanner(
            source,
            new CharSequenceReader(contents),
            AnalysisErrorListener.NULL_LISTENER);
        tokens[0] = scanner.tokenize();
        lineInfo[0] = new LineInfo(scanner.getLineStarts());
      }

      @Override
      public void accept(String contents, long modificationTime) {
        Scanner scanner = new Scanner(
            source,
            new CharSequenceReader(contents),
            AnalysisErrorListener.NULL_LISTENER);
        tokens[0] = scanner.tokenize();
        lineInfo[0] = new LineInfo(scanner.getLineStarts());
      }
    };

    // Two identical token streams
    try {
      source.getContents(receiver);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
    final Token oldTokens = tokens[0];
    try {
      source.getContents(receiver);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
    final Token newTokens = tokens[0];

    // Parse using the first token stream
    Parser parser = new Parser(source, AnalysisErrorListener.NULL_LISTENER);
    CompilationUnit oldUnit = parser.parseCompilationUnit(oldTokens);
    PrintStringWriter writer = new PrintStringWriter();
    oldUnit.accept(new ToSourceVisitor(writer));
    String oldCode = writer.toString();

    // Create map of first token stream to second token stream
    TokenMap tokenMap = new TokenMap();
    Token oldHead = oldTokens;
    Token newHead = newTokens;
    try {
      while (oldHead != null) {
        Token oldComment = oldHead.getPrecedingComments();
        Token newComment = newHead.getPrecedingComments();
        while (oldComment != null) {
          tokenMap.put(oldComment, newComment);
          oldComment = oldComment.getNext();
          newComment = newComment.getNext();
        }
        tokenMap.put(oldHead, newHead);
        oldHead = oldHead.getNext();
        newHead = newHead.getNext();
        if (oldHead == oldHead.getNext()) {
          break;
        }
      }
    } catch (RuntimeException e) {
      System.out.println(" Skip scan");
      e.printStackTrace();
      return;
    }

    // Clone AST
    IncrementalASTCloner cloner = new IncrementalASTCloner(null, null, tokenMap);
    ASTNode newUnit = oldUnit.accept(cloner);
    assertNotNull(newUnit);
    writer = new PrintStringWriter();
    oldUnit.accept(new ToSourceVisitor(writer));
    String newCode = writer.toString();
    assertEquals(oldCode, newCode);
    scanCount++;

  }

  private static void traverse(File file) {
    if (file.isDirectory()) {
      for (File child : file.listFiles()) {
        traverse(child);
      }
    } else if (file.getName().endsWith(".dart")) {
      System.out.println("scanning " + file);
      scan(file);
    }
  }
}
