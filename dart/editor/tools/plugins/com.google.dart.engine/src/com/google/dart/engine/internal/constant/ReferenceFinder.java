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

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SuperConstructorInvocation;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.utilities.collection.DirectedGraph;

import java.util.HashMap;

/**
 * Instances of the class {@code ReferenceFinder} add reference information for a given variable to
 * the bi-directional mapping used to order the evaluation of constants.
 */
public class ReferenceFinder extends RecursiveAstVisitor<Void> {
  /**
   * The element representing the construct that will be visited.
   */
  private AstNode source;

  /**
   * A graph in which the nodes are the constant variables and the edges are from each variable to
   * the other constant variables that are referenced in the head's initializer.
   */
  private DirectedGraph<AstNode> referenceGraph;

  /**
   * A table mapping constant variables to the declarations of those variables.
   */
  private HashMap<VariableElement, VariableDeclaration> variableDeclarationMap;

  /**
   * A table mapping constant constructors to the declarations of those constructors.
   */
  private HashMap<ConstructorElement, ConstructorDeclaration> constructorDeclarationMap;

  /**
   * Initialize a newly created reference finder to find references from the given variable to other
   * variables and to add those references to the given graph.
   * 
   * @param source the element representing the variable whose initializer will be visited
   * @param referenceGraph a graph recording which variables (heads) reference which other variables
   *          (tails) in their initializers
   * @param variableDeclarationMap A table mapping constant variables to the declarations of those
   *          variables.
   * @param constructorDeclarationMap A table mapping constant constructors to the declarations of
   *          those constructors.
   */
  public ReferenceFinder(AstNode source, DirectedGraph<AstNode> referenceGraph,
      HashMap<VariableElement, VariableDeclaration> variableDeclarationMap,
      HashMap<ConstructorElement, ConstructorDeclaration> constructorDeclarationMap) {
    this.source = source;
    this.referenceGraph = referenceGraph;
    this.variableDeclarationMap = variableDeclarationMap;
    this.constructorDeclarationMap = constructorDeclarationMap;
  }

  @Override
  public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
    if (node.isConst()) {
      referenceGraph.addEdge(source, node);
    }
    return null;
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
        VariableDeclaration variableDeclaration = variableDeclarationMap.get(variable);
        // The declaration will be null when the variable is not defined in the compilation units
        // that were used to produce the variableDeclarationMap.  In such cases, the variable should
        // already have a value associated with it, but we don't bother to check because there's
        // nothing we can do about it at this point.
        if (variableDeclaration != null) {
          referenceGraph.addEdge(source, variableDeclaration);
        }
      }
    }
    return null;
  }

  @Override
  public Void visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    super.visitSuperConstructorInvocation(node);
    ConstructorElement constructor = node.getStaticElement();
    if (constructor != null && constructor.isConst()) {
      ConstructorDeclaration constructorDeclaration = constructorDeclarationMap.get(constructor);
      // The declaration will be null when the constructor is not defined in the compilation
      // units that were used to produce the constructorDeclarationMap.  In such cases, the
      // constructor should already have its initializer AST's stored in it, but we don't bother
      // to check because there's nothing we can do about it at this point.
      if (constructorDeclaration != null) {
        referenceGraph.addEdge(source, constructorDeclaration);
      }
    }
    return null;
  }
}
