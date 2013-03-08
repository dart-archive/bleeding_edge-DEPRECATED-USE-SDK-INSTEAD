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
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.LabelElement;

/**
 * Instances of the class {@code LabelElementHandle} implement a handle to a {@code LabelElement}.
 * 
 * @coverage dart.engine.element
 */
public class LabelElementHandle extends ElementHandle implements LabelElement {
  /**
   * Initialize a newly created element handle to represent the given element.
   * 
   * @param element the element being represented
   */
  public LabelElementHandle(LabelElement element) {
    super(element);
  }

  @Override
  public ExecutableElement getEnclosingElement() {
    return (ExecutableElement) super.getEnclosingElement();
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.LABEL;
  }
}
