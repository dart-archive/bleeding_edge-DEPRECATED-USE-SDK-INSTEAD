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
package com.google.dart.engine.element;

import com.google.dart.engine.source.Source;

/**
 * The interface {@code ExternalHtmlScriptElement} defines the behavior of elements representing a
 * script tag in an HTML file having a {@code source} attribute that references a Dart library
 * source file.
 * 
 * @coverage dart.engine.element
 */
public interface ExternalHtmlScriptElement extends HtmlScriptElement {

  /**
   * Return the source referenced by this element, or {@code null} if this element does not
   * reference a Dart library source file.
   * 
   * @return the source for the external Dart library
   */
  public Source getScriptSource();
}
