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

package com.google.dart.engine.internal.html.angular;

import com.google.common.collect.Lists;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.html.ast.EmbeddedExpression;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.scanner.Token;

import java.util.List;

/**
 * {@link NgDirectiveProcessor} describes any <code>NgDirective</code> annotation instance.
 */
abstract class NgDirectiveProcessor extends NgProcessor {
  protected static EmbeddedExpression newEmbeddedExpression(Expression e) {
    return new EmbeddedExpression(e.getOffset(), e, e.getEnd());
  }

  protected Expression parseExpression(AngularHtmlUnitResolver resolver, XmlAttributeNode attribute) {
    int offset = attribute.getValueToken().getOffset() + 1;
    String value = attribute.getText();
    Token token = resolver.scanDart(value, 0, value.length(), offset);
    return resolver.parseExpression(token);
  }

  /**
   * Sets single {@link Expression} for {@link XmlAttributeNode}.
   */
  protected final void setExpression(XmlAttributeNode attribute, Expression expression) {
    attribute.setExpressions(new EmbeddedExpression[] {newEmbeddedExpression(expression)});
  }

  /**
   * Sets {@link Expression}s for {@link XmlAttributeNode}.
   */
  protected final void setExpressions(XmlAttributeNode attribute, List<Expression> expressions) {
    List<EmbeddedExpression> embExpressions = Lists.newArrayList();
    for (Expression expression : expressions) {
      embExpressions.add(newEmbeddedExpression(expression));
    }
    attribute.setExpressions(embExpressions.toArray(new EmbeddedExpression[embExpressions.size()]));
  }
}
