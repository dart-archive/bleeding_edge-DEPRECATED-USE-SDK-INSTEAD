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
 * Instances of the class <code>InitializedFormalParameter</code> represent a formal parameter that
 * has an initializer associated with it.
 * 
 * <pre>
 * initializedFormalParameter ::=
 *     {@link SimpleFormalParameter simpleFormalParameter} '=' {@link Expression expression}
 * </pre>
 */
public class InitializedFormalParameter extends SimpleFormalParameter {
  /**
   * The equals token that introduces the initialization expression.
   */
  private Token equals;

  /**
   * The expression whose value will be used as the initial value for the parameter.
   */
  private Expression initializer;

  /**
   * Initialize a newly created initialized formal parameter.
   */
  public InitializedFormalParameter() {
  }

  /**
   * Initialize a newly created initialized formal parameter.
   * 
   * @param keyword the token representing either the 'final' or 'var' keyword
   * @param type the declared type of the parameter
   * @param identifier the name of the parameter being declared
   * @param initializer the expression whose value will be used as the initial value for the
   *          parameter
   */
  public InitializedFormalParameter(Token keyword, TypeName type, SimpleIdentifier identifier,
      Token equals, Expression initializer) {
    super(keyword, type, identifier);
    this.equals = equals;
    this.initializer = initializer;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitInitializedFormalParameter(this);
  }

  @Override
  public Token getEndToken() {
    return initializer.getEndToken();
  }

  /**
   * Return the equals token that introduces the initialization expression.
   * 
   * @return the equals token that introduces the initialization expression
   */
  public Token getEquals() {
    return equals;
  }

  /**
   * Return the expression whose value will be used as the initial value for the parameter.
   * 
   * @return the expression whose value will be used as the initial value for the parameter
   */
  public Expression getInitializer() {
    return initializer;
  }

  /**
   * Set the equals token that introduces the initialization expression to the given token.
   * 
   * @param equals the equals token that introduces the initialization expression
   */
  public void setEquals(Token equals) {
    this.equals = equals;
  }

  /**
   * Set the expression whose value will be used as the initial value for the parameter to the given
   * expression.
   * 
   * @param expression the expression whose value will be used as the initial value for the
   *          parameter
   */
  public void setInitializer(Expression expression) {
    initializer = becomeParentOf(expression);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(initializer, visitor);
  }
}
