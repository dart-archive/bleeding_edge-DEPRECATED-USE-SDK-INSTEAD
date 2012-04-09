/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.model.info;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartLibrary;

/**
 * Instances of the class <code>DartLibraryInfo</code> maintain the cached data shared by all equal
 * libraries.
 */
public class DartLibraryInfo extends OpenableElementInfo {
  /**
   * The name of the library as it appears in the #library directive.
   */
  private String name;

  /**
   * The compilation unit that defines this library.
   */
  private CompilationUnit definingCompilationUnit;

  /**
   * An array containing all of the libraries that are imported by this library.
   */
  private DartLibrary[] importedLibraries = DartLibrary.EMPTY_LIBRARY_ARRAY;

  /**
   * An array containing all of the prefixes used when importing libraries into this library. There
   * are no duplicates, but the order is not specified.
   */
  private String[] prefixes = new String[0];

  /**
   * Add the given library to the array of imported libraries. If the library was already in the
   * array, then the array of imported libraries will not be modified.
   * 
   * @param library the library to be added
   */
  public void addImport(DartLibrary library) {
    if (library == null) {
      return;
    }
    int length = importedLibraries.length;
    if (length == 0) {
      importedLibraries = new DartLibrary[] {library};
    } else {
      for (int i = 0; i < length; i++) {
        if (importedLibraries[i].equals(library)) {
          return; // already included
        }
      }
      System.arraycopy(importedLibraries, 0, importedLibraries = new DartLibrary[length + 1], 0,
          length);
      importedLibraries[length] = library;
    }
  }

  /**
   * Return the compilation unit that defines this library.
   * 
   * @return the compilation unit that defines this library
   */
  public CompilationUnit getDefiningCompilationUnit() {
    return definingCompilationUnit;
  }

  /**
   * Answer the imported libraries. Imported libraries are NOT returned as part of
   * {@link #getChildren()}
   * 
   * @return an array of imported libraries (not <code>null</code>, contains no <code>null</code>s)
   */
  public DartLibrary[] getImportedLibraries() {
    return importedLibraries;
  }

  /**
   * Return the name of the library as it appears in the #library directive.
   * 
   * @return the name of the library
   */
  public String getName() {
    return name;
  }

  /**
   * Return an array containing all of the prefixes used when importing libraries into this library.
   * There are no duplicates, but the order is not specified.
   * 
   * @return an array containing all of the prefixes used when importing libraries into this library
   */
  public String[] getPrefixes() {
    return prefixes;
  }

  /**
   * Remove the given library from the array of imported libraries. If the library was not in the
   * array, then the array of imported libraries will not be modified.
   * 
   * @param library the library to be added
   */
  public void removeImport(DartLibrary library) {
    if (library == null) {
      return;
    }
    for (int i = 0, length = importedLibraries.length; i < length; i++) {
      DartLibrary element = importedLibraries[i];
      if (element.equals(library)) {
        if (length == 1) {
          importedLibraries = DartLibrary.EMPTY_LIBRARY_ARRAY;
        } else {
          DartLibrary[] newChildren = new DartLibrary[length - 1];
          System.arraycopy(importedLibraries, 0, newChildren, 0, i);
          if (i < length - 1) {
            System.arraycopy(importedLibraries, i + 1, newChildren, i, length - 1 - i);
          }
          importedLibraries = newChildren;
        }
        break;
      }
    }
  }

  /**
   * Set the compilation unit that defines this library to the given compilation unit.
   * 
   * @param unit the compilation unit that defines this library
   */
  public void setDefiningCompilationUnit(CompilationUnit unit) {
    definingCompilationUnit = unit;
  }

  /**
   * Set the imported libraries. Imported libraries should NOT be part of the receiver's children.
   * 
   * @param importedLibraries the imported libraries (not <code>null</code>, contains no
   *          <code>null</code>s)
   */
  public void setImportedLibraries(DartLibrary[] importedLibraries) {
    this.importedLibraries = importedLibraries;
  }

  /**
   * Set the name of the library to the given name.
   * 
   * @param newName the name of the library
   */
  public void setName(String newName) {
    name = newName;
  }

  /**
   * Set the prefixes used when importing libraries into this library to the given array of strings.
   * There should not be any duplicates, but the order is not specified.
   * 
   * @param prefixes an array containing all of the prefixes used when importing libraries into this
   *          library
   */
  public void setPrefixes(String[] prefixes) {
    this.prefixes = prefixes;
  }
}
