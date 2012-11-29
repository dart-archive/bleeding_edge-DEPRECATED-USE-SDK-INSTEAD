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
package com.google.dart.engine.internal.element;

import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ExportSpecification;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.ImportSpecification;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.PrefixElement;

import java.util.HashSet;

/**
 * Instances of the class {@code LibraryElementImpl} implement a {@code LibraryElement}.
 */
public class LibraryElementImpl extends ElementImpl implements LibraryElement {
  /**
   * An empty array of library elements.
   */
  public static final LibraryElement[] EMPTY_ARRAY = new LibraryElement[0];

  /**
   * The compilation unit that defines this library.
   */
  private CompilationUnitElement definingCompilationUnit;

  /**
   * The entry point for this library, or {@code null} if this library does not have an entry point.
   */
  private FunctionElement entryPoint;

  /**
   * An array containing specifications of all of the imports defined in this library.
   */
  private ImportSpecification[] imports = ImportSpecification.EMPTY_ARRAY;

  /**
   * An array containing specifications of all of the exports defined in this library.
   */
  private ExportSpecification[] exports = ExportSpecification.EMPTY_ARRAY;

  /**
   * An array containing all of the compilation units that are included in this library using a
   * {@code part} directive.
   */
  private CompilationUnitElement[] parts = CompilationUnitElementImpl.EMPTY_ARRAY;

  /**
   * Initialize a newly created library element to have the given name.
   * 
   * @param name the name of this element
   */
  public LibraryElementImpl(Identifier name) {
    super(name);
  }

  @Override
  public boolean equals(Object object) {
    return this.getClass() == object.getClass()
        && definingCompilationUnit.equals(((LibraryElementImpl) object).getDefiningCompilationUnit());
  }

  @Override
  public CompilationUnitElement getDefiningCompilationUnit() {
    return definingCompilationUnit;
  }

  @Override
  public FunctionElement getEntryPoint() {
    return entryPoint;
  }

  @Override
  public ExportSpecification[] getExports() {
    return exports;
  }

  @Override
  public LibraryElement[] getImportedLibraries() {
    HashSet<LibraryElement> libraries = new HashSet<LibraryElement>(imports.length);
    for (ImportSpecification specification : imports) {
      LibraryElement prefix = specification.getImportedLibrary();
      libraries.add(prefix);
    }
    return libraries.toArray(new LibraryElement[libraries.size()]);
  }

  @Override
  public ImportSpecification[] getImports() {
    return imports;
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.LIBRARY;
  }

  @Override
  public CompilationUnitElement[] getParts() {
    return parts;
  }

  @Override
  public PrefixElement[] getPrefixes() {
    HashSet<PrefixElement> prefixes = new HashSet<PrefixElement>(imports.length);
    for (ImportSpecification specification : imports) {
      PrefixElement prefix = specification.getPrefix();
      if (prefix != null) {
        prefixes.add(prefix);
      }
    }
    return prefixes.toArray(new PrefixElement[prefixes.size()]);
  }

  @Override
  public int hashCode() {
    return definingCompilationUnit.hashCode();
  }

  /**
   * Set the compilation unit that defines this library to the given compilation unit.
   * 
   * @param definingCompilationUnit the compilation unit that defines this library
   */
  public void setDefiningCompilationUnit(CompilationUnitElement definingCompilationUnit) {
    ((CompilationUnitElementImpl) definingCompilationUnit).setEnclosingElement(this);
    this.definingCompilationUnit = definingCompilationUnit;
  }

  /**
   * Set the entry point for this library to the given function.
   * 
   * @param entryPoint the entry point for this library
   */
  public void setEntryPoint(FunctionElement entryPoint) {
    ((FunctionElementImpl) entryPoint).setEnclosingElement(this);
    this.entryPoint = entryPoint;
  }

  /**
   * Set the specifications of all of the exports defined in this library to the given array.
   * 
   * @param exports the specifications of all of the exports defined in this library
   */
  public void setExports(ExportSpecification[] exports) {
    this.exports = exports;
  }

  /**
   * Set the specifications of all of the imports defined in this library to the given array.
   * 
   * @param imports the specifications of all of the imports defined in this library
   */
  public void setImports(ImportSpecification[] imports) {
    this.imports = imports;
  }

  /**
   * Set the compilation units that are included in this library using a {@code part} directive.
   * 
   * @param parts the compilation units that are included in this library using a {@code part}
   *          directive
   */
  public void setParts(CompilationUnitElement[] parts) {
    for (CompilationUnitElement compilationUnit : parts) {
      ((CompilationUnitElementImpl) compilationUnit).setEnclosingElement(this);
    }
    this.parts = parts;
  }
}
