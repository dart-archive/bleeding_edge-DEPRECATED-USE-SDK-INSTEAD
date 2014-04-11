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
package com.google.dart.engine.internal.scope;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExportElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.HideElementCombinator;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.NamespaceCombinator;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.ShowElementCombinator;
import com.google.dart.engine.internal.context.InternalAnalysisContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Instances of the class {@code NamespaceBuilder} are used to build a {@code Namespace}. Namespace
 * builders are thread-safe and re-usable.
 * 
 * @coverage dart.engine.resolver
 */
public class NamespaceBuilder {
  /**
   * Initialize a newly created namespace builder.
   */
  public NamespaceBuilder() {
    super();
  }

  /**
   * Create a namespace representing the export namespace of the given {@link ExportElement}.
   * 
   * @param element the export element whose export namespace is to be created
   * @return the export namespace that was created
   */
  public Namespace createExportNamespaceForDirective(ExportElement element) {
    LibraryElement exportedLibrary = element.getExportedLibrary();
    if (exportedLibrary == null) {
      //
      // The exported library will be null if the URI does not reference a valid library.
      //
      return Namespace.EMPTY;
    }
    HashMap<String, Element> definedNames = createExportMapping(
        exportedLibrary,
        new HashSet<LibraryElement>());
    definedNames = applyCombinators(definedNames, element.getCombinators());
    return new Namespace(definedNames);
  }

  /**
   * Create a namespace representing the export namespace of the given library.
   * 
   * @param library the library whose export namespace is to be created
   * @return the export namespace that was created
   */
  public Namespace createExportNamespaceForLibrary(LibraryElement library) {
    return new Namespace(createExportMapping(library, new HashSet<LibraryElement>()));
  }

  /**
   * Create a namespace representing the import namespace of the given library.
   * 
   * @param library the library whose import namespace is to be created
   * @return the import namespace that was created
   */
  public Namespace createImportNamespaceForDirective(ImportElement element) {
    LibraryElement importedLibrary = element.getImportedLibrary();
    if (importedLibrary == null) {
      //
      // The imported library will be null if the URI does not reference a valid library.
      //
      return Namespace.EMPTY;
    }
    HashMap<String, Element> definedNames = createExportMapping(
        importedLibrary,
        new HashSet<LibraryElement>());
    definedNames = applyCombinators(definedNames, element.getCombinators());
    definedNames = applyPrefix(definedNames, element.getPrefix());
    return new Namespace(definedNames);
  }

  /**
   * Create a namespace representing the public namespace of the given library.
   * 
   * @param library the library whose public namespace is to be created
   * @return the public namespace that was created
   */
  public Namespace createPublicNamespaceForLibrary(LibraryElement library) {
    HashMap<String, Element> definedNames = new HashMap<String, Element>();
    addPublicNames(definedNames, library.getDefiningCompilationUnit());
    for (CompilationUnitElement compilationUnit : library.getParts()) {
      addPublicNames(definedNames, compilationUnit);
    }
    return new Namespace(definedNames);
  }

