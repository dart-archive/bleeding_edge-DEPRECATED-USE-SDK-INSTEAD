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
 * Instances of the class {@code ListLiteral} represent a list literal.
 * 
 * <pre>
 * listLiteral ::=
 *     'const'? ('<' {@link TypeName type} '>')? '[' ({@link Expression expressionList} ','?)? ']'
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class ListLiteral extends TypedLiteral {
  /**
   * The left square bracket.
   */
  private Token leftBracket;

  /**
   * The expressions used to compute the elements of the list.
   */
  private NodeList<Expression> elements = new NodeList<Expression>(this);

  /**
   * The right square bracket.
   */
  private Token rightBracket;

  /**
   * Initialize a newly created list literal.
   * 
   * @param constKeyword the token representing the 'const' keyword
   * @param typeArguments the type argument associated with this literal, or {@code null} if no type
   *          arguments were declared
   * @param leftBracket the left square bracket
   * @param elements the expressions used to compute the elements of the list
   * @param rightBracket the right square bracket
   */
  public ListLiteral(Token constKeyword, TypeArgumentList typeArguments, Token leftBracket,
      List<Expression> elements, Token rightBracket) {
    super(constKeyword, typeArguments);
    this.leftBracket = leftBracket;
    this.elements.addAll(elements);
    this.rightBracket = rightBracket;
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitListLiteral(this);
  }

  @Override
  public Token getBeginToken() {
    Token token = getConstKeyword();
    if (token != null) {
      return token;
    }
    TypeArgumentList typeArguments = getTypeArguments();
    if (typeArguments != null) {
      return typeArguments.getBeginToken();
    }
    return leftBracket;
  }

  /**
   * Return the expressions used to compute the elements of the list.
   * 
   * @return the expressions used to compute the elements of the list
   */
  public NodeList<Expression> getElements() {
    return elements;
  }

  @Override
  public Token getEndToken() {
    return rightBracket;
  }

  /**
   * Return the left square bracket.
   * 
   * @return the left square bracket
   */
  public Token getLeftBracket() {
    return leftBracket;
  }

  /**
   * Return the right square bracket.
   * 
   * @return the right square bracket
   */
  public Token getRightBracket() {
    return rightBracket;
  }

  /**
   * Set the left square bracket to the given token.
   * 
   * @param bracket the left square bracket
   */
  public void setLeftBracket(Token bracket) {
    leftBracket = bracket;
  }

  /**
   * Set the right square bracket to the given token.
   * 
   * @param bracket the right square bracket
   */
  public void setRightBracket(Token bracket) {
    rightBracket = bracket;
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    super.visitChildren(visitor);
    elements.accept(visitor);
  }
}
