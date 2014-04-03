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
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.NamespaceCombinator;
import com.google.dart.engine.element.PrefixElement;

/**
 * Instances of the class {@code ImportElementHandle} implement a handle to an {@code ImportElement}
 * .
 * 
 * @coverage dart.engine.element
 */
public class ImportElementHandle extends ElementHandle implements ImportElement {
  /**
   * Initialize a newly created element handle to represent the given element.
   * 
   * @param element the element being represented
   */
  public ImportElementHandle(ImportElement element) {
    super(element);
  }

  @Override
  public NamespaceCombinator[] getCombinators() {
    return getActualElement().getCombinators();
  }

  @Override
  public LibraryElement getImportedLibrary() {
    return getActualElement().getImportedLibrary();
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.IMPORT;
  }

  @Override
  public PrefixElement getPrefix() {
    return getActualElement().getPrefix();
  }

  @Override
  public int getPrefixOffset() {
    return getActualElement().getPrefixOffset();
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
  public boolean isDeferred() {
    return getActualElement().isDeferred();
  }

  @Override
  protected ImportElement getActualElement() {
    return (ImportElement) super.getActualElement();
  }
}
