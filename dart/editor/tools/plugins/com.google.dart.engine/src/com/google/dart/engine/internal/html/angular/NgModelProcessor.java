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
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.internal.element.LocalVariableElementImpl;
import com.google.dart.engine.type.InterfaceType;

/**
 * {@link NgModelProcessor} describes built-in <code>NgModel</code> directive.
 */
class NgModelProcessor extends NgDirectiveProcessor {
  private static final String NG_MODEL = "ng-model";

  public static final NgModelProcessor INSTANCE = new NgModelProcessor();

  private NgModelProcessor() {
  }

  @Override
  public void apply(AngularHtmlUnitResolver resolver, XmlTagNode node) {
    XmlAttributeNode attribute = node.getAttribute(NG_MODEL);
    Expression expression = parseDartExpression(resolver, attribute);
    // identifiers have been already handled by "apply top"
    if (expression instanceof SimpleIdentifier) {
      return;
    }
    // resolve
    resolver.resolveNode(expression);
    // remember expression
    setExpression(attribute, expression);
  }

  @Override
  public boolean canApply(XmlTagNode node) {
    return node.getAttribute(NG_MODEL) != null;
  }

  /**
   * This method is used to define top-level {@link VariableElement}s for each "ng-model" with
   * simple identifier model.
   */
  void applyTopDeclarations(AngularHtmlUnitResolver resolver, XmlTagNode node) {
    XmlAttributeNode attribute = node.getAttribute(NG_MODEL);
    Expression expression = parseDartExpression(resolver, attribute);
    // if not identifier, then not a top-level model, delay until "apply"
    if (!(expression instanceof SimpleIdentifier)) {
      return;
    }
    SimpleIdentifier identifier = (SimpleIdentifier) expression;
    // define variable Element
    InterfaceType type = resolver.getTypeProvider().getStringType();
    LocalVariableElementImpl element = resolver.createLocalVariableFromIdentifier(type, identifier);
    resolver.defineTopVariable(element);
    // remember expression
    identifier.setStaticElement(element);
    identifier.setStaticType(type);
    setExpression(attribute, identifier);
  }
}
