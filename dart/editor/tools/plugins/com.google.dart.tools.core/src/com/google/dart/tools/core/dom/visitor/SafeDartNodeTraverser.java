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
package com.google.dart.tools.core.dom.visitor;

import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.tools.core.DartCore;

/**
 * Instances of the class <code>SafeDartNodeTraverser</code> implement a {@link ASTVisitor} that
 * will not throw an exception as a result of finding an incorrectly constructed AST structure. In
 * order to catch exceptions, any method that needs to visit the children of a given node should use
 * the method {@link #visitChildren(DartNode)} rather than asking the node to visit its children
 * directly.
 */
public class SafeDartNodeTraverser<R> extends ASTVisitor<R> {
  /*
   * Note that this implementation does not maximize the number of nodes that are visited, even
   * though doing so might be a desirable trait. In order to do so I believe that we would have to
   * copy the logic from each node class' visitChildren method into the corresponding visit method
   * and use this class as a wrapper class rather than as a superclass. Until we know that we need
   * to maximize coverage I don't want to duplicate all of that code.
   */

  @Override
  public R visitNode(DartNode node) {
    visitChildren(node);
    return null;
  }

  /**
   * Visit the children of the given node, catching and ignoring any exceptions thrown while trying
   * to do so.
   * 
   * @param node the node whose children are to be visited
   */
  protected void visitChildren(DartNode node) {
    try {
      node.visitChildren(this);
    } catch (Exception exception) {
      // Ignore the exception and proceed in order to visit the rest of the structure.
      DartCore.logInformation(
          "Exception caught while traversing an AST structure. Please report to the dartc team.",
          exception);
    }
  }
}
