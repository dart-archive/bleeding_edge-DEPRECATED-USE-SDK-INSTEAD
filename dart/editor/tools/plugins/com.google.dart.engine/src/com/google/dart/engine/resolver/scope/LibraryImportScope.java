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

import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.internal.element.MultiplyDefinedElementImpl;

import java.util.ArrayList;

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
          foundElement = new MultiplyDefinedElementImpl(
              definingLibrary.getContext(),
              foundElement,
              element);
        }
      }
    }
    if (foundElement != null) {
      defineWithoutChecking(foundElement);
    }
    return foundElement;
  }

  /**
   * Create all of the namespaces associated with the libraries imported into this library. The
   * names are not added to this scope, but are stored for later reference.
   * 
   * @param definingLibrary the element representing the library that imports the libraries for
   *          which namespaces will be created
   */
  private final void createImportedNamespaces(LibraryElement definingLibrary) {
    NamespaceBuilder builder = new NamespaceBuilder();
    for (ImportElement element : definingLibrary.getImports()) {
      importedNamespaces.add(builder.createImportNamespace(element));
    }
  }
}
