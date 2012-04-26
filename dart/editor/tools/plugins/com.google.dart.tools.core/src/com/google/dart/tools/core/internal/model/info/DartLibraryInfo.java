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
package com.google.dart.tools.core.internal.model.info;

import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartLibraryImport;

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
   * All imports of this library.
   */
  private DartLibraryImport[] imports = DartLibraryImpl.EMPTY_IMPORT_ARRAY;

  /**
   * Add the given {@link DartLibrary} to the imported libraries. If the library was already added
   * with same the prefix, then request will be ignored.
   * 
   * @param library the {@link DartLibrary} to add, not <code>null</code>
   * @param prefix the prefix used to import library, may be <code>null</code>
   */
  public void addImport(DartLibrary library, String prefix) {
    // may be no information
    if (library == null) {
      return;
    }
    // may be already added
    for (DartLibraryImport imp : imports) {
      if (imp.equals(library, prefix)) {
        return;
      }
    }
    // do add
    int length = imports.length;
    System.arraycopy(imports, 0, imports = new DartLibraryImport[length + 1], 0, length);
    imports[length] = new DartLibraryImport(library, prefix);
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
   * @return the {@link DartLibraryImport}s for all imported libraries into this library
   */
  public DartLibraryImport[] getImports() {
    return imports;
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
   * Remove the given library from the array of imported libraries. If the library was not in the
   * array, then the array of imported libraries will not be modified.
   * 
   * @param library the library to be removed
   * @param prefix the prefix with which library to be removed was imported
   */
  public void removeImport(DartLibrary library, String prefix) {
    // may be no information
    if (library == null) {
      return;
    }
    // do remove
    for (int i = 0; i < imports.length; i++) {
      DartLibraryImport imp = imports[i];
      if (imp.equals(library, prefix)) {
        int length = imports.length;
        if (length == 1) {
          imports = DartLibraryImpl.EMPTY_IMPORT_ARRAY;
        } else {
          DartLibraryImport[] newChildren = new DartLibraryImport[length - 1];
          System.arraycopy(imports, 0, newChildren, 0, i);
          if (i < length - 1) {
            System.arraycopy(imports, i + 1, newChildren, i, length - 1 - i);
          }
          imports = newChildren;
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
   * Set the name of the library to the given name.
   * 
   * @param newName the name of the library
   */
  public void setName(String newName) {
    name = newName;
  }
}
