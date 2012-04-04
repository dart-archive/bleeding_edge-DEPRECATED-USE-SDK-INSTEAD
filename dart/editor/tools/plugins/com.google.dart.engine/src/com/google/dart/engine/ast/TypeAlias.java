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
 * Instances of the class <code>TypeAlias</code> represent a type alias.
 * 
 * <pre>
 * typeAlias ::=
 *     'typedef' {@link TypeName returnType}? {@link SimpleIdentifier name}
 *     {@link TypeParameterList typeParameterList}? {@link FormalParameterList formalParameterList} ';'
 * </pre>
 */
public class TypeAlias extends CompilationUnitMember {
  /**
   * The token representing the 'typedef' keyword.
   */
  private Token keyword;

  /**
   * The name of the return type of the function type being defined.
   */
  private TypeName returnType;

  /**
   * The name of the type being declared.
   */
  private SimpleIdentifier name;

  /**
   * The type parameters for the type, or <code>null</code> if the type does not have any type
   * parameters.
   */
  private TypeParameterList typeParameters;

  /**
   * The parameters associated with the function.
   */
  private FormalParameterList parameters;

  /**
   * The semicolon terminating the declaration.
   */
  private Token semicolon;

  /**
   * Initialize a newly created type alias.
   */
  public TypeAlias() {
  }

  /**
   * Initialize a newly created type alias.
   * 
   * @param comment the documentation comment associated with this member
   * @param keyword the token representing the 'typedef' keyword
   * @param returnType the name of the return type of the function type being defined
   * @param name the name of the type being declared
   * @param typeParameters the type parameters for the type
   * @param parameters the parameters associated with the function
   * @param semicolon the semicolon terminating the declaration
   */
  public TypeAlias(Comment comment, Token keyword, TypeName returnType, SimpleIdentifier name,
      TypeParameterList typeParameters, FormalParameterList parameters, Token semicolon) {
    super(comment);
    this.keyword = keyword;
    this.returnType = becomeParentOf(returnType);
    this.name = becomeParentOf(name);
    this.typeParameters = becomeParentOf(typeParameters);
    this.parameters = becomeParentOf(parameters);
    this.semicolon = semicolon;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitTypeAlias(this);
  }

  @Override
  public Token getBeginToken() {
    return keyword;
  }

  @Override
  public Token getEndToken() {
    return semicolon;
  }

  /**
   * Return the token representing the 'typedef' keyword.
   * 
   * @return the token representing the 'typedef' keyword
   */
  public Token getKeyword() {
    return keyword;
  }

  /**
   * Return the name of the type being declared.
   * 
   * @return the name of the type being declared
   */
  public SimpleIdentifier getName() {
    return name;
  }

  /**
   * Return the parameters associated with the function type.
   * 
   * @return the parameters associated with the function type
   */
  public FormalParameterList getParameters() {
    return parameters;
  }

  /**
   * Return the name of the return type of the function type being defined.
   * 
   * @return the name of the return type of the function type being defined
   */
  public TypeName getReturnType() {
    return returnType;
  }

  /**
   * Return the semicolon terminating the declaration.
   * 
   * @return the semicolon terminating the declaration
   */
  public Token getSemicolon() {
    return semicolon;
  }

  /**
   * Return the type parameters for the function type, or <code>null</code> if the type does not
   * have any type parameters.
   * 
   * @return the type parameters for the function type
   */
  public TypeParameterList getTypeParameters() {
    return typeParameters;
  }

  /**
   * Set the token representing the 'typedef' keyword to the given token.
   * 
   * @param keyword the token representing the 'typedef' keyword
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
  }

  /**
   * Set the name of the type being declared to the given identifier.
   * 
   * @param identifier the name of the type being declared
   */
  public void setName(SimpleIdentifier identifier) {
    name = becomeParentOf(identifier);
  }

  /**
   * Set the parameters associated with the function type to the given list of parameters.
   * 
   * @param parameters the parameters associated with the function type
   */
  public void setParameters(FormalParameterList parameters) {
    this.parameters = parameters;
  }

  /**
   * Set the name of the return type of the function type being defined to the given type name.
   * 
   * @param typeName the name of the return type of the function type being defined
   */
  public void setReturnType(TypeName typeName) {
    returnType = becomeParentOf(typeName);
  }

  /**
   * Set the semicolon terminating the declaration to the given token.
   * 
   * @param semicolon the semicolon terminating the declaration
   */
  public void setSemicolon(Token semicolon) {
    this.semicolon = semicolon;
  }

  /**
   * Set the type parameters for the function type to the given list of parameters.
   * 
   * @param typeParameters the type parameters for the function type
   */
  public void setTypeParameters(TypeParameterList typeParameters) {
    this.typeParameters = typeParameters;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(getDocumentationComment(), visitor);
    safelyVisitChild(returnType, visitor);
    safelyVisitChild(name, visitor);
    safelyVisitChild(typeParameters, visitor);
    safelyVisitChild(parameters, visitor);
  }
}
