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
package com.google.dart.engine.html.ast;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.html.ast.visitor.ToSourceVisitor;
import com.google.dart.engine.html.ast.visitor.XmlVisitor;
import com.google.dart.engine.html.scanner.Token;
import com.google.dart.engine.utilities.dart.ParameterKind;
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.engine.utilities.translation.DartOmit;
import com.google.dart.engine.utilities.translation.DartOptional;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The abstract class {@code XmlNode} defines behavior common to all XML/HTML nodes.
 * 
 * @coverage dart.engine.html
 */
public abstract class XmlNode {
  /**
   * The parent of the node, or {@code null} if the node is the root of an AST structure.
   */
  private XmlNode parent;

  /**
   * The element associated with this node or {@code null} if the receiver is not resolved.
   */
  private Element element;

  /**
   * Use the given visitor to visit this node.
   * 
   * @param visitor the visitor that will visit this node
   * @return the value returned by the visitor as a result of visiting this node
   */
  public abstract <R> R accept(XmlVisitor<R> visitor);

  /**
   * Return the first token included in this node's source range.
   * 
   * @return the first token or {@code null} if none
   */
  public abstract Token getBeginToken();

  /**
   * Return the element associated with this node.
   * 
   * @return the element or {@code null} if the receiver is not resolved
   */
  public Element getElement() {
    return element;
  }

  /**
   * Return the offset of the character immediately following the last character of this node's
   * source range. This is equivalent to {@code node.getOffset() + node.getLength()}. For an html
   * unit this will be equal to the length of the unit's source.
   * 
   * @return the offset of the character just past the node's source range
   */
  public int getEnd() {
    return getOffset() + getLength();
  }

  /**
   * Return the last token included in this node's source range.
   * 
   * @return the last token or {@code null} if none
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
  public XmlNode getParent() {
    return parent;
  }

  /**
   * Set the element associated with this node.
   * 
   * @param element the element
   */
  public void setElement(Element element) {
    this.element = element;
  }

  @Override
  public String toString() {
    PrintStringWriter writer = new PrintStringWriter();
    accept(new ToSourceVisitor(writer));
    return writer.toString();
  }

  /**
   * Use the given visitor to visit all of the children of this node. The children will be visited
   * in source order.
   * 
   * @param visitor the visitor that will be used to visit the children of this node
   */
  public abstract void visitChildren(XmlVisitor<?> visitor);

  /**
   * Make this node the parent of the given child node.
   * 
   * @param child the node that will become a child of this node
   * @return the node that was made a child of this node
   */
  protected <T extends XmlNode> T becomeParentOf(T child) {
    if (child != null) {
      XmlNode node = child; // Java 7 access rules require a temp of a concrete type.
      node.setParent(this);
    }
    return child;
  }

  /**
   * Make this node the parent of the given child nodes.
   * 
   * @param children the nodes that will become the children of this node
   * @return the nodes that were made children of this node
   */
  @DartOmit
  protected final <T extends XmlNode> List<T> becomeParentOfAll(List<T> children) {
    return becomeParentOfAll(children, null);
  }

  /**
   * Make this node the parent of the given child nodes.
   * 
   * @param children the nodes that will become the children of this node
   * @param ifEmpty the (empty) nodes to return if "children" is empty
   * @return the nodes that were made children of this node
   */
  protected final <T extends XmlNode> List<T> becomeParentOfAll(List<T> children,
      @DartOptional(kind = ParameterKind.NAMED) List<T> ifEmpty) {
    if (children == null || children.isEmpty()) {
      if (ifEmpty != null) {
        return ifEmpty;
      }
    }
    if (children != null) {
      for (Iterator<T> iter = children.iterator(); iter.hasNext();) {
        XmlNode node = iter.next(); // Java 7 access rules require a temp of a concrete type.
        node.setParent(this);
      }
      // This will create ArrayList for exactly given number of elements.
      return new ArrayList<T>(children);
    }
    return children;
  }

  /**
   * This method exists for debugging purposes only.
   */
  private void appendIdentifier(StringBuilder builder, XmlNode node) {
    if (node instanceof XmlTagNode) {
      builder.append(((XmlTagNode) node).getTag());
    } else if (node instanceof XmlAttributeNode) {
      builder.append(((XmlAttributeNode) node).getName());
    } else {
      builder.append("htmlUnit");
    }
  }

  /**
   * This method exists for debugging purposes only.
   */
  private String buildRecursiveStructureMessage(XmlNode newParent) {
    StringBuilder builder = new StringBuilder();
    builder.append("Attempt to create recursive structure: ");
    XmlNode current = newParent;
    while (current != null) {
      if (current != newParent) {
        builder.append(" -> ");
      }
      if (current == this) {
        builder.append('*');
        appendIdentifier(builder, current);
        builder.append('*');
      } else {
        appendIdentifier(builder, current);
      }
      current = current.getParent();
    }
    return builder.toString();
  }

  /**
   * Set the parent of this node to the given node.
   * 
   * @param newParent the node that is to be made the parent of this node
   */
  private void setParent(XmlNode newParent) {
    XmlNode current = newParent;
    while (current != null) {
      if (current == this) {
        AnalysisEngine.getInstance().getLogger().logError(
            "Circular structure while setting an XML node's parent",
            new IllegalArgumentException(buildRecursiveStructureMessage(newParent)));
        return;
      }
      current = current.getParent();
    }
    parent = newParent;
  }
}
