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

package com.google.dart.engine.internal.element.angular;

import com.google.dart.engine.element.angular.AngularHasClassSelectorElement;
import com.google.dart.engine.element.angular.AngularSelectorElement;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlTagNode;

import org.apache.commons.lang3.StringUtils;

/**
 * Implementation of {@link AngularSelectorElement} based on presence of a class.
 */
public class AngularHasClassSelectorElementImpl extends AngularSelectorElementImpl implements
    AngularHasClassSelectorElement {
  public AngularHasClassSelectorElementImpl(String name, int offset) {
    super(name, offset);
  }

  @Override
  public boolean apply(XmlTagNode node) {
    XmlAttributeNode attribute = node.getAttribute("class");
    if (attribute != null) {
      String text = attribute.getText();
      if (text != null) {
        String name = getName();
        for (String className : StringUtils.split(text)) {
          if (className.equals(name)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  @Override
  protected void appendTo(StringBuilder builder) {
    builder.append(".");
    builder.append(getName());
  }
}
