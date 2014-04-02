/*
 * Copyright (c) 2012, the Dart project authors.
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
 * Instances of the class {@code Label} represent a label.
 * 
 * <pre>
 * label ::=
 *     {@link SimpleIdentifier label} ':'
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class Label extends AstNode {
  /**
   * The label being associated with the statement.
   */
  private SimpleIdentifier label;

  /**
   * The colon that separates the label from the statement.
   */
  private Token colon;

  /**
   * Initialize a newly created label.
   * 
   * @param label the label being applied
   * @param colon the colon that separates the label from whatever follows
   */
  public Label(SimpleIdentifier label, Token colon) {
    this.label = becomeParentOf(label);
    this.colon = colon;
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitLabel(this);
  }

  @Override
  public Token getBeginToken() {
    return label.getBeginToken();
  }

  /**
   * Return the colon that separates the label from the statement.
   * 
   * @return the colon that separates the label from the statement
   */
  public Token getColon() {
    return colon;
  }

  @Override
  public Token getEndToken() {
    return colon;
  }

  /**
   * Return the label being associated with the statement.
   * 
   * @return the label being associated with the statement
   */
  public SimpleIdentifier getLabel() {
    return label;
  }

  /**
   * Set the colon that separates the label from the statement to the given token.
   * 
   * @param colon the colon that separates the label from the statement
   */
  public void setColon(Token colon) {
    this.colon = colon;
  }

  /**
   * Set the label being associated with the statement to the given label.
   * 
   * @param label the label being associated with the statement
   */
  public void setLabel(SimpleIdentifier label) {
    this.label = becomeParentOf(label);
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(label, visitor);
  }
}
