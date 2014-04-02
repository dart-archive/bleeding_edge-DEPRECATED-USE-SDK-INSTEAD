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
 * Instances of the class {@code TryStatement} represent a try statement.
 * 
 * <pre>
 * tryStatement ::=
 *     'try' {@link Block block} ({@link CatchClause catchClause}+ finallyClause? | finallyClause)
 *
 * finallyClause ::=
 *     'finally' {@link Block block}
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class TryStatement extends Statement {
  /**
   * The token representing the 'try' keyword.
   */
  private Token tryKeyword;

  /**
   * The body of the statement.
   */
  private Block body;

  /**
   * The catch clauses contained in the try statement.
   */
  private NodeList<CatchClause> catchClauses = new NodeList<CatchClause>(this);

  /**
   * The token representing the 'finally' keyword, or {@code null} if the statement does not contain
   * a finally clause.
   */
  private Token finallyKeyword;

  /**
   * The finally block contained in the try statement, or {@code null} if the statement does not
   * contain a finally clause.
   */
  private Block finallyBlock;

  /**
   * Initialize a newly created try statement.
   * 
   * @param tryKeyword the token representing the 'try' keyword
   * @param body the body of the statement
   * @param catchClauses the catch clauses contained in the try statement
   * @param finallyKeyword the token representing the 'finally' keyword
   * @param finallyBlock the finally block contained in the try statement
   */
  public TryStatement(Token tryKeyword, Block body, List<CatchClause> catchClauses,
      Token finallyKeyword, Block finallyBlock) {
    this.tryKeyword = tryKeyword;
    this.body = becomeParentOf(body);
    this.catchClauses.addAll(catchClauses);
    this.finallyKeyword = finallyKeyword;
    this.finallyBlock = becomeParentOf(finallyBlock);
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitTryStatement(this);
  }

  @Override
  public Token getBeginToken() {
    return tryKeyword;
  }

  /**
   * Return the body of the statement.
   * 
   * @return the body of the statement
   */
  public Block getBody() {
    return body;
  }

  /**
   * Return the catch clauses contained in the try statement.
   * 
   * @return the catch clauses contained in the try statement
   */
  public NodeList<CatchClause> getCatchClauses() {
    return catchClauses;
  }

  @Override
  public Token getEndToken() {
    if (finallyBlock != null) {
      return finallyBlock.getEndToken();
    } else if (finallyKeyword != null) {
      return finallyKeyword;
    } else if (!catchClauses.isEmpty()) {
      return catchClauses.getEndToken();
    }
    return body.getEndToken();
  }

  /**
   * Return the finally block contained in the try statement, or {@code null} if the statement does
   * not contain a finally clause.
   * 
   * @return the finally block contained in the try statement
   */
  public Block getFinallyBlock() {
    return finallyBlock;
  }

  /**
   * Return the token representing the 'finally' keyword, or {@code null} if the statement does not
   * contain a finally clause.
   * 
   * @return the token representing the 'finally' keyword
   */
  public Token getFinallyKeyword() {
    return finallyKeyword;
  }

  /**
   * Return the token representing the 'try' keyword.
   * 
   * @return the token representing the 'try' keyword
   */
  public Token getTryKeyword() {
    return tryKeyword;
  }

  /**
   * Set the body of the statement to the given block.
   * 
   * @param block the body of the statement
   */
  public void setBody(Block block) {
    body = becomeParentOf(block);
  }

  /**
   * Set the finally block contained in the try statement to the given block.
   * 
   * @param block the finally block contained in the try statement
   */
  public void setFinallyBlock(Block block) {
    finallyBlock = becomeParentOf(block);
  }

  /**
   * Set the token representing the 'finally' keyword to the given token.
   * 
   * @param finallyKeyword the token representing the 'finally' keyword
   */
  public void setFinallyKeyword(Token finallyKeyword) {
    this.finallyKeyword = finallyKeyword;
  }

  /**
   * Set the token representing the 'try' keyword to the given token.
   * 
   * @param tryKeyword the token representing the 'try' keyword
   */
  public void setTryKeyword(Token tryKeyword) {
    this.tryKeyword = tryKeyword;
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(body, visitor);
    catchClauses.accept(visitor);
    safelyVisitChild(finallyBlock, visitor);
  }
}
