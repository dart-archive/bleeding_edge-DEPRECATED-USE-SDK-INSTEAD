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

import java.util.LinkedList;

/**
 * Instances of the class {@code BreadthFirstVisitor} implement an AST visitor that will recursively
 * visit all of the nodes in an AST structure, similar to {@link GeneralizingASTVisitor}. This
 * visitor uses a breadth-first ordering rather than the depth-first ordering of
 * {@link GeneralizingASTVisitor}.
 * 
 * @coverage dart.engine.ast
 */
public class BreadthFirstVisitor<R> extends GeneralizingASTVisitor<R> {
  private final LinkedList<ASTNode> queue = new LinkedList<ASTNode>();

  private GeneralizingASTVisitor<Void> childVisitor = new GeneralizingASTVisitor<Void>() {
    @Override
    public Void visitNode(ASTNode node) {
      queue.add(node);
      return null;
    }
  };

  /**
   * Visit all nodes in the tree starting at the given {@code root} node, in depth-first order.
   * 
   * @param root the root of the ASTNode tree
   */
  public void visitAllNodes(ASTNode root) {
    queue.add(root);
    while (!queue.isEmpty()) {
      ASTNode next = queue.removeFirst();
      next.accept(this);
    }
  }

  @Override
  public R visitNode(ASTNode node) {
    node.visitChildren(childVisitor);
    return null;
  }
}
