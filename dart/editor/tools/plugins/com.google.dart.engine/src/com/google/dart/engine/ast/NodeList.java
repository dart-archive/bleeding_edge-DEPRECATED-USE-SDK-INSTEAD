/*
 * Copyright 2012, the Dart project authors.
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Instances of the class <code>NodeList</code> represent a list of AST nodes that have a common
 * parent.
 */
public class NodeList<E extends ASTNode> implements Iterable<E> {
  /**
   * The node that is the parent of each of the elements in the list.
   */
  private ASTNode owner;

  /**
   * The elements of the list.
   */
  private List<E> elements = new ArrayList<E>();

  /**
   * Initialize a newly created list of nodes to be empty.
   * 
   * @param owner the node that is the parent of each of the elements in the list
   */
  public NodeList(ASTNode owner) {
    this.owner = owner;
  }

  /**
   * Use the given visitor to visit each of the nodes in this list.
   * 
   * @param visitor the visitor to be used to visit the elements of this list
   */
  public void accept(ASTVisitor<?> visitor) {
    for (E element : elements) {
      element.accept(visitor);
    }
  }

  /**
   * Add the given node to this list.
   * 
   * @param node the node to be added
   */
  public void add(E node) {
    owner.becomeParentOf(node);
    elements.add(node);
  }

  /**
   * Add all of the nodes in the given list to this list.
   * 
   * @param nodes the list of nodes to be added
   */
  public void addAll(List<E> nodes) {
    if (nodes != null) {
      for (E node : nodes) {
        owner.becomeParentOf(node);
        elements.add(node);
      }
    }
  }

  /**
   * Return the node that is the parent of each of the elements in the list.
   * 
   * @return the node that is the parent of each of the elements in the list
   */
  public ASTNode getOwner() {
    return owner;
  }

  @Override
  public Iterator<E> iterator() {
    return elements.iterator();
  }

  /**
   * Remove the given node from this list.
   * 
   * @param node the node to be removed
   */
  public void remove(E node) {
    elements.remove(node);
  }
}
