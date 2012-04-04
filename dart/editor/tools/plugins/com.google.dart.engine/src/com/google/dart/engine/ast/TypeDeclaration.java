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
 * The abstract class <code>TypeDeclaration</code> defines the behavior common to the declaration of
 * either a class or an interface.
 * 
 * <pre>
 * typeDeclaration ::=
 *     {@link ClassDeclaration classDeclaration}
 *   | {@link InterfaceDeclaration interfaceDeclaration}
 * </pre>
 */
public abstract class TypeDeclaration extends CompilationUnitMember {
  /**
   * The token representing the 'class' or 'interface' keyword.
   */
  private Token keyword;

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
   * The left curly bracket.
   */
  private Token leftBracket;

  /**
   * The members defined by the type.
   */
  private NodeList<TypeMember> members = new NodeList<TypeMember>(this);

  /**
   * The right curly bracket.
   */
  private Token rightBracket;

  /**
   * Initialize a newly created type declaration.
   */
  public TypeDeclaration() {
  }

  /**
   * Initialize a newly created type declaration.
   * 
   * @param comment the documentation comment associated with this member
   * @param keyword the token representing the 'class' or 'interface' keyword
   * @param name the name of the type being declared
   * @param typeParameters the type parameters for the type
   * @param leftBracket the left curly bracket
   * @param members the members defined by the type
   * @param rightBracket the right curly bracket
   */
  public TypeDeclaration(Comment comment, Token keyword, SimpleIdentifier name,
      TypeParameterList typeParameters, Token leftBracket, List<TypeMember> members,
      Token rightBracket) {
    super(comment);
    this.keyword = keyword;
    this.name = becomeParentOf(name);
    this.typeParameters = becomeParentOf(typeParameters);
    this.leftBracket = leftBracket;
    this.members.addAll(members);
    this.rightBracket = rightBracket;
  }

  @Override
  public Token getBeginToken() {
    Comment comment = getDocumentationComment();
    if (comment != null) {
      return comment.getBeginToken();
    }
    return keyword;
  }

  @Override
  public Token getEndToken() {
    return rightBracket;
  }

  /**
   * Return the token representing the 'class' or 'interface' keyword.
   * 
   * @return the token representing the 'class' or 'interface' keyword
   */
  public Token getKeyword() {
    return keyword;
  }

  /**
   * Return the left curly bracket.
   * 
   * @return the left curly bracket
   */
  public Token getLeftBracket() {
    return leftBracket;
  }

  /**
   * Return the members defined by the type.
   * 
   * @return the members defined by the type
   */
  public NodeList<TypeMember> getMembers() {
    return members;
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
   * Return the right curly bracket.
   * 
   * @return the right curly bracket
   */
  public Token getRightBracket() {
    return rightBracket;
  }

  /**
   * Return the type parameters for the type, or <code>null</code> if the type does not have any
   * type parameters.
   * 
   * @return the type parameters for the type
   */
  public TypeParameterList getTypeParameters() {
    return typeParameters;
  }

  /**
   * Set the token representing the 'class' or 'interface' keyword to the given token.
   * 
   * @param keyword the token representing the 'class' or 'interface' keyword
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
  }

  /**
   * Set the left curly bracket to the given token.
   * 
   * @param leftBracket the left curly bracket
   */
  public void setLeftBracket(Token leftBracket) {
    this.leftBracket = leftBracket;
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
   * Set the right curly bracket to the given token.
   * 
   * @param rightBracket the right curly bracket
   */
  public void setRightBracket(Token rightBracket) {
    this.rightBracket = rightBracket;
  }

  /**
   * Set the type parameters for the type to the given list of type parameters.
   * 
   * @param typeParameters the type parameters for the type
   */
  public void setTypeParameters(TypeParameterList typeParameters) {
    this.typeParameters = typeParameters;
  }
}
