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
 * Instances of the class <code>BreakStatement</code> represent a break statement.
 * 
 * <pre>
 * breakStatement ::=
 *     'break' {@link SimpleIdentifier label}? ';'
 * </pre>
 */
public class BreakStatement extends Statement {
  /**
   * The token representing the 'break' keyword.
   */
  private Token keyword;

  /**
   * The label associated with the statement, or <code>null</code> if there is no label.
   */
  private SimpleIdentifier label;

  /**
   * The semicolon terminating the statement.
   */
  private Token semicolon;

  /**
   * Initialize a newly created break statement.
   */
  public BreakStatement() {
  }

  /**
   * Initialize a newly created break statement.
   * 
   * @param keyword the token representing the 'break' keyword
   * @param label the label associated with the statement
   * @param semicolon the semicolon terminating the statement
   */
  public BreakStatement(Token keyword, SimpleIdentifier label, Token semicolon) {
    this.keyword = keyword;
    this.label = becomeParentOf(label);
    this.semicolon = semicolon;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitBreakStatement(this);
  }

  @Override
  public Token getBeginToken() {
    return keyword;
  }

  @Override
  public Token getEndToken() {
    return semicolon;
  }

  /**
   * Return the token representing the 'break' keyword.
   * 
   * @return the token representing the 'break' keyword
   */
  public Token getKeyword() {
    return keyword;
  }

  /**
   * Return the label associated with the statement, or <code>null</code> if there is no label.
   * 
   * @return the label associated with the statement
   */
  public SimpleIdentifier getLabel() {
    return label;
  }

  /**
   * Return the semicolon terminating the statement.
   * 
   * @return the semicolon terminating the statement
   */
  public Token getSemicolon() {
    return semicolon;
  }

  /**
   * Set the token representing the 'break' keyword to the given token.
   * 
   * @param keyword the token representing the 'break' keyword
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
  }

  /**
   * Set the label associated with the statement to the given identifier.
   * 
   * @param identifier the label associated with the statement
   */
  public void setLabel(SimpleIdentifier identifier) {
    label = becomeParentOf(identifier);
  }

  /**
   * Set the semicolon terminating the statement to the given token.
   * 
   * @param semicolon the semicolon terminating the statement
   */
  public void setSemicolon(Token semicolon) {
    this.semicolon = semicolon;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(label, visitor);
  }
}
