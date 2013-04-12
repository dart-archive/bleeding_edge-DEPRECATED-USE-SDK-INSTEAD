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
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceKind;

/**
 * Instances of the class {@code LibraryInfo} maintain the information cached by an analysis context
 * about an individual library.
 * 
 * @coverage dart.engine
 */
public class LibraryInfo extends CompilationUnitInfo {
  /**
   * Mask indicating that this library is launchable: that the file has a main method.
   */
  private static final int LAUNCHABLE = 1 << 1;

  /**
   * Mask indicating that the library is client code: that the library depends on the html library.
   * If the library is not "client code", then it is referenced as "server code".
   */
  private static final int CLIENT_CODE = 1 << 2;

  /**
   * The state of the cached library element.
   */
  private CacheState elementState = CacheState.INVALID;

  /**
   * The element representing the library, or {@code null} if the element is not currently cached.
   */
  private LibraryElement element;

  /**
   * The state of the cached unit sources.
   */
  private CacheState unitSourcesState = CacheState.INVALID;

  /**
   * The sources of the compilation units that compose the library, including both the defining
   * compilation unit and any parts.
   */
  private Source[] unitSources = Source.EMPTY_ARRAY;

  /**
   * The state of the cached public namespace.
   */
  private CacheState publicNamespaceState = CacheState.INVALID;

  /**
   * The public namespace of the library, or {@code null} if the namespace is not currently cached.
   */
  private Namespace publicNamespace;

  /**
   * The state of the cached client/ server flag.
   */
  private CacheState clientServerState = CacheState.INVALID;

  /**
   * The state of the cached launchable flag.
   */
  private CacheState launchableState = CacheState.INVALID;

  /**
   * An integer holding bit masks such as {@link #LAUNCHABLE} and {@link #CLIENT_CODE}.
   */
  private int bitmask = 0;

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
    elementState = CacheState.FLUSHED;
  }

  /**
   * Remove the public namespace from the cache.
   */
  public void clearPublicNamespace() {
    publicNamespace = null;
    publicNamespaceState = CacheState.FLUSHED;
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
   * Return the sources of the compilation units that compose the library, including both the
   * defining compilation unit and any parts.
   * 
   * @return the sources of the compilation units that compose the library
   */
  public Source[] getUnitSources() {
    return unitSources;
  }

  /**
   * Return {@code true} if the client/ server flag needs to be recomputed.
   * 
   * @return {@code true} if the client/ server flag needs to be recomputed
   */
  public boolean hasInvalidClientServer() {
    return clientServerState == CacheState.INVALID;
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
   * Return {@code true} if the launchable flag needs to be recomputed.
   * 
   * @return {@code true} if the launchable flag needs to be recomputed
   */
  public boolean hasInvalidLaunchable() {
    return launchableState == CacheState.INVALID;
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
   * Mark the client/ server flag as needing to be recomputed.
   */
  public void invalidateClientServer() {
    clientServerState = CacheState.INVALID;
    bitmask &= ~CLIENT_CODE;
  }

  /**
   * Mark the library element as needing to be recomputed.
   */
  public void invalidateElement() {
    elementState = CacheState.INVALID;
    element = null;
  }

  /**
   * Mark the launchable flag as needing to be recomputed.
   */
  public void invalidateLaunchable() {
    launchableState = CacheState.INVALID;
    bitmask &= ~LAUNCHABLE;
  }

  /**
   * Mark the public namespace as needing to be recomputed.
   */
  public void invalidatePublicNamespace() {
    publicNamespaceState = CacheState.INVALID;
    publicNamespace = null;
  }

  /**
   * Mark the compilation unit sources as needing to be recomputed.
   */
  public void invalidateUnitSources() {
    unitSourcesState = CacheState.INVALID;
    unitSources = Source.EMPTY_ARRAY;
  }

  /**
   * Return {@code true} if this library is client based code: the library depends on the html
   * library.
   * 
   * @return {@code true} if this library is client based code: the library depends on the html
   *         library
   */
  public boolean isClient() {
    return (bitmask & CLIENT_CODE) != 0;
  }

  /**
   * Return {@code true} if this library is launchable: the file includes a main method.
   * 
   * @return {@code true} if this library is launchable: the file includes a main method
   */
  public boolean isLaunchable() {
    return (bitmask & LAUNCHABLE) != 0;
  }

  /**
   * Return {@code true} if this library is server based code: the library does not depends on the
   * html library.
   * 
   * @return {@code true} if this library is server based code: the library does not depends on the
   *         html library
   */
  public boolean isServer() {
    return (bitmask & CLIENT_CODE) == 0;
  }

  /**
   * Sets the value of the client/ server flag.
   * <p>
   * <b>Note:</b> Do not use this method to invalidate the flag, use
   * {@link #invalidateClientServer()}.
   * 
   * @param isClient the new value of the client flag
   */
  public void setClient(boolean isClient) {
    if (isClient) {
      bitmask |= CLIENT_CODE;
    } else {
      bitmask &= ~CLIENT_CODE;
    }
    clientServerState = CacheState.VALID;
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
   * Sets the value of the launchable flag.
   * <p>
   * <b>Note:</b> Do not use this method to invalidate the flag, use {@link #invalidateLaunchable()}.
   * 
   * @param isClient the new value of the client flag
   */
  public void setLaunchable(boolean launchable) {
    if (launchable) {
      bitmask |= LAUNCHABLE;
    } else {
      bitmask &= ~LAUNCHABLE;
    }
    launchableState = CacheState.VALID;
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

  /**
   * Set the sources of the compilation units that compose the library to the given sources.
   * 
   * @param sources the sources of the compilation units that compose the library
   */
  public void setUnitSources(Source[] sources) {
    unitSourcesState = CacheState.VALID;
    unitSources = sources;
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
//      unitSourcesState = libraryInfo.unitSourcesState;
//      unitSources = libraryInfo.unitSources;
//    }
  }
}
