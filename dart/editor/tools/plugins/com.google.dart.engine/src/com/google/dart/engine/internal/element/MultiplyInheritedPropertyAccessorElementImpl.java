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
package com.google.dart.engine.internal.element;

import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.MultiplyInheritedExecutableElement;

/**
 * The interface {@link MultiplyInheritedPropertyAccessorElementImpl} defines all of the behavior of
 * an {@link PropertyAccessorElementImpl}, with the additional information of an array of
 * {@link ExecutableElement}s from which this element was composed.
 * 
 * @coverage dart.engine.element
 */
public class MultiplyInheritedPropertyAccessorElementImpl extends PropertyAccessorElementImpl
    implements MultiplyInheritedExecutableElement {

  /**
   * An array the array of executable elements that were used to compose this element.
   */
  private ExecutableElement[] elements = PropertyAccessorElementImpl.EMPTY_ARRAY;

  public MultiplyInheritedPropertyAccessorElementImpl(Identifier name) {
    super(name);
    setSynthetic(true);
  }

  @Override
  public ExecutableElement[] getInheritedElements() {
    return elements;
  }

  public void setInheritedElements(ExecutableElement[] elements) {
    this.elements = elements;
  }

}
