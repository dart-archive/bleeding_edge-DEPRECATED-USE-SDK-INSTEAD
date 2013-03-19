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
package com.google.dart.engine.internal.context;

import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.source.SourceKind;

/**
 * Instances of the class {@code LibraryInfo} maintain the information cached by an analysis context
 * about an individual library.
 * 
 * @coverage dart.engine
 */
public class LibraryInfo extends CompilationUnitInfo {
  /**
   * The state of the cached library element.
   */
  private CacheState elementState = CacheState.INVALID;

  /**
   * The element representing the library, or {@code null} if the element is not currently cached.
   */
  private LibraryElement element;

  /**
   * The state of the cached public namespace.
   */
  private CacheState publicNamespaceState = CacheState.INVALID;

  /**
   * The public namespace of the library, or {@code null} if the namespace is not currently cached.
   */
  private Namespace publicNamespace;

  /**
   * Initialize a newly created information holder to be empty.
   */
  public LibraryInfo() {
    super();
  }

  /**
   * Remove the library element from the cache.
   */
  public void clearElement() {
    element = null;
  }

  /**
   * Remove the public namespace from the cache.
   */
  public void clearPublicNamespace() {
    publicNamespace = null;
  }

  @Override
  public LibraryInfo copy() {
    LibraryInfo copy = new LibraryInfo();
    copy.copyFrom(this);
    return copy;
  }

  /**
   * Return the element representing the library, or {@code null} if the element is not currently
   * cached.
   * 
   * @return the element representing the library
   */
  public LibraryElement getElement() {
    return element;
  }

  @Override
  public SourceKind getKind() {
    return SourceKind.LIBRARY;
  }

  /**
   * Return the public namespace of the library, or {@code null} if the namespace is not currently
   * cached.
   * 
   * @return the public namespace of the library
   */
  public Namespace getPublicNamespace() {
    return publicNamespace;
  }

  /**
   * Return {@code true} if the library element needs to be recomputed.
   * 
   * @return {@code true} if the library element needs to be recomputed
   */
  public boolean hasInvalidElement() {
    return elementState == CacheState.INVALID;
  }

  /**
   * Return {@code true} if the public namespace needs to be recomputed.
   * 
   * @return {@code true} if the public namespace needs to be recomputed
   */
  public boolean hasInvalidPublicNamespace() {
    return publicNamespaceState == CacheState.INVALID;
  }

  /**
   * Mark the library element as needing to be recomputed.
   */
  public void invalidateElement() {
    elementState = CacheState.INVALID;
    element = null;
  }

  /**
   * Mark the public namespace as needing to be recomputed.
   */
  public void invalidatePublicNamespace() {
    publicNamespaceState = CacheState.INVALID;
    publicNamespace = null;
  }

  /**
   * Set the element representing the library to the given element.
   * <p>
   * <b>Note:</b> Do not use this method to clear or invalidate the element. Use either
   * {@link #clearElement()} or {@link #invalidateElement()}.
   * 
   * @param element the element representing the library
   */
  public void setElement(LibraryElement element) {
    this.element = element;
    elementState = CacheState.VALID;
  }

  /**
   * Set the public namespace of the library to the given namespace.
   * <p>
   * <b>Note:</b> Do not use this method to clear or invalidate the element. Use either
   * {@link #clearPublicNamespace()} or {@link #invalidatePublicNamespace()}.
   * 
   * @param namespace the public namespace of the library
   */
  public void setPublicNamespace(Namespace namespace) {
    publicNamespace = namespace;
    publicNamespaceState = CacheState.VALID;
  }

  @Override
  protected void copyFrom(SourceInfo info) {
    super.copyFrom(info);
    // TODO(brianwilkerson) Decide how much of this data we can safely copy.
//    if (info instanceof LibraryInfo) {
//      LibraryInfo libraryInfo = (LibraryInfo) info;
//      elementState = libraryInfo.elementState;
//      element = libraryInfo.element;
//      publicNamespaceState = libraryInfo.publicNamespaceState;
//      publicNamespace = libraryInfo.publicNamespace;
//    }
  }
}
