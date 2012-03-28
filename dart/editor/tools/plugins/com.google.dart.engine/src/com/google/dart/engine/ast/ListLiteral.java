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
 * Instances of the class <code>ListLiteral</code> represent a list literal.
 * 
 * <pre>
 * listLiteral ::=
 *     'const'? ('<' {@link TypeName type} '>')? '[' ({@link Expression expressionList} ','?)? ']'
 * </pre>
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
   */
  public ListLiteral() {
  }

  /**
   * Initialize a newly created list literal.
   * 
   * @param modifier the const modifier associated with this literal
   * @param leftAngleBracket the left angle bracket
   * @param typeArgument the type of the elements of the literal
   * @param rightAngleBracket the right angle bracket
   * @param leftBracket the left square bracket
   * @param elements the expressions used to compute the elements of the list
   * @param rightBracket the right square bracket
   */
  public ListLiteral(Token modifier, Token leftAngleBracket, TypeName typeArgument,
      Token rightAngleBracket, Token leftBracket, List<Expression> elements, Token rightBracket) {
    super(modifier, leftAngleBracket, typeArgument, rightAngleBracket);
    this.leftBracket = leftBracket;
    elements.addAll(elements);
    this.rightBracket = rightBracket;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitListLiteral(this);
  }

  @Override
  public Token getBeginToken() {
    Token token = getModifier();
    if (token == null) {
      token = getLeftAngleBracket();
    }
    if (token == null) {
      token = leftBracket;
    }
    return token;
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
  public void visitChildren(ASTVisitor<?> visitor) {
    super.visitChildren(visitor);
    elements.accept(visitor);
  }
}
