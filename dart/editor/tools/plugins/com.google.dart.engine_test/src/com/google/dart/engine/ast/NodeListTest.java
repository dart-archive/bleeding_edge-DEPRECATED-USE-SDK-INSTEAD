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

import static com.google.dart.engine.ast.ASTFactory.argumentList;
import static com.google.dart.engine.ast.ASTFactory.booleanLiteral;
import static com.google.dart.engine.ast.ASTFactory.integer;
import static com.google.dart.engine.ast.ASTFactory.parenthesizedExpression;

import java.util.ArrayList;

public class NodeListTest extends EngineTestCase {
  public void test_add() {
    ASTNode parent = argumentList();
    ASTNode firstNode = booleanLiteral(true);
    ASTNode secondNode = booleanLiteral(false);
    NodeList<ASTNode> list = new NodeList<ASTNode>(parent);
    list.add(0, secondNode);
    list.add(0, firstNode);
    assertSize(2, list);
    assertSame(firstNode, list.get(0));
    assertSame(secondNode, list.get(1));
    assertSame(parent, firstNode.getParent());
    assertSame(parent, secondNode.getParent());

    ASTNode thirdNode = booleanLiteral(false);
    list.add(1, thirdNode);
    assertSize(3, list);
    assertSame(firstNode, list.get(0));
    assertSame(thirdNode, list.get(1));
    assertSame(secondNode, list.get(2));
    assertSame(parent, firstNode.getParent());
    assertSame(parent, secondNode.getParent());
    assertSame(parent, thirdNode.getParent());
  }

  public void test_add_negative() {
    NodeList<ASTNode> list = new NodeList<ASTNode>(argumentList());
    try {
      list.add(-1, booleanLiteral(true));
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException exception) {
      // Expected
    }
  }

  public void test_add_tooBig() {
    NodeList<ASTNode> list = new NodeList<ASTNode>(argumentList());
    try {
      list.add(1, booleanLiteral(true));
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException exception) {
      // Expected
    }
  }

  public void test_addAll() {
    ASTNode parent = argumentList();
    ArrayList<ASTNode> firstNodes = new ArrayList<ASTNode>();
    ASTNode firstNode = booleanLiteral(true);
    ASTNode secondNode = booleanLiteral(false);
    firstNodes.add(firstNode);
    firstNodes.add(secondNode);
    NodeList<ASTNode> list = new NodeList<ASTNode>(parent);
    list.addAll(firstNodes);
    assertSize(2, list);
    assertSame(firstNode, list.get(0));
    assertSame(secondNode, list.get(1));
    assertSame(parent, firstNode.getParent());
    assertSame(parent, secondNode.getParent());

    ArrayList<ASTNode> secondNodes = new ArrayList<ASTNode>();
    ASTNode thirdNode = booleanLiteral(true);
    ASTNode fourthNode = booleanLiteral(false);
    secondNodes.add(thirdNode);
    secondNodes.add(fourthNode);
    list.addAll(secondNodes);
    assertSize(4, list);
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
    ASTNode owner = argumentList();
    NodeList<ASTNode> list = NodeList.create(owner);
    assertNotNull(list);
    assertSize(0, list);
    assertSame(owner, list.getOwner());
  }

  public void test_creation() {
    ASTNode owner = argumentList();
    NodeList<ASTNode> list = new NodeList<ASTNode>(owner);
    assertNotNull(list);
    assertSize(0, list);
    assertSame(owner, list.getOwner());
  }

  public void test_get_negative() {
    NodeList<ASTNode> list = new NodeList<ASTNode>(argumentList());
    try {
      list.get(-1);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException exception) {
      // Expected
    }
  }

  public void test_get_tooBig() {
    NodeList<ASTNode> list = new NodeList<ASTNode>(argumentList());
    try {
      list.get(1);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException exception) {
      // Expected
    }
  }

  public void test_getBeginToken_empty() {
    NodeList<ASTNode> list = new NodeList<ASTNode>(argumentList());
    assertNull(list.getBeginToken());
  }

  public void test_getBeginToken_nonEmpty() {
    NodeList<ASTNode> list = new NodeList<ASTNode>(argumentList());
    ASTNode node = parenthesizedExpression(booleanLiteral(true));
    list.add(node);
    assertSame(node.getBeginToken(), list.getBeginToken());
  }

  public void test_getEndToken_empty() {
    NodeList<ASTNode> list = new NodeList<ASTNode>(argumentList());
    assertNull(list.getEndToken());
  }

  public void test_getEndToken_nonEmpty() {
    NodeList<ASTNode> list = new NodeList<ASTNode>(argumentList());
    ASTNode node = parenthesizedExpression(booleanLiteral(true));
    list.add(node);
    assertSame(node.getEndToken(), list.getEndToken());
  }

  public void test_remove() {
    ArrayList<ASTNode> nodes = new ArrayList<ASTNode>();
    ASTNode firstNode = booleanLiteral(true);
    ASTNode secondNode = booleanLiteral(false);
    ASTNode thirdNode = booleanLiteral(true);
    nodes.add(firstNode);
    nodes.add(secondNode);
    nodes.add(thirdNode);
    NodeList<ASTNode> list = new NodeList<ASTNode>(argumentList());
    list.addAll(nodes);
    assertSize(3, list);

    assertSame(secondNode, list.remove(1));
    assertSize(2, list);
    assertSame(firstNode, list.get(0));
    assertSame(thirdNode, list.get(1));
  }

  public void test_remove_negative() {
    NodeList<ASTNode> list = new NodeList<ASTNode>(argumentList());
    try {
      list.remove(-1);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException exception) {
      // Expected
    }
  }

  public void test_remove_tooBig() {
    NodeList<ASTNode> list = new NodeList<ASTNode>(argumentList());
    try {
      list.remove(1);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException exception) {
      // Expected
    }
  }

  public void test_set() {
    ArrayList<ASTNode> nodes = new ArrayList<ASTNode>();
    ASTNode firstNode = booleanLiteral(true);
    ASTNode secondNode = booleanLiteral(false);
    ASTNode thirdNode = booleanLiteral(true);
    nodes.add(firstNode);
    nodes.add(secondNode);
    nodes.add(thirdNode);
    NodeList<ASTNode> list = new NodeList<ASTNode>(argumentList());
    list.addAll(nodes);
    assertSize(3, list);

    ASTNode fourthNode = integer(0);
    assertSame(secondNode, list.set(1, fourthNode));
    assertSize(3, list);
    assertSame(firstNode, list.get(0));
    assertSame(fourthNode, list.get(1));
    assertSame(thirdNode, list.get(2));
  }

  public void test_set_negative() {
    ASTNode node = booleanLiteral(true);
    NodeList<ASTNode> list = new NodeList<ASTNode>(argumentList());
    try {
      list.set(-1, node);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException exception) {
      // Expected
    }
  }

  public void test_set_tooBig() {
    ASTNode node = booleanLiteral(true);
    NodeList<ASTNode> list = new NodeList<ASTNode>(argumentList());
    try {
      list.set(1, node);
      fail("Expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException exception) {
      // Expected
    }
  }
}
