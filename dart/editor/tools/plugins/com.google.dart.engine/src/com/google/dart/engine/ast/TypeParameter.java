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
 * Instances of the class {@code TypeParameter} represent a type parameter.
 * 
 * <pre>
 * typeParameter ::=
 *     {@link SimpleIdentifier name} ('extends' {@link TypeName bound})?
 * </pre>
 */
public class TypeParameter extends Declaration {
  /**
   * The name of the type parameter.
   */
  private SimpleIdentifier name;

  /**
   * The token representing the 'extends' keyword, or {@code null} if there was no explicit upper
   * bound.
   */
  private Token keyword;

  /**
   * The name of the upper bound for legal arguments, or {@code null} if there was no explicit upper
   * bound.
   */
  private TypeName bound;

  /**
   * Initialize a newly created type parameter.
   */
  public TypeParameter() {
  }

  /**
   * Initialize a newly created type parameter.
   * 
   * @param comment the documentation comment associated with the type parameter
   * @param metadata the annotations associated with the type parameter
   * @param name the name of the type parameter
   * @param keyword the token representing the 'extends' keyword
   * @param bound the name of the upper bound for legal arguments
   */
  public TypeParameter(Comment comment, List<Annotation> metadata, SimpleIdentifier name,
      Token keyword, TypeName bound) {
    super(comment, metadata);
    this.name = becomeParentOf(name);
    this.keyword = keyword;
    this.bound = becomeParentOf(bound);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitTypeParameter(this);
  }

  @Override
  public Token getBeginToken() {
    return name.getBeginToken();
  }

  /**
   * Return the name of the upper bound for legal arguments, or {@code null} if there was no
   * explicit upper bound.
   * 
   * @return the name of the upper bound for legal arguments
   */
  public TypeName getBound() {
    return bound;
  }

  @Override
  public Token getEndToken() {
    if (bound == null) {
      return name.getEndToken();
    }
    return bound.getEndToken();
  }

  /**
   * Return the token representing the 'assert' keyword.
   * 
   * @return the token representing the 'assert' keyword
   */
  public Token getKeyword() {
    return keyword;
  }

  /**
   * Return the name of the type parameter.
   * 
   * @return the name of the type parameter
   */
  public SimpleIdentifier getName() {
    return name;
  }

  /**
   * Set the name of the upper bound for legal arguments to the given type name.
   * 
   * @param typeName the name of the upper bound for legal arguments
   */
  public void setBound(TypeName typeName) {
    bound = becomeParentOf(typeName);
  }

  /**
   * Set the token representing the 'assert' keyword to the given token.
   * 
   * @param keyword the token representing the 'assert' keyword
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
  }

  /**
   * Set the name of the type parameter to the given identifier.
   * 
   * @param identifier the name of the type parameter
   */
  public void setName(SimpleIdentifier identifier) {
    name = becomeParentOf(identifier);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(name, visitor);
    safelyVisitChild(bound, visitor);
  }
}
