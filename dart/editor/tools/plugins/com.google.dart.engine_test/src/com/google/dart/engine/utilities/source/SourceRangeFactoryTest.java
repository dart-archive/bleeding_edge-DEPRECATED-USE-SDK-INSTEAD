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
package com.google.dart.engine.utilities.source;

import com.google.common.collect.ImmutableList;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.scanner.Token;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

public class SourceRangeFactoryTest extends TestCase {
  private static void assertRange(SourceRange range, int offset, int length) {
    assertEquals(offset, range.getOffset());
    assertEquals(length, range.getLength());
  }

  private static void assertRangeEnd(SourceRange range, int offset, int end) {
    assertEquals(offset, range.getOffset());
    assertEquals(end, range.getEnd());
  }

  private static ASTNode mockNode(int offset, int length) {
    ASTNode node = mock(ASTNode.class);
    when(node.getOffset()).thenReturn(offset);
    when(node.getLength()).thenReturn(length);
    when(node.getEnd()).thenReturn(offset + length);
    return node;
  }

  private static Token mockToken(int offset, int length) {
    Token token = mock(Token.class);
    when(token.getOffset()).thenReturn(offset);
    when(token.getLength()).thenReturn(length);
    when(token.getEnd()).thenReturn(offset + length);
    return token;
  }

  public void test_rangeElementName() throws Exception {
    Element element = mock(Element.class);
    when(element.getNameOffset()).thenReturn(5);
    when(element.getName()).thenReturn("MyClass");
    SourceRange range = SourceRangeFactory.rangeElementName(element);
    assertRange(range, 5, 7);
  }

  public void test_rangeEndEnd_NI() throws Exception {
    ASTNode a = mockNode(10, 1);
    SourceRange range = SourceRangeFactory.rangeEndEnd(a, 30);
    assertRangeEnd(range, 10 + 1, 30);
  }

  public void test_rangeEndEnd_NN() throws Exception {
    ASTNode a = mockNode(10, 1);
    ASTNode b = mockNode(20, 2);
    SourceRange range = SourceRangeFactory.rangeEndEnd(a, b);
    assertRangeEnd(range, 10 + 1, 20 + 2);
  }

  public void test_rangeEndEnd_NR() throws Exception {
    ASTNode a = mockNode(10, 1);
    SourceRange b = new SourceRange(20, 2);
    SourceRange range = SourceRangeFactory.rangeEndEnd(a, b);
    assertRangeEnd(range, 10 + 1, 20 + 2);
  }

  public void test_rangeEndEnd_RR() throws Exception {
    SourceRange a = new SourceRange(10, 1);
    SourceRange b = new SourceRange(20, 2);
    SourceRange range = SourceRangeFactory.rangeEndEnd(a, b);
    assertRangeEnd(range, 10 + 1, 20 + 2);
  }

  public void test_rangeEndLength_NI() throws Exception {
    ASTNode a = mockNode(10, 1);
    SourceRange range = SourceRangeFactory.rangeEndLength(a, 5);
    assertRange(range, 10 + 1, 5);
  }

  public void test_rangeEndLength_RI() throws Exception {
    SourceRange a = new SourceRange(10, 1);
    SourceRange range = SourceRangeFactory.rangeEndLength(a, 5);
    assertRange(range, 10 + 1, 5);
  }

  public void test_rangeEndStart_NI() throws Exception {
    ASTNode a = mockNode(10, 1);
    SourceRange range = SourceRangeFactory.rangeEndStart(a, 20);
    assertRangeEnd(range, 10 + 1, 20);
  }

  public void test_rangeEndStart_NN() throws Exception {
    ASTNode a = mockNode(10, 1);
    ASTNode b = mockNode(20, 2);
    SourceRange range = SourceRangeFactory.rangeEndStart(a, b);
    assertRangeEnd(range, 10 + 1, 20);
  }

  public void test_rangeEndStart_NT() throws Exception {
    ASTNode a = mockNode(10, 1);
    Token b = mockToken(20, 2);
    SourceRange range = SourceRangeFactory.rangeEndStart(a, b);
    assertRangeEnd(range, 10 + 1, 20);
  }

  public void test_rangeEndStart_RR() throws Exception {
    SourceRange a = new SourceRange(10, 1);
    SourceRange b = new SourceRange(20, 2);
    SourceRange range = SourceRangeFactory.rangeEndStart(a, b);
    assertRangeEnd(range, 10 + 1, 20);
  }

  public void test_rangeFromBase_NI() throws Exception {
    ASTNode a = mockNode(10, 1);
    int base = 4;
    SourceRange range = SourceRangeFactory.rangeFromBase(a, base);
    assertRange(range, 6, 1);
  }

  public void test_rangeFromBase_NR() throws Exception {
    ASTNode a = mockNode(10, 1);
    SourceRange base = new SourceRange(4, 50);
    SourceRange range = SourceRangeFactory.rangeFromBase(a, base);
    assertRange(range, 6, 1);
  }

  public void test_rangeFromBase_RR() throws Exception {
    SourceRange a = new SourceRange(10, 1);
    SourceRange base = new SourceRange(4, 50);
    SourceRange range = SourceRangeFactory.rangeFromBase(a, base);
    assertRange(range, 6, 1);
  }

  public void test_rangeNode() throws Exception {
    ASTNode node = mockNode(1, 10);
    SourceRange range = SourceRangeFactory.rangeNode(node);
    assertRange(range, 1, 10);
  }

  public void test_rangeNode_null() throws Exception {
    SourceRange range = SourceRangeFactory.rangeNode((ASTNode) null);
    assertNull(range);
  }

