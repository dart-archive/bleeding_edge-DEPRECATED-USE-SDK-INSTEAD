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

import com.google.dart.engine.element.FieldElement;

/**
 * The interface {@code PolymerAttributeElement} defines an attribute in
 * {@link PolymerTagHtmlElement}.
 * 
 * <pre>
 * <polymer-element name="my-example" attributes='attrA attrB'>
 * </polymer-element>
 * </pre>
 * 
 * @coverage dart.engine.element
 */
public interface PolymerAttributeElement extends PolymerElement {
  /**
   * An empty array of Polymer custom tag attributes.
   */
  PolymerAttributeElement[] EMPTY_ARRAY = new PolymerAttributeElement[0];

  /**
   * Return the {@link FieldElement} associated with this attribute. Maybe {@code null} if
   * {@link PolymerTagDartElement} does not have a field associated with it.
   */
  FieldElement getField();
}
