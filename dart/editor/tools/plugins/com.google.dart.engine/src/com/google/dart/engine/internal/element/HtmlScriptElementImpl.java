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
package com.google.dart.engine.internal.element;

import com.google.dart.engine.element.HtmlScriptElement;
import com.google.dart.engine.html.ast.XmlTagNode;

/**
 * Instances of the class {@code HtmlScriptElementImpl} implement an {@link HtmlScriptElement}.
 * 
 * @coverage dart.engine.element
 */
public abstract class HtmlScriptElementImpl extends ElementImpl implements HtmlScriptElement {

  /**
   * An empty array of HTML script elements.
   */
  public static final HtmlScriptElement[] EMPTY_ARRAY = new HtmlScriptElement[0];

  /**
   * Initialize a newly created script element to have the specified tag name and offset.
   * 
   * @param node the XML node from which this element is derived (not {@code null})
   */
  public HtmlScriptElementImpl(XmlTagNode node) {
    super(node.getTag(), node.getTagToken().getOffset());
  }
}