  public void test_rangeNodes() throws Exception {
    ASTNode first = mockNode(10, 1);
    ASTNode middle = mockNode(15, 1);
    ASTNode last = mockNode(20, 2);
    List<ASTNode> nodes = ImmutableList.of(first, middle, last);
    SourceRange range = SourceRangeFactory.rangeNodes(nodes);
    assertRangeEnd(range, 10, 20 + 2);
  }

  public void test_rangeNodes_empty() throws Exception {
    List<ASTNode> nodes = ImmutableList.of();
    SourceRange range = SourceRangeFactory.rangeNodes(nodes);
    assertRange(range, 0, 0);
  }

  public void test_rangeStartEnd_IN() throws Exception {
    ASTNode b = mockNode(20, 2);
    SourceRange range = SourceRangeFactory.rangeStartEnd(10, b);
    assertRangeEnd(range, 10, 20 + 2);
  }

  public void test_rangeStartEnd_NI() throws Exception {
    ASTNode a = mockNode(10, 1);
    SourceRange range = SourceRangeFactory.rangeStartEnd(a, 22);
    assertRangeEnd(range, 10, 22);
  }

  public void test_rangeStartEnd_NN() throws Exception {
    ASTNode a = mockNode(10, 1);
    ASTNode b = mockNode(20, 2);
    SourceRange range = SourceRangeFactory.rangeStartEnd(a, b);
    assertRangeEnd(range, 10, 20 + 2);
  }

  public void test_rangeStartEnd_RI() throws Exception {
    SourceRange a = new SourceRange(10, 1);
    SourceRange range = SourceRangeFactory.rangeStartEnd(a, 22);
    assertRangeEnd(range, 10, 22);
  }

  public void test_rangeStartEnd_RN() throws Exception {
    SourceRange a = new SourceRange(10, 1);
    ASTNode b = mockNode(20, 2);
    SourceRange range = SourceRangeFactory.rangeStartEnd(a, b);
    assertRangeEnd(range, 10, 20 + 2);
  }

  public void test_rangeStartEnd_RR() throws Exception {
    SourceRange a = new SourceRange(10, 1);
    SourceRange b = new SourceRange(20, 2);
    SourceRange range = SourceRangeFactory.rangeStartEnd(a, b);
    assertRangeEnd(range, 10, 20 + 2);
  }

  public void test_rangeStartEnd_TN() throws Exception {
    Token a = mockToken(10, 1);
    ASTNode b = mockNode(20, 2);
    SourceRange range = SourceRangeFactory.rangeStartEnd(a, b);
    assertRangeEnd(range, 10, 20 + 2);
  }

  public void test_rangeStartEnd_TT() throws Exception {
    Token a = mockToken(10, 1);
    Token b = mockToken(20, 2);
    SourceRange range = SourceRangeFactory.rangeStartEnd(a, b);
    assertRangeEnd(range, 10, 20 + 2);
  }

  public void test_rangeStartLength_NI() throws Exception {
    ASTNode a = mockNode(10, 1);
    SourceRange range = SourceRangeFactory.rangeStartLength(a, 5);
    assertRange(range, 10, 5);
  }

  public void test_rangeStartLength_RI() throws Exception {
    SourceRange a = new SourceRange(10, 1);
    SourceRange range = SourceRangeFactory.rangeStartLength(a, 5);
    assertRange(range, 10, 5);
  }

  public void test_rangeStartStart_IN() throws Exception {
    ASTNode b = mockNode(20, 2);
    SourceRange range = SourceRangeFactory.rangeStartStart(10, b);
    assertRangeEnd(range, 10, 20);
  }

  public void test_rangeStartStart_NN() throws Exception {
    ASTNode a = mockNode(10, 1);
    ASTNode b = mockNode(20, 2);
    SourceRange range = SourceRangeFactory.rangeStartStart(a, b);
    assertRangeEnd(range, 10, 20);
  }

  public void test_rangeStartStart_RN() throws Exception {
    SourceRange a = new SourceRange(10, 1);
    ASTNode b = mockNode(20, 2);
    SourceRange range = SourceRangeFactory.rangeStartStart(a, b);
    assertRangeEnd(range, 10, 20);
  }

  public void test_rangeStartStart_RR() throws Exception {
    SourceRange a = new SourceRange(10, 1);
    SourceRange b = new SourceRange(20, 2);
    SourceRange range = SourceRangeFactory.rangeStartStart(a, b);
    assertRangeEnd(range, 10, 20);
  }

  public void test_rangeToken() throws Exception {
    Token token = mockToken(10, 5);
    SourceRange range = SourceRangeFactory.rangeToken(token);
    assertRange(range, 10, 5);
  }

  public void test_rangeWithBase_IR() throws Exception {
    int base = 4;
    SourceRange r = new SourceRange(10, 1);
    SourceRange range = SourceRangeFactory.rangeWithBase(base, r);
    assertRange(range, 14, 1);
  }

  public void test_rangeWithBase_NR() throws Exception {
    ASTNode base = mockNode(4, 100);
    SourceRange r = new SourceRange(10, 1);
    SourceRange range = SourceRangeFactory.rangeWithBase(base, r);
    assertRange(range, 14, 1);
  }

  public void test_rangeWithBase_RR() throws Exception {
    SourceRange base = new SourceRange(4, 100);
    SourceRange r = new SourceRange(10, 1);
    SourceRange range = SourceRangeFactory.rangeWithBase(base, r);
    assertRange(range, 14, 1);
  }
}
