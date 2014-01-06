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

import com.google.common.annotations.VisibleForTesting;
import com.google.dart.engine.element.angular.AngularSelector;
import com.google.dart.engine.html.ast.XmlTagNode;

/**
 * Implementation of {@link AngularSelector} based on presence of attribute.
 */
public class HasAttributeSelector implements AngularSelector {
  private final String attributeName;

  public HasAttributeSelector(String attributeName) {
    this.attributeName = attributeName;
  }

  @Override
  public boolean apply(XmlTagNode node) {
    return node.getAttribute(attributeName) != null;
  }

  /**
   * @return the attribute name for check presence for.
   */
  @VisibleForTesting
  public String getAttributeName() {
    return attributeName;
  }
}
