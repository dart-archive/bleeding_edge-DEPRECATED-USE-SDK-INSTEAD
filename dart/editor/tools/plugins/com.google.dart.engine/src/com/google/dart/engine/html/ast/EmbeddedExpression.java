/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.html.ast;

import com.google.dart.engine.ast.Expression;

/**
 * Instances of the class {@code EmbeddedExpression} represent an expression enclosed between
 * <code>{{</code> and <code>}}</code> delimiters.
 */
public class EmbeddedExpression {
  /**
   * The offset of the first character of the opening delimiter.
   */
  private int openingOffset;

  /**
   * The expression that is enclosed between the delimiters.
   */
  private Expression expression;

  /**
   * The offset of the first character of the closing delimiter.
   */
  private int closingOffset;

  /**
   * An empty array of embedded expressions.
   */
  public static final EmbeddedExpression[] EMPTY_ARRAY = new EmbeddedExpression[0];

  /**
   * Initialize a newly created embedded expression to represent the given expression.
   * 
   * @param openingOffset the offset of the first character of the opening delimiter
   * @param expression the expression that is enclosed between the delimiters
   * @param closingOffset the offset of the first character of the closing delimiter
   */
  public EmbeddedExpression(int openingOffset, Expression expression, int closingOffset) {
    this.openingOffset = openingOffset;
    this.expression = expression;
    this.closingOffset = closingOffset;
  }

  /**
   * Return the offset of the first character of the closing delimiter.
   * 
   * @return the offset of the first character of the closing delimiter
   */
  public int getClosingOffset() {
    return closingOffset;
  }

  /**
   * Return the expression that is enclosed between the delimiters.
   * 
   * @return the expression that is enclosed between the delimiters
   */
  public Expression getExpression() {
    return expression;
  }

  /**
   * Return the offset of the first character of the opening delimiter.
   * 
   * @return the offset of the first character of the opening delimiter
   */
  public int getOpeningOffset() {
    return openingOffset;
  }
}
