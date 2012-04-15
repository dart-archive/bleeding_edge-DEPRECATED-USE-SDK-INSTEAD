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

/**
 * Instances of the class <code>LibrarySearchScope</code> implement a search scope that encompasses
 * everything in a given library.
 */
public class LibrarySearchScope implements SearchScope {
  /**
   * The library defining which elements are included in the scope.
   */
  private final DartLibrary library;

  /**
   * Create a search scope that encompasses everything in the given library.
   * 
   * @param library the library defining which elements are included in the scope
   */
  public LibrarySearchScope(DartLibrary library) {
    this.library = library;
  }

  @Override
  public boolean encloses(DartElement element) {
    DartLibrary elementLibrary = element.getAncestor(DartLibrary.class);
    return elementLibrary.equals(library);
  }

  /**
   * Return the library defining which elements are included in the scope.
   * 
   * @return the library defining which elements are included in the scope
   */
  public DartLibrary getLibrary() {
    return library;
  }

}
