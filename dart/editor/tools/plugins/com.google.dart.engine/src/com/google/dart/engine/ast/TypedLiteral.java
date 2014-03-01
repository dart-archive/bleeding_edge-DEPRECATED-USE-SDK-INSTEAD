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
 * The abstract class {@code TypedLiteral} defines the behavior common to literals that have a type
 * associated with them.
 * 
 * <pre>
 * listLiteral ::=
 *     {@link ListLiteral listLiteral}
 *   | {@link MapLiteral mapLiteral}
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public abstract class TypedLiteral extends Literal {
  /**
   * The token representing the 'const' keyword, or {@code null} if the literal is not a constant.
   */
  private Token constKeyword;

  /**
   * The type argument associated with this literal, or {@code null} if no type arguments were
   * declared.
   */
  private TypeArgumentList typeArguments;

  /**
   * Initialize a newly created typed literal.
   * 
   * @param constKeyword the token representing the 'const' keyword
   * @param typeArguments the type argument associated with this literal, or {@code null} if no type
   *          arguments were declared
   */
  public TypedLiteral(Token constKeyword, TypeArgumentList typeArguments) {
    this.constKeyword = constKeyword;
    this.typeArguments = becomeParentOf(typeArguments);
  }

  /**
   * Return the token representing the 'const' keyword.
   * 
   * @return the token representing the 'const' keyword
   */
  public Token getConstKeyword() {
    return constKeyword;
  }

  /**
   * Return the type argument associated with this literal, or {@code null} if no type arguments
   * were declared.
   * 
   * @return the type argument associated with this literal
   */
  public TypeArgumentList getTypeArguments() {
    return typeArguments;
  }

  /**
   * Set the token representing the 'const' keyword to the given keyword.
   * 
   * @param keyword the token representing the 'const' keyword
   */
  public void setConstKeyword(Token keyword) {
    this.constKeyword = keyword;
  }

  /**
   * Set the type argument associated with this literal to the given arguments.
   * 
   * @param typeArguments the type argument associated with this literal
   */
  public void setTypeArguments(TypeArgumentList typeArguments) {
    this.typeArguments = typeArguments;
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(typeArguments, visitor);
  }
}
