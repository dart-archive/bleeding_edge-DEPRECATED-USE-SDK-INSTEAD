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

import com.google.dart.engine.element.angular.AngularComponentElement;
import com.google.dart.engine.element.angular.AngularPropertyElement;
import com.google.dart.engine.element.angular.AngularPropertyKind;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlTagNode;

/**
 * {@link NgComponentElementProcessor} applies {@link AngularComponentElement} by parsing mapped
 * attributes as expressions.
 */
class NgComponentElementProcessor extends NgDirectiveProcessor {
  private final AngularComponentElement element;

  public NgComponentElementProcessor(AngularComponentElement element) {
    this.element = element;
  }

  @Override
  public void apply(AngularHtmlUnitResolver resolver, XmlTagNode node) {
    node.setElement(element.getSelector());
    for (AngularPropertyElement property : element.getProperties()) {
      String name = property.getName();
      XmlAttributeNode attribute = node.getAttribute(name);
      if (attribute != null) {
        attribute.setElement(property);
        // resolve if binding
        if (property.getPropertyKind() != AngularPropertyKind.ATTR) {
          AngularExpression expression = parseAngularExpression(resolver, attribute);
          resolver.resolveExpression(expression);
          setAngularExpression(attribute, expression);
        }
      }
    }
  }

  @Override
  public boolean canApply(XmlTagNode node) {
    return element.getSelector().apply(node);
  }
}
