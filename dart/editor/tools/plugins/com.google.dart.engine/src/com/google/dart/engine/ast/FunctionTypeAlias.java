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

import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.scanner.Token;

import java.util.List;

/**
 * Instances of the class {@code FunctionTypeAlias} represent a function type alias.
 * 
 * <pre>
 * functionTypeAlias ::=
 *      functionPrefix {@link TypeParameterList typeParameterList}? {@link FormalParameterList formalParameterList} ';'
 *
 * functionPrefix ::=
 *     {@link TypeName returnType}? {@link SimpleIdentifier name}
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class FunctionTypeAlias extends TypeAlias {
  /**
   * The name of the return type of the function type being defined, or {@code null} if no return
   * type was given.
   */
  private TypeName returnType;

  /**
   * The name of the function type being declared.
   */
  private SimpleIdentifier name;

  /**
   * The type parameters for the function type, or {@code null} if the function type does not have
   * any type parameters.
   */
  private TypeParameterList typeParameters;

  /**
   * The parameters associated with the function type.
   */
  private FormalParameterList parameters;

  /**
   * Initialize a newly created function type alias.
   * 
   * @param comment the documentation comment associated with this type alias
   * @param metadata the annotations associated with this type alias
   * @param keyword the token representing the 'typedef' keyword
   * @param returnType the name of the return type of the function type being defined
   * @param name the name of the type being declared
   * @param typeParameters the type parameters for the type
   * @param parameters the parameters associated with the function
   * @param semicolon the semicolon terminating the declaration
   */
  public FunctionTypeAlias(Comment comment, List<Annotation> metadata, Token keyword,
      TypeName returnType, SimpleIdentifier name, TypeParameterList typeParameters,
      FormalParameterList parameters, Token semicolon) {
    super(comment, metadata, keyword, semicolon);
    this.returnType = becomeParentOf(returnType);
    this.name = becomeParentOf(name);
    this.typeParameters = becomeParentOf(typeParameters);
    this.parameters = becomeParentOf(parameters);
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitFunctionTypeAlias(this);
  }

  @Override
  public FunctionTypeAliasElement getElement() {
    return name != null ? (FunctionTypeAliasElement) name.getStaticElement() : null;
  }

  /**
   * Return the name of the function type being declared.
   * 
   * @return the name of the function type being declared
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
   * Return the name of the return type of the function type being defined, or {@code null} if no
   * return type was given.
   * 
   * @return the name of the return type of the function type being defined
   */
  public TypeName getReturnType() {
    return returnType;
  }

  /**
   * Return the type parameters for the function type, or {@code null} if the function type does not
   * have any type parameters.
   * 
   * @return the type parameters for the function type
   */
  public TypeParameterList getTypeParameters() {
    return typeParameters;
  }

  /**
   * Set the name of the function type being declared to the given identifier.
   * 
   * @param name the name of the function type being declared
   */
  public void setName(SimpleIdentifier name) {
    this.name = becomeParentOf(name);
  }

  /**
   * Set the parameters associated with the function type to the given list of parameters.
   * 
   * @param parameters the parameters associated with the function type
   */
  public void setParameters(FormalParameterList parameters) {
    this.parameters = becomeParentOf(parameters);
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
   * Set the type parameters for the function type to the given list of parameters.
   * 
   * @param typeParameters the type parameters for the function type
   */
  public void setTypeParameters(TypeParameterList typeParameters) {
    this.typeParameters = becomeParentOf(typeParameters);
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(returnType, visitor);
    safelyVisitChild(name, visitor);
    safelyVisitChild(typeParameters, visitor);
    safelyVisitChild(parameters, visitor);
  }
}
