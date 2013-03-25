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
package com.google.dart.engine.internal.constant;

import com.google.dart.engine.EngineTestCase;

import java.util.ArrayList;

public class DirectedGraphTest extends EngineTestCase {
  /**
   * Instances of the class {@code Node} represent simple nodes used for testing purposes.
   */
  private static class Node {
  }

  public void fail_findCycle() {
    Node node1 = new Node();
    Node node2 = new Node();
    DirectedGraph<Node> graph = new DirectedGraph<Node>();
    assertNull(graph.findCycle());
    graph.addEdge(node1, node2);
    assertNull(graph.findCycle());
    graph.addEdge(node2, node1);
    assertNotNull(graph.findCycle());
  }

  public void test_addEdge() {
    DirectedGraph<Node> graph = new DirectedGraph<Node>();
    assertTrue(graph.isEmpty());
    graph.addEdge(new Node(), new Node());
    assertFalse(graph.isEmpty());
  }

  public void test_addNode() {
    DirectedGraph<Node> graph = new DirectedGraph<Node>();
    assertTrue(graph.isEmpty());
    graph.addNode(new Node());
    assertFalse(graph.isEmpty());
  }

  public void test_creation() {
    assertNotNull(new DirectedGraph<Node>());
  }

  public void test_getNodeCount() {
    Node node1 = new Node();
    Node node2 = new Node();
    DirectedGraph<Node> graph = new DirectedGraph<Node>();
    assertEquals(0, graph.getNodeCount());
    graph.addNode(node1);
    assertEquals(1, graph.getNodeCount());
    graph.addNode(node2);
    assertEquals(2, graph.getNodeCount());
    graph.removeNode(node1);
    assertEquals(1, graph.getNodeCount());
  }

  public void test_getTails() {
    Node node1 = new Node();
    Node node2 = new Node();
    Node node3 = new Node();
    DirectedGraph<Node> graph = new DirectedGraph<Node>();
    assertSize(0, graph.getTails(node1));
    graph.addEdge(node1, node2);
    assertSize(1, graph.getTails(node1));
    graph.addEdge(node1, node3);
    assertSize(2, graph.getTails(node1));
  }

  public void test_removeAllNodes() {
    Node node1 = new Node();
    Node node2 = new Node();
    ArrayList<Node> nodes = new ArrayList<Node>();
    nodes.add(node1);
    nodes.add(node2);
    DirectedGraph<Node> graph = new DirectedGraph<Node>();
    graph.addEdge(node1, node2);
    graph.addEdge(node2, node1);
    assertFalse(graph.isEmpty());
    graph.removeAllNodes(nodes);
    assertTrue(graph.isEmpty());
  }

  public void test_removeEdge() {
    Node node1 = new Node();
    Node node2 = new Node();
    Node node3 = new Node();
    DirectedGraph<Node> graph = new DirectedGraph<Node>();
    graph.addEdge(node1, node2);
    graph.addEdge(node1, node3);
    assertSize(2, graph.getTails(node1));
    graph.removeEdge(node1, node2);
    assertSize(1, graph.getTails(node1));
  }

  public void test_removeNode() {
    Node node1 = new Node();
    Node node2 = new Node();
    Node node3 = new Node();
    DirectedGraph<Node> graph = new DirectedGraph<Node>();
    graph.addEdge(node1, node2);
    graph.addEdge(node1, node3);
    assertSize(2, graph.getTails(node1));
    graph.removeNode(node2);
    assertSize(1, graph.getTails(node1));
  }

  public void test_removeSink() {
    Node node1 = new Node();
    Node node2 = new Node();
    DirectedGraph<Node> graph = new DirectedGraph<Node>();
    graph.addEdge(node1, node2);
    assertSame(node2, graph.removeSink());
    assertSame(node1, graph.removeSink());
    assertTrue(graph.isEmpty());
  }
}
