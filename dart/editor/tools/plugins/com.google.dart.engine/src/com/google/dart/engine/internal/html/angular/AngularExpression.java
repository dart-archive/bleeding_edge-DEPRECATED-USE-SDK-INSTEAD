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

import com.google.common.collect.Lists;
import com.google.dart.engine.ast.Expression;

import java.util.Collections;
import java.util.List;

/**
 * An {@link Expression} with optional {@link AngularFormatterNode}s.
 * 
 * @coverage dart.engine.ast
 */
public class AngularExpression {

  /**
   * The {@link Expression} to apply formatters to.
   */
  private final Expression expression;

  /**
   * The formatters to apply.
   */
  private final List<AngularFormatterNode> formatters;

  public AngularExpression(Expression expression, List<AngularFormatterNode> formatters) {
    this.expression = expression;
    this.formatters = formatters;
  }

  /**
   * Return the offset of the character immediately following the last character of this node's
   * source range. This is equivalent to {@code node.getOffset() + node.getLength()}.
   * 
   * @return the offset of the character just past the node's source range
   */
  public int getEnd() {
    if (formatters.isEmpty()) {
      return expression.getEnd();
    }
    AngularFormatterNode lastFormatter = formatters.get(formatters.size() - 1);
    List<AngularFormatterArgument> formatterArguments = lastFormatter.getArguments();
    if (formatterArguments.isEmpty()) {
      return lastFormatter.getName().getEnd();
    }
    return formatterArguments.get(formatterArguments.size() - 1).getExpression().getEnd();
  }

  /**
   * Returns the {@link Expression} to apply formatters to.
   */
  public Expression getExpression() {
    return expression;
  }

  /**
   * Return Dart {@link Expression}s this Angular expression consists of.
   */
  public List<Expression> getExpressions() {
    List<Expression> expressions = Lists.newArrayList();
    expressions.add(expression);
    for (AngularFormatterNode formatter : formatters) {
      expressions.add(formatter.getName());
      for (AngularFormatterArgument formatterArgument : formatter.getArguments()) {
        Collections.addAll(expressions, formatterArgument.getSubExpressions());
        expressions.add(formatterArgument.getExpression());
      }
    }
    return expressions;
  }

  /**
   * Returns the formatters to apply.
   */
  public List<AngularFormatterNode> getFormatters() {
    return formatters;
  }

  /**
   * Return the number of characters in the expression's source range.
   */
  public int getLength() {
    return getEnd() - getOffset();
  }

  /**
   * Return the offset of the first character in the expression's source range.
   */
  public int getOffset() {
    return expression.getOffset();
  }
}
