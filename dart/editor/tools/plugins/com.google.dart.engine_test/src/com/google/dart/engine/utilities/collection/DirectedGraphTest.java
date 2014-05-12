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
package com.google.dart.engine.utilities.collection;

import com.google.dart.engine.EngineTestCase;

import java.util.ArrayList;
import java.util.List;

public class DirectedGraphTest extends EngineTestCase {
  /**
   * Instances of the class {@code Node} represent simple nodes used for testing purposes.
   */
  private static class Node {
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

  public void test_containsPath_noCycles() {
    Node node1 = new Node();
    Node node2 = new Node();
    Node node3 = new Node();
    DirectedGraph<Node> graph = new DirectedGraph<Node>();
    graph.addEdge(node1, node2);
    graph.addEdge(node2, node3);
    assertTrue(graph.containsPath(node1, node1));
    assertTrue(graph.containsPath(node1, node2));
    assertTrue(graph.containsPath(node1, node3));
    assertFalse(graph.containsPath(node2, node1));
    assertTrue(graph.containsPath(node2, node2));
    assertTrue(graph.containsPath(node2, node3));
    assertFalse(graph.containsPath(node3, node1));
    assertFalse(graph.containsPath(node3, node2));
    assertTrue(graph.containsPath(node3, node3));
  }

  public void test_containsPath_withCycles() {
    Node node1 = new Node();
    Node node2 = new Node();
    Node node3 = new Node();
    Node node4 = new Node();
    DirectedGraph<Node> graph = new DirectedGraph<Node>();
    graph.addEdge(node1, node2);
    graph.addEdge(node2, node1);
    graph.addEdge(node1, node3);
    graph.addEdge(node3, node4);
    graph.addEdge(node4, node3);
    assertTrue(graph.containsPath(node1, node1));
    assertTrue(graph.containsPath(node1, node2));
    assertTrue(graph.containsPath(node1, node3));
    assertTrue(graph.containsPath(node1, node4));
    assertTrue(graph.containsPath(node2, node1));
    assertTrue(graph.containsPath(node2, node2));
    assertTrue(graph.containsPath(node2, node3));
    assertTrue(graph.containsPath(node2, node4));
    assertFalse(graph.containsPath(node3, node1));
    assertFalse(graph.containsPath(node3, node2));
    assertTrue(graph.containsPath(node3, node3));
    assertTrue(graph.containsPath(node3, node4));
    assertFalse(graph.containsPath(node4, node1));
    assertFalse(graph.containsPath(node4, node2));
    assertTrue(graph.containsPath(node4, node3));
    assertTrue(graph.containsPath(node4, node4));
  }

  public void test_creation() {
    assertNotNull(new DirectedGraph<Node>());
  }

  public void test_findCycleContaining_complexCycle() {
    // Two overlapping loops: (1, 2, 3) and (3, 4, 5)
    Node node1 = new Node();
    Node node2 = new Node();
    Node node3 = new Node();
    Node node4 = new Node();
    Node node5 = new Node();
    DirectedGraph<Node> graph = new DirectedGraph<Node>();
    graph.addEdge(node1, node2);
    graph.addEdge(node2, node3);
    graph.addEdge(node3, node1);
    graph.addEdge(node3, node4);
    graph.addEdge(node4, node5);
    graph.addEdge(node5, node3);
    List<Node> cycle = graph.findCycleContaining(node1);
    assertSizeOfList(5, cycle);
    assertTrue(cycle.contains(node1));
    assertTrue(cycle.contains(node2));
    assertTrue(cycle.contains(node3));
    assertTrue(cycle.contains(node4));
    assertTrue(cycle.contains(node5));
  }

