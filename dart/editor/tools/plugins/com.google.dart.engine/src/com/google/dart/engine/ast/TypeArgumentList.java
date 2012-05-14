/*
 * Copyright (c) 2012, the Dart project authors.
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
 * Instances of the class {@code TypeArgumentList} represent a list of type arguments.
 * 
 * <pre>
 * typeArguments ::=
 *     '<' typeName (',' typeName)* '>'
 * </pre>
 */
public class TypeArgumentList extends ASTNode {
  /**
   * The left bracket.
   */
  private Token leftBracket;

  /**
   * The type arguments associated with the type.
   */
  private NodeList<TypeName> arguments = new NodeList<TypeName>(this);

  /**
   * The right bracket.
   */
  private Token rightBracket;

  /**
   * Initialize a newly created list of type arguments.
   * 
   * @param leftBracket the left bracket
   * @param arguments the type arguments associated with the type
   * @param rightBracket the right bracket
   */
  public TypeArgumentList(Token leftBracket, List<TypeName> arguments, Token rightBracket) {
    this.leftBracket = leftBracket;
    this.arguments.addAll(arguments);
    this.rightBracket = rightBracket;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitTypeArguments(this);
  }

  /**
   * Return the type arguments associated with the type.
   * 
   * @return the type arguments associated with the type
   */
  public NodeList<TypeName> getArguments() {
    return arguments;
  }

  @Override
  public Token getBeginToken() {
    return leftBracket;
  }

  @Override
  public Token getEndToken() {
    return rightBracket;
  }

  /**
   * Return the left bracket.
   * 
   * @return the left bracket
   */
  public Token getLeftBracket() {
    return leftBracket;
  }

  /**
   * Return the right bracket.
   * 
   * @return the right bracket
   */
  public Token getRightBracket() {
    return rightBracket;
  }

  /**
   * Set the left bracket to the given token.
   * 
   * @param leftBracket the left bracket
   */
  public void setLeftBracket(Token leftBracket) {
    this.leftBracket = leftBracket;
  }

  /**
   * Set the right bracket to the given token.
   * 
   * @param rightBracket the right bracket
   */
  public void setRightBracket(Token rightBracket) {
    this.rightBracket = rightBracket;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    arguments.accept(visitor);
  }
}
