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

import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.utilities.collection.DirectedGraph;

/**
 * Instances of the class {@code ReferenceFinder} add reference information for a given variable to
 * the bi-directional mapping used to order the evaluation of constants.
 */
public class ReferenceFinder extends RecursiveAstVisitor<Void> {
  /**
   * The element representing the variable whose initializer will be visited.
   */
  private VariableElement source;

  /**
   * A graph in which the nodes are the constant variables and the edges are from each variable to
   * the other constant variables that are referenced in the head's initializer.
   */
  private DirectedGraph<VariableElement> referenceGraph;

  /**
   * Initialize a newly created reference finder to find references from the given variable to other
   * variables and to add those references to the given graph.
   * 
   * @param source the element representing the variable whose initializer will be visited
   * @param referenceGraph a graph recording which variables (heads) reference which other variables
   *          (tails) in their initializers
   */
  public ReferenceFinder(VariableElement source, DirectedGraph<VariableElement> referenceGraph) {
    this.source = source;
    this.referenceGraph = referenceGraph;
  }

  @Override
  public Void visitSimpleIdentifier(SimpleIdentifier node) {
    Element element = node.getStaticElement();
    if (element instanceof PropertyAccessorElement) {
      element = ((PropertyAccessorElement) element).getVariable();
    }
    if (element instanceof VariableElement) {
      VariableElement variable = (VariableElement) element;
      if (variable.isConst()) {
        referenceGraph.addEdge(source, variable);
      }
    }
    return null;
  }
}
