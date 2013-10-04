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
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.internal.element.MultiplyDefinedElementImpl;
import com.google.dart.engine.source.Source;

import java.util.ArrayList;

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
    if (!isPrivateName(element.getDisplayName())) {
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
  protected Element lookup(Identifier identifier, String name, LibraryElement referencingLibrary) {
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
          foundElement = new MultiplyDefinedElementImpl(
              definingLibrary.getContext(),
              foundElement,
              element);
        }
      }
    }
    if (foundElement instanceof MultiplyDefinedElementImpl) {
      foundElement = removeSdkElements((MultiplyDefinedElementImpl) foundElement);
    }
    if (foundElement instanceof MultiplyDefinedElementImpl) {
      String foundEltName = foundElement.getDisplayName();
      String libName1 = "", libName2 = "";
      Element[] conflictingMembers = ((MultiplyDefinedElementImpl) foundElement).getConflictingElements();
      LibraryElement enclosingLibrary = conflictingMembers[0].getAncestor(LibraryElement.class);
      if (enclosingLibrary != null) {
        libName1 = enclosingLibrary.getDefiningCompilationUnit().getDisplayName();
      }
      enclosingLibrary = conflictingMembers[1].getAncestor(LibraryElement.class);
      if (enclosingLibrary != null) {
        libName2 = enclosingLibrary.getDefiningCompilationUnit().getDisplayName();
      }
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
      defineWithoutChecking(name, foundElement);
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

  /**
   * Return the source that contains the given identifier, or the source associated with this scope
   * if the source containing the identifier could not be determined.
   * 
   * @param identifier the identifier whose source is to be returned
   * @return the source that contains the given identifier
   */
  private Source getSource(Identifier identifier) {
    CompilationUnit unit = identifier.getAncestor(CompilationUnit.class);
    if (unit != null) {
      CompilationUnitElement element = unit.getElement();
      if (element != null) {
        Source source = element.getSource();
        if (source != null) {
          return source;
        }
      }
    }
    return getSource();
  }

  /**
   * Given a collection of elements that a single name could all be mapped to, remove from the list
   * all of the names defined in the SDK. Return the element(s) that remain.
   * 
   * @param foundElement the element encapsulating the collection of elements
   * @return all of the elements that are not defined in the SDK
   */
  private Element removeSdkElements(MultiplyDefinedElementImpl foundElement) {
    Element[] conflictingMembers = foundElement.getConflictingElements();
    int length = conflictingMembers.length;
    int to = 0;
    for (Element member : conflictingMembers) {
      if (!member.getLibrary().isInSdk()) {
        conflictingMembers[to++] = member;
      }
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
