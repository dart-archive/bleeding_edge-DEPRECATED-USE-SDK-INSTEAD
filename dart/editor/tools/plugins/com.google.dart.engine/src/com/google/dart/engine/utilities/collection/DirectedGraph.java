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

import com.google.dart.engine.utilities.translation.DartBlockBody;

import java.util.ArrayList;
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
   * Instances of the class {@code NodeInfo} are used by the {@link SccFinder} to maintain
   * information about the nodes that have been examined.
   * 
   * @param N the type of the nodes corresponding to the entries
   */
  private static class NodeInfo<N> {
    /**
     * The depth of this node.
     */
    public int index;

    /**
     * The depth of the first node in a cycle.
     */
    public int lowlink;

    /**
     * A flag indicating whether the corresponding node is on the stack. Used to remove the need for
     * searching a collection for the node each time the question needs to be asked.
     */
    public boolean onStack;

    /**
     * The component that contains the corresponding node.
     */
    public ArrayList<N> component;

    /**
     * Initialize a newly created information holder to represent a node at the given depth.
     * 
     * @param depth the depth of the node being represented
     */
    public NodeInfo(int depth) {
      index = depth;
      lowlink = depth;
      onStack = false;
    }
  }

  /**
   * Instances of the class {@code SccFinder} implement Tarjan's Algorithm for finding the strongly
   * connected components in a graph.
   */
  private static class SccFinder<N> {
    /**
     * The graph to work with.
     */
    private DirectedGraph<N> graph;

    /**
     * The index used to uniquely identify the depth of nodes.
     */
    private int index = 0;

    /**
     * The stack of nodes that are being visited in order to identify components.
     */
    private ArrayList<N> stack = new ArrayList<N>();

    /**
     * A table mapping nodes to information about the nodes that is used by this algorithm.
     */
    private HashMap<N, NodeInfo<N>> nodeMap = new HashMap<N, NodeInfo<N>>();

    /**
     * Initialize a newly created finder.
     */
    public SccFinder(DirectedGraph<N> graph) {
      super();
      this.graph = graph;
    }

//    public HashSet<ArrayList<N>> allComponents() {
//      for (N node : edges.keySet()) {
//        NodeInfo<N> nodeInfo = nodeMap.get(node);
//        if (nodeInfo == null) {
//          strongConnect(node);
//        }
//      }
//      HashSet<ArrayList<N>> components = new HashSet<ArrayList<N>>();
//      for (NodeInfo<N> info : nodeMap.values()) {
//        components.add(info.component);
//      }
//      return components;
//    }

    /**
     * Return a list containing the nodes that are part of the strongly connected component that
     * contains the given node.
     * 
     * @param node the node used to identify the strongly connected component to be returned
     * @return the nodes that are part of the strongly connected component that contains the given
     *         node
     */
    public ArrayList<N> componentContaining(N node) {
      return strongConnect(node).component;
    }

    /**
     * Remove and return the top-most element from the stack.
     * 
     * @return the element that was removed
     */
    private N pop() {
      N node = stack.remove(stack.size() - 1);
      nodeMap.get(node).onStack = false;
      return node;
    }

    /**
     * Add the given node to the stack.
     * 
     * @param node the node to be added to the stack
     */
    private void push(N node) {
      nodeMap.get(node).onStack = true;
      stack.add(node);
    }

    /**
     * Compute the strongly connected component that contains the given node as well as any
     * components containing nodes that are reachable from the given component.
     * 
     * @param v the node from which the search will begin
     * @return the information about the given node
     */
    private NodeInfo<N> strongConnect(N v) {
      //
      // Set the depth index for v to the smallest unused index
      //
      NodeInfo<N> vInfo = new NodeInfo<N>(index++);
      nodeMap.put(v, vInfo);
      push(v);
      //
      // Consider successors of v
      //
      HashSet<N> tails = graph.edges.get(v);
      if (tails != null) {
        for (N w : tails) {
          NodeInfo<N> wInfo = nodeMap.get(w);
          if (wInfo == null) {
            // Successor w has not yet been visited; recurse on it
            wInfo = strongConnect(w);
            vInfo.lowlink = Math.min(vInfo.lowlink, wInfo.lowlink);
          } else if (wInfo.onStack) {
            // Successor w is in stack S and hence in the current SCC
            vInfo.lowlink = Math.min(vInfo.lowlink, wInfo.index);
          }
        }
      }
      //
      // If v is a root node, pop the stack and generate an SCC
      //
      if (vInfo.lowlink == vInfo.index) {
        ArrayList<N> component = new ArrayList<N>();
        N w;
        do {
          w = pop();
          component.add(w);
          nodeMap.get(w).component = component;
        } while (w != v);
      }
      return vInfo;
    }
  }

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
    if (edges.get(tail) == null) {
      edges.put(tail, new HashSet<N>());
    }
    //
    // Then create the edge.
    //
    HashSet<N> tails = edges.get(head);
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
   * Return a list of nodes that form a cycle containing the given node. If the node is not part of
   * this graph, then a list containing only the node itself will be returned.
   * 
   * @return a list of nodes that form a cycle containing the given node
   */
  public List<N> findCycleContaining(N node) {
    if (node == null) {
      throw new IllegalArgumentException();
    }
    SccFinder<N> finder = new SccFinder<N>(this);
    return finder.componentContaining(node);
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
  @DartBlockBody({//
  "for (N key in _edges.keys) {",//
      "  if (_edges[key].isEmpty) return key;",//
      "}",//
      "return null;"})
  private N findSink() {
    for (Map.Entry<N, HashSet<N>> entry : edges.entrySet()) {
      if (entry.getValue().isEmpty()) {
        return entry.getKey();
      }
    }
    return null;
  }
}
