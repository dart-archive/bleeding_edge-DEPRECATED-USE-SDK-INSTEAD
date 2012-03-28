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
 * The abstract class <code>TypedLiteral</code> defines the behavior common to literals that have a
 * type associated with them.
 * 
 * <pre>
 * listLiteral ::=
 *     {@link ListLiteral listLiteral}
 *   | {@link MapLiteral mapLiteral}
 * </pre>
 */
public abstract class TypedLiteral extends Literal {
  /**
   * The const modifier associated with this literal, or <code>null</code> if the literal is not a
   * constant.
   */
  private Token modifier;

  /**
   * The left angle bracket.
   */
  private Token leftAngleBracket;

  /**
   * The name of the type of the elements of the literal.
   */
  private TypeName typeArgument;

  /**
   * The right angle bracket.
   */
  private Token rightAngleBracket;

  /**
   * Initialize a newly created typed literal.
   */
  public TypedLiteral() {
  }

  /**
   * Initialize a newly created typed literal.
   * 
   * @param modifier the const modifier associated with this literal
   * @param leftAngleBracket the left angle bracket
   * @param typeArgument the name of the type of the elements of the literal
   * @param rightAngleBracket the right angle bracket
   */
  public TypedLiteral(Token modifier, Token leftAngleBracket, TypeName typeArgument,
      Token rightAngleBracket) {
    this.modifier = modifier;
    this.leftAngleBracket = leftAngleBracket;
    this.typeArgument = becomeParentOf(typeArgument);
    this.rightAngleBracket = rightAngleBracket;
  }

  /**
   * Return the left angle bracket.
   * 
   * @return the left angle bracket
   */
  public Token getLeftAngleBracket() {
    return leftAngleBracket;
  }

  /**
   * Return the const modifier associated with this literal.
   * 
   * @return the const modifier associated with this literal
   */
  public Token getModifier() {
    return modifier;
  }

  /**
   * Return the right angle bracket.
   * 
   * @return the right angle bracket
   */
  public Token getRightAngleBracket() {
    return rightAngleBracket;
  }

  /**
   * Return the name of the type of the elements of the literal.
   * 
   * @return the name of the type of the elements of the literal
   */
  public TypeName getTypeArgument() {
    return typeArgument;
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
   * Set the modifiers associated with this literal to the given modifiers.
   * 
   * @param modifiers the modifiers associated with this literal
   */
  public void setModifier(Token modifier) {
    this.modifier = modifier;
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
   * Set the name of the type of the elements of the literal to the given type name.
   * 
   * @param typeName the name of the type of the elements of the literal
   */
  public void setTypeArgument(TypeName typeName) {
    typeArgument = becomeParentOf(typeName);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(typeArgument, visitor);
  }
}
