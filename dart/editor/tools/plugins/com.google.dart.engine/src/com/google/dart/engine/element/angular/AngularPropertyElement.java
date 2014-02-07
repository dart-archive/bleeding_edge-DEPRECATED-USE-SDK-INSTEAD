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
package com.google.dart.engine.element.angular;

import com.google.dart.engine.element.FieldElement;

/**
 * The interface {@code AngularPropertyElement} defines a single property in
 * {@link AngularComponentElement}.
 * 
 * @coverage dart.engine.element
 */
public interface AngularPropertyElement extends AngularElement {
  /**
   * An empty array of property elements.
   */
  AngularPropertyElement[] EMPTY_ARRAY = {};

  /**
   * Returns the field this property is mapped to.
   * 
   * @return the field this property is mapped to.
   */
  FieldElement getField();

  /**
   * Return the offset of the field name of this property in the property map, or {@code -1} if
   * property was created using annotation on {@link FieldElement}.
   * 
   * @return the offset of the field name of this property
   */
  int getFieldNameOffset();

  /**
   * Returns the kind of this property.
   * 
   * @return the kind of this property
   */
  AngularPropertyKind getPropertyKind();
}
