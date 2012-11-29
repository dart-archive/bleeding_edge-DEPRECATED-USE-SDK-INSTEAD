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

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExportSpecification;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.HideCombinator;
import com.google.dart.engine.element.ImportCombinator;
import com.google.dart.engine.element.ImportSpecification;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.ShowCombinator;
import com.google.dart.engine.element.TypeAliasElement;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.internal.element.MultiplyDefinedElementImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Instances of the class {@code LibraryImportScope} represent the scope containing all of the names
 * available from imported libraries.
 */
public class LibraryImportScope extends Scope {
  /**
   * Instances of the class {@code Namespace} represent the names that are exported from some
   * library. This class is not thread-safe.
   */
  private static class Namespace {
    /**
     * A table mapping names that are defined in this namespace to the element representing the
     * thing declared with that name.
     */
    private HashMap<String, Element> definedNames = new HashMap<String, Element>();

    /**
     * Add all of the names in the given namespace to this namespace.
     * 
     * @param namespace the namespace containing the names to be added to this namespace
     */
    public void add(Namespace namespace) {
      for (Map.Entry<String, Element> entry : namespace.definedNames.entrySet()) {
        definedNames.put(entry.getKey(), entry.getValue());
      }
    }

    public void apply(ImportCombinator combinator) {
      if (combinator instanceof HideCombinator) {
        hide(((HideCombinator) combinator).getHiddenNames());
      } else if (combinator instanceof ShowCombinator) {
        show(((ShowCombinator) combinator).getShownNames());
      } else {
        // Internal error.
      }
    }

    /**
     * Apply the given prefix to all of the names in this namespace.
     * 
     * @param prefixElement the element defining the prefix to be added to the names
     */
    public void apply(PrefixElement prefixElement) {
      if (prefixElement != null) {
        String prefix = prefixElement.getName();
        HashMap<String, Element> newNames = new HashMap<String, Element>(definedNames.size());
        for (Map.Entry<String, Element> entry : definedNames.entrySet()) {
          newNames.put(prefix + "." + entry.getKey(), entry.getValue());
        }
        definedNames = newNames;
      }
    }

    /**
     * Return the element in this namespace that is available to the containing scope using the
     * given name.
     * 
     * @param name the name used to reference the
     * @return
     */
    public Element get(String name) {
      return definedNames.get(name);
    }

    /**
     * Add to this namespace all of the public top-level names that are defined in the given
     * compilation unit.
     * 
     * @param compilationUnit the compilation unit defining the top-level names to be added to this
     *          namespace
     */
    private void addPublicNames(CompilationUnitElement compilationUnit) {
      for (PropertyAccessorElement element : compilationUnit.getAccessors()) {
        definedNames.put(element.getName(), element);
      }
      for (FieldElement element : compilationUnit.getFields()) {
        definedNames.put(element.getName(), element);
      }
      for (FunctionElement element : compilationUnit.getFunctions()) {
        definedNames.put(element.getName(), element);
      }
      for (TypeAliasElement element : compilationUnit.getTypeAliases()) {
        definedNames.put(element.getName(), element);
      }
      for (ClassElement element : compilationUnit.getTypes()) {
        definedNames.put(element.getName(), element);
      }
    }

    /**
     * Hide all of the given names by removing them from this namespace.
     * 
     * @param hiddenNames the names to be hidden
     */
    private void hide(String[] hiddenNames) {
      for (String name : hiddenNames) {
        definedNames.remove(name);
      }
    }

    /**
     * Compute a new set of names for this namespace that consists of the intersection of the names
     * that are defined in this namespace and the names in the given array.
     * 
     * @param shownNames the names that are to be available in this namespace
     */
    private void show(String[] shownNames) {
      HashMap<String, Element> newNames = new HashMap<String, Element>(definedNames.size());
      for (String name : shownNames) {
        Element element = definedNames.get(name);
        if (element != null) {
          newNames.put(name, element);
        }
      }
      definedNames = newNames;
    }
  }

  /**
   * The element representing the library in which this scope is enclosed.
   */
  private LibraryElement definingLibrary;

  /**
   * The listener that is to be informed when an error is encountered.
   */
  private AnalysisErrorListener errorListener;

  /**
   * A list of the namespaces representing the names that are available in this scope from imported
   * libraries.
   */
  private ArrayList<Namespace> importedNamespaces = new ArrayList<Namespace>();

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
    createImportedNamespaces(definingLibrary);
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
  protected Element lookup(String name, LibraryElement referencingLibrary) {
    if (isPrivateName(name)) {
      return null;
    }
    Element foundElement = localLookup(name, referencingLibrary);
    if (foundElement != null) {
      return foundElement;
    }
    for (Namespace nameSpace : importedNamespaces) {
      Element element = nameSpace.get(name);
      if (element != null) {
        if (foundElement == null) {
          foundElement = element;
        } else {
          foundElement = new MultiplyDefinedElementImpl(foundElement, element);
        }
      }
    }
    if (foundElement != null) {
      defineWithoutChecking(foundElement);
    }
    return foundElement;
  }

  /**
   * Build a namespace representing all of the names available to this scope from the library
   * exported from the given specification.
   * 
   * @param specification the specification of the library being imported
   * @return the namespace that was built
   */
  private Namespace buildExportedNamespace(ExportSpecification specification) {
    return buildExportedNamespace(
        specification.getExportedLibrary(),
        specification.getCombinators());
  }

  /**
   * Build a namespace representing all of the names available to this scope from the library
   * imported from the given specification.
   * 
   * @param specification the specification of the library being imported
   * @return the namespace that was built
   */
  private Namespace buildExportedNamespace(ImportSpecification specification) {
    Namespace namespace = buildExportedNamespace(
        specification.getImportedLibrary(),
        specification.getCombinators());
    namespace.apply(specification.getPrefix());
    return namespace;
  }

  /**
   * Build a namespace representing all of the names exported from the given library after applying
   * the given combinators.
   * 
   * @param library the library defining the exported names
   * @param combinators the combinators controlling which names are visible
   * @return the namespace that was built
   */
  private Namespace buildExportedNamespace(LibraryElement library, ImportCombinator[] combinators) {
    Namespace namespace = new Namespace();
    namespace.addPublicNames(library.getDefiningCompilationUnit());
    for (CompilationUnitElement compilationUnit : library.getParts()) {
      namespace.addPublicNames(compilationUnit);
    }
    for (ExportSpecification innerSpecification : library.getExports()) {
      namespace.add(buildExportedNamespace(innerSpecification));
    }
    for (ImportCombinator combinator : combinators) {
      namespace.apply(combinator);
    }
    return namespace;
  }

  /**
   * Create all of the namespaces associated with the libraries imported into this library. The
   * names are not added to this scope, but are stored for later reference.
   * 
   * @param definingLibrary the element representing the library that imports the libraries for
   *          which namespaces will be created
   */
  private final void createImportedNamespaces(LibraryElement definingLibrary) {
    for (ImportSpecification specification : definingLibrary.getImports()) {
      importedNamespaces.add(buildExportedNamespace(specification));
    }
  }
}
