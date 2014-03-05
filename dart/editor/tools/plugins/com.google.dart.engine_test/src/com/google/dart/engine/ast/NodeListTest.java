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
package com.google.dart.engine.ast;

import com.google.dart.engine.EngineTestCase;

import static com.google.dart.engine.ast.AstFactory.argumentList;
import static com.google.dart.engine.ast.AstFactory.booleanLiteral;
import static com.google.dart.engine.ast.AstFactory.integer;
import static com.google.dart.engine.ast.AstFactory.parenthesizedExpression;

import java.util.ArrayList;

public class NodeListTest extends EngineTestCase {
  public void test_add() {
    AstNode parent = argumentList();
    AstNode firstNode = booleanLiteral(true);
    AstNode secondNode = booleanLiteral(false);
    NodeList<AstNode> list = new NodeList<AstNode>(parent);
    list.add(0, secondNode);
    list.add(0, firstNode);
    assertSizeOfList(2, list);
    assertSame(firstNode, list.get(0));
    assertSame(secondNode, list.get(1));
    assertSame(parent, firstNode.getParent());
    assertSame(parent, secondNode.getParent());

    AstNode thirdNode = booleanLiteral(false);
    list.add(1, thirdNode);
    assertSizeOfList(3, list);
    assertSame(firstNode, list.get(0));
    assertSame(thirdNode, list.get(1));
    assertSame(secondNode, list.get(2));
    assertSame(parent, firstNode.getParent());
    assertSame(parent, secondNode.getParent());
    assertSame(parent, thirdNode.getParent());
  }

  public void test_add_negative() {
    NodeList<AstNode> list = new NodeList<AstNode>(argumentList());
    try {
      list.add(-1, booleanLiteral(true));
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException exception) {
      // Expected
    }
  }

  public void test_add_tooBig() {
    NodeList<AstNode> list = new NodeList<AstNode>(argumentList());
    try {
      list.add(1, booleanLiteral(true));
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException exception) {
      // Expected
    }
  }

  public void test_addAll() {
    AstNode parent = argumentList();
    ArrayList<AstNode> firstNodes = new ArrayList<AstNode>();
    AstNode firstNode = booleanLiteral(true);
    AstNode secondNode = booleanLiteral(false);
    firstNodes.add(firstNode);
    firstNodes.add(secondNode);
    NodeList<AstNode> list = new NodeList<AstNode>(parent);
    list.addAll(firstNodes);
    assertSizeOfList(2, list);
    assertSame(firstNode, list.get(0));
    assertSame(secondNode, list.get(1));
    assertSame(parent, firstNode.getParent());
    assertSame(parent, secondNode.getParent());

    ArrayList<AstNode> secondNodes = new ArrayList<AstNode>();
    AstNode thirdNode = booleanLiteral(true);
    AstNode fourthNode = booleanLiteral(false);
    secondNodes.add(thirdNode);
    secondNodes.add(fourthNode);
    list.addAll(secondNodes);
    assertSizeOfList(4, list);
    assertSame(firstNode, list.get(0));
    assertSame(secondNode, list.get(1));
    assertSame(thirdNode, list.get(2));
    assertSame(fourthNode, list.get(3));
    assertSame(parent, firstNode.getParent());
    assertSame(parent, secondNode.getParent());
    assertSame(parent, thirdNode.getParent());
    assertSame(parent, fourthNode.getParent());
  }

  public void test_create() {
    AstNode owner = argumentList();
    NodeList<AstNode> list = NodeList.create(owner);
    assertNotNull(list);
    assertSizeOfList(0, list);
    assertSame(owner, list.getOwner());
  }

  public void test_creation() {
    AstNode owner = argumentList();
    NodeList<AstNode> list = new NodeList<AstNode>(owner);
    assertNotNull(list);
    assertSizeOfList(0, list);
    assertSame(owner, list.getOwner());
  }

  public void test_get_negative() {
    NodeList<AstNode> list = new NodeList<AstNode>(argumentList());
    try {
      list.get(-1);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException exception) {
      // Expected
    }
  }

  public void test_get_tooBig() {
    NodeList<AstNode> list = new NodeList<AstNode>(argumentList());
    try {
      list.get(1);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException exception) {
      // Expected
    }
  }

