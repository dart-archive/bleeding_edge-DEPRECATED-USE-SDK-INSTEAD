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
   * The left angle bracket, or <code>null</code> if there are no type parameters.
   */
  private Token leftAngleBracket;

  /**
   * The type parameters for the type.
   */
  private NodeList<TypeParameter> typeParameters = new NodeList<TypeParameter>(this);

  /**
   * The right angle bracket, or <code>null</code> if there are no type parameters.
   */
  private Token rightAngleBracket;

  /**
   * The left curly bracket.
   */
  private Token leftCurlyBracket;

  /**
   * The members defined by the type.
   */
  private NodeList<TypeMember> members = new NodeList<TypeMember>(this);

  /**
   * The right curly bracket.
   */
  private Token rightCurlyBracket;

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
   * @param leftAngleBracket the left angle bracket
   * @param typeParameters the type parameters for the type
   * @param rightAngleBracket the right angle bracket
   * @param leftCurlyBracket the left curly bracket
   * @param members the members defined by the type
   * @param rightCurlyBracket the right curly bracket
   */
  public TypeDeclaration(Comment comment, Token keyword, SimpleIdentifier name,
      Token leftAngleBracket, List<TypeParameter> typeParameters, Token rightAngleBracket,
      Token leftCurlyBracket, List<TypeMember> members, Token rightCurlyBracket) {
    super(comment);
    this.keyword = keyword;
    this.name = becomeParentOf(name);
    this.leftAngleBracket = leftAngleBracket;
    this.typeParameters.addAll(typeParameters);
    this.rightAngleBracket = rightAngleBracket;
    this.leftCurlyBracket = leftCurlyBracket;
    this.members.addAll(members);
    this.rightCurlyBracket = rightCurlyBracket;
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
    return rightCurlyBracket;
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
   * Return the left angle bracket, or <code>null</code> if there are no type parameters.
   * 
   * @return the left angle bracket
   */
  public Token getLeftAngleBracket() {
    return leftAngleBracket;
  }

  /**
   * Return the left curly bracket.
   * 
   * @return the left curly bracket
   */
  public Token getLeftBracket() {
    return leftCurlyBracket;
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
   * Return the right angle bracket, or <code>null</code> if there are no type parameters.
   * 
   * @return the right angle bracket
   */
  public Token getRightAngleBracket() {
    return rightAngleBracket;
  }

  /**
   * Return the right curly bracket.
   * 
   * @return the right curly bracket
   */
  public Token getRightBracket() {
    return rightCurlyBracket;
  }

  /**
   * Return the type parameters for the type.
   * 
   * @return the type parameters for the type
   */
  public NodeList<TypeParameter> getTypeParameters() {
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
   * Set the left angle bracket to the given token.
   * 
   * @param bracket the left angle bracket
   */
  public void setLeftAngleBracket(Token bracket) {
    leftAngleBracket = bracket;
  }

  /**
   * Set the left curly bracket to the given token.
   * 
   * @param leftBracket the left curly bracket
   */
  public void setLeftBracket(Token leftBracket) {
    this.leftCurlyBracket = leftBracket;
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
   * Set the right angle bracket to the given token.
   * 
   * @param bracket the right angle bracket
   */
  public void setRightAngleBracket(Token bracket) {
    rightAngleBracket = bracket;
  }

  /**
   * Set the right curly bracket to the given token.
   * 
   * @param rightBracket the right curly bracket
   */
  public void setRightBracket(Token rightBracket) {
    this.rightCurlyBracket = rightBracket;
  }
}
