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
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.TypeVariableElement;

/**
 * Instances of the class {@code ClassScope} implement the scope defined by a class.
 */
public class ClassScope extends EnclosedScope {
  // TODO(brianwilkerson) This does not yet distinguish between static and instance scopes. It isn't
  // clear whether we need to do so or whether it might not be easier and better to simply check
  // when resolving to an instance element that the reference is in an instance scope.

  /**
   * Initialize a newly created scope enclosed within another scope.
   * 
   * @param enclosingScope the scope in which this scope is lexically enclosed
   * @param typeElement the element representing the type represented by this scope
   */
  public ClassScope(Scope enclosingScope, ClassElement typeElement) {
    super(new EnclosedScope(enclosingScope));
    defineTypeParameters(typeElement);
    defineMembers(typeElement);
  }

  @Override
  protected Element lookup(String name, LibraryElement referencingLibrary) {
    //
    // First look in the lexical scope.
    //
    Element element = super.lookup(name, referencingLibrary);
    if (element != null) {
      return element;
    }
    //
    // Then look in the inheritance scope.
    //
    // TODO(brianwilkerson) Look in the superclass and interfaces.
    return null;
  }

  /**
   * Define the instance members defined by the class.
   * 
   * @param typeElement the element representing the type represented by this scope
   */
  private void defineMembers(ClassElement typeElement) {
    for (PropertyAccessorElement accessor : typeElement.getAccessors()) {
      define(accessor);
    }
    for (FieldElement field : typeElement.getFields()) {
      define(field);
    }
    for (MethodElement method : typeElement.getMethods()) {
      define(method);
    }
  }

  /**
   * Define the type parameters for the class.
   * 
   * @param typeElement the element representing the type represented by this scope
   */
  private void defineTypeParameters(ClassElement typeElement) {
    Scope parameterScope = getEnclosingScope();
    for (TypeVariableElement parameter : typeElement.getTypeVariables()) {
      parameterScope.define(parameter);
    }
  }
}
