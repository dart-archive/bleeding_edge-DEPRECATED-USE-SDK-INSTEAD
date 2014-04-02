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

import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.CompileTimeErrorCode;

import java.util.HashMap;

/**
 * Instances of the class {@code EnclosedScope} implement a scope that is lexically enclosed in
 * another scope.
 * 
 * @coverage dart.engine.resolver
 */
public class EnclosedScope extends Scope {
  /**
   * The scope in which this scope is lexically enclosed.
   */
  private Scope enclosingScope;

  /**
   * A table mapping names that will be defined in this scope, but right now are not initialized.
   * According to the scoping rules these names are hidden, even if they were defined in an outer
   * scope.
   */
  private HashMap<String, Element> hiddenElements = new HashMap<String, Element>();

  /**
   * A flag indicating whether there are any names defined in this scope.
   */
  private boolean hasHiddenName = false;

  /**
   * Initialize a newly created scope enclosed within another scope.
   * 
   * @param enclosingScope the scope in which this scope is lexically enclosed
   */
  public EnclosedScope(Scope enclosingScope) {
    this.enclosingScope = enclosingScope;
  }

  @Override
  public Scope getEnclosingScope() {
    return enclosingScope;
  }

  @Override
  public AnalysisErrorListener getErrorListener() {
    return enclosingScope.getErrorListener();
  }

  /**
   * Record that given element is declared in this scope, but hasn't been initialized yet, so it is
   * error to use. If there is already an element with the given name defined in an outer scope,
   * then it will become unavailable.
   * 
   * @param element the element declared, but not initialized in this scope
   */
  public void hide(Element element) {
    if (element != null) {
      String name = element.getName();
      if (name != null && !name.isEmpty()) {
        hiddenElements.put(name, element);
        hasHiddenName = true;
      }
    }
  }

  @Override
  protected Element internalLookup(Identifier identifier, String name, LibraryElement referencingLibrary) {
    Element element = localLookup(name, referencingLibrary);
    if (element != null) {
      return element;
    }
    // May be there is a hidden Element.
    if (hasHiddenName) {
      Element hiddenElement = hiddenElements.get(name);
      if (hiddenElement != null) {
        getErrorListener().onError(
            new AnalysisError(
                getSource(identifier),
                identifier.getOffset(),
                identifier.getLength(),
                CompileTimeErrorCode.REFERENCED_BEFORE_DECLARATION));
        return hiddenElement;
      }
    }
    // Check enclosing scope.
    return enclosingScope.internalLookup(identifier, name, referencingLibrary);
  }
}
