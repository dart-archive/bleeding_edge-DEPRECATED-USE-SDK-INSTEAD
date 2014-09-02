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
 * The abstract class {@code FunctionBody} defines the behavior common to objects representing the
 * body of a function or method.
 * 
 * <pre>
 * functionBody ::=
 *     {@link BlockFunctionBody blockFunctionBody}
 *   | {@link EmptyFunctionBody emptyFunctionBody}
 *   | {@link ExpressionFunctionBody expressionFunctionBody}
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public abstract class FunctionBody extends AstNode {
  /**
   * Return the token representing the 'async' or 'sync' keyword, or {@code null} if there is no
   * such keyword.
   * 
   * @return the token representing the 'async' or 'sync' keyword
   */
  public Token getKeyword() {
    return null;
  }

  /**
   * Return the star following the 'async' or 'sync' keyword, or {@code null} if there is no star.
   * 
   * @return the star following the 'async' or 'sync' keyword
   */
  public Token getStar() {
    return null;
  }

  /**
   * Return {@code true} if this function body is asynchronous.
   * 
   * @return {@code true} if this function body is asynchronous
   */
  public boolean isAsynchronous() {
    return false;
  }

  /**
   * Return {@code true} if this function body is a generator.
   * 
   * @return {@code true} if this function body is a generator
   */
  public boolean isGenerator() {
    return false;
  }

  /**
   * Return {@code true} if this function body is synchronous.
   * 
   * @return {@code true} if this function body is synchronous
   */
  public boolean isSynchronous() {
    return true;
  }
}
