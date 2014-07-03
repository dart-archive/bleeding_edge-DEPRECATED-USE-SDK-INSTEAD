/*
 * Copyright (c) 2014, the Dart project authors.
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
 * Instances of the class {@code YieldStatement} implement a yield statement.
 */
public class YieldStatement extends Statement {
  /**
   * The 'yield' keyword.
   */
  private Token yieldKeyword;

  /**
   * The star optionally following the 'yield' keyword.
   */
  private Token star;

  /**
   * The expression whose value will be yielded.
   */
  private Expression expression;

  /**
   * The semicolon following the expression.
   */
  private Token semicolon;

  /**
   * Initialize a newly created yield expression.
   * 
   * @param yieldKeyword the 'yield' keyword
   * @param star the star following the 'yield' keyword
   * @param expression the expression whose value will be yielded
   * @param semicolon the semicolon following the expression
   */
  public YieldStatement(Token yieldKeyword, Token star, Expression expression, Token semicolon) {
    this.yieldKeyword = yieldKeyword;
    this.star = star;
    this.expression = becomeParentOf(expression);
    this.semicolon = semicolon;
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitYieldStatement(this);
  }

  @Override
  public Token getBeginToken() {
    if (yieldKeyword != null) {
      return yieldKeyword;
    }
    return expression.getBeginToken();
  }

  @Override
  public Token getEndToken() {
    if (semicolon != null) {
      return semicolon;
    }
    return expression.getEndToken();
  }

  /**
   * Return the expression whose value will be yielded.
   * 
   * @return the expression whose value will be yielded
   */
  public Expression getExpression() {
    return expression;
  }

  /**
   * Return the semicolon following the expression.
   * 
   * @return the semicolon following the expression
   */
  public Token getSemicolon() {
    return semicolon;
  }

  /**
   * Return the star following the 'yield' keyword, or {@code null} if there is no star.
   * 
   * @return the star following the 'yield' keyword
   */
  public Token getStar() {
    return star;
  }

  /**
   * Return the 'yield' keyword.
   * 
   * @return the 'yield' keyword
   */
  public Token getYieldKeyword() {
    return yieldKeyword;
  }

  /**
   * Set the expression whose value will be yielded to the given expression.
   * 
   * @param expression the expression whose value will be yielded
   */
  public void setExpression(Expression expression) {
    this.expression = becomeParentOf(expression);
  }

  /**
   * Set the semicolon following the expression to the given token.
   * 
   * @param semicolon the semicolon following the expression
   */
  public void setSemicolon(Token semicolon) {
    this.semicolon = semicolon;
  }

  /**
   * Set the star following the 'yield' keyword to the given token.
   * 
   * @param star the star following the 'yield' keyword
   */
  public void setStar(Token star) {
    this.star = star;
  }

  /**
   * Set the 'yield' keyword to the given token.
   * 
   * @param yieldKeyword the 'yield' keyword
   */
  public void setYieldKeyword(Token yieldKeyword) {
    this.yieldKeyword = yieldKeyword;
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(expression, visitor);
  }
}
