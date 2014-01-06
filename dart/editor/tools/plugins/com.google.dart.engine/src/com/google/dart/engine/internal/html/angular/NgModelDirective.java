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
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.element.angular.AngularSelector;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.internal.element.LocalVariableElementImpl;
import com.google.dart.engine.internal.element.angular.HasAttributeSelector;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.type.InterfaceType;

/**
 * {@link NgModelDirective} describes built-in <code>NgModel</code> directive.
 */
public class NgModelDirective extends NgDirective {
  private static final String NG_MODEL = "ng-model";
  private static final AngularSelector SELECTOR = new HasAttributeSelector(NG_MODEL);

  public static final NgModelDirective INSTANCE = new NgModelDirective();

  private NgModelDirective() {
    super(SELECTOR);
  }

  @Override
  public void apply(AngularHtmlUnitResolver resolver, XmlTagNode node) {
    XmlAttributeNode attribute = node.getAttribute(NG_MODEL);
    Expression expression = parseExpression(resolver, attribute);
    // identifiers have been already handled by "apply top"
    if (expression instanceof SimpleIdentifier) {
      return;
    }
    // resolve
    resolver.resolveNode(expression);
    // remember expression
    setExpression(attribute, expression);
  }

  /**
   * This method is used to define top-level {@link VariableElement}s for each "ng-model" with
   * simple identifier model.
   */
  void applyTopDeclarations(AngularHtmlUnitResolver resolver, XmlTagNode node) {
    XmlAttributeNode attribute = node.getAttribute(NG_MODEL);
    Expression expression = parseExpression(resolver, attribute);
    // if not identifier, then not a top-level model, delay until "apply"
    if (!(expression instanceof SimpleIdentifier)) {
      return;
    }
    SimpleIdentifier identifier = (SimpleIdentifier) expression;
    // define variable Element
    InterfaceType type = resolver.getTypeProvider().getStringType();
    LocalVariableElementImpl element = resolver.createLocalVariable(type, identifier);
    resolver.defineTopVariable(element);
    // remember expression
    identifier.setStaticElement(element);
    identifier.setStaticType(type);
    setExpression(attribute, identifier);
  }

  private Expression parseExpression(AngularHtmlUnitResolver resolver, XmlAttributeNode attribute) {
    int offset = attribute.getValue().getOffset() + 1;
    String value = attribute.getText();
    Token token = resolver.scanDart(value, 0, value.length(), offset);
    return resolver.parseExpression(token);
  }
}
