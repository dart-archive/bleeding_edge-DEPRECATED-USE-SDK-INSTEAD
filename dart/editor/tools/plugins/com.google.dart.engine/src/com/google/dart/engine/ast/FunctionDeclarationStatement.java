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
 * Instances of the class <code>FunctionDeclarationStatement</code> wrap a
 * {@link FunctionExpression function expression} as a statement.
 */
public class FunctionDeclarationStatement extends Statement {
  /**
   * The function expression being wrapped.
   */
  private FunctionExpression functionExpression;

  /**
   * Initialize a newly created function declaration statement.
   */
  public FunctionDeclarationStatement() {
  }

  /**
   * Initialize a newly created function declaration statement.
   * 
   * @param functionExpression the the function expression being wrapped
   */
  public FunctionDeclarationStatement(FunctionExpression functionExpression) {
    this.functionExpression = becomeParentOf(functionExpression);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitFunctionDeclarationStatement(this);
  }

  @Override
  public Token getBeginToken() {
    return functionExpression.getBeginToken();
  }

  @Override
  public Token getEndToken() {
    return functionExpression.getEndToken();
  }

  /**
   * Return the function expression being wrapped.
   * 
   * @return the function expression being wrapped
   */
  public FunctionExpression getFunctionExpression() {
    return functionExpression;
  }

  /**
   * Set the function expression being wrapped to the given function expression.
   * 
   * @param functionExpression the function expression being wrapped
   */
  public void setFunctionExpression(FunctionExpression functionExpression) {
    functionExpression = becomeParentOf(functionExpression);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(functionExpression, visitor);
  }
}
