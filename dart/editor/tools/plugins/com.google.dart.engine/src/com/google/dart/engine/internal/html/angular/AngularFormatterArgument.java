/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.engine.internal.html.angular;

import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;

/**
 * Angular formatter argument.
 * 
 * @coverage dart.engine.ast
 */
public class AngularFormatterArgument {
  /**
   * The {@link TokenType#COLON} token.
   */
  private final Token token;

  /**
   * The argument expression.
   */
  private final Expression expression;

  /**
   * The optional sub-{@link Expression}s.
   */
  private Expression[] subExpressions = Expression.EMPTY_ARRAY;

  public AngularFormatterArgument(Token token, Expression expression) {
    this.token = token;
    this.expression = expression;
  }

  /**
   * Returns the argument expression.
   */
  public Expression getExpression() {
    return expression;
  }

  /**
   * All {@link Expression}s of this argument, in the most cases - single {@link #getExpression()},
   * but sometimes resolved sub-expressions instead.
   * 
   * @return sub-expressions, not {@code null}
   */
  public Expression[] getSubExpressions() {
    return subExpressions;
  }

  /**
   * Returns the {@link TokenType#COLON} token.
   */
  public Token getToken() {
    return token;
  }

  /**
   * Sets optional sub-{@link Expression}s.
   */
  public void setSubExpressions(Expression[] expressions) {
    this.subExpressions = expressions;
  }
}
