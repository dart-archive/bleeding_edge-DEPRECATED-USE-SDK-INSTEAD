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
   * The type argument associated with this literal, or <code>null</code> if no type arguments were
   * declared.
   */
  private TypeArgumentList typeArguments;

  /**
   * Initialize a newly created typed literal.
   */
  public TypedLiteral() {
  }

  /**
   * Initialize a newly created typed literal.
   * 
   * @param modifier the const modifier associated with this literal
   * @param typeArguments the type argument associated with this literal, or <code>null</code> if no
   *          type arguments were declared
   */
  public TypedLiteral(Token modifier, TypeArgumentList typeArguments) {
    this.modifier = modifier;
    this.typeArguments = becomeParentOf(typeArguments);
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
   * Return the type argument associated with this literal, or <code>null</code> if no type
   * arguments were declared.
   * 
   * @return the type argument associated with this literal
   */
  public TypeArgumentList getTypeArguments() {
    return typeArguments;
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
   * Set the type argument associated with this literal to the given arguments.
   * 
   * @param typeArguments the type argument associated with this literal
   */
  public void setTypeArguments(TypeArgumentList typeArguments) {
    this.typeArguments = typeArguments;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(typeArguments, visitor);
  }
}
