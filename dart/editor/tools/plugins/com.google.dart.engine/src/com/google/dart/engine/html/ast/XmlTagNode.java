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

import com.google.dart.engine.html.ast.visitor.XmlVisitor;
import com.google.dart.engine.html.scanner.Token;
import com.google.dart.engine.html.scanner.TokenType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Instances of {@code XmlTagNode} represent XML or HTML elements such as {@code <p>} and
 * {@code <body foo="bar"> ... </body>}.
 * 
 * @coverage dart.engine.html
 */
public class XmlTagNode extends XmlNode {
  /**
   * Constant representing empty list of attributes.
   */
  public static final List<XmlAttributeNode> NO_ATTRIBUTES = Collections.unmodifiableList(new ArrayList<XmlAttributeNode>());

  /**
   * Constant representing empty list of tag nodes.
   */
  public static final List<XmlTagNode> NO_TAG_NODES = Collections.unmodifiableList(new ArrayList<XmlTagNode>());

  /**
   * The starting {@link TokenType#LT} token (not {@code null}).
   */
  private final Token nodeStart;

  /**
   * The {@link TokenType#TAG} token after the starting '&lt;' (not {@code null}).
   */
  private final Token tag;

  /**
   * The attributes contained by the receiver (not {@code null}, contains no {@code null}s).
   */
  private List<XmlAttributeNode> attributes;

  /**
   * The {@link TokenType#GT} or {@link TokenType#SLASH_GT} token after the attributes (not
   * {@code null}). The token may be the same token as {@link #nodeEnd} if there are no child
   * {@link #tagNodes}.
   */
  private final Token attributeEnd;

  /**
   * The tag nodes contained in the receiver (not {@code null}, contains no {@code null}s).
   */
  private final List<XmlTagNode> tagNodes;

  /**
   * The token (not {@code null}) after the content, which may be
   * <ul>
   * <li>(1) {@link TokenType#LT_SLASH} for nodes with open and close tags, or</li>
   * <li>(2) the {@link TokenType#LT} nodeStart of the next sibling node if this node is self
   * closing or the attributeEnd is {@link TokenType#SLASH_GT}, or</li>
   * <li>(3) {@link TokenType#EOF} if the node does not have a closing tag and is the last node in
   * the stream {@link TokenType#LT_SLASH} token after the content, or {@code null} if there is no
   * content and the attributes ended with {@link TokenType#SLASH_GT}.</li>
   * </ul>
   */
  private final Token contentEnd;

  /**
   * The closing {@link TokenType#TAG} after the child elements or {@code null} if there is no
   * content and the attributes ended with {@link TokenType#SLASH_GT}
   */
  private final Token closingTag;

  /**
   * The ending {@link TokenType#GT} or {@link TokenType#SLASH_GT} token (not {@code null}).
   */
  private final Token nodeEnd;

  /**
   * The expressions that are embedded in the tag's content.
   */
  private XmlExpression[] expressions = XmlExpression.EMPTY_ARRAY;

  /**
   * Construct a new instance representing an XML or HTML element
   * 
   * @param nodeStart the starting {@link TokenType#LT} token (not {@code null})
   * @param tag the {@link TokenType#TAG} token after the starting '&lt;' (not {@code null}).
   * @param attributes the attributes associated with this element or {@link #NO_ATTRIBUTES} (not
   *          {@code null}, contains no {@code null}s)
   * @param attributeEnd The {@link TokenType#GT} or {@link TokenType#SLASH_GT} token after the
   *          attributes (not {@code null}). The token may be the same token as {@link #nodeEnd} if
   *          there are no child {@link #tagNodes}.
   * @param tagNodes child tag nodes of the receiver or {@link #NO_TAG_NODES} (not {@code null},
   *          contains no {@code null}s)
   * @param contentEnd the token (not {@code null}) after the content, which may be
   *          <ul>
   *          <li>(1) {@link TokenType#LT_SLASH} for nodes with open and close tags, or</li>
   *          <li>(2) the {@link TokenType#LT} nodeStart of the next sibling node if this node is
   *          self closing or the attributeEnd is {@link TokenType#SLASH_GT}, or</li>
   *          <li>(3) {@link TokenType#EOF} if the node does not have a closing tag and is the last
   *          node in the stream {@link TokenType#LT_SLASH} token after the content, or {@code null}
   *          if there is no content and the attributes ended with {@link TokenType#SLASH_GT}.</li>
   *          </ul>
   * @param closingTag the closing {@link TokenType#TAG} after the child elements or {@code null} if
   *          there is no content and the attributes ended with {@link TokenType#SLASH_GT}
   * @param nodeEnd the ending {@link TokenType#GT} or {@link TokenType#SLASH_GT} token (not
   *          {@code null})
   */
  public XmlTagNode(Token nodeStart, Token tag, List<XmlAttributeNode> attributes,
      Token attributeEnd, List<XmlTagNode> tagNodes, Token contentEnd, Token closingTag,
      Token nodeEnd) {
    this.nodeStart = nodeStart;
    this.tag = tag;
    this.attributes = becomeParentOfAll(attributes, NO_ATTRIBUTES);
    this.attributeEnd = attributeEnd;
    this.tagNodes = becomeParentOfAll(tagNodes, NO_TAG_NODES);
    this.contentEnd = contentEnd;
    this.closingTag = closingTag;
    this.nodeEnd = nodeEnd;
  }

  @Override
  public <R> R accept(XmlVisitor<R> visitor) {
    return visitor.visitXmlTagNode(this);
  }

