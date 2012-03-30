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
 * Instances of the class <code>NamedFormalParameter</code> represent a formal parameter with a
 * default value.
 * 
 * <pre>
 * namedFormalParameter ::=
 *     {@link NormalFormalParameter parameter} '=' {@link Expression defaultValue}
 * </pre>
 */
public class NamedFormalParameter extends FormalParameter {
  /**
   * The formal parameter with which the default value is associated.
   */
  private NormalFormalParameter parameter;

  /**
   * The equal sign separating the parameter from the default value.
   */
  private Token equals;

  /**
   * The expression computing the default value for the parameter.
   */
  private Expression defaultValue;

  /**
   * Initialize a newly created named formal parameter.
   */
  public NamedFormalParameter() {
  }

  /**
   * Initialize a newly created named formal parameter.
   * 
   * @param parameter the formal parameter with which the default value is associated
   * @param equals the equal sign separating the parameter from the default value
   * @param defaultValue the expression computing the default value for the parameter
   */
  public NamedFormalParameter(NormalFormalParameter parameter, Token equals, Expression defaultValue) {
    this.parameter = becomeParentOf(parameter);
    this.equals = equals;
    this.defaultValue = becomeParentOf(defaultValue);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitNamedFormalParameter(this);
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
   * Return the equal sign separating the parameter from the default value.
   * 
   * @return the equal sign separating the parameter from the default value
   */
  public Token getEquals() {
    return equals;
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
   * Set the expression computing the default value for the parameter to the given expression.
   * 
   * @param expression the expression computing the default value for the parameter
   */
  public void setDefaultValue(Expression expression) {
    defaultValue = becomeParentOf(expression);
  }

  /**
   * Set the equal sign separating the parameter from the default value to the given token.
   * 
   * @param equals the equal sign separating the parameter from the default value
   */
  public void setEquals(Token equals) {
    this.equals = equals;
  }

  /**
   * Set the formal parameter with which the default value is associated to the given parameter.
   * 
   * @param formalParameter the formal parameter with which the default value is associated
   */
  public void setParameter(NormalFormalParameter formalParameter) {
    parameter = becomeParentOf(formalParameter);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(parameter, visitor);
    safelyVisitChild(defaultValue, visitor);
  }
}
