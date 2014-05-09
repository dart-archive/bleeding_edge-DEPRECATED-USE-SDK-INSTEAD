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

import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.VariableElement;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Instances of the class {@code ConstantFinder} are used to traverse the AST structures of all of
 * the compilation units being resolved and build a table mapping constant variable elements to the
 * declarations of those variables.
 */
public class ConstantFinder extends RecursiveAstVisitor<Void> {
  /**
   * A table mapping constant variable elements to the declarations of those variables.
   */
  private HashMap<VariableElement, VariableDeclaration> variableMap = new HashMap<VariableElement, VariableDeclaration>();

  /**
   * A table mapping constant constructors to the declarations of those constructors.
   */
  private HashMap<ConstructorElement, ConstructorDeclaration> constructorMap = new HashMap<ConstructorElement, ConstructorDeclaration>();

  /**
   * A collection of constant constructor invocations.
   */
  private ArrayList<InstanceCreationExpression> constructorInvocations = new ArrayList<InstanceCreationExpression>();

  /**
   * Initialize a newly created constant finder.
   */
  public ConstantFinder() {
    super();
  }

  /**
   * Return a collection of constant constructor invocations.
   */
  public ArrayList<InstanceCreationExpression> getConstructorInvocations() {
    return constructorInvocations;
  }

  /**
   * Return a table mapping constant constructors to the declarations of those constructors.
   */
  public HashMap<ConstructorElement, ConstructorDeclaration> getConstructorMap() {
    return constructorMap;
  }

  /**
   * Return a table mapping constant variable elements to the declarations of those variables.
   * 
   * @return a table mapping constant variable elements to the declarations of those variables
   */
  public HashMap<VariableElement, VariableDeclaration> getVariableMap() {
    return variableMap;
  }

  @Override
  public Void visitConstructorDeclaration(ConstructorDeclaration node) {
    super.visitConstructorDeclaration(node);
    if (node.getConstKeyword() != null) {
      ConstructorElement element = node.getElement();
      if (element != null) {
        constructorMap.put(element, node);
      }
    }
    return null;
  }

  @Override
  public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
    super.visitInstanceCreationExpression(node);
    if (node.isConst()) {
      constructorInvocations.add(node);
    }
    return null;
  }

  @Override
  public Void visitVariableDeclaration(VariableDeclaration node) {
    super.visitVariableDeclaration(node);
    Expression initializer = node.getInitializer();
    if (initializer != null && node.isConst()) {
      VariableElement element = node.getElement();
      if (element != null) {
        variableMap.put(element, node);
      }
    }
    return null;
  }
}
