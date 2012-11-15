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
import com.google.dart.engine.utilities.dart.ParameterKind;

/**
 * Instances of the class {@code DefaultFormalParameter} represent a formal parameter with a default
 * value. There are two kinds of parameters that are both represented by this class: named formal
 * parameters and positional formal parameters.
 * 
 * <pre>
 * defaultFormalParameter ::=
 *     {@link NormalFormalParameter normalFormalParameter} ('=' {@link Expression defaultValue})?
 *
 * defaultNamedParameter ::=
 *     {@link NormalFormalParameter normalFormalParameter} (':' {@link Expression defaultValue})?
 * </pre>
 */
public class DefaultFormalParameter extends FormalParameter {
  /**
   * The formal parameter with which the default value is associated.
   */
  private NormalFormalParameter parameter;

  /**
   * The kind of this parameter.
   */
  private ParameterKind kind;

  /**
   * The token separating the parameter from the default value, or {@code null} if there is no
   * default value.
   */
  private Token separator;

  /**
   * The expression computing the default value for the parameter, or {@code null} if there is no
   * default value.
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
   * @param kind the kind of this parameter
   * @param separator the token separating the parameter from the default value
   * @param defaultValue the expression computing the default value for the parameter
   */
  public DefaultFormalParameter(NormalFormalParameter parameter, ParameterKind kind,
      Token separator, Expression defaultValue) {
    this.parameter = becomeParentOf(parameter);
    this.kind = kind;
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
   * Return the expression computing the default value for the parameter, or {@code null} if there
   * is no default value.
   * 
   * @return the expression computing the default value for the parameter
   */
  public Expression getDefaultValue() {
    return defaultValue;
  }

  @Override
  public Token getEndToken() {
    if (defaultValue != null) {
      return defaultValue.getEndToken();
    }
    return parameter.getEndToken();
  }

  @Override
  public ParameterKind getKind() {
    return kind;
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
   * Return the token separating the parameter from the default value, or {@code null} if there is
   * no default value.
   * 
   * @return the token separating the parameter from the default value
   */
  public Token getSeparator() {
    return separator;
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
   * Set the kind of this parameter to the given kind.
   * 
   * @param kind the kind of this parameter
   */
  public void setKind(ParameterKind kind) {
    this.kind = kind;
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
