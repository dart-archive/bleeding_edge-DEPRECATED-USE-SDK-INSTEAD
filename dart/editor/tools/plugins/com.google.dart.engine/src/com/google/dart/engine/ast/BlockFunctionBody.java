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

/**
 * Instances of the class <code>BlockFunctionBody</code> represent a function body that consists of
 * a block of statements.
 * 
 * <pre>
 * blockFunctionBody ::=
 *     {@link Block block}
 * </pre>
 */
public class BlockFunctionBody extends FunctionBody {
  /**
   * The block representing the body of the function.
   */
  private Block block;

  /**
   * Initialize a newly created function body consisting of a block of statements.
   */
  public BlockFunctionBody() {
  }

  /**
   * Initialize a newly created function body consisting of a block of statements.
   * 
   * @param block the block representing the body of the function
   */
  public BlockFunctionBody(Block block) {
    this.block = becomeParentOf(block);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
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
   * Set the block representing the body of the function to the given block.
   * 
   * @param block the block representing the body of the function
   */
  public void setBlock(Block block) {
    this.block = becomeParentOf(block);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(block, visitor);
  }
}
