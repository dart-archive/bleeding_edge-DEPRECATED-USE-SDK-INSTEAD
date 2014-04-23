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
import com.google.dart.engine.type.Type;

/**
 * Instances of the class {@code TypeName} represent the name of a type, which can optionally
 * include type arguments.
 * 
 * <pre>
 * typeName ::=
 *     {@link Identifier identifier} typeArguments?
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class TypeName extends AstNode {
  /**
   * The name of the type.
   */
  private Identifier name;

  /**
   * The type arguments associated with the type, or {@code null} if there are no type arguments.
   */
  private TypeArgumentList typeArguments;

  /**
   * The type being named, or {@code null} if the AST structure has not been resolved.
   */
  private Type type;

  /**
   * Initialize a newly created type name.
   * 
   * @param name the name of the type
   * @param typeArguments the type arguments associated with the type, or {@code null} if there are
   *          no type arguments
   */
  public TypeName(Identifier name, TypeArgumentList typeArguments) {
    this.name = becomeParentOf(name);
    this.typeArguments = becomeParentOf(typeArguments);
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitTypeName(this);
  }

  @Override
  public Token getBeginToken() {
    return name.getBeginToken();
  }

  @Override
  public Token getEndToken() {
    if (typeArguments != null) {
      return typeArguments.getEndToken();
    }
    return name.getEndToken();
  }

  /**
   * Return the name of the type.
   * 
   * @return the name of the type
   */
  public Identifier getName() {
    return name;
  }

  /**
   * Return the type being named, or {@code null} if the AST structure has not been resolved.
   * 
   * @return the type being named
   */
  public Type getType() {
    return type;
  }

  /**
   * Return the type arguments associated with the type, or {@code null} if there are no type
   * arguments.
   * 
   * @return the type arguments associated with the type
   */
  public TypeArgumentList getTypeArguments() {
    return typeArguments;
  }

  /**
   * Return {@code true} if this type is a deferred type.
   * <p>
   * 15.1 Static Types: A type <i>T</i> is deferred iff it is of the form </i>p.T</i> where <i>p</i>
   * is a deferred prefix.
   * 
   * @return {@code true} if this type is a deferred type
   */
  public boolean isDeferred() {
    Identifier identifier = getName();
    if (!(identifier instanceof PrefixedIdentifier)) {
      return false;
    }
    return ((PrefixedIdentifier) identifier).isDeferred();
  }

  @Override
  public boolean isSynthetic() {
    return name.isSynthetic() && typeArguments == null;
  }

  /**
   * Set the name of the type to the given identifier.
   * 
   * @param identifier the name of the type
   */
  public void setName(Identifier identifier) {
    name = becomeParentOf(identifier);
  }

  /**
   * Set the type being named to the given type.
   * 
   * @param type the type being named
   */
  public void setType(Type type) {
    this.type = type;
  }

  /**
   * Set the type arguments associated with the type to the given type arguments.
   * 
   * @param typeArguments the type arguments associated with the type
   */
  public void setTypeArguments(TypeArgumentList typeArguments) {
    this.typeArguments = becomeParentOf(typeArguments);
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(name, visitor);
    safelyVisitChild(typeArguments, visitor);
  }
}
