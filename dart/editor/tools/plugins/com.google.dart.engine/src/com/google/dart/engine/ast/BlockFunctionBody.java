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

import com.google.dart.engine.parser.Parser;
import com.google.dart.engine.scanner.Token;

/**
 * Instances of the class {@code BlockFunctionBody} represent a function body that consists of a
 * block of statements.
 * 
 * <pre>
 * blockFunctionBody ::=
 *     ('async' | 'async' '*' | 'sync' '*')? {@link Block block}
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class BlockFunctionBody extends FunctionBody {
  /**
   * The token representing the 'async' or 'sync' keyword, or {@code null} if there is no such
   * keyword.
   */
  private Token keyword;

  /**
   * The star optionally following the 'async' or following the 'sync' keyword.
   */
  private Token star;

  /**
   * The block representing the body of the function.
   */
  private Block block;

  /**
   * Initialize a newly created function body consisting of a block of statements.
   * 
   * @param keyword the token representing the 'async' or 'sync' keyword
   * @param star the star following the 'async' or 'sync' keyword
   * @param block the block representing the body of the function
   */
  public BlockFunctionBody(Token keyword, Token star, Block block) {
    this.keyword = keyword;
    this.star = star;
    this.block = becomeParentOf(block);
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitBlockFunctionBody(this);
  }

  @Override
  public Token getBeginToken() {
    return block.getBeginToken();
  }

  /**
   * Return the block representing the body of the function.
   * 
   * @return the block representing the body of the function
   */
  public Block getBlock() {
    return block;
  }

  @Override
  public Token getEndToken() {
    return block.getEndToken();
  }

  /**
   * Return the token representing the 'async' or 'sync' keyword, or {@code null} if there is no
   * such keyword.
   * 
   * @return the token representing the 'async' or 'sync' keyword
   */
  public Token getKeyword() {
    return keyword;
  }

  /**
   * Return the star following the 'async' or 'sync' keyword, or {@code null} if there is no star.
   * 
   * @return the star following the 'async' or 'sync' keyword
   */
  public Token getStar() {
    return star;
  }

  @Override
  public boolean isAsynchronous() {
    if (keyword == null) {
      return false;
    }
    String keywordValue = keyword.getLexeme();
    return keywordValue.equals(Parser.ASYNC);
  }

  @Override
  public boolean isGenerator() {
    return star != null;
  }

  @Override
  public boolean isSynchronous() {
    return keyword == null || !keyword.getLexeme().equals(Parser.ASYNC);
  }

  /**
   * Set the block representing the body of the function to the given block.
   * 
   * @param block the block representing the body of the function
   */
  public void setBlock(Block block) {
    this.block = becomeParentOf(block);
  }

  /**
   * Set the token representing the 'async' or 'sync' keyword to the given token.
   * 
   * @param keyword the token representing the 'async' or 'sync' keyword
   */
  public void setKeyword(Token keyword) {
    this.keyword = keyword;
  }

  /**
   * Set the star following the 'async' or 'sync' keyword to the given token.
   * 
   * @param star the star following the 'async' or 'sync' keyword
   */
  public void setStar(Token star) {
    this.star = star;
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(block, visitor);
  }
}
