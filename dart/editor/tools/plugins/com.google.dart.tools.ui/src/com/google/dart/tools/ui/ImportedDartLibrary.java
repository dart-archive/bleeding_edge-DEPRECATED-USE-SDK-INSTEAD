/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui;

import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartProject;

/**
 * This class wraps some {@link DartLibrary}. This class is used by the
 * {@link StandardDartElementContentProvider} to distinguish between {@link DartLibrary}s which are
 * children of some {@link DartProject} versus the children of an "Imported Libraries" element, see
 * {@link ImportedDartLibraryContainer}.
 */
public class ImportedDartLibrary {

  private final DartLibrary dartLibrary;

  private final ImportedDartLibraryContainer libraryContainer;

  public static final ImportedDartLibrary[] EMPTY_ARRAY = new ImportedDartLibrary[0];

  public ImportedDartLibrary(DartLibrary dartLibrary, ImportedDartLibraryContainer libraryContainer) {
    this.dartLibrary = dartLibrary;
    this.libraryContainer = libraryContainer;
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof ImportedDartLibrary) {
      ImportedDartLibrary container = (ImportedDartLibrary) object;
      return libraryContainer.equals(container.libraryContainer)
          && ImportedDartLibraryContainer.getUri(dartLibrary).equals(
              ImportedDartLibraryContainer.getUri(container.dartLibrary));
    }
    return false;
  }

  /**
   * Return the {@link DartLibrary}.
   */
  public DartLibrary getDartLibrary() {
    return dartLibrary;
  }

  /**
   * The name of this element for the label provider, this returns the display name of the library
   * returned from {@link #getDartLibrary()}.
   */
  public String getName() {
    return getDartLibrary().getDisplayName();
  }

  /**
   * Return the library container that contains this library.
   * 
   * @return the library container that contains this library
   */
  public ImportedDartLibraryContainer getParent() {
    return libraryContainer;
  }

  @Override
  public int hashCode() {
    return libraryContainer.hashCode() << 7
        | ImportedDartLibraryContainer.getUri(dartLibrary).hashCode();
  }

  /**
   * Returns {@link #getName()}, this is currently only used by the {@link DartElementComparator}.
   */
  @Override
  public String toString() {
    return getName();
  }

}
