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

import java.util.List;

/**
 * Instances of the class <code>TypeAlias</code> represent a type alias.
 * 
 * <pre>
 * typeAlias ::=
 *     'typedef' {@link TypeName returnType}? {@link SimpleIdentifier name} typeParameters? formalParameterList ';'
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
   * The left bracket, or <code>null</code> if there are no type parameters.
   */
  private Token leftBracket;

  /**
   * The type parameters for the type.
   */
  private NodeList<TypeParameter> typeParameters = new NodeList<TypeParameter>(this);

  /**
   * The right bracket, or <code>null</code> if there are no type parameters.
   */
  private Token rightBracket;

  /**
   * The left parenthesis.
   */
  private Token leftParenthesis;

  /**
   * The parameters associated with the function.
   */
  private NodeList<FormalParameter> parameters = new NodeList<FormalParameter>(this);

  /**
   * The right parenthesis.
   */
  private Token rightParenthesis;

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
   * @param leftBracket the left bracket
   * @param typeParameters the type parameters for the type
   * @param rightBracket the right bracket
   * @param leftParenthesis the left parenthesis
   * @param parameters the parameters associated with the function
   * @param rightParenthesis the right parenthesis
   * @param semicolon the semicolon terminating the declaration
   */
  public TypeAlias(Comment comment, Token keyword, TypeName returnType, SimpleIdentifier name,
      Token leftBracket, List<TypeParameter> typeParameters, Token rightBracket,
      Token leftParenthesis, List<FormalParameter> parameters, Token rightParenthesis,
      Token semicolon) {
    super(comment);
    this.keyword = keyword;
    this.returnType = becomeParentOf(returnType);
    this.name = becomeParentOf(name);
    this.leftBracket = leftBracket;
    this.typeParameters.addAll(typeParameters);
    this.rightBracket = rightBracket;
    this.leftParenthesis = leftParenthesis;
    this.parameters.addAll(parameters);
    this.rightParenthesis = rightParenthesis;
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
   * Return the left bracket, or <code>null</code> if there are no type parameters.
   * 
   * @return the left bracket
   */
  public Token getLeftBracket() {
    return leftBracket;
  }

  /**
   * Return the left parenthesis.
   * 
   * @return the left parenthesis
   */
  public Token getLeftParenthesis() {
    return leftParenthesis;
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
  public NodeList<FormalParameter> getParameters() {
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
   * Return the right bracket, or <code>null</code> if there are no type parameters.
   * 
   * @return the right bracket
   */
  public Token getRightBracket() {
    return rightBracket;
  }

  /**
   * Return the right parenthesis.
   * 
   * @return the right parenthesis
   */
  public Token getRightParenthesis() {
    return rightParenthesis;
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
   * Return the type parameters for the function type.
   * 
   * @return the type parameters for the function type
   */
  public NodeList<TypeParameter> getTypeParameters() {
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
   * Set the left bracket to the given token.
   * 
   * @param bracket the left bracket
   */
  public void setLeftBracket(Token bracket) {
    leftBracket = bracket;
  }

  /**
   * Set the left parenthesis to the given token.
   * 
   * @param parenthesis the left parenthesis
   */
  public void setLeftParenthesis(Token parenthesis) {
    leftParenthesis = parenthesis;
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
   * Set the name of the return type of the function type being defined to the given type name.
   * 
   * @param typeName the name of the return type of the function type being defined
   */
  public void setReturnType(TypeName typeName) {
    returnType = becomeParentOf(typeName);
  }

  /**
   * Set the right bracket to the given token.
   * 
   * @param bracket the right bracket
   */
  public void setRightBracket(Token bracket) {
    rightBracket = bracket;
  }

  /**
   * Set the right parenthesis to the given token.
   * 
   * @param parenthesis the right parenthesis
   */
  public void setRightParenthesis(Token parenthesis) {
    rightParenthesis = parenthesis;
  }

  /**
   * Set the semicolon terminating the declaration to the given token.
   * 
   * @param semicolon the semicolon terminating the declaration
   */
  public void setSemicolon(Token semicolon) {
    this.semicolon = semicolon;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(getDocumentationComment(), visitor);
    safelyVisitChild(returnType, visitor);
    safelyVisitChild(name, visitor);
    typeParameters.accept(visitor);
    parameters.accept(visitor);
  }
}
