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

import com.google.dart.engine.scanner.Token;

/**
 * Instances of the class <code>MapLiteralEntry</code> represent a single key/value pair in a map
 * literal.
 * 
 * <pre>
 * mapLiteralEntry ::=
 *     {@link StringLiteral key} ':' {@link Expression value}
 * </pre>
 */
public class MapLiteralEntry extends ASTNode {
  /**
   * The key with which the value will be associated.
   */
  private StringLiteral key;

  /**
   * The colon that separates the key from the value.
   */
  private Token separator;

  /**
   * The expression computing the value that will be associated with the key.
   */
  private Expression value;

  /**
   * Initialize a newly created map literal entry.
   */
  public MapLiteralEntry() {
  }

  /**
   * Initialize a newly created map literal entry.
   * 
   * @param key the key with which the value will be associated
   * @param separator the colon that separates the key from the value
   * @param value the expression computing the value that will be associated with the key
   */
  public MapLiteralEntry(StringLiteral key, Token separator, Expression value) {
    this.key = becomeParentOf(key);
    this.separator = separator;
    this.value = becomeParentOf(value);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitMapLiteralEntry(this);
  }

  @Override
  public Token getBeginToken() {
    return key.getBeginToken();
  }

  @Override
  public Token getEndToken() {
    return value.getEndToken();
  }

  /**
   * Return the key with which the value will be associated.
   * 
   * @return the key with which the value will be associated
   */
  public StringLiteral getKey() {
    return key;
  }

  /**
   * Return the colon that separates the key from the value.
   * 
   * @return the colon that separates the key from the value
   */
  public Token getSeparator() {
    return separator;
  }

  /**
   * Return the expression computing the value that will be associated with the key.
   * 
   * @return the expression computing the value that will be associated with the key
   */
  public Expression getValue() {
    return value;
  }

  /**
   * Set the key with which the value will be associated to the given string.
   * 
   * @param string the key with which the value will be associated
   */
  public void setKey(StringLiteral string) {
    key = becomeParentOf(string);
  }

  /**
   * Set the colon that separates the key from the value to the given token.
   * 
   * @param separator the colon that separates the key from the value
   */
  public void setSeparator(Token separator) {
    this.separator = separator;
  }

  /**
   * Set the expression computing the value that will be associated with the key to the given
   * expression.
   * 
   * @param expression the expression computing the value that will be associated with the key
   */
  public void setValue(Expression expression) {
    value = becomeParentOf(expression);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(key, visitor);
    safelyVisitChild(value, visitor);
  }
}
