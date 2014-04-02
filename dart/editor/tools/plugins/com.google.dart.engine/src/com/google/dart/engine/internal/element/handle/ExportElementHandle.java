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
import com.google.dart.engine.element.ExportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.NamespaceCombinator;

/**
 * Instances of the class {@code ExportElementHandle} implement a handle to an {@code ExportElement}
 * .
 * 
 * @coverage dart.engine.element
 */
public class ExportElementHandle extends ElementHandle implements ExportElement {
  /**
   * Initialize a newly created element handle to represent the given element.
   * 
   * @param element the element being represented
   */
  public ExportElementHandle(ExportElement element) {
    super(element);
  }

  @Override
  public NamespaceCombinator[] getCombinators() {
    return getActualElement().getCombinators();
  }

  @Override
  public LibraryElement getExportedLibrary() {
    return getActualElement().getExportedLibrary();
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.EXPORT;
  }

  @Override
  public String getUri() {
    return getActualElement().getUri();
  }

  @Override
  public int getUriEnd() {
    return getActualElement().getUriEnd();
  }

  @Override
  public int getUriOffset() {
    return getActualElement().getUriOffset();
  }

  @Override
  protected ExportElement getActualElement() {
    return (ExportElement) super.getActualElement();
  }
}
