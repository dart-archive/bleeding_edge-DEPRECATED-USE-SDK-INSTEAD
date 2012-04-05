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
 * Instances of the class <code>FunctionDeclaration</code> wrap a {@link FunctionExpression function
 * expression} as a top-level declaration.
 * 
 * <pre>
 * functionDeclaration ::=
 *     functionSignature {@link FunctionBody functionBody}
 *
 * functionSignature ::=
 *     {@link Type returnType}? {@link SimpleIdentifier functionName} {@link FormalParameterList formalParameterList}
 * </pre>
 */
public class FunctionDeclaration extends CompilationUnitMember {
  /**
   * The function expression being wrapped.
   */
  private FunctionExpression functionExpression;

  /**
   * Initialize a newly created function declaration.
   */
  public FunctionDeclaration() {
  }

  /**
   * Initialize a newly created function declaration.
   * 
   * @param returnType the return type of the function
   * @param name the name of the function
   * @param parameters the parameters associated with the function
   * @param body the body of the function
   */
  public FunctionDeclaration(FunctionExpression functionExpression) {
    this.functionExpression = becomeParentOf(functionExpression);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitFunctionDeclaration(this);
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
