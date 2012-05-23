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
 * Instances of the class <code>FunctionTypedFormalParameter</code> represent a function-typed
 * formal parameter.
 * 
 * <pre>
 * functionSignature ::=
 *     {@link TypeName returnType}? {@link SimpleIdentifier identifier} {@link FormalParameterList formalParameterList}
 * </pre>
 */
public class FunctionTypedFormalParameter extends NormalFormalParameter {
  /**
   * The return type of the function, or <code>null</code> if the function does not have a return
   * type.
   */
  private TypeName returnType;

  /**
   * The name of the function-typed parameter.
   */
  private SimpleIdentifier identifier;

  /**
   * The parameters of the function-typed parameter.
   */
  private FormalParameterList parameters;

  /**
   * Initialize a newly created formal parameter.
   */
  public FunctionTypedFormalParameter() {
  }

  /**
   * Initialize a newly created formal parameter.
   * 
   * @param returnType the return type of the function, or <code>null</code> if the function does
   *          not have a return type
   * @param identifier the name of the function-typed parameter
   * @param parameters the parameters of the function-typed parameter
   */
  public FunctionTypedFormalParameter(Token keyword, TypeName returnType,
      SimpleIdentifier identifier, FormalParameterList parameters) {
    this.returnType = becomeParentOf(returnType);
    this.identifier = becomeParentOf(identifier);
    this.parameters = becomeParentOf(parameters);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitFunctionTypedFormalParameter(this);
  }

  @Override
  public Token getBeginToken() {
    if (returnType != null) {
      return returnType.getBeginToken();
    }
    return identifier.getBeginToken();
  }

  @Override
  public Token getEndToken() {
    return parameters.getEndToken();
  }

  /**
   * Return the name of the parameter.
   * 
   * @return the name of the parameter
   */
  public SimpleIdentifier getIdentifier() {
    return identifier;
  }

  /**
   * Return the parameters of the function-typed parameter.
   * 
   * @return the parameters of the function-typed parameter
   */
  public FormalParameterList getParameters() {
    return parameters;
  }

  /**
   * Return the return type of the function, or <code>null</code> if the function does not have a
   * return type.
   * 
   * @return the return type of the function
   */
  public TypeName getReturnType() {
    return returnType;
  }

  /**
   * Set the name of the parameter to the given identifier.
   * 
   * @param identifier the name of the parameter
   */
  public void setIdentifier(SimpleIdentifier identifier) {
    this.identifier = becomeParentOf(identifier);
  }

  /**
   * Set the parameters of the function-typed parameter to the given parameters.
   * 
   * @param parameters the parameters of the function-typed parameter
   */
  public void setParameters(FormalParameterList parameters) {
    this.parameters = becomeParentOf(parameters);
  }

  /**
   * Set the return type of the function to the given type.
   * 
   * @param returnType the return type of the function
   */
  public void setReturnType(TypeName returnType) {
    this.returnType = becomeParentOf(returnType);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(returnType, visitor);
    safelyVisitChild(identifier, visitor);
    safelyVisitChild(parameters, visitor);
  }
}
