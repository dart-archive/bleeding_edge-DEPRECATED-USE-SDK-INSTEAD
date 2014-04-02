/*
 * Copyright 2013, the Dart project authors.
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
 * Instances of the class {@code NativeClause} represent the "native" clause in an class
 * declaration.
 * 
 * <pre>
 * nativeClause ::=
 *     'native' {@link StringLiteral name}
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class NativeClause extends AstNode {
  /**
   * The token representing the 'native' keyword.
   */
  private Token keyword;

  /**
   * The name of the native object that implements the class.
   */
  private StringLiteral name;

  /**
   * Initialize a newly created native clause.
   * 
   * @param keyword the token representing the 'native' keyword
   * @param name the name of the native object that implements the class.
   */
  public NativeClause(Token keyword, StringLiteral name) {
    this.keyword = keyword;
    this.name = name;
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitNativeClause(this);
  }

  @Override
  public Token getBeginToken() {
    return keyword;
  }

  @Override
  public Token getEndToken() {
    return name.getEndToken();
  }

  /**
   * Return the token representing the 'native' keyword.
   * 
   * @return the token representing the 'native' keyword
   */
  public Token getKeyword() {
    return keyword;
  }

  /**
   * Return the name of the native object that implements the class.
   * 
   * @return the name of the native object that implements the class
   */
  public StringLiteral getName() {
    return name;
  }

  /**
   * Set the token representing the 'native' keyword to the given token.
   * 
   * @param keyword the token representing the 'native' keyword
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
  }

  /**
   * Sets the name of the native object that implements the class.
   * 
   * @param name the name of the native object that implements the class.
   */
  public void setName(StringLiteral name) {
    this.name = name;
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(name, visitor);
  }
}
