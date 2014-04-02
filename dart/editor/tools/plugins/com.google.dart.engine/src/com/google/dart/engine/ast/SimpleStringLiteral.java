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

import com.google.dart.engine.element.Element;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.utilities.general.StringUtilities;

/**
 * Instances of the class {@code SimpleStringLiteral} represent a string literal expression that
 * does not contain any interpolations.
 * 
 * <pre>
 * simpleStringLiteral ::=
 *     rawStringLiteral
 *   | basicStringLiteral
 *
 * rawStringLiteral ::=
 *     'r' basicStringLiteral
 *
 * simpleStringLiteral ::=
 *     multiLineStringLiteral
 *   | singleLineStringLiteral
 *
 * multiLineStringLiteral ::=
 *     "'''" characters "'''"
 *   | '"""' characters '"""'
 *
 * singleLineStringLiteral ::=
 *     "'" characters "'"
 *     '"' characters '"'
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class SimpleStringLiteral extends StringLiteral {
  /**
   * The token representing the literal.
   */
  private Token literal;

  /**
   * The value of the literal.
   */
  private String value;

  /**
   * The toolkit specific element associated with this literal, or {@code null}.
   */
  private Element toolkitElement;

  /**
   * Initialize a newly created simple string literal.
   * 
   * @param literal the token representing the literal
   * @param value the value of the literal
   */
  public SimpleStringLiteral(Token literal, String value) {
    this.literal = literal;
    this.value = StringUtilities.intern(value);
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitSimpleStringLiteral(this);
  }

  @Override
  public Token getBeginToken() {
    return literal;
  }

  @Override
  public Token getEndToken() {
    return literal;
  }

  /**
   * Return the token representing the literal.
   * 
   * @return the token representing the literal
   */
  public Token getLiteral() {
    return literal;
  }

  /**
   * Return the toolkit specific, non-Dart, element associated with this literal, or {@code null}.
   * 
   * @return the element associated with this literal
   */
  public Element getToolkitElement() {
    return toolkitElement;
  }

  /**
   * Return the value of the literal.
   * 
   * @return the value of the literal
   */
  public String getValue() {
    return value;
  }

  /**
   * Return the offset of the first value character.
   * 
   * @return the offset of the first value character
   */
  public int getValueOffset() {
    int valueOffset = 0;
    if (isRaw()) {
      valueOffset += 1;
    }
    if (isMultiline()) {
      valueOffset += 3;
    } else {
      valueOffset += 1;
    }
    return getOffset() + valueOffset;
  }

  /**
   * Return {@code true} if this string literal is a multi-line string.
   * 
   * @return {@code true} if this string literal is a multi-line string
   */
  public boolean isMultiline() {
    String lexeme = literal.getLexeme();
    if (lexeme.length() < 6) {
      return false;
    }
    return StringUtilities.endsWith3(lexeme, '"', '"', '"')
        || StringUtilities.endsWith3(lexeme, '\'', '\'', '\'');
  }

  /**
   * Return {@code true} if this string literal is a raw string.
   * 
   * @return {@code true} if this string literal is a raw string
   */
  public boolean isRaw() {
    return literal.getLexeme().charAt(0) == 'r';
  }

  @Override
  public boolean isSynthetic() {
    return literal.isSynthetic();
  }

  /**
   * Set the token representing the literal to the given token.
   * 
   * @param literal the token representing the literal
   */
  public void setLiteral(Token literal) {
    this.literal = literal;
  }

  /**
   * Set the toolkit specific, non-Dart, element associated with this literal.
   * 
   * @param element the toolkit specific element to be associated with this literal
   */
  public void setToolkitElement(Element element) {
    toolkitElement = element;
  }

  /**
   * Set the value of the literal to the given string.
   * 
   * @param string the value of the literal
   */
  public void setValue(String string) {
    value = StringUtilities.intern(value);
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    // There are no children to visit.
  }

  @Override
  protected void appendStringValue(StringBuilder builder) {
    builder.append(getValue());
  }
}
