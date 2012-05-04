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
package com.google.dart.tools.core.internal.search.scope;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.search.SearchScope;

import java.util.Collection;

/**
 * Instances of the class <code>LibrarySearchScope</code> implement a search scope that encompasses
 * everything in a given collection of libraries.
 */
public class LibrarySearchScope implements SearchScope {
  /**
   * The libraries defining which elements are included in the scope.
   */
  private final DartLibrary[] libraries;

  /**
   * Create a search scope that encompasses everything in the given libraries.
   * 
   * @param libraries the libraries defining which elements are included in the scope
   */
  public LibrarySearchScope(Collection<DartLibrary> libraries) {
    this(libraries.toArray(new DartLibrary[libraries.size()]));
  }

  /**
   * Create a search scope that encompasses everything in the given libraries.
   * 
   * @param libraries the libraries defining which elements are included in the scope
   */
  public LibrarySearchScope(DartLibrary... libraries) {
    this.libraries = libraries;
  }

  /**
   * Create a search scope that encompasses everything in the given library.
   * 
   * @param library the library defining which elements are included in the scope
   */
  public LibrarySearchScope(DartLibrary library) {
    this(new DartLibrary[] {library});
  }

  @Override
  public boolean encloses(DartElement element) {
    DartLibrary elementLibrary = element.getAncestor(DartLibrary.class);
    for (DartLibrary library : libraries) {
      if (elementLibrary.equals(library)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return the libraries defining which elements are included in the scope.
   * 
   * @return the collection of libraries defining which elements are included in the scope
   */
  public DartLibrary[] getLibraries() {
    return libraries;
  }
}
