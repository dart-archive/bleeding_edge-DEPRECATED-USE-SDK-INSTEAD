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
 * Instances of the class {@code FunctionDeclarationStatement} wrap a {@link FunctionDeclaration
 * function declaration} as a statement.
 * 
 * @coverage dart.engine.ast
 */
public class FunctionDeclarationStatement extends Statement {
  /**
   * The function declaration being wrapped.
   */
  private FunctionDeclaration functionDeclaration;

  /**
   * Initialize a newly created function declaration statement.
   * 
   * @param functionDeclaration the the function declaration being wrapped
   */
  public FunctionDeclarationStatement(FunctionDeclaration functionDeclaration) {
    this.functionDeclaration = becomeParentOf(functionDeclaration);
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitFunctionDeclarationStatement(this);
  }

  @Override
  public Token getBeginToken() {
    return functionDeclaration.getBeginToken();
  }

  @Override
  public Token getEndToken() {
    return functionDeclaration.getEndToken();
  }

  /**
   * Return the function declaration being wrapped.
   * 
   * @return the function declaration being wrapped
   */
  public FunctionDeclaration getFunctionDeclaration() {
    return functionDeclaration;
  }

  /**
   * Set the function declaration being wrapped to the given function declaration.
   * 
   * @param functionDeclaration the function declaration being wrapped
   */
  public void setFunctionExpression(FunctionDeclaration functionDeclaration) {
    this.functionDeclaration = becomeParentOf(functionDeclaration);
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(functionDeclaration, visitor);
  }
}
