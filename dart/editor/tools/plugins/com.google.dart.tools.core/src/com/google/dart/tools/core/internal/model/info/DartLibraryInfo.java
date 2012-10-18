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

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartImport;
import com.google.dart.tools.core.model.DartPart;

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
  private DartImport[] imports = DartImport.EMPTY_ARRAY;

  /**
   * All "part" declarations of this library.
   */
  private DartPart[] parts = DartPart.EMPTY_ARRAY;

  /**
   * Adds the given {@link DartImport}. If the library was already added with same the prefix, then
   * request will be ignored.
   */
  public void addImport(DartImport newImport) {
    // may be already added
    for (DartImport imprt : imports) {
      if (imprt.equals(newImport)) {
        return;
      }
    }
    // do add
    int length = imports.length;
    System.arraycopy(imports, 0, imports = new DartImport[length + 1], 0, length);
    imports[length] = newImport;
  }

  /**
   * Adds the given {@link DartPart}.
   */
  public void addPart(DartPart newPart) {
    // may be already added
    for (DartPart part : parts) {
      if (part.equals(newPart)) {
        return;
      }
    }
    // do add
    int length = parts.length;
    System.arraycopy(parts, 0, parts = new DartPart[length + 1], 0, length);
    parts[length] = newPart;
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
   * @return the {@link DartImport}s for all imported libraries into this library
   */
  public DartImport[] getImports() {
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

  public DartPart[] getParts() {
    return parts;
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
