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

import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.PropertyInducingElement;

/**
 * Instances of the class {@code PropertyInducingElementImpl} implement a
 * {@code PropertyInducingElement}.
 * 
 * @coverage dart.engine.element
 */
public abstract class PropertyInducingElementImpl extends VariableElementImpl implements
    PropertyInducingElement {
  /**
   * The getter associated with this element.
   */
  private PropertyAccessorElement getter;

  /**
   * The setter associated with this element, or {@code null} if the element is effectively
   * {@code final} and therefore does not have a setter associated with it.
   */
  private PropertyAccessorElement setter;

  /**
   * An empty array of elements.
   */
  public static final PropertyInducingElement[] EMPTY_ARRAY = new PropertyInducingElement[0];

  /**
   * Initialize a newly created element to have the given name.
   * 
   * @param name the name of this element
   */
  public PropertyInducingElementImpl(Identifier name) {
    super(name);
  }

  /**
   * Initialize a newly created synthetic element to have the given name.
   * 
   * @param name the name of this element
   */
  public PropertyInducingElementImpl(String name) {
    super(name, -1);
    setSynthetic(true);
  }

  @Override
  public PropertyAccessorElement getGetter() {
    return getter;
  }

  @Override
  public PropertyAccessorElement getSetter() {
    return setter;
  }

  /**
   * Set the getter associated with this element to the given accessor.
   * 
   * @param getter the getter associated with this element
   */
  public void setGetter(PropertyAccessorElement getter) {
    this.getter = getter;
  }

  /**
   * Set the setter associated with this element to the given accessor.
   * 
   * @param setter the setter associated with this element
   */
  public void setSetter(PropertyAccessorElement setter) {
    this.setter = setter;
  }
}
