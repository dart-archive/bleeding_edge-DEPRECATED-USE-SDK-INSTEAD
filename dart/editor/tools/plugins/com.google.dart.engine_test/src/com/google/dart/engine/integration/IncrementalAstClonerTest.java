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

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.visitor.ToSourceVisitor;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.parser.Parser;
import com.google.dart.engine.scanner.CharSequenceReader;
import com.google.dart.engine.scanner.Scanner;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.ast.IncrementalAstCloner;
import com.google.dart.engine.utilities.collection.TokenMap;
import com.google.dart.engine.utilities.io.PrintStringWriter;

import junit.framework.TestCase;

import java.io.File;

public class IncrementalAstClonerTest extends TestCase {
  private static int fileCount;
  private static int scanCount;

  public static void main(String[] args) {
    if (args.length > 0) {
      for (String path : args) {
        File file = new File(path);
        if (file.exists()) {
          try {
            traverse(file);
          } catch (Exception exception) {
            exception.printStackTrace();
            fail();
          }
        } else {
          System.out.println("File does not exist: " + file);
        }
      }
      System.out.println("" + scanCount + " of " + fileCount + " files scanned");
    } else {
      System.out.println("Missing file or directory name to traverse");
    }
  }

  private static void scan(File dartFile) throws Exception {
    fileCount++;
    final FileBasedSource source = new FileBasedSource(dartFile);

    // Two identical token streams
    final Token oldTokens = scan(source);
    final Token newTokens = scan(source);

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
    IncrementalAstCloner cloner = new IncrementalAstCloner(null, null, tokenMap);
    AstNode newUnit = oldUnit.accept(cloner);
    assertNotNull(newUnit);
    writer = new PrintStringWriter();
    oldUnit.accept(new ToSourceVisitor(writer));
    String newCode = writer.toString();
    assertEquals(oldCode, newCode);
    scanCount++;

  }

  private static Token scan(Source source) throws Exception {
    Scanner scanner = new Scanner(
        source,
        new CharSequenceReader(source.getContents().getData()),
        AnalysisErrorListener.NULL_LISTENER);
    return scanner.tokenize();
  }

  private static void traverse(File file) throws Exception {
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
