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
import com.google.dart.engine.scanner.TokenType;

/**
 * Instances of the class {@code DefaultFormalParameter} represent a formal parameter with a default
 * value.
 * 
 * <pre>
 * defaultFormalParameter ::=
 *     {@link NormalFormalParameter parameter} '=' {@link Expression defaultValue}
 *
 * defaultNamedParameter ::=
 *     {@link NormalFormalParameter parameter} ':' {@link Expression defaultValue}
 * </pre>
 */
public class DefaultFormalParameter extends FormalParameter {
  /**
   * The formal parameter with which the default value is associated.
   */
  private NormalFormalParameter parameter;

  /**
   * The token separating the parameter from the default value.
   */
  private Token separator;

  /**
   * The expression computing the default value for the parameter.
   */
  private Expression defaultValue;

  /**
   * Initialize a newly created default formal parameter.
   */
  public DefaultFormalParameter() {
  }

  /**
   * Initialize a newly created default formal parameter.
   * 
   * @param parameter the formal parameter with which the default value is associated
   * @param separator the token separating the parameter from the default value
   * @param defaultValue the expression computing the default value for the parameter
   */
  public DefaultFormalParameter(NormalFormalParameter parameter, Token separator,
      Expression defaultValue) {
    this.parameter = becomeParentOf(parameter);
    this.separator = separator;
    this.defaultValue = becomeParentOf(defaultValue);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitDefaultFormalParameter(this);
  }

  @Override
  public Token getBeginToken() {
    return parameter.getBeginToken();
  }

  /**
   * Return the expression computing the default value for the parameter.
   * 
   * @return the expression computing the default value for the parameter
   */
  public Expression getDefaultValue() {
    return defaultValue;
  }

  @Override
  public Token getEndToken() {
    return defaultValue.getEndToken();
  }

  /**
   * Return the formal parameter with which the default value is associated.
   * 
   * @return the formal parameter with which the default value is associated
   */
  public NormalFormalParameter getParameter() {
    return parameter;
  }

  /**
   * Return the token separating the parameter from the default value.
   * 
   * @return the token separating the parameter from the default value
   */
  public Token getSeparator() {
    return separator;
  }

  /**
   * Return {@code true} if this parameter represents a named parameter.
   * 
   * @return {@code true} if the separator is a colon
   */
  public boolean isNamed() {
    return separator != null && separator.getType() == TokenType.COLON;
  }

  /**
   * Set the expression computing the default value for the parameter to the given expression.
   * 
   * @param expression the expression computing the default value for the parameter
   */
  public void setDefaultValue(Expression expression) {
    defaultValue = becomeParentOf(expression);
  }

  /**
   * Set the formal parameter with which the default value is associated to the given parameter.
   * 
   * @param formalParameter the formal parameter with which the default value is associated
   */
  public void setParameter(NormalFormalParameter formalParameter) {
    parameter = becomeParentOf(formalParameter);
  }

  /**
   * Set the token separating the parameter from the default value to the given token.
   * 
   * @param separator the token separating the parameter from the default value
   */
  public void setSeparator(Token separator) {
    this.separator = separator;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(parameter, visitor);
    safelyVisitChild(defaultValue, visitor);
  }
}
