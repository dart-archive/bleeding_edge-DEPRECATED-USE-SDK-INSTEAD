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

import java.util.List;

/**
 * Instances of the class {@code SwitchStatement} represent a switch statement.
 * 
 * <pre>
 * switchStatement ::=
 *     'switch' '(' {@link Expression expression} ')' '{' {@link SwitchCase switchCase}* {@link SwitchDefault defaultCase}? '}'
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class SwitchStatement extends Statement {
  /**
   * The token representing the 'switch' keyword.
   */
  private Token keyword;

  /**
   * The left parenthesis.
   */
  private Token leftParenthesis;

  /**
   * The expression used to determine which of the switch members will be selected.
   */
  private Expression expression;

  /**
   * The right parenthesis.
   */
  private Token rightParenthesis;

  /**
   * The left curly bracket.
   */
  private Token leftBracket;

  /**
   * The switch members that can be selected by the expression.
   */
  private NodeList<SwitchMember> members = new NodeList<SwitchMember>(this);

  /**
   * The right curly bracket.
   */
  private Token rightBracket;

  /**
   * Initialize a newly created switch statement.
   * 
   * @param keyword the token representing the 'switch' keyword
   * @param leftParenthesis the left parenthesis
   * @param expression the expression used to determine which of the switch members will be selected
   * @param rightParenthesis the right parenthesis
   * @param leftBracket the left curly bracket
   * @param members the switch members that can be selected by the expression
   * @param rightBracket the right curly bracket
   */
  public SwitchStatement(Token keyword, Token leftParenthesis, Expression expression,
      Token rightParenthesis, Token leftBracket, List<SwitchMember> members, Token rightBracket) {
    this.keyword = keyword;
    this.leftParenthesis = leftParenthesis;
    this.expression = becomeParentOf(expression);
    this.rightParenthesis = rightParenthesis;
    this.leftBracket = leftBracket;
    this.members.addAll(members);
    this.rightBracket = rightBracket;
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitSwitchStatement(this);
  }

  @Override
  public Token getBeginToken() {
    return keyword;
  }

  @Override
  public Token getEndToken() {
    return rightBracket;
  }

  /**
   * Return the expression used to determine which of the switch members will be selected.
   * 
   * @return the expression used to determine which of the switch members will be selected
   */
  public Expression getExpression() {
    return expression;
  }

  /**
   * Return the token representing the 'switch' keyword.
   * 
   * @return the token representing the 'switch' keyword
   */
  public Token getKeyword() {
    return keyword;
  }

  /**
   * Return the left curly bracket.
   * 
   * @return the left curly bracket
   */
  public Token getLeftBracket() {
    return leftBracket;
  }

  /**
   * Return the left parenthesis.
   * 
   * @return the left parenthesis
   */
  public Token getLeftParenthesis() {
    return leftParenthesis;
  }

  /**
   * Return the switch members that can be selected by the expression.
   * 
   * @return the switch members that can be selected by the expression
   */
  public NodeList<SwitchMember> getMembers() {
    return members;
  }

  /**
   * Return the right curly bracket.
   * 
   * @return the right curly bracket
   */
  public Token getRightBracket() {
    return rightBracket;
  }

  /**
   * Return the right parenthesis.
   * 
   * @return the right parenthesis
   */
  public Token getRightParenthesis() {
    return rightParenthesis;
  }

  /**
   * Set the expression used to determine which of the switch members will be selected to the given
   * expression.
   * 
   * @param expression the expression used to determine which of the switch members will be selected
   */
  public void setExpression(Expression expression) {
    this.expression = becomeParentOf(expression);
  }

  /**
   * Set the token representing the 'switch' keyword to the given token.
   * 
   * @param keyword the token representing the 'switch' keyword
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
  }

  /**
   * Set the left curly bracket to the given token.
   * 
   * @param leftBracket the left curly bracket
   */
  public void setLeftBracket(Token leftBracket) {
    this.leftBracket = leftBracket;
  }

  /**
   * Set the left parenthesis to the given token.
   * 
   * @param leftParenthesis the left parenthesis
   */
  public void setLeftParenthesis(Token leftParenthesis) {
    this.leftParenthesis = leftParenthesis;
  }

  /**
   * Set the right curly bracket to the given token.
   * 
   * @param rightBracket the right curly bracket
   */
  public void setRightBracket(Token rightBracket) {
    this.rightBracket = rightBracket;
  }

  /**
   * Set the right parenthesis to the given token.
   * 
   * @param rightParenthesis the right parenthesis
   */
  public void setRightParenthesis(Token rightParenthesis) {
    this.rightParenthesis = rightParenthesis;
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(expression, visitor);
    members.accept(visitor);
  }
}
