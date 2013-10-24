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

import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.PropertyInducingElement;

/**
 * Instances of the class {@code PropertyAccessorElementHandle} implement a handle to a
 * {@code PropertyAccessorElement}.
 * 
 * @coverage dart.engine.element
 */
public class PropertyAccessorElementHandle extends ExecutableElementHandle implements
    PropertyAccessorElement {
  /**
   * Initialize a newly created element handle to represent the given element.
   * 
   * @param element the element being represented
   */
  public PropertyAccessorElementHandle(PropertyAccessorElement element) {
    super(element);
  }

  @Override
  public PropertyAccessorElement getCorrespondingGetter() {
    return getActualElement().getCorrespondingGetter();
  }

  @Override
  public PropertyAccessorElement getCorrespondingSetter() {
    return getActualElement().getCorrespondingSetter();
  }

  @Override
  public ElementKind getKind() {
    if (isGetter()) {
      return ElementKind.GETTER;
    } else {
      return ElementKind.SETTER;
    }
  }

  @Override
  public PropertyInducingElement getVariable() {
    return getActualElement().getVariable();
  }

  @Override
  public boolean isAbstract() {
    return getActualElement().isAbstract();
  }

  @Override
  public boolean isGetter() {
    return getActualElement().isGetter();
  }

  @Override
  public boolean isSetter() {
    return getActualElement().isSetter();
  }

  @Override
  protected PropertyAccessorElement getActualElement() {
    return (PropertyAccessorElement) super.getActualElement();
  }
}
