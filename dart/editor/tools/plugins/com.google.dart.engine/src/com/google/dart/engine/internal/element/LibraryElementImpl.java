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

import com.google.dart.engine.ast.LibraryIdentifier;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ExportElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.ImportElement;
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
   * The analysis context in which this library is defined.
   */
  private AnalysisContext context;

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
  private ImportElement[] imports = ImportElement.EMPTY_ARRAY;

  /**
   * An array containing specifications of all of the exports defined in this library.
   */
  private ExportElement[] exports = ExportElement.EMPTY_ARRAY;

  /**
   * An array containing all of the compilation units that are included in this library using a
   * {@code part} directive.
   */
  private CompilationUnitElement[] parts = CompilationUnitElementImpl.EMPTY_ARRAY;

  /**
   * Initialize a newly created library element to have the given name.
   * 
   * @param context the analysis context in which the library is defined
   * @param name the name of this element
   */
  public LibraryElementImpl(AnalysisContext context, LibraryIdentifier name) {
    super(name);
    this.context = context;
  }

  @Override
  public boolean equals(Object object) {
    return this.getClass() == object.getClass()
        && definingCompilationUnit.equals(((LibraryElementImpl) object).getDefiningCompilationUnit());
  }

  @Override
  public ElementImpl getChild(String identifier) {
    if (((CompilationUnitElementImpl) definingCompilationUnit).getIdentifier().equals(identifier)) {
      return (CompilationUnitElementImpl) definingCompilationUnit;
    }
    for (CompilationUnitElement part : parts) {
      if (((CompilationUnitElementImpl) part).getIdentifier().equals(identifier)) {
        return (CompilationUnitElementImpl) part;
      }
    }
    return null;
  }

  @Override
  public AnalysisContext getContext() {
    return context;
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
  public ExportElement[] getExports() {
    return exports;
  }

  @Override
  public String getIdentifier() {
    return definingCompilationUnit.getSource().getFullName();
  }

  @Override
  public LibraryElement[] getImportedLibraries() {
    HashSet<LibraryElement> libraries = new HashSet<LibraryElement>(imports.length);
    for (ImportElement element : imports) {
      LibraryElement prefix = element.getImportedLibrary();
      libraries.add(prefix);
    }
    return libraries.toArray(new LibraryElement[libraries.size()]);
  }

  @Override
  public ImportElement[] getImports() {
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
    for (ImportElement element : imports) {
      PrefixElement prefix = element.getPrefix();
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
  public void setExports(ExportElement[] exports) {
    this.exports = exports;
  }

  /**
   * Set the specifications of all of the imports defined in this library to the given array.
   * 
   * @param imports the specifications of all of the imports defined in this library
   */
  public void setImports(ImportElement[] imports) {
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