  /**
   * Add all of the names in the given namespace to the given mapping table.
   * 
   * @param definedNames the mapping table to which the names in the given namespace are to be added
   * @param namespace the namespace containing the names to be added to this namespace
   */
  private void addAllFromMap(Map<String, Element> definedNames, Map<String, Element> newNames) {
    for (Map.Entry<String, Element> entry : newNames.entrySet()) {
      definedNames.put(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Add all of the names in the given namespace to the given mapping table.
   * 
   * @param definedNames the mapping table to which the names in the given namespace are to be added
   * @param namespace the namespace containing the names to be added to this namespace
   */
  private void addAllFromNamespace(Map<String, Element> definedNames, Namespace namespace) {
    if (namespace != null) {
      addAllFromMap(definedNames, namespace.getDefinedNames());
    }
  }

  /**
   * Add the given element to the given mapping table if it has a publicly visible name.
   * 
   * @param definedNames the mapping table to which the public name is to be added
   * @param element the element to be added
   */
  private void addIfPublic(Map<String, Element> definedNames, Element element) {
    String name = element.getName();
    if (name != null && !Scope.isPrivateName(name)) {
      definedNames.put(name, element);
    }
  }

  /**
   * Add to the given mapping table all of the public top-level names that are defined in the given
   * compilation unit.
   * 
   * @param definedNames the mapping table to which the public names are to be added
   * @param compilationUnit the compilation unit defining the top-level names to be added to this
   *          namespace
   */
  private void addPublicNames(Map<String, Element> definedNames,
      CompilationUnitElement compilationUnit) {
    for (PropertyAccessorElement element : compilationUnit.getAccessors()) {
      addIfPublic(definedNames, element);
    }
    for (FunctionElement element : compilationUnit.getFunctions()) {
      addIfPublic(definedNames, element);
    }
    for (FunctionTypeAliasElement element : compilationUnit.getFunctionTypeAliases()) {
      addIfPublic(definedNames, element);
    }
    for (ClassElement element : compilationUnit.getTypes()) {
      addIfPublic(definedNames, element);
    }
  }

  /**
   * Apply the given combinators to all of the names in the given mapping table.
   * 
   * @param definedNames the mapping table to which the namespace operations are to be applied
   * @param combinators the combinators to be applied
   */
  private HashMap<String, Element> applyCombinators(HashMap<String, Element> definedNames,
      NamespaceCombinator[] combinators) {
    for (NamespaceCombinator combinator : combinators) {
      if (combinator instanceof HideElementCombinator) {
        hide(definedNames, ((HideElementCombinator) combinator).getHiddenNames());
      } else if (combinator instanceof ShowElementCombinator) {
        definedNames = show(definedNames, ((ShowElementCombinator) combinator).getShownNames());
      } else {
        // Internal error.
        AnalysisEngine.getInstance().getLogger().logError(
            "Unknown type of combinator: " + combinator.getClass().getName());
      }
    }
    return definedNames;
  }

  /**
   * Apply the given prefix to all of the names in the table of defined names.
   * 
   * @param definedNames the names that were defined before this operation
   * @param prefixElement the element defining the prefix to be added to the names
   */
  private HashMap<String, Element> applyPrefix(HashMap<String, Element> definedNames,
      PrefixElement prefixElement) {
    if (prefixElement != null) {
      String prefix = prefixElement.getName();
      HashMap<String, Element> newNames = new HashMap<String, Element>(definedNames.size());
      for (Map.Entry<String, Element> entry : definedNames.entrySet()) {
        newNames.put(prefix + "." + entry.getKey(), entry.getValue());
      }
      return newNames;
    } else {
      return definedNames;
    }
  }

  /**
   * Create a mapping table representing the export namespace of the given library.
   * 
   * @param library the library whose public namespace is to be created
   * @param visitedElements a set of libraries that do not need to be visited when processing the
   *          export directives of the given library because all of the names defined by them will
   *          be added by another library
   * @return the mapping table that was created
   */
  private HashMap<String, Element> createExportMapping(LibraryElement library,
      HashSet<LibraryElement> visitedElements) {
    visitedElements.add(library);
    try {
      HashMap<String, Element> definedNames = new HashMap<String, Element>();
      for (ExportElement element : library.getExports()) {
        LibraryElement exportedLibrary = element.getExportedLibrary();
        if (exportedLibrary != null && !visitedElements.contains(exportedLibrary)) {
          //
          // The exported library will be null if the URI does not reference a valid library.
          //
          HashMap<String, Element> exportedNames = createExportMapping(
              exportedLibrary,
              visitedElements);
          exportedNames = applyCombinators(exportedNames, element.getCombinators());
          addAllFromMap(definedNames, exportedNames);
        }
      }
      addAllFromNamespace(
          definedNames,
          ((InternalAnalysisContext) library.getContext()).getPublicNamespace(library));
      return definedNames;
    } finally {
      visitedElements.remove(library);
    }
  }

  /**
   * Hide all of the given names by removing them from the given collection of defined names.
   * 
   * @param definedNames the names that were defined before this operation
   * @param hiddenNames the names to be hidden
   */
  private void hide(HashMap<String, Element> definedNames, String[] hiddenNames) {
    for (String name : hiddenNames) {
      definedNames.remove(name);
      definedNames.remove(name + "=");
    }
  }

  /**
   * Show only the given names by removing all other names from the given collection of defined
   * names.
   * 
   * @param definedNames the names that were defined before this operation
   * @param shownNames the names to be shown
   */
  private HashMap<String, Element> show(HashMap<String, Element> definedNames, String[] shownNames) {
    HashMap<String, Element> newNames = new HashMap<String, Element>(definedNames.size());
    for (String name : shownNames) {
      Element element = definedNames.get(name);
      if (element != null) {
        newNames.put(name, element);
      }
      String setterName = name + "=";
      element = definedNames.get(setterName);
      if (element != null) {
        newNames.put(setterName, element);
      }
    }
    return newNames;
  }
}
