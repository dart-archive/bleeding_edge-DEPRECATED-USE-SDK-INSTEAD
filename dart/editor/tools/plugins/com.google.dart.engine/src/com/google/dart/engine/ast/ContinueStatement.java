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
 * Instances of the class {@code ContinueStatement} represent a continue statement.
 * 
 * <pre>
 * continueStatement ::=
 *     'continue' {@link SimpleIdentifier label}? ';'
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class ContinueStatement extends Statement {
  /**
   * The token representing the 'continue' keyword.
   */
  private Token keyword;

  /**
   * The label associated with the statement, or {@code null} if there is no label.
   */
  private SimpleIdentifier label;

  /**
   * The semicolon terminating the statement.
   */
  private Token semicolon;

  /**
   * Initialize a newly created continue statement.
   * 
   * @param keyword the token representing the 'continue' keyword
   * @param label the label associated with the statement
   * @param semicolon the semicolon terminating the statement
   */
  public ContinueStatement(Token keyword, SimpleIdentifier label, Token semicolon) {
    this.keyword = keyword;
    this.label = becomeParentOf(label);
    this.semicolon = semicolon;
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitContinueStatement(this);
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
   * Return the token representing the 'continue' keyword.
   * 
   * @return the token representing the 'continue' keyword
   */
  public Token getKeyword() {
    return keyword;
  }

  /**
   * Return the label associated with the statement, or {@code null} if there is no label.
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
   * Set the token representing the 'continue' keyword to the given token.
   * 
   * @param keyword the token representing the 'continue' keyword
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
  }

  /**
   * Set the label associated with the statement to the given label.
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
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(label, visitor);
  }
}
