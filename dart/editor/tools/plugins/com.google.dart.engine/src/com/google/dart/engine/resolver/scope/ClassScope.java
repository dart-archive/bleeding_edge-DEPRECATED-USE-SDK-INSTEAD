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
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.TypeElement;
import com.google.dart.engine.element.TypeVariableElement;

/**
 * Instances of the class {@code ClassScope} implement the scope defined by a class.
 */
public class ClassScope extends EnclosedScope {
  // TODO(brianwilkerson) This does not yet distinguish between static and instance scopes.

  /**
   * Initialize a newly created scope enclosed within another scope.
   * 
   * @param enclosingScope the scope in which this scope is lexically enclosed
   * @param typeElement the element representing the type represented by this scope
   */
  public ClassScope(Scope enclosingScope, TypeElement typeElement) {
    super(new EnclosedScope(enclosingScope));
    defineTypeParameters(typeElement);
  }

  @Override
  public Element lookup(String name, LibraryElement referencingLibrary) {
    Element element = super.lookup(name, referencingLibrary);
    if (element != null) {
      return element;
    }
    // TODO(brianwilkerson) Look in the superclass and interfaces.
    return null;
  }

  /**
   * Define the type parameters for the class.
   * 
   * @param typeElement the element representing the type represented by this scope
   */
  private void defineTypeParameters(TypeElement typeElement) {
    Scope parameterScope = getEnclosingScope();
    for (TypeVariableElement parameter : typeElement.getTypeVariables()) {
      parameterScope.define(parameter);
    }
  }
}
