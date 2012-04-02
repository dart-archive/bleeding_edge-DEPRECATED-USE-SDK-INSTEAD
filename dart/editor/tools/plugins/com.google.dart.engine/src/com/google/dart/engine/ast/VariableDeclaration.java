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
 * Instances of the class <code>VariableDeclaration</code> represent an identifier that has an
 * initial value associated with it. Instances of this class are always children of the class
 * {@link VariableDeclarationList}.
 * 
 * <pre>
 * variableDeclaration ::=
 *     {@link SimpleIdentifier identifier} '=' {@link Expression initialValue}
 * </pre>
 */
public class VariableDeclaration extends Declaration {
  /**
   * The name of the variable being declared.
   */
  private SimpleIdentifier name;

  /**
   * The equal sign separating the variable name from the initial value, or <code>null</code> if the
   * initial value was not specified.
   */
  private Token equals;

  /**
   * The expression used to compute the initial value for the variable, or <code>null</code> if the
   * initial value was not specified.
   */
  private Expression initializer;

  /**
   * Initialize a newly created variable declaration.
   */
  public VariableDeclaration() {
  }

  /**
   * Initialize a newly created variable declaration.
   * 
   * @param comment the documentation comment associated with this declaration
   * @param name the name of the variable being declared
   * @param equals the equal sign separating the variable name from the initial value
   * @param initializer the expression used to compute the initial value for the variable
   */
  public VariableDeclaration(Comment comment, SimpleIdentifier name, Token equals,
      Expression initializer) {
    super(comment);
    this.name = becomeParentOf(name);
    this.equals = equals;
    this.initializer = becomeParentOf(initializer);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitVariableDeclaration(this);
  }

  @Override
  public Token getBeginToken() {
    return name.getBeginToken();
  }

  @Override
  public Token getEndToken() {
    if (initializer != null) {
      return initializer.getEndToken();
    }
    return name.getEndToken();
  }

  /**
   * Return the equal sign separating the variable name from the initial value, or <code>null</code>
   * if the initial value was not specified.
   * 
   * @return the equal sign separating the variable name from the initial value
   */
  public Token getEquals() {
    return equals;
  }

  /**
   * Return the expression used to compute the initial value for the variable, or <code>null</code>
   * if the initial value was not specified.
   * 
   * @return the expression used to compute the initial value for the variable
   */
  public Expression getInitializer() {
    return initializer;
  }

  /**
   * Return the name of the variable being declared.
   * 
   * @return the name of the variable being declared
   */
  public SimpleIdentifier getName() {
    return name;
  }

  /**
   * Set the equal sign separating the variable name from the initial value to the given token.
   * 
   * @param equals the equal sign separating the variable name from the initial value
   */
  public void setEquals(Token equals) {
    this.equals = equals;
  }

  /**
   * Set the expression used to compute the initial value for the variable to the given expression.
   * 
   * @param initializer the expression used to compute the initial value for the variable
   */
  public void setInitializer(Expression initializer) {
    this.initializer = becomeParentOf(initializer);
  }

  /**
   * Set the name of the variable being declared to the given identifier.
   * 
   * @param name the name of the variable being declared
   */
  public void setName(SimpleIdentifier name) {
    this.name = becomeParentOf(name);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(name, visitor);
    safelyVisitChild(initializer, visitor);
  }
}
