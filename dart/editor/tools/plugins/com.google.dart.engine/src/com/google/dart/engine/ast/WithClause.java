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
 * Instances of the class {@code WithClause} represent the with clause in a class declaration.
 * 
 * <pre>
 * withClause ::=
 *     'with' {@link TypeName mixin} (',' {@link TypeName mixin})*
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class WithClause extends AstNode {
  /**
   * The token representing the 'with' keyword.
   */
  private Token withKeyword;

  /**
   * The names of the mixins that were specified.
   */
  private NodeList<TypeName> mixinTypes = new NodeList<TypeName>(this);

  /**
   * Initialize a newly created with clause.
   * 
   * @param withKeyword the token representing the 'with' keyword
   * @param mixinTypes the names of the mixins that were specified
   */
  public WithClause(Token withKeyword, List<TypeName> mixinTypes) {
    this.withKeyword = withKeyword;
    this.mixinTypes.addAll(mixinTypes);
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitWithClause(this);
  }

  @Override
  public Token getBeginToken() {
    return withKeyword;
  }

  @Override
  public Token getEndToken() {
    return mixinTypes.getEndToken();
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
   * Return the token representing the 'with' keyword.
   * 
   * @return the token representing the 'with' keyword
   */
  public Token getWithKeyword() {
    return withKeyword;
  }

  /**
   * Set the token representing the 'with' keyword to the given token.
   * 
   * @param withKeyword the token representing the 'with' keyword
   */
  public void setMixinKeyword(Token withKeyword) {
    this.withKeyword = withKeyword;
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    mixinTypes.accept(visitor);
  }
}
