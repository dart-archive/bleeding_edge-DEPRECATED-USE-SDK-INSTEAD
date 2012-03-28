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
 * Instances of the class <code>MapLiteral</code> represent a literal map.
 * 
 * <pre>
 * mapLiteral ::=
 *     'const'? ('<' {@link TypeName type} '>')? '{' ({@link MapLiteralEntry entry} (',' {@link MapLiteralEntry entry})* ','?)? '}'
 * </pre>
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
   */
  public MapLiteral() {
  }

  /**
   * Initialize a newly created map literal.
   * 
   * @param modifier the const modifier associated with this literal
   * @param leftAngleBracket the left angle bracket
   * @param typeArgument the type of the elements of the literal
   * @param rightAngleBracket the right angle bracket
   * @param leftBracket the left curly bracket
   * @param entries the entries in the map
   * @param rightBracket the right curly bracket
   */
  public MapLiteral(Token modifier, Token leftAngleBracket, TypeName typeArgument,
      Token rightAngleBracket, Token leftBracket, List<MapLiteralEntry> entries, Token rightBracket) {
    super(modifier, leftAngleBracket, typeArgument, rightAngleBracket);
    this.leftBracket = leftBracket;
    this.entries.addAll(entries);
    this.rightBracket = rightBracket;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitMapLiteral(this);
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
  public void visitChildren(ASTVisitor<?> visitor) {
    super.visitChildren(visitor);
    entries.accept(visitor);
  }
}
