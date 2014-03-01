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
 * Instances of the class {@code NativeFunctionBody} represent a function body that consists of a
 * native keyword followed by a string literal.
 * 
 * <pre>
 * nativeFunctionBody ::=
 *     'native' {@link SimpleStringLiteral simpleStringLiteral} ';'
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class NativeFunctionBody extends FunctionBody {
  /**
   * The token representing 'native' that marks the start of the function body.
   */
  private Token nativeToken;

  /**
   * The string literal, after the 'native' token.
   */
  private StringLiteral stringLiteral;

  /**
   * The token representing the semicolon that marks the end of the function body.
   */
  private Token semicolon;

  /**
   * Initialize a newly created function body consisting of the 'native' token, a string literal,
   * and a semicolon.
   * 
   * @param nativeToken the token representing 'native' that marks the start of the function body
   * @param stringLiteral the string literal
   * @param semicolon the token representing the semicolon that marks the end of the function body
   */
  public NativeFunctionBody(Token nativeToken, StringLiteral stringLiteral, Token semicolon) {
    this.nativeToken = nativeToken;
    this.stringLiteral = becomeParentOf(stringLiteral);
    this.semicolon = semicolon;
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitNativeFunctionBody(this);
  }

  @Override
  public Token getBeginToken() {
    return nativeToken;
  }

  @Override
  public Token getEndToken() {
    return semicolon;
  }

  /**
   * Return the simple identifier representing the 'native' token.
   * 
   * @return the simple identifier representing the 'native' token
   */
  public Token getNativeToken() {
    return nativeToken;
  }

  /**
   * Return the token representing the semicolon that marks the end of the function body.
   * 
   * @return the token representing the semicolon that marks the end of the function body
   */
  public Token getSemicolon() {
    return semicolon;
  }

  /**
   * Return the string literal representing the string after the 'native' token.
   * 
   * @return the string literal representing the string after the 'native' token
   */
  public StringLiteral getStringLiteral() {
    return stringLiteral;
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(stringLiteral, visitor);
  }
}
