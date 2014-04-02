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

import com.google.dart.engine.ast.AstNode;

import java.util.LinkedList;

/**
 * Instances of the class {@code BreadthFirstVisitor} implement an AST visitor that will recursively
 * visit all of the nodes in an AST structure, similar to {@link GeneralizingAstVisitor}. This
 * visitor uses a breadth-first ordering rather than the depth-first ordering of
 * {@link GeneralizingAstVisitor}.
 * <p>
 * Subclasses that override a visit method must either invoke the overridden visit method or
 * explicitly invoke the more general visit method. Failure to do so will cause the visit methods
 * for superclasses of the node to not be invoked and will cause the children of the visited node to
 * not be visited.
 * <p>
 * In addition, subclasses should <b>not</b> explicitly visit the children of a node, but should
 * ensure that the method {@link #visitNode(AstNode)} is used to visit the children (either directly
 * or indirectly). Failure to do will break the order in which nodes are visited.
 * 
 * @coverage dart.engine.ast
 */
public class BreadthFirstVisitor<R> extends GeneralizingAstVisitor<R> {
  /**
   * A queue holding the nodes that have not yet been visited in the order in which they ought to be
   * visited.
   */
  private final LinkedList<AstNode> queue = new LinkedList<AstNode>();

  /**
   * A visitor, used to visit the children of the current node, that will add the nodes it visits to
   * the {@link #queue}.
   */
  private GeneralizingAstVisitor<Void> childVisitor = new GeneralizingAstVisitor<Void>() {
    @Override
    public Void visitNode(AstNode node) {
      queue.add(node);
      return null;
    }
  };

  /**
   * Visit all nodes in the tree starting at the given {@code root} node, in breadth-first order.
   * 
   * @param root the root of the AST structure to be visited
   */
  public void visitAllNodes(AstNode root) {
    queue.add(root);
    while (!queue.isEmpty()) {
      AstNode next = queue.removeFirst();
      next.accept(this);
    }
  }

  @Override
  public R visitNode(AstNode node) {
    node.visitChildren(childVisitor);
    return null;
  }
}
