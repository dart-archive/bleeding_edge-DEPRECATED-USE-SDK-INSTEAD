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
 * Instances of the class {@code ConstructorFieldInitializer} represent the initialization of a
 * field within a constructor's initialization list.
 * 
 * <pre>
 * fieldInitializer ::=
 *     ('this' '.')? {@link SimpleIdentifier fieldName} '=' {@link Expression conditionalExpression cascadeSection*}
 * </pre>
 */
public class ConstructorFieldInitializer extends ConstructorInitializer {
  /**
   * The token for the 'this' keyword, or {@code null} if there is no 'this' keyword.
   */
  private Token keyword;

  /**
   * The token for the period after the 'this' keyword, or {@code null} if there is no 'this'
   * keyword.
   */
  private Token period;

  /**
   * The name of the field being initialized.
   */
  private SimpleIdentifier fieldName;

  /**
   * The token for the equal sign between the field name and the expression.
   */
  private Token equals;

  /**
   * The expression computing the value to which the field will be initialized.
   */
  private Expression expression;

  /**
   * Initialize a newly created field initializer to initialize the field with the given name to the
   * value of the given expression.
   */
  public ConstructorFieldInitializer() {
  }

  /**
   * Initialize a newly created field initializer to initialize the field with the given name to the
   * value of the given expression.
   * 
   * @param keyword the token for the 'this' keyword
   * @param period the token for the period after the 'this' keyword
   * @param fieldName the name of the field being initialized
   * @param equals the token for the equal sign between the field name and the expression
   * @param expression the expression computing the value to which the field will be initialized
   */
  public ConstructorFieldInitializer(Token keyword, Token period, SimpleIdentifier fieldName,
      Token equals, Expression expression) {
    this.keyword = keyword;
    this.period = period;
    this.fieldName = becomeParentOf(fieldName);
    this.equals = equals;
    this.expression = becomeParentOf(expression);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitConstructorFieldInitializer(this);
  }

  @Override
  public Token getBeginToken() {
    if (keyword != null) {
      return keyword;
    }
    return fieldName.getBeginToken();
  }

  @Override
  public Token getEndToken() {
    return expression.getEndToken();
  }

  /**
   * Return the token for the equal sign between the field name and the expression.
   * 
   * @return the token for the equal sign between the field name and the expression
   */
  public Token getEquals() {
    return equals;
  }

  /**
   * Return the expression computing the value to which the field will be initialized.
   * 
   * @return the expression computing the value to which the field will be initialized
   */
  public Expression getExpression() {
    return expression;
  }

  /**
   * Return the name of the field being initialized.
   * 
   * @return the name of the field being initialized
   */
  public SimpleIdentifier getFieldName() {
    return fieldName;
  }

  /**
   * Return the token for the 'this' keyword, or {@code null} if there is no 'this' keyword.
   * 
   * @return the token for the 'this' keyword
   */
  public Token getKeyword() {
    return keyword;
  }

  /**
   * Return the token for the period after the 'this' keyword, or {@code null} if there is no 'this'
   * keyword.
   * 
   * @return the token for the period after the 'this' keyword
   */
  public Token getPeriod() {
    return period;
  }

  /**
   * Set the token for the equal sign between the field name and the expression to the given token.
   * 
   * @param equals the token for the equal sign between the field name and the expression
   */
  public void setEquals(Token equals) {
    this.equals = equals;
  }

  /**
   * Set the expression computing the value to which the field will be initialized to the given
   * expression.
   * 
   * @param expression the expression computing the value to which the field will be initialized
   */
  public void setExpression(Expression expression) {
    this.expression = becomeParentOf(expression);
  }

  /**
   * Set the name of the field being initialized to the given identifier.
   * 
   * @param identifier the name of the field being initialized
   */
  public void setFieldName(SimpleIdentifier identifier) {
    fieldName = becomeParentOf(identifier);
  }

  /**
   * Set the token for the 'this' keyword to the given token.
   * 
   * @param keyword the token for the 'this' keyword
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
  }

  /**
   * Set the token for the period after the 'this' keyword to the given token.
   * 
   * @param period the token for the period after the 'this' keyword
   */
  public void setPeriod(Token period) {
    this.period = period;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(fieldName, visitor);
    safelyVisitChild(expression, visitor);
  }
}
