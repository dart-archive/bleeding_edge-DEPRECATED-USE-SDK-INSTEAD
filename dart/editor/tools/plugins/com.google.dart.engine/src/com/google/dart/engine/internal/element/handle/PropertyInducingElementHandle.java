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
package com.google.dart.engine.internal.element.handle;

import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.PropertyInducingElement;

/**
 * The abstract class {@code PropertyInducingElementHandle} implements the behavior common to
 * objects that implement a handle to an {@code PropertyInducingElement}.
 * 
 * @coverage dart.engine.element
 */
public abstract class PropertyInducingElementHandle extends VariableElementHandle implements
    PropertyInducingElement {
  /**
   * Initialize a newly created element handle to represent the given element.
   * 
   * @param element the element being represented
   */
  public PropertyInducingElementHandle(PropertyInducingElement element) {
    super(element);
  }

  @Override
  public PropertyAccessorElement getGetter() {
    return getActualElement().getGetter();
  }

  @Override
  public PropertyAccessorElement getSetter() {
    return getActualElement().getSetter();
  }

  @Override
  public boolean isStatic() {
    return getActualElement().isStatic();
  }

  @Override
  protected PropertyInducingElement getActualElement() {
    return (PropertyInducingElement) super.getActualElement();
  }
}
