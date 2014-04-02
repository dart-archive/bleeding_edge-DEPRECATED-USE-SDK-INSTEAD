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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * The abstract class {@code AstNode} defines the behavior common to all nodes in the AST structure
 * for a Dart program.
 * 
 * @coverage dart.engine.ast
 */
public abstract class AstNode {
  /**
   * An empty array of ast nodes.
   */
  public static final AstNode[] EMPTY_ARRAY = new AstNode[0];

  /**
   * The parent of the node, or {@code null} if the node is the root of an AST structure.
   */
  private AstNode parent;

  /**
   * A table mapping the names of properties to their values, or {@code null} if this node does not
   * have any properties associated with it.
   */
  private Map<String, Object> propertyMap;

  /**
   * A comparator that can be used to sort AST nodes in lexical order. In other words,
   * {@code compare} will return a negative value if the offset of the first node is less than the
   * offset of the second node, zero (0) if the nodes have the same offset, and a positive value if
   * if the offset of the first node is greater than the offset of the second node.
   */
  public static final Comparator<AstNode> LEXICAL_ORDER = new Comparator<AstNode>() {
    @Override
    public int compare(AstNode first, AstNode second) {
      return second.getOffset() - first.getOffset();
    }
  };

  /**
   * Use the given visitor to visit this node.
   * 
   * @param visitor the visitor that will visit this node
   * @return the value returned by the visitor as a result of visiting this node
   */
  public abstract <R> R accept(AstVisitor<R> visitor);

  /**
   * Return the node of the given class that most immediately encloses this node, or {@code null} if
   * there is no enclosing node of the given class.
   * 
   * @param nodeClass the class of the node to be returned
   * @return the node of the given type that encloses this node
   */
  @SuppressWarnings("unchecked")
  public <E extends AstNode> E getAncestor(Class<E> enclosingClass) {
    AstNode node = this;
    while (node != null && !enclosingClass.isInstance(node)) {
      node = node.getParent();
    }
    return (E) node;
  }

  /**
   * Return the first token included in this node's source range.
   * 
   * @return the first token included in this node's source range
   */
  public abstract Token getBeginToken();

  /**
   * Return the offset of the character immediately following the last character of this node's
   * source range. This is equivalent to {@code node.getOffset() + node.getLength()}. For a
   * compilation unit this will be equal to the length of the unit's source. For synthetic nodes
   * this will be equivalent to the node's offset (because the length is zero (0) by definition).
   * 
   * @return the offset of the character just past the node's source range
   */
  public int getEnd() {
    return getOffset() + getLength();
  }

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
    Token beginToken = getBeginToken();
    Token endToken = getEndToken();
    if (beginToken == null || endToken == null) {
      return -1;
    }
    return endToken.getOffset() + endToken.getLength() - beginToken.getOffset();
  }

  /**
   * Return the offset from the beginning of the file to the first character in the node's source
   * range.
   * 
   * @return the offset from the beginning of the file to the first character in the node's source
   *         range
   */
  public int getOffset() {
    Token beginToken = getBeginToken();
    if (beginToken == null) {
      return -1;
    }
    return beginToken.getOffset();
  }

  /**
   * Return this node's parent node, or {@code null} if this node is the root of an AST structure.
   * <p>
   * Note that the relationship between an AST node and its parent node may change over the lifetime
   * of a node.
   * 
   * @return the parent of this node, or {@code null} if none
   */
  public AstNode getParent() {
    return parent;
  }

  /**
   * Return the value of the property with the given name, or {@code null} if this node does not
   * have a property with the given name.
   * 
   * @return the value of the property with the given name
   */
  public Object getProperty(String propertyName) {
    if (propertyMap == null) {
      return null;
    }
    return propertyMap.get(propertyName);
  }

  /**
   * Return the node at the root of this node's AST structure. Note that this method's performance
   * is linear with respect to the depth of the node in the AST structure (O(depth)).
   * 
   * @return the node at the root of this node's AST structure
   */
  public final AstNode getRoot() {
    AstNode root = this;
    AstNode parent = getParent();
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
   * Set the value of the property with the given name to the given value. If the value is
   * {@code null}, the property will effectively be removed.
   * 
   * @param propertyName the name of the property whose value is to be set
   * @param propertyValue the new value of the property
   */
  public void setProperty(String propertyName, Object propertyValue) {
    if (propertyValue == null) {
      if (propertyMap != null) {
        propertyMap.remove(propertyName);
        if (propertyMap.isEmpty()) {
          propertyMap = null;
        }
      }
    } else {
      if (propertyMap == null) {
        propertyMap = new HashMap<String, Object>();
      }
      propertyMap.put(propertyName, propertyValue);
    }
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
  public abstract void visitChildren(AstVisitor<?> visitor);

  /**
   * Make this node the parent of the given child node.
   * 
   * @param child the node that will become a child of this node
   * @return the node that was made a child of this node
   */
  protected <T extends AstNode> T becomeParentOf(T child) {
    if (child != null) {
      AstNode node = child; // Java 7 access rules require a temp of a concrete type.
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
  protected void safelyVisitChild(AstNode child, AstVisitor<?> visitor) {
    if (child != null) {
      child.accept(visitor);
    }
  }

  /**
   * Set the parent of this node to the given node.
   * 
   * @param newParent the node that is to be made the parent of this node
   */
  private void setParent(AstNode newParent) {
    parent = newParent;
  }
}
