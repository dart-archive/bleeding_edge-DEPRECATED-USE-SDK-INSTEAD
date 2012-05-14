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
 * Instances of the class <code>TypeName</code> represent the name of a type, which can optionally
 * include type arguments.
 * 
 * <pre>
 * typeName ::=
 *     {@link Identifier identifier} typeArguments?
 * </pre>
 */
public class TypeName extends ASTNode {
  /**
   * The name of the type.
   */
  private Identifier name;

  /**
   * The type arguments associated with the type, or <code>null</code> if there are no type
   * arguments.
   */
  private TypeArgumentList typeArguments;

  /**
   * Initialize a newly created type name.
   */
  public TypeName() {
  }

  /**
   * Initialize a newly created type name.
   * 
   * @param name the name of the type
   * @param typeArguments the type arguments associated with the type, or <code>null</code> if there
   *          are no type arguments
   */
  public TypeName(Identifier name, TypeArgumentList typeArguments) {
    this.name = becomeParentOf(name);
    this.typeArguments = becomeParentOf(typeArguments);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
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
   * Return the type arguments associated with the type, or <code>null</code> if there are no type
   * arguments.
   * 
   * @return the type arguments associated with the type
   */
  public TypeArgumentList getTypeArguments() {
    return typeArguments;
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
   * Set the type arguments associated with the type to the given type arguments.
   * 
   * @param typeArguments the type arguments associated with the type
   */
  public void setTypeArguments(TypeArgumentList typeArguments) {
    this.typeArguments = becomeParentOf(typeArguments);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(name, visitor);
    safelyVisitChild(typeArguments, visitor);
  }
}
