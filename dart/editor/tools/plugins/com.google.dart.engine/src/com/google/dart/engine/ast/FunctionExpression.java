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
 * Instances of the class <code>FunctionExpression</code> represent a function expression.
 * 
 * <pre>
 * functionExpression ::=
 *     ({@link Type returnType}? {@link SimpleIdentifier functionName})? {@link FormalParameterList formalParameterList} {@link FunctionBody functionBody}
 * </pre>
 */
public class FunctionExpression extends Expression {
  /**
   * The return type of the function, or <code>null</code> if no return type was declared.
   */
  private TypeName returnType;

  /**
   * The name of the function, or <code>null</code> if the function is not named.
   */
  private SimpleIdentifier name;

  /**
   * The parameters associated with the function.
   */
  private FormalParameterList parameters;

  /**
   * The body of the function.
   */
  private FunctionBody body;

  /**
   * Initialize a newly created function declaration.
   */
  public FunctionExpression() {
  }

  /**
   * Initialize a newly created function declaration.
   * 
   * @param returnType the return type of the function
   * @param name the name of the function
   * @param parameters the parameters associated with the function
   * @param body the body of the function
   */
  public FunctionExpression(TypeName returnType, SimpleIdentifier name,
      FormalParameterList parameters, FunctionBody body) {
    this.returnType = becomeParentOf(returnType);
    this.name = becomeParentOf(name);
    this.parameters = becomeParentOf(parameters);
    this.body = becomeParentOf(body);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitFunctionExpression(this);
  }

  @Override
  public Token getBeginToken() {
    if (returnType != null) {
      return returnType.getBeginToken();
    } else if (name != null) {
      return name.getBeginToken();
    }
    return parameters.getBeginToken();
  }

  /**
   * Return the body of the function.
   * 
   * @return the body of the function
   */
  public FunctionBody getBody() {
    return body;
  }

  @Override
  public Token getEndToken() {
    return body.getEndToken();
  }

  /**
   * Return the name of the function, or <code>null</code> if the function is not named.
   * 
   * @return the name of the function
   */
  public SimpleIdentifier getName() {
    return name;
  }

  /**
   * Return the parameters associated with the function.
   * 
   * @return the parameters associated with the function
   */
  public FormalParameterList getParameters() {
    return parameters;
  }

  /**
   * Return the return type of the function, or <code>null</code> if no return type was declared.
   * 
   * @return the return type of the function
   */
  public TypeName getReturnType() {
    return returnType;
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
   * Set the name of the function to the given identifier.
   * 
   * @param identifier the name of the function
   */
  public void setName(SimpleIdentifier identifier) {
    name = becomeParentOf(identifier);
  }

  /**
   * Set the parameters associated with the function to the given list of parameters.
   * 
   * @param parameters the parameters associated with the function
   */
  public void setParameters(FormalParameterList parameters) {
    this.parameters = becomeParentOf(parameters);
  }

  /**
   * Set the return type of the function to the given name.
   * 
   * @param name the return type of the function
   */
  public void setReturnType(TypeName name) {
    returnType = becomeParentOf(name);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(returnType, visitor);
    safelyVisitChild(name, visitor);
    safelyVisitChild(parameters, visitor);
    safelyVisitChild(body, visitor);
  }
}
