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
package com.google.dart.engine.element.polymer;

/**
 * The interface {@code PolymerTagHtmlElement} defines a Polymer custom tag in HTML.
 * 
 * <pre>
 * <polymer-element name="my-example" attributes='attrA attrB'>
 * </polymer-element>
 * </pre>
 * 
 * @coverage dart.engine.element
 */
public interface PolymerTagHtmlElement extends PolymerElement {
  /**
   * An empty array of {@link PolymerTagHtmlElement}s.
   */
  PolymerTagHtmlElement[] EMPTY_ARRAY = new PolymerTagHtmlElement[0];

  /**
   * Return an array containing all of the attributes declared by this tag.
   */
  PolymerAttributeElement[] getAttributes();

  /**
   * Return the {@link PolymerTagDartElement} part on this Polymer custom tag. Maybe {@code null} if
   * it has not been resolved yet or there are no corresponding Dart part defined.
   */
  PolymerTagDartElement getDartElement();
}
