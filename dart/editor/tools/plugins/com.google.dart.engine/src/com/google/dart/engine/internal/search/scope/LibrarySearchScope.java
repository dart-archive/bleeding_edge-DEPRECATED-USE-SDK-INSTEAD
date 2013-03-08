/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.engine.internal.search.scope;

import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.search.SearchScope;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Collection;

/**
 * Instances of the class <code>LibrarySearchScope</code> implement a search scope that encompasses
 * everything in a given collection of libraries.
 * 
 * @coverage dart.engine.search
 */
public class LibrarySearchScope implements SearchScope {
  /**
   * The libraries defining which elements are included in the scope.
   */
  private final LibraryElement[] libraries;

  /**
   * Create a search scope that encompasses everything in the given libraries.
   * 
   * @param libraries the libraries defining which elements are included in the scope
   */
  public LibrarySearchScope(Collection<LibraryElement> libraries) {
    this(libraries.toArray(new LibraryElement[libraries.size()]));
  }

  /**
   * Create a search scope that encompasses everything in the given libraries.
   * 
   * @param libraries the libraries defining which elements are included in the scope
   */
  public LibrarySearchScope(LibraryElement... libraries) {
    this.libraries = libraries;
  }

  @Override
  public boolean encloses(Element element) {
    LibraryElement elementLibrary = element.getAncestor(LibraryElement.class);
    return ArrayUtils.contains(libraries, elementLibrary);
  }

  /**
   * Return the libraries defining which elements are included in the scope.
   * 
   * @return the collection of libraries defining which elements are included in the scope
   */
  public LibraryElement[] getLibraries() {
    return libraries;
  }
}
