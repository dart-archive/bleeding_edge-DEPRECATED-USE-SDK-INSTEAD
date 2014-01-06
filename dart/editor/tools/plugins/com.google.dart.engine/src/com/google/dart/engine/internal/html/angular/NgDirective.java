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

import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.element.angular.AngularSelector;
import com.google.dart.engine.html.ast.EmbeddedExpression;
import com.google.dart.engine.html.ast.XmlAttributeNode;

/**
 * {@link NgDirective} describes any <code>NgDirective</code> annotation instance.
 */
abstract class NgDirective extends NgAnnotation {
  protected static EmbeddedExpression newEmbeddedExpression(Expression e) {
    return new EmbeddedExpression(e.getOffset(), e, e.getEnd());
  }

  public NgDirective(AngularSelector selector) {
    super(selector);
  }

  /**
   * Sets single {@link Expression} for {@link XmlAttributeNode}.
   */
  protected final void setExpression(XmlAttributeNode attribute, Expression expression) {
    attribute.setExpressions(new EmbeddedExpression[] {newEmbeddedExpression(expression)});
  }
}
