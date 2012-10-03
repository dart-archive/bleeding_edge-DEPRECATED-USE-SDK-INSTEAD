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
package com.google.dart.engine.resolver.scope;

import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TypeAliasElement;
import com.google.dart.engine.element.TypeElement;
import com.google.dart.engine.error.AnalysisErrorListener;

import java.util.HashSet;
import java.util.Set;

/**
 * Instances of the class {@code LibraryImportScope} represent the scope containing all of the names
 * available from imported libraries.
 */
public class LibraryImportScope extends Scope {
  /**
   * The element representing the library in which this scope is enclosed.
   */
  private LibraryElement definingLibrary;

  /**
   * The listener that is to be informed when an error is encountered.
   */
  private AnalysisErrorListener errorListener;

  /**
   * Initialize a newly created scope representing the names imported into the given library.
   * 
   * @param definingLibrary the element representing the library that imports the names defined in
   *          this scope
   * @param errorListener the listener that is to be informed when an error is encountered
   */
  public LibraryImportScope(LibraryElement definingLibrary, AnalysisErrorListener errorListener) {
    this.definingLibrary = definingLibrary;
    this.errorListener = errorListener;
    defineImportedNames(definingLibrary);
  }

  @Override
  public void define(Element element) {
    if (!isPrivateName(element.getName())) {
      super.define(element);
    }
  }

  @Override
  public LibraryElement getDefiningLibrary() {
    return definingLibrary;
  }

  @Override
  public AnalysisErrorListener getErrorListener() {
    return errorListener;
  }

  @Override
  public Element lookup(String name, LibraryElement referencingLibrary) {
    return localLookup(name, referencingLibrary);
  }

  /**
   * Add to this scope all of the public top-level names that are defined in the given compilation
   * unit.
   * 
   * @param compilationUnit the compilation unit defining the top-level names to be added to this
   *          scope
   */
  private void defineImportedNames(CompilationUnitElement compilationUnit) {
    for (PropertyAccessorElement element : compilationUnit.getAccessors()) {
      define(element);
    }
    for (FieldElement element : compilationUnit.getFields()) {
      define(element);
    }
    for (FunctionElement element : compilationUnit.getFunctions()) {
      define(element);
    }
    for (TypeAliasElement element : compilationUnit.getTypeAliases()) {
      define(element);
    }
    for (TypeElement element : compilationUnit.getTypes()) {
      define(element);
    }
  }

  /**
   * Add to this scope all of the names that are imported into the given library.
   * 
   * @param definingLibrary the element representing the library that imports the names defined in
   *          this scope
   */
  private final void defineImportedNames(LibraryElement definingLibrary) {
    // TODO(brianwilkerson) This does not yet handle combinators such as "show:" and "hide:".
    Set<LibraryElement> prefixedLibraries = new HashSet<LibraryElement>();
    for (PrefixElement prefix : definingLibrary.getPrefixes()) {
      for (LibraryElement importedLibrary : prefix.getImportedLibraries()) {
        prefixedLibraries.add(importedLibrary);
      }
    }
    for (LibraryElement importedLibrary : definingLibrary.getImportedLibraries()) {
      if (!prefixedLibraries.contains(importedLibrary)) {
        defineImportedNames(importedLibrary.getDefiningCompilationUnit());
        for (CompilationUnitElement compilationUnit : importedLibrary.getSourcedCompilationUnits()) {
          defineImportedNames(compilationUnit);
        }
      }
    }
  }
}
