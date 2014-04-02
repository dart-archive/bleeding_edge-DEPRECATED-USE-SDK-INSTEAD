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
 * Instances of the class {@code MapLiteral} represent a literal map.
 * 
 * <pre>
 * mapLiteral ::=
 *     'const'? ('<' {@link TypeName type} (',' {@link TypeName type})* '>')? '{' ({@link MapLiteralEntry entry} (',' {@link MapLiteralEntry entry})* ','?)? '}'
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class MapLiteral extends TypedLiteral {
  /**
   * The left curly bracket.
   */
  private Token leftBracket;

  /**
   * The entries in the map.
   */
  private NodeList<MapLiteralEntry> entries = new NodeList<MapLiteralEntry>(this);

  /**
   * The right curly bracket.
   */
  private Token rightBracket;

  /**
   * Initialize a newly created map literal.
   * 
   * @param constKeyword the token representing the 'const' keyword
   * @param typeArguments the type argument associated with this literal, or {@code null} if no type
   *          arguments were declared
   * @param leftBracket the left curly bracket
   * @param entries the entries in the map
   * @param rightBracket the right curly bracket
   */
  public MapLiteral(Token constKeyword, TypeArgumentList typeArguments, Token leftBracket,
      List<MapLiteralEntry> entries, Token rightBracket) {
    super(constKeyword, typeArguments);
    this.leftBracket = leftBracket;
    this.entries.addAll(entries);
    this.rightBracket = rightBracket;
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitMapLiteral(this);
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

  @Override
  public Token getEndToken() {
    return rightBracket;
  }

  /**
   * Return the entries in the map.
   * 
   * @return the entries in the map
   */
  public NodeList<MapLiteralEntry> getEntries() {
    return entries;
  }

  /**
   * Return the left curly bracket.
   * 
   * @return the left curly bracket
   */
  public Token getLeftBracket() {
    return leftBracket;
  }

  /**
   * Return the right curly bracket.
   * 
   * @return the right curly bracket
   */
  public Token getRightBracket() {
    return rightBracket;
  }

  /**
   * Set the left curly bracket to the given token.
   * 
   * @param bracket the left curly bracket
   */
  public void setLeftBracket(Token bracket) {
    leftBracket = bracket;
  }

  /**
   * Set the right curly bracket to the given token.
   * 
   * @param bracket the right curly bracket
   */
  public void setRightBracket(Token bracket) {
    rightBracket = bracket;
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    super.visitChildren(visitor);
    entries.accept(visitor);
  }
}
