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

import java.util.HashSet;
import java.util.Set;

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
   * A set of names that will be defined in this scope, but right now are not defined. However
   * according to the scoping rules these names are hidden, even if they were defined in an outer
   * scope.
   */
  private Set<String> hiddenNames = new HashSet<String>();

  /**
   * Initialize a newly created scope enclosed within another scope.
   * 
   * @param enclosingScope the scope in which this scope is lexically enclosed
   */
  public EnclosedScope(Scope enclosingScope) {
    this.enclosingScope = enclosingScope;
  }

  @Override
  public AnalysisErrorListener getErrorListener() {
    return enclosingScope.getErrorListener();
  }

  /**
   * Hides the name of the given element in this scope. If there is already an element with the
   * given name defined in an outer scope, then it will become unavailable.
   * 
   * @param element the element to be hidden in this scope
   */
  public void hide(Element element) {
    if (element != null) {
      String name = element.getName();
      if (name != null && !name.isEmpty()) {
        hiddenNames.add(name);
      }
    }
  }

  /**
   * Return the scope in which this scope is lexically enclosed.
   * 
   * @return the scope in which this scope is lexically enclosed
   */
  protected Scope getEnclosingScope() {
    return enclosingScope;
  }

  @Override
  protected Element lookup(Identifier identifier, String name, LibraryElement referencingLibrary) {
    Element element = localLookup(name, referencingLibrary);
    if (element != null) {
      return element;
    }
    if (hiddenNames.contains(name)) {
      getErrorListener().onError(
          new AnalysisError(
              getSource(identifier),
              identifier.getOffset(),
              identifier.getLength(),
              CompileTimeErrorCode.REFERENCED_BEFORE_DECLARATION));
      //return null;
    }
    return enclosingScope.lookup(identifier, name, referencingLibrary);
  }
}