  public void test_findCycleContaining_cycle() {
    Node node1 = new Node();
    Node node2 = new Node();
    Node node3 = new Node();
    DirectedGraph<Node> graph = new DirectedGraph<Node>();
    graph.addEdge(node1, node2);
    graph.addEdge(node2, node3);
    graph.addEdge(node2, new Node());
    graph.addEdge(node3, node1);
    graph.addEdge(node3, new Node());
    List<Node> cycle = graph.findCycleContaining(node1);
    assertSizeOfList(3, cycle);
    assertTrue(cycle.contains(node1));
    assertTrue(cycle.contains(node2));
    assertTrue(cycle.contains(node3));
  }

  public void test_findCycleContaining_notInGraph() {
    Node node = new Node();
    DirectedGraph<Node> graph = new DirectedGraph<Node>();
    List<Node> cycle = graph.findCycleContaining(node);
    assertSizeOfList(1, cycle);
    assertEquals(node, cycle.get(0));
  }

  public void test_findCycleContaining_null() {
    DirectedGraph<Node> graph = new DirectedGraph<Node>();
    try {
      graph.findCycleContaining(null);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException exception) {
      // Expected
    }
  }

  public void test_findCycleContaining_singleton() {
    Node node1 = new Node();
    Node node2 = new Node();
    Node node3 = new Node();
    DirectedGraph<Node> graph = new DirectedGraph<Node>();
    graph.addEdge(node1, node2);
    graph.addEdge(node2, node3);
    List<Node> cycle = graph.findCycleContaining(node1);
    assertSizeOfList(1, cycle);
    assertEquals(node1, cycle.get(0));
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
    assertSizeOfSet(0, graph.getTails(node1));
    graph.addEdge(node1, node2);
    assertSizeOfSet(1, graph.getTails(node1));
    graph.addEdge(node1, node3);
    assertSizeOfSet(2, graph.getTails(node1));
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
    assertSizeOfSet(2, graph.getTails(node1));
    graph.removeEdge(node1, node2);
    assertSizeOfSet(1, graph.getTails(node1));
  }

  public void test_removeNode() {
    Node node1 = new Node();
    Node node2 = new Node();
    Node node3 = new Node();
    DirectedGraph<Node> graph = new DirectedGraph<Node>();
    graph.addEdge(node1, node2);
    graph.addEdge(node1, node3);
    assertSizeOfSet(2, graph.getTails(node1));
    graph.removeNode(node2);
    assertSizeOfSet(1, graph.getTails(node1));
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

  public void test_topologicalSort_noCycles() {
    Node node1 = new Node();
    Node node2 = new Node();
    Node node3 = new Node();
    DirectedGraph<Node> graph = new DirectedGraph<Node>();
    graph.addEdge(node1, node2);
    graph.addEdge(node1, node3);
    graph.addEdge(node2, node3);
    ArrayList<ArrayList<Node>> topologicalSort = graph.computeTopologicalSort();
    assertSizeOfList(3, topologicalSort);
    assertSizeOfList(1, topologicalSort.get(0));
    assertEquals(node3, topologicalSort.get(0).get(0));
    assertSizeOfList(1, topologicalSort.get(1));
    assertEquals(node2, topologicalSort.get(1).get(0));
    assertSizeOfList(1, topologicalSort.get(2));
    assertEquals(node1, topologicalSort.get(2).get(0));
  }

  public void test_topologicalSort_withCycles() {
    Node node1 = new Node();
    Node node2 = new Node();
    Node node3 = new Node();
    Node node4 = new Node();
    DirectedGraph<Node> graph = new DirectedGraph<Node>();
    graph.addEdge(node1, node2);
    graph.addEdge(node2, node1);
    graph.addEdge(node1, node3);
    graph.addEdge(node3, node4);
    graph.addEdge(node4, node3);
    ArrayList<ArrayList<Node>> topologicalSort = graph.computeTopologicalSort();
    assertSizeOfList(2, topologicalSort);
    assertContains(topologicalSort.get(0).toArray(), node3, node4);
    assertContains(topologicalSort.get(1).toArray(), node1, node2);
  }
}
