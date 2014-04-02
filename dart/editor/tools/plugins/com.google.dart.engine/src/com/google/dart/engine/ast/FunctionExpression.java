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

import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.scanner.Token;

/**
 * Instances of the class {@code FunctionExpression} represent a function expression.
 * 
 * <pre>
 * functionExpression ::=
 *     {@link FormalParameterList formalParameterList} {@link FunctionBody functionBody}
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class FunctionExpression extends Expression {
  /**
   * The parameters associated with the function.
   */
  private FormalParameterList parameters;

  /**
   * The body of the function, or {@code null} if this is an external function.
   */
  private FunctionBody body;

  /**
   * The element associated with the function, or {@code null} if the AST structure has not been
   * resolved.
   */
  private ExecutableElement element;

  /**
   * Initialize a newly created function declaration.
   * 
   * @param parameters the parameters associated with the function
   * @param body the body of the function
   */
  public FunctionExpression(FormalParameterList parameters, FunctionBody body) {
    this.parameters = becomeParentOf(parameters);
    this.body = becomeParentOf(body);
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitFunctionExpression(this);
  }

  @Override
  public Token getBeginToken() {
    if (parameters != null) {
      return parameters.getBeginToken();
    } else if (body != null) {
      return body.getBeginToken();
    }
    // This should never be reached because external functions must be named, hence either the body
    // or the name should be non-null.
    throw new IllegalStateException("Non-external functions must have a body");
  }

  /**
   * Return the body of the function, or {@code null} if this is an external function.
   * 
   * @return the body of the function
   */
  public FunctionBody getBody() {
    return body;
  }

  /**
   * Return the element associated with this function, or {@code null} if the AST structure has not
   * been resolved.
   * 
   * @return the element associated with this function
   */
  public ExecutableElement getElement() {
    return element;
  }

  @Override
  public Token getEndToken() {
    if (body != null) {
      return body.getEndToken();
    } else if (parameters != null) {
      return parameters.getEndToken();
    }
    // This should never be reached because external functions must be named, hence either the body
    // or the name should be non-null.
    throw new IllegalStateException("Non-external functions must have a body");
  }

  /**
   * Return the parameters associated with the function.
   * 
   * @return the parameters associated with the function
   */
  public FormalParameterList getParameters() {
    return parameters;
  }

  @Override
  public int getPrecedence() {
    return 16;
  }

  /**
   * Set the body of the function to the given function body.
   * 
   * @param functionBody the body of the function
   */
  public void setBody(FunctionBody functionBody) {
    body = becomeParentOf(functionBody);
  }

  /**
   * Set the element associated with this function to the given element.
   * 
   * @param element the element associated with this function
   */
  public void setElement(ExecutableElement element) {
    this.element = element;
  }

  /**
   * Set the parameters associated with the function to the given list of parameters.
   * 
   * @param parameters the parameters associated with the function
   */
  public void setParameters(FormalParameterList parameters) {
    this.parameters = becomeParentOf(parameters);
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(parameters, visitor);
    safelyVisitChild(body, visitor);
  }
}
