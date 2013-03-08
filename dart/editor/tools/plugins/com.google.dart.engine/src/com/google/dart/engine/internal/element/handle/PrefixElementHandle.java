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
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.PrefixElement;

/**
 * Instances of the class {@code PrefixElementHandle} implement a handle to a {@code PrefixElement}.
 * 
 * @coverage dart.engine.element
 */
public class PrefixElementHandle extends ElementHandle implements PrefixElement {
  /**
   * Initialize a newly created element handle to represent the given element.
   * 
   * @param element the element being represented
   */
  public PrefixElementHandle(PrefixElement element) {
    super(element);
  }

  @Override
  public LibraryElement getEnclosingElement() {
    return (LibraryElement) super.getEnclosingElement();
  }

  @Override
  public LibraryElement[] getImportedLibraries() {
    return getActualElement().getImportedLibraries();
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.PREFIX;
  }

  @Override
  protected PrefixElement getActualElement() {
    return (PrefixElement) super.getActualElement();
  }
}
