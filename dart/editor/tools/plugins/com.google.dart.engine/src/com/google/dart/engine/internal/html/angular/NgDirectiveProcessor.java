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
import com.google.dart.engine.html.ast.RawXmlExpression;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlExpression;
import com.google.dart.engine.scanner.Token;

import java.util.List;

/**
 * {@link NgDirectiveProcessor} describes any <code>NgDirective</code> annotation instance.
 */
abstract class NgDirectiveProcessor extends NgProcessor {
  protected static AngularRawXmlExpression newAngularRawXmlExpression(AngularExpression e) {
    return new AngularRawXmlExpression(e);
  }

  protected static RawXmlExpression newRawXmlExpression(Expression e) {
    return new RawXmlExpression(e);
  }

  protected AngularExpression parseAngularExpression(AngularHtmlUnitResolver resolver,
      XmlAttributeNode attribute) {
    Token token = scanAttribute(resolver, attribute);
    return resolver.parseAngularExpression(token);
  }

  protected Expression parseDartExpression(AngularHtmlUnitResolver resolver,
      XmlAttributeNode attribute) {
    Token token = scanAttribute(resolver, attribute);
    return resolver.parseDartExpression(token);
  }

  /**
   * Sets single {@link AngularExpression} for {@link XmlAttributeNode}.
   */
  protected final void setExpression(XmlAttributeNode attribute, AngularExpression expression) {
    setExpression(attribute, newAngularRawXmlExpression(expression));
  }

  /**
   * Sets single {@link Expression} for {@link XmlAttributeNode}.
   */
  protected final void setExpression(XmlAttributeNode attribute, Expression expression) {
    setExpression(attribute, newRawXmlExpression(expression));
  }

  protected void setExpressions(XmlAttributeNode attribute, List<XmlExpression> xmlExpressions) {
    attribute.setExpressions(xmlExpressions.toArray(new XmlExpression[xmlExpressions.size()]));
  }

//  /**
//   * Sets {@link Expression}s for {@link XmlAttributeNode}.
//   */
//  protected final void setExpressions(XmlAttributeNode attribute, List<Expression> expressions) {
//    List<EmbeddedExpression> embExpressions = Lists.newArrayList();
//    for (Expression expression : expressions) {
//      embExpressions.add(newEmbeddedExpression(expression));
//    }
//    attribute.setExpressions(embExpressions.toArray(new EmbeddedExpression[embExpressions.size()]));
//  }

  private Token scanAttribute(AngularHtmlUnitResolver resolver, XmlAttributeNode attribute) {
    int offset = attribute.getValueToken().getOffset() + 1;
    String value = attribute.getText();
    return resolver.scanDart(value, 0, value.length(), offset);
  }

  private void setExpression(XmlAttributeNode attribute, XmlExpression xmlExpression) {
    attribute.setExpressions(new XmlExpression[] {xmlExpression});
  }
}
