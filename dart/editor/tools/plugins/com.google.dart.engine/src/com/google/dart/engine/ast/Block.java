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
 * Instances of the class <code>Block</code> represent a sequence of statements.
 * 
 * <pre>
 * block ::=
 *     '{' statement* '}'
 * </pre>
 */
public class Block extends Statement {
  /**
   * The left curly bracket.
   */
  private Token leftBracket;

  /**
   * The statements contained in the block.
   */
  private NodeList<Statement> statements = new NodeList<Statement>(this);

  /**
   * The right curly bracket.
   */
  private Token rightBracket;

  /**
   * Initialize a newly created block of code.
   */
  public Block() {
  }

  /**
   * Initialize a newly created block of code.
   * 
   * @param leftBracket the left curly bracket
   * @param statements the statements contained in the block
   * @param rightBracket the right curly bracket
   */
  public Block(Token leftBracket, List<Statement> statements, Token rightBracket) {
    this.leftBracket = leftBracket;
    this.statements.addAll(statements);
    this.rightBracket = rightBracket;
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitBlock(this);
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
   * Return the statements contained in the block.
   * 
   * @return the statements contained in the block
   */
  public NodeList<Statement> getStatements() {
    return statements;
  }

  /**
   * Set the left curly bracket to the given token.
   * 
   * @param leftBracket the left curly bracket
   */
  public void setLeftBracket(Token leftBracket) {
    this.leftBracket = leftBracket;
  }

  /**
   * Set the right curly bracket to the given token.
   * 
   * @param rightBracket the right curly bracket
   */
  public void setRightBracket(Token rightBracket) {
    this.rightBracket = rightBracket;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    statements.accept(visitor);
  }
}
