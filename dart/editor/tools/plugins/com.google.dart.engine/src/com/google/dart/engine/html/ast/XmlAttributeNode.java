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
import com.google.dart.engine.utilities.general.StringUtilities;

/**
 * Instances of {@code XmlAttributeNode} represent name/value pairs owned by an {@link XmlTagNode}.
 * 
 * @coverage dart.engine.html
 */
public class XmlAttributeNode extends XmlNode {

  private final Token name;
  private final Token equals;
  private final Token value;
  private XmlExpression[] expressions = XmlExpression.EMPTY_ARRAY;

  /**
   * Construct a new instance representing an XML attribute.
   * 
   * @param name the name token (not {@code null}). This may be a zero length token if the attribute
   *          is badly formed.
   * @param equals the equals sign or {@code null} if none
   * @param value the value token (not {@code null})
   */
  public XmlAttributeNode(Token name, Token equals, Token value) {
    this.name = name;
    this.equals = equals;
    this.value = value;
  }

  @Override
  public <R> R accept(XmlVisitor<R> visitor) {
    return visitor.visitXmlAttributeNode(this);
  }

  @Override
  public Token getBeginToken() {
    return name;
  }

  @Override
  public Token getEndToken() {
    return value;
  }

  /**
   * Answer the equals sign token that appears between the name and value tokens. This may be
   * {@code null} if the attribute is badly formed.
   * 
   * @return the token or {@code null} if there is no equals sign between the name and value
   */
  public Token getEquals() {
    return equals;
  }

  /**
   * Return the expressions that are embedded in the attribute's value.
   * 
   * @return the expressions that are embedded in the attribute's value
   */
  public XmlExpression[] getExpressions() {
    return expressions;
  }

  /**
   * Answer the attribute name. This may be a zero length string if the attribute is badly formed.
   * 
   * @return the name (not {@code null})
   */
  public String getName() {
    return name.getLexeme();
  }

  /**
   * Answer the attribute name token. This may be a zero length token if the attribute is badly
   * formed.
   * 
   * @return the name token (not {@code null})
   */
  public Token getNameToken() {
    return name;
  }

  /**
   * Answer the lexeme for the value token without the leading and trailing quotes.
   * 
   * @return the text or {@code null} if the value is not specified
   */
  public String getText() {
    if (value == null) {
      return null;
    }
    //TODO (danrubel): replace HTML character encodings with the actual characters
    String text = value.getLexeme();
    int len = text.length();
    if (len > 0) {
      if (text.charAt(0) == '"') {
        if (len > 1 && text.charAt(len - 1) == '"') {
          return text.substring(1, len - 1);
        } else {
          return text.substring(1);
        }
      } else if (text.charAt(0) == '\'') {
        if (len > 1 && text.charAt(len - 1) == '\'') {
          return text.substring(1, len - 1);
        } else {
          return text.substring(1);
        }
      }
    }
    return text;
  }

  /**
   * Answer the offset of the value after the leading quote.
   * 
   * @return the offset of the value, or {@code -1} if the value is not specified
   */
  public int getTextOffset() {
    if (value == null) {
      return -1;
    }
    String text = value.getLexeme();
    if (StringUtilities.startsWithChar(text, '"') || StringUtilities.startsWithChar(text, '\'')) {
      return value.getOffset() + 1;
    }
    return value.getOffset();
  }

  /**
   * Answer the attribute value token. A properly formed value will start and end with matching
   * quote characters, but the value returned may not be properly formed.
   * 
   * @return the value token or {@code null} if this represents a badly formed attribute
   */
  public Token getValueToken() {
    return value;
  }

  /**
   * Set the expressions that are embedded in the attribute's value.
   * 
   * @param expressions expressions that are embedded in the attribute's value
   */
  public void setExpressions(XmlExpression[] expressions) {
    this.expressions = expressions;
  }

  @Override
  public void visitChildren(XmlVisitor<?> visitor) {
    // no children to visit
  }
}
