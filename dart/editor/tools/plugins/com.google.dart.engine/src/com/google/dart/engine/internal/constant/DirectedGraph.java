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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Instances of the class {@code DirectedGraph} implement a directed graph in which the nodes are
 * arbitrary (client provided) objects and edges are represented implicitly. The graph will allow an
 * edge from any node to any other node, including itself, but will not represent multiple edges
 * between the same pair of nodes.
 * 
 * @param N the type of the nodes in the graph
 */
public class DirectedGraph<N> {
  /**
   * The table encoding the edges in the graph. An edge is represented by an entry mapping the head
   * to a set of tails. Nodes that are not the head of any edge are represented by an entry mapping
   * the node to an empty set of tails.
   */
  private HashMap<N, HashSet<N>> edges = new HashMap<N, HashSet<N>>();

  /**
   * Initialize a newly create directed graph to be empty.
   */
  public DirectedGraph() {
    super();
  }

  /**
   * Add an edge from the given head node to the given tail node. Both nodes will be a part of the
   * graph after this method is invoked, whether or not they were before.
   * 
   * @param head the node at the head of the edge
   * @param tail the node at the tail of the edge
   */
  public void addEdge(N head, N tail) {
    //
    // First, ensure that the tail is a node known to the graph.
    //
    HashSet<N> tails = edges.get(tail);
    if (tails == null) {
      edges.put(tail, new HashSet<N>());
    }
    //
    // Then create the edge.
    //
    tails = edges.get(head);
    if (tails == null) {
      tails = new HashSet<N>();
      edges.put(head, tails);
    }
    tails.add(tail);
  }

  /**
   * Add the given node to the set of nodes in the graph.
   * 
   * @param node the node to be added
   */
  public void addNode(N node) {
    HashSet<N> tails = edges.get(node);
    if (tails == null) {
      edges.put(node, new HashSet<N>());
    }
  }

  /**
   * Return a list of nodes that form a cycle, or {@code null} if there are no cycles in this graph.
   * 
   * @return a list of nodes that form a cycle
   */
  public List<N> findCycle() {
    // TODO(brianwilkerson) Implement this.
    return null;
  }

  /**
   * Return the number of nodes in this graph.
   * 
   * @return the number of nodes in this graph
   */
  public int getNodeCount() {
    return edges.size();
  }

  /**
   * Return a set containing the tails of edges that have the given node as their head. The set will
   * be empty if there are no such edges or if the node is not part of the graph. Clients must not
   * modify the returned set.
   * 
   * @param head the node at the head of all of the edges whose tails are to be returned
   * @return a set containing the tails of edges that have the given node as their head
   */
  public Set<N> getTails(N head) {
    HashSet<N> tails = edges.get(head);
    if (tails == null) {
      return new HashSet<N>();
    }
    return tails;
  }

  /**
   * Return {@code true} if this graph is empty.
   * 
   * @return {@code true} if this graph is empty
   */
  public boolean isEmpty() {
    return edges.isEmpty();
  }

  /**
   * Remove all of the given nodes from this graph. As a consequence, any edges for which those
   * nodes were either a head or a tail will also be removed.
   * 
   * @param nodes the nodes to be removed
   */
  public void removeAllNodes(List<N> nodes) {
    for (N node : nodes) {
      removeNode(node);
    }
  }

  /**
   * Remove the edge from the given head node to the given tail node. If there was no such edge then
   * the graph will be unmodified: the number of edges will be the same and the set of nodes will be
   * the same (neither node will either be added or removed).
   * 
   * @param head the node at the head of the edge
   * @param tail the node at the tail of the edge
   * @return {@code true} if the graph was modified as a result of this operation
   */
  public void removeEdge(N head, N tail) {
    HashSet<N> tails = edges.get(head);
    if (tails != null) {
      tails.remove(tail);
    }
  }

  /**
   * Remove the given node from this graph. As a consequence, any edges for which that node was
   * either a head or a tail will also be removed.
   * 
   * @param node the node to be removed
   */
  public void removeNode(N node) {
    edges.remove(node);
    for (HashSet<N> tails : edges.values()) {
      tails.remove(node);
    }
  }

  /**
   * Find one node (referred to as a sink node) that has no outgoing edges (that is, for which there
   * are no edges that have that node as the head of the edge) and remove it from this graph. Return
   * the node that was removed, or {@code null} if there are no such nodes either because the graph
   * is empty or because every node in the graph has at least one outgoing edge. As a consequence of
   * removing the node from the graph any edges for which that node was a tail will also be removed.
   * 
   * @return the sink node that was removed
   */
  public N removeSink() {
    N sink = findSink();
    if (sink == null) {
      return null;
    }
    removeNode(sink);
    return sink;
  }

  /**
   * Return one node that has no outgoing edges (that is, for which there are no edges that have
   * that node as the head of the edge), or {@code null} if there are no such nodes.
   * 
   * @return a sink node
   */
  private N findSink() {
    for (Map.Entry<N, HashSet<N>> entry : edges.entrySet()) {
      if (entry.getValue().isEmpty()) {
        return entry.getKey();
      }
    }
    return null;
  }
}
