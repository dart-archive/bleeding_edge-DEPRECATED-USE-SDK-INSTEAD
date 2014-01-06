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
 * Implementation of {@link AngularSelector} based tag name.
 */
public class IsTagSelector implements AngularSelector {
  private final String name;

  public IsTagSelector(String name) {
    this.name = name;
  }

  @Override
  public boolean apply(XmlTagNode node) {
    // TODO(scheglov) May be replace getTag() with getTagToken() and return String from getTag().
    // TODO(scheglov) or even just add isTag(String name)
    return node.getTag().getLexeme().equals(name);
  }

  /**
   * @return the tag name to check for.
   */
  @VisibleForTesting
  public String getName() {
    return name;
  }
}
