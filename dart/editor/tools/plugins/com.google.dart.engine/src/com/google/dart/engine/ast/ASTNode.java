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

import com.google.dart.engine.ast.visitor.ToSourceVisitor;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.utilities.io.PrintStringWriter;

/**
 * The abstract class {@code ASTNode} defines the behavior common to all nodes in the AST structure
 * for a Dart program.
 */
public abstract class ASTNode {
  /**
   * The parent of the node, or {@code null} if the node is the root of an AST structure.
   */
  private ASTNode parent;

  /**
   * Use the given visitor to visit this node.
   * 
   * @param visitor the visitor that will visit this node
   * @return the value returned by the visitor as a result of visiting this node
   */
  public abstract <R> R accept(ASTVisitor<R> visitor);

  /**
   * Return the first token included in this node's source range.
   * 
   * @return the first token included in this node's source range
   */
  public abstract Token getBeginToken();

  /**
   * Return the last token included in this node's source range.
   * 
   * @return the last token included in this node's source range
   */
  public abstract Token getEndToken();

  /**
   * Return the number of characters in the node's source range.
   * 
   * @return the number of characters in the node's source range
   */
  public int getLength() {
    Token endToken = getEndToken();
    return endToken.getOffset() + endToken.getLength() - getBeginToken().getOffset();
  }

  /**
   * Return the offset from the beginning of the file to the first character in the node's source
   * range.
   * 
   * @return the offset from the beginning of the file to the first character in the node's source
   *         range
   */
  public int getOffset() {
    return getBeginToken().getOffset();
  }

  /**
   * Return this node's parent node, or {@code null} if this node is the root of an AST structure.
   * <p>
   * Note that the relationship between an AST node and its parent node may change over the lifetime
   * of a node.
   * 
   * @return the parent of this node, or {@code null} if none
   */
  public final ASTNode getParent() {
    return parent;
  }

  /**
   * Return the node at the root of this node's AST structure. Note that this method's performance
   * is linear with respect to the depth of the node in the AST structure (O(depth)).
   * 
   * @return the node at the root of this node's AST structure
   */
  public final ASTNode getRoot() {
    ASTNode root = this;
    ASTNode parent = getParent();
    while (parent != null) {
      root = parent;
      parent = root.getParent();
    }
    return root;
  }

  /**
   * Return {@code true} if this node is a synthetic node. A synthetic node is a node that was
   * introduced by the parser in order to recover from an error in the code. Synthetic nodes always
   * have a length of zero ({@code 0}).
   * 
   * @return {@code true} if this node is a synthetic node
   */
  public boolean isSynthetic() {
    return false;
  }

  /**
   * Return a textual description of this node in a form approximating valid source. The returned
   * string will not be valid source primarily in the case where the node itself is not well-formed.
   * 
   * @return the source code equivalent of this node
   */
  public String toSource() {
    PrintStringWriter writer = new PrintStringWriter();
    accept(new ToSourceVisitor(writer));
    return writer.toString();
  }

  @Override
  public String toString() {
    return toSource();
  }

  /**
   * Use the given visitor to visit all of the children of this node. The children will be visited
   * in source order.
   * 
   * @param visitor the visitor that will be used to visit the children of this node
   */
  public abstract void visitChildren(ASTVisitor<?> visitor);

  /**
   * Make this node the parent of the given child node.
   * 
   * @param child the node that will become a child of this node
   * @return the node that was made a child of this node
   */
  protected <T extends ASTNode> T becomeParentOf(T child) {
    if (child != null) {
      ASTNode node = child; // Java 7 access rules require a temp of a concrete type.
      node.setParent(this);
    }
    return child;
  }

  /**
   * If the given child is not {@code null}, use the given visitor to visit it.
   * 
   * @param child the child to be visited
   * @param visitor the visitor that will be used to visit the child
   */
  protected void safelyVisitChild(ASTNode child, ASTVisitor<?> visitor) {
    if (child != null) {
      child.accept(visitor);
    }
  }

  /**
   * Set the parent of this node to the given node.
   * 
   * @param newParent the node that is to be made the parent of this node
   */
  private void setParent(ASTNode newParent) {
    parent = newParent;
  }
}
