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
 * Instances of the class {@code MixinClause} represent the "mixin" clause in a class declaration.
 * 
 * <pre>
 * mixinClause ::=
 *     'mixin' {@link TypeName mixin} (',' {@link TypeName mixin})*
 * </pre>
 */
public class MixinClause extends ASTNode {
  /**
   * The token representing the 'mixin' keyword.
   */
  private Token mixinKeyword;

  /**
   * The names of the mixins that were specified.
   */
  private NodeList<TypeName> mixinTypes = new NodeList<TypeName>(this);

  /**
   * Initialize a newly created mixin clause.
   */
  public MixinClause() {
  }

  /**
   * Initialize a newly created mixin clause.
   * 
   * @param keyword the token representing the 'mixin' keyword
   * @param mixinTypes the names of the mixins that were specified
   */
  public MixinClause(Token keyword, List<TypeName> mixinTypes) {
    this.mixinKeyword = keyword;
    this.mixinTypes.addAll(mixinTypes);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitMixinClause(this);
  }

  @Override
  public Token getBeginToken() {
    return mixinKeyword;
  }

  @Override
  public Token getEndToken() {
    return mixinTypes.getEndToken();
  }

  /**
   * Return the token representing the 'mixin' keyword.
   * 
   * @return the token representing the 'mixin' keyword
   */
  public Token getMixinKeyword() {
    return mixinKeyword;
  }

  /**
   * Return the names of the mixins that were specified.
   * 
   * @return the names of the mixins that were specified
   */
  public NodeList<TypeName> getMixinTypes() {
    return mixinTypes;
  }

  /**
   * Set the token representing the 'mixin' keyword to the given token.
   * 
   * @param keyword the token representing the 'mixin' keyword
   */
  public void setMixinKeyword(Token keyword) {
    this.mixinKeyword = keyword;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    mixinTypes.accept(visitor);
  }
}
