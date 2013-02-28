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
package com.google.dart.engine.ast.visitor;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.element.Element;

/**
 * Instances of the class {@code ElementLocator} locate the {@link Element Dart model element}
 * associated with a given {@link ASTNode AST node}.
 */
public class ElementLocator extends GeneralizingASTVisitor<Void> {

  /**
   * Locate the {@link Element Dart model element} associated with the given {@link ASTNode AST
   * node}.
   * 
   * @param node the node (not {@code null})
   * @return the associated element, or {@code null} if none is found
   */
  public static Element locate(ASTNode node) {
    ElementLocator locator = new ElementLocator();
    node.accept(locator);
    return locator.element;
  }

  /**
   * The found element (or {@code null} in case none is located).
   */
  private Element element;

  /**
   * Clients should use {@link #locate(ASTNode)}.
   */
  private ElementLocator() {
  }

  @Override
  public Void visitIdentifier(Identifier node) {
    element = node.getElement();
    return null;
  }

}
