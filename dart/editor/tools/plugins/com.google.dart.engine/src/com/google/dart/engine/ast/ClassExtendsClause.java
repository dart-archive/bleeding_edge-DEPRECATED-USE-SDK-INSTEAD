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
 * Instances of the class <code>ClassExtendsClause</code> represent the "extends" clause in a class
 * declaration.
 * 
 * <pre>
 * classExtendsClause ::=
 *     'extends' {@link TypeName superclass}
 * </pre>
 */
public class ClassExtendsClause extends ASTNode {
  /**
   * The token representing the 'extends' keyword.
   */
  private Token keyword;

  /**
   * The name of the class that is being extended.
   */
  private TypeName superclass;

  /**
   * Initialize a newly created extends clause.
   */
  public ClassExtendsClause() {
  }

  /**
   * Initialize a newly created extends clause.
   * 
   * @param keyword the token representing the 'extends' keyword
   * @param superclass the name of the class that is being extended
   */
  public ClassExtendsClause(Token keyword, TypeName superclass) {
    this.keyword = keyword;
    this.superclass = becomeParentOf(superclass);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitClassExtendsClause(this);
  }

  @Override
  public Token getBeginToken() {
    return keyword;
  }

  @Override
  public Token getEndToken() {
    return superclass.getEndToken();
  }

  /**
   * Return the token representing the 'extends' keyword.
   * 
   * @return the token representing the 'extends' keyword
   */
  public Token getKeyword() {
    return keyword;
  }

  /**
   * Return the name of the class that is being extended.
   * 
   * @return the name of the class that is being extended
   */
  public TypeName getSuperclass() {
    return superclass;
  }

  /**
   * Set the token representing the 'extends' keyword to the given token.
   * 
   * @param keyword the token representing the 'extends' keyword
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
  }

  /**
   * Set the name of the class that is being extended to the given name.
   * 
   * @param name the name of the class that is being extended
   */
  public void setSuperclass(TypeName name) {
    superclass = becomeParentOf(name);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(superclass, visitor);
  }
}
