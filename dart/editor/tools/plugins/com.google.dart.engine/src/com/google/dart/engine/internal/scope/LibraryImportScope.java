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
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.internal.element.MultiplyDefinedElementImpl;

/**
 * Instances of the class {@code LibraryImportScope} represent the scope containing all of the names
 * available from imported libraries.
 * 
 * @coverage dart.engine.resolver
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
  private Namespace[] importedNamespaces;

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
    if (!isPrivateName(element.getDisplayName())) {
      super.define(element);
    }
  }

  @Override
  public AnalysisErrorListener getErrorListener() {
    return errorListener;
  }

  @Override
  protected Element internalLookup(Identifier identifier, String name, LibraryElement referencingLibrary) {
    Element foundElement = localLookup(name, referencingLibrary);
    if (foundElement != null) {
      return foundElement;
    }
    for (Namespace nameSpace : importedNamespaces) {
      Element element = nameSpace.get(name);
      if (element != null) {
        if (foundElement == null) {
          foundElement = element;
        } else if (foundElement != element) {
          foundElement = MultiplyDefinedElementImpl.fromElements(
              definingLibrary.getContext(),
              foundElement,
              element);
        }
      }
    }
    if (foundElement instanceof MultiplyDefinedElementImpl) {
      foundElement = removeSdkElements(identifier, name, (MultiplyDefinedElementImpl) foundElement);
    }
    if (foundElement instanceof MultiplyDefinedElementImpl) {
      String foundEltName = foundElement.getDisplayName();
      Element[] conflictingMembers = ((MultiplyDefinedElementImpl) foundElement).getConflictingElements();
      String libName1 = getLibraryName(conflictingMembers[0], "");
      String libName2 = getLibraryName(conflictingMembers[1], "");
      // TODO (jwren) Change the error message to include a list of all library names instead of
      // just the first two
      errorListener.onError(new AnalysisError(
          getSource(identifier),
          identifier.getOffset(),
          identifier.getLength(),
          StaticWarningCode.AMBIGUOUS_IMPORT,
          foundEltName,
          libName1,
          libName2));
      return foundElement;
    }
    if (foundElement != null) {
      defineNameWithoutChecking(name, foundElement);
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
    ImportElement[] imports = definingLibrary.getImports();
    int count = imports.length;
    importedNamespaces = new Namespace[count];
    for (int i = 0; i < count; i++) {
      importedNamespaces[i] = builder.createImportNamespaceForDirective(imports[i]);
    }
  }

  /**
   * Returns the name of the library that defines given element.
   * 
   * @param element the element to get library name
   * @param def the default name to use
   * @return the name of the library that defines given element
   */
  private String getLibraryName(Element element, String def) {
    if (element == null) {
      return def;
    }
    LibraryElement library = element.getLibrary();
    if (library == null) {
      return def;
    }
    return library.getDefiningCompilationUnit().getDisplayName();
  }

  /**
   * Given a collection of elements that a single name could all be mapped to, remove from the list
   * all of the names defined in the SDK. Return the element(s) that remain.
   * 
   * @param identifier the identifier node to lookup element for, used to report correct kind of a
   *          problem and associate problem with
   * @param name the name associated with the element
   * @param foundElement the element encapsulating the collection of elements
   * @return all of the elements that are not defined in the SDK
   */
  private Element removeSdkElements(Identifier identifier, String name,
      MultiplyDefinedElementImpl foundElement) {
    Element[] conflictingMembers = foundElement.getConflictingElements();
    int length = conflictingMembers.length;
    int to = 0;
    Element sdkElement = null;
    for (Element member : conflictingMembers) {
      if (member.getLibrary().isInSdk()) {
        sdkElement = member;
      } else {
        conflictingMembers[to++] = member;
      }
    }
    if (sdkElement != null && to > 0) {
      String sdkLibName = getLibraryName(sdkElement, "");
      String otherLibName = getLibraryName(conflictingMembers[0], "");
      errorListener.onError(new AnalysisError(
          getSource(identifier),
          identifier.getOffset(),
          identifier.getLength(),
          StaticWarningCode.CONFLICTING_DART_IMPORT,
          name,
          sdkLibName,
          otherLibName));
    }
    if (to == length) {
      // None of the members were removed
      return foundElement;
    } else if (to == 1) {
      // All but one member was removed
      return conflictingMembers[0];
    } else if (to == 0) {
      // All members were removed
      AnalysisEngine.getInstance().getLogger().logInformation(
          "Multiply defined SDK element: " + foundElement);
      return foundElement;
    }
    Element[] remaining = new Element[to];
    System.arraycopy(conflictingMembers, 0, remaining, 0, to);
    return new MultiplyDefinedElementImpl(definingLibrary.getContext(), remaining);
  }
}
