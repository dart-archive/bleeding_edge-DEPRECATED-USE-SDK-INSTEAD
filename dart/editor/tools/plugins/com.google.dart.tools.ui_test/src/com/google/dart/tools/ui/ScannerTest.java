/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui;

import com.google.dart.compiler.parser.Token;
import com.google.dart.tools.core.internal.formatter.InvalidInputException;
import com.google.dart.tools.core.internal.formatter.Scanner;

import junit.framework.TestCase;

public class ScannerTest extends TestCase {

  private static final String SOURCE1 = "  \t X";
  private static final String SOURCE2 = "\n\n";
  private static final String SOURCE3 = "'''\n'x'\n\t'''";
  private static final String SOURCE4 = "/*\n*/\n ";
  private static final String SOURCE5 = "//comment\n\t\t";
  private static final String SOURCE6 = "/*\n*/\n //comment\nX Y";
  private static final String SOURCE7 = "/*\n*\n //comment\nX Y";

  public void testScanner1() throws Exception {
    Scanner scanner = new Scanner();
    scanner.setSource(SOURCE1);
    String ws = scanner.peekWhitespace();
    assertEquals(SOURCE1.substring(0, SOURCE1.length() - 1), ws);
  }

  public void testScanner2() throws Exception {
    Scanner scanner = new Scanner();
    scanner.setSource(SOURCE2);
    int n = scanner.countLinesBetween(0, SOURCE2.length() - 1);
    assertEquals(2, n);
  }

  public void testScanner3() throws Exception {
    Scanner scanner = new Scanner();
    scanner.setSource(SOURCE3);
    int n = scanner.countLinesBetween(0, SOURCE3.length() - 1);
    assertEquals(0, n);
    int m = scanner.charsAfterLastLineEnd();
    assertEquals(1, m);
  }

  public void testScanner4() throws Exception {
    Scanner scanner = new Scanner();
    scanner.setSource(SOURCE4);
    int n = scanner.countLinesBetween(0, SOURCE4.length() - 1);
    assertEquals(1, n);
    int m = scanner.charsAfterLastLineEnd();
    assertEquals(1, m);
  }

  public void testScanner4b() throws Exception {
    Scanner scanner = new Scanner();
    scanner.setSource(SOURCE4);
    Token t = scanner.getNextToken();
    assertEquals(Token.EOS, t);
    assertEquals(1, scanner.commentLocs.size());
    int[] loc = scanner.commentLocs.get(0);
    assertEquals(0, loc[0]);
    assertEquals(5, loc[1]);
  }

  public void testScanner5() throws Exception {
    Scanner scanner = new Scanner();
    scanner.setSource(SOURCE5);
    int n = scanner.countLinesBetween(0, SOURCE5.length() - 1);
    assertEquals(1, n);
    int m = scanner.charsAfterLastLineEnd();
    assertEquals(2, m);
  }

  public void testScanner5b() throws Exception {
    Scanner scanner = new Scanner();
    scanner.setSource(SOURCE5);
    Token t = scanner.getNextToken();
    assertEquals(Token.EOS, t);
  }

  public void testScanner6() throws Exception {
    Scanner scanner = new Scanner();
    scanner.setSource(SOURCE6);
    Token t = scanner.getNextToken();
    assertEquals(Token.IDENTIFIER, t);
  }

  public void testScanner7() throws Exception {
    Scanner scanner = new Scanner();
    scanner.setSource(SOURCE7);
    try {
      scanner.getNextToken();
      fail();
    } catch (InvalidInputException ex) {
      // continue
    }
    int[] loc = scanner.commentLocs.get(0);
    assertEquals(SOURCE7, SOURCE7.substring(loc[0], loc[1]));
  }
}