  /**
   * Answer the attribute with the specified name.
   * 
   * @param name the attribute name
   * @return the attribute or {@code null} if no matching attribute is found
   */
  public XmlAttributeNode getAttribute(String name) {
    for (XmlAttributeNode attribute : attributes) {
      if (attribute.getName().equals(name)) {
        return attribute;
      }
    }
    return null;
  }

  /**
   * The {@link TokenType#GT} or {@link TokenType#SLASH_GT} token after the attributes (not
   * {@code null}). The token may be the same token as {@link #nodeEnd} if there are no child
   * {@link #tagNodes}.
   * 
   * @return the token (not {@code null})
   */
  public Token getAttributeEnd() {
    return attributeEnd;
  }

  /**
   * Answer the receiver's attributes. Callers should not manipulate the returned list to edit the
   * AST structure.
   * 
   * @return the attributes (not {@code null}, contains no {@code null}s)
   */
  public List<XmlAttributeNode> getAttributes() {
    return attributes;
  }

  /**
   * Find the attribute with the given name (see {@link #getAttribute(String)} and answer the lexeme
   * for the attribute's value token without the leading and trailing quotes (see
   * {@link XmlAttributeNode#getText()}).
   * 
   * @param name the attribute name
   * @return the attribute text or {@code null} if no matching attribute is found
   */
  public String getAttributeText(String name) {
    XmlAttributeNode attribute = getAttribute(name);
    return attribute != null ? attribute.getText() : null;
  }

  @Override
  public Token getBeginToken() {
    return nodeStart;
  }

  /**
   * The the closing {@link TokenType#TAG} after the child elements or {@code null} if there is no
   * content and the attributes ended with {@link TokenType#SLASH_GT}
   * 
   * @return the closing tag or {@code null}
   */
  public Token getClosingTag() {
    return closingTag;
  }

  /**
   * Answer a string representing the content contained in the receiver. This includes the textual
   * representation of any child tag nodes ({@link #getTagNodes()}). Whitespace between '&lt;',
   * '&lt;/', and '>', '/>' is discarded, but all other whitespace is preserved.
   * 
   * @return the content (not {@code null})
   */
  public String getContent() {
    Token token = attributeEnd.getNext();
    if (token == contentEnd) {
      return "";
    }
    //TODO (danrubel): handle CDATA and replace HTML character encodings with the actual characters
    String content = token.getLexeme();
    token = token.getNext();
    if (token == contentEnd) {
      return content;
    }
    StringBuilder buffer = new StringBuilder(content);
    while (token != contentEnd) {
      buffer.append(token.getLexeme());
      token = token.getNext();
    }
    return buffer.toString();
  }

  /**
   * Answer the token (not {@code null}) after the content, which may be
   * <ul>
   * <li>(1) {@link TokenType#LT_SLASH} for nodes with open and close tags, or</li>
   * <li>(2) the {@link TokenType#LT} nodeStart of the next sibling node if this node is self
   * closing or the attributeEnd is {@link TokenType#SLASH_GT}, or</li>
   * <li>(3) {@link TokenType#EOF} if the node does not have a closing tag and is the last node in
   * the stream {@link TokenType#LT_SLASH} token after the content, or {@code null} if there is no
   * content and the attributes ended with {@link TokenType#SLASH_GT}.</li>
   * </ul>
   * 
   * @return the token (not {@code null})
   */
  public Token getContentEnd() {
    return contentEnd;
  }

  @Override
  public Token getEndToken() {
    if (nodeEnd != null) {
      return nodeEnd;
    }
    if (closingTag != null) {
      return closingTag;
    }
    if (contentEnd != null) {
      return contentEnd;
    }
    if (!tagNodes.isEmpty()) {
      return tagNodes.get(tagNodes.size() - 1).getEndToken();
    }
    if (attributeEnd != null) {
      return attributeEnd;
    }
    if (!attributes.isEmpty()) {
      return attributes.get(attributes.size() - 1).getEndToken();
    }
    return tag;
  }

  /**
   * Return the expressions that are embedded in the tag's content.
   * 
   * @return the expressions that are embedded in the tag's content
   */
  public XmlExpression[] getExpressions() {
    return expressions;
  }

  /**
   * Answer the ending {@link TokenType#GT} or {@link TokenType#SLASH_GT} token.
   * 
   * @return the token (not {@code null})
   */
  public Token getNodeEnd() {
    return nodeEnd;
  }

  /**
   * Answer the starting {@link TokenType#LT} token.
   * 
   * @return the token (not {@code null})
   */
  public Token getNodeStart() {
    return nodeStart;
  }

  /**
   * Answer the tag name after the starting '&lt;'.
   * 
   * @return the tag name (not {@code null})
   */
  public String getTag() {
    return tag.getLexeme();
  }

  /**
   * Answer the tag nodes contained in the receiver. Callers should not manipulate the returned list
   * to edit the AST structure.
   * 
   * @return the children (not {@code null}, contains no {@code null}s)
   */
  public List<XmlTagNode> getTagNodes() {
    return tagNodes;
  }

  /**
   * Answer the {@link TokenType#TAG} token after the starting '&lt;'.
   * 
   * @return the token (not {@code null})
   */
  public Token getTagToken() {
    return tag;
  }

  /**
   * Set the expressions that are embedded in the tag's content.
   * 
   * @param expressions expressions that are embedded in the tag's content
   */
  public void setExpressions(XmlExpression[] expressions) {
    this.expressions = expressions;
  }

  @Override
  public void visitChildren(XmlVisitor<?> visitor) {
    for (XmlAttributeNode node : attributes) {
      node.accept(visitor);
    }
    for (XmlTagNode node : tagNodes) {
      node.accept(visitor);
    }
  }
}
