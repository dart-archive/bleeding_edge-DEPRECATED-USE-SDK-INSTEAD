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

import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.html.ast.visitor.XmlVisitor;
import com.google.dart.engine.html.scanner.Token;
import com.google.dart.engine.html.scanner.TokenType;

import java.util.List;

/**
 * Instances of the class {@code HtmlUnit} represent the contents of an HTML file.
 * 
 * @coverage dart.engine.html
 */
public class HtmlUnit extends XmlNode {

  /**
   * The first token in the token stream that was parsed to form this HTML unit.
   */
  private final Token beginToken;

  /**
   * The last token in the token stream that was parsed to form this compilation unit. This token
   * should always have a type of {@link TokenType.EOF}.
   */
  private final Token endToken;

  /**
   * The tag nodes contained in the receiver (not {@code null}, contains no {@code null}s).
   */
  private final List<XmlTagNode> tagNodes;

  /**
   * Construct a new instance representing the content of an HTML file.
   * 
   * @param beginToken the first token in the file (not {@code null})
   * @param tagNodes child tag nodes of the receiver (not {@code null}, contains no {@code null}s)
   * @param endToken the last token in the token stream which should be of type
   *          {@link TokenType.EOF}
   */
  public HtmlUnit(Token beginToken, List<XmlTagNode> tagNodes, Token endToken) {
    this.beginToken = beginToken;
    this.tagNodes = becomeParentOfAll(tagNodes);
    this.endToken = endToken;
  }

  @Override
  public <R> R accept(XmlVisitor<R> visitor) {
    return visitor.visitHtmlUnit(this);
  }

  @Override
  public Token getBeginToken() {
    return beginToken;
  }

  /**
   * Return the element associated with this HTML unit.
   * 
   * @return the element or {@code null} if the receiver is not resolved
   */
  @Override
  public HtmlElement getElement() {
    return (HtmlElement) super.getElement();
  }

  @Override
  public Token getEndToken() {
    return endToken;
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

  @Override
  public void setElement(Element element) {
    if (element != null && !(element instanceof HtmlElement)) {
      throw new IllegalArgumentException("HtmlElement expected, but " + element.getClass()
          + " given");
    }
    super.setElement(element);
  }

  @Override
  public void visitChildren(XmlVisitor<?> visitor) {
    for (XmlTagNode node : tagNodes) {
      node.accept(visitor);
    }
  }
}
