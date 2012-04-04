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
 * Instances of the class <code>DefaultClause</code> represent the default clause in an interface
 * declaration.
 * 
 * <pre>
 * defaultClause ::=
 *     'default' {@link SimpleIdentifier identifier} {@link TypeParameterList typeParameterList}?
 * </pre>
 */
public class DefaultClause extends ASTNode {
  /**
   * The token representing the 'default' keyword.
   */
  private Token keyword;

  /**
   * The name of the default class associated with the interface.
   */
  private Identifier defaultName;

  /**
   * The type parameters for the default class, or <code>null</code> if the default type does not
   * have any type parameters.
   */
  private TypeParameterList defaultTypeParameters;

  /**
   * Initialize a newly created default clause.
   */
  public DefaultClause() {
  }

  /**
   * Initialize a newly created default clause.
   * 
   * @param defaultName the name of the default class associated with the interface
   * @param defaultTypeParameters the type parameters for the default class
   */
  public DefaultClause(Identifier defaultName, TypeParameterList defaultTypeParameters) {
    this.defaultName = becomeParentOf(defaultName);
    this.defaultTypeParameters = becomeParentOf(defaultTypeParameters);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitDefaultClause(this);
  }

  @Override
  public Token getBeginToken() {
    return keyword;
  }

  /**
   * Return the name of the default class associated with the interface.
   * 
   * @return the name of the default class associated with the interface
   */
  public Identifier getDefaultName() {
    return defaultName;
  }

  /**
   * Return the type parameters for the default class, or <code>null</code> if the default type does
   * not have any type parameters.
   * 
   * @return the type parameters for the default class
   */
  public TypeParameterList getDefaultTypeParameters() {
    return defaultTypeParameters;
  }

  @Override
  public Token getEndToken() {
    if (defaultTypeParameters != null) {
      return defaultName.getEndToken();
    }
    return defaultTypeParameters.getEndToken();
  }

  /**
   * Return the token representing the 'default' keyword.
   * 
   * @return the token representing the 'default' keyword
   */
  public Token getKeyword() {
    return keyword;
  }

  /**
   * Set the name of the default class associated with the interface to the given identifier.
   * 
   * @param identifier the name of the default class associated with the interface
   */
  public void setDefaultName(Identifier identifier) {
    defaultName = becomeParentOf(identifier);
  }

  /**
   * Set the type parameters for the default class to the given list of parameters.
   * 
   * @param defaultTypeParameters the type parameters for the default class
   */
  public void setDefaultTypeParameters(TypeParameterList defaultTypeParameters) {
    this.defaultTypeParameters = defaultTypeParameters;
  }

  /**
   * Set the token representing the 'default' keyword to the given token.
   * 
   * @param keyword the token representing the 'default' keyword
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(defaultName, visitor);
    safelyVisitChild(defaultTypeParameters, visitor);
  }
}