  public void test_getBeginToken_empty() {
    NodeList<AstNode> list = new NodeList<AstNode>(argumentList());
    assertNull(list.getBeginToken());
  }

  public void test_getBeginToken_nonEmpty() {
    NodeList<AstNode> list = new NodeList<AstNode>(argumentList());
    AstNode node = parenthesizedExpression(booleanLiteral(true));
    list.add(node);
    assertSame(node.getBeginToken(), list.getBeginToken());
  }

  public void test_getEndToken_empty() {
    NodeList<AstNode> list = new NodeList<AstNode>(argumentList());
    assertNull(list.getEndToken());
  }

  public void test_getEndToken_nonEmpty() {
    NodeList<AstNode> list = new NodeList<AstNode>(argumentList());
    AstNode node = parenthesizedExpression(booleanLiteral(true));
    list.add(node);
    assertSame(node.getEndToken(), list.getEndToken());
  }

  public void test_indexOf() {
    ArrayList<AstNode> nodes = new ArrayList<AstNode>();
    AstNode firstNode = booleanLiteral(true);
    AstNode secondNode = booleanLiteral(false);
    AstNode thirdNode = booleanLiteral(true);
    AstNode fourthNode = booleanLiteral(false);
    nodes.add(firstNode);
    nodes.add(secondNode);
    nodes.add(thirdNode);
    NodeList<AstNode> list = new NodeList<AstNode>(argumentList());
    list.addAll(nodes);
    assertSizeOfList(3, list);

    assertEquals(0, list.indexOf(firstNode));
    assertEquals(1, list.indexOf(secondNode));
    assertEquals(2, list.indexOf(thirdNode));
    assertEquals(-1, list.indexOf(fourthNode));
    assertEquals(-1, list.indexOf(null));
  }

  public void test_remove() {
    ArrayList<AstNode> nodes = new ArrayList<AstNode>();
    AstNode firstNode = booleanLiteral(true);
    AstNode secondNode = booleanLiteral(false);
    AstNode thirdNode = booleanLiteral(true);
    nodes.add(firstNode);
    nodes.add(secondNode);
    nodes.add(thirdNode);
    NodeList<AstNode> list = new NodeList<AstNode>(argumentList());
    list.addAll(nodes);
    assertSizeOfList(3, list);

    assertSame(secondNode, list.remove(1));
    assertSizeOfList(2, list);
    assertSame(firstNode, list.get(0));
    assertSame(thirdNode, list.get(1));
  }

  public void test_remove_negative() {
    NodeList<AstNode> list = new NodeList<AstNode>(argumentList());
    try {
      list.remove(-1);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException exception) {
      // Expected
    }
  }

  public void test_remove_tooBig() {
    NodeList<AstNode> list = new NodeList<AstNode>(argumentList());
    try {
      list.remove(1);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException exception) {
      // Expected
    }
  }

  public void test_set() {
    ArrayList<AstNode> nodes = new ArrayList<AstNode>();
    AstNode firstNode = booleanLiteral(true);
    AstNode secondNode = booleanLiteral(false);
    AstNode thirdNode = booleanLiteral(true);
    nodes.add(firstNode);
    nodes.add(secondNode);
    nodes.add(thirdNode);
    NodeList<AstNode> list = new NodeList<AstNode>(argumentList());
    list.addAll(nodes);
    assertSizeOfList(3, list);

    AstNode fourthNode = integer(0);
    assertSame(secondNode, list.set(1, fourthNode));
    assertSizeOfList(3, list);
    assertSame(firstNode, list.get(0));
    assertSame(fourthNode, list.get(1));
    assertSame(thirdNode, list.get(2));
  }

  public void test_set_negative() {
    AstNode node = booleanLiteral(true);
    NodeList<AstNode> list = new NodeList<AstNode>(argumentList());
    try {
      list.set(-1, node);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException exception) {
      // Expected
    }
  }

  public void test_set_tooBig() {
    AstNode node = booleanLiteral(true);
    NodeList<AstNode> list = new NodeList<AstNode>(argumentList());
    try {
      list.set(1, node);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException exception) {
      // Expected
    }
  }
}
