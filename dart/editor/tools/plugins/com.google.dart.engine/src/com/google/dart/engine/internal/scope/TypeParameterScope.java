/*
 * Copyright (c) 2014, the Dart project authors.
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

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.TypeParameterElement;

/**
 * Instances of the class {@code TypeParameterScope} implement the scope defined by the type
 * parameters in a class.
 * 
 * @coverage dart.engine.resolver
 */
public class TypeParameterScope extends EnclosedScope {
  /**
   * Initialize a newly created scope enclosed within another scope.
   * 
   * @param enclosingScope the scope in which this scope is lexically enclosed
   * @param typeElement the element representing the type represented by this scope
   */
  public TypeParameterScope(Scope enclosingScope, ClassElement typeElement) {
    super(enclosingScope);
    if (typeElement == null) {
      throw new IllegalArgumentException("class element cannot be null");
    }
    defineTypeParameters(typeElement);
  }

  /**
   * Define the type parameters for the class.
   * 
   * @param typeElement the element representing the type represented by this scope
   */
  private void defineTypeParameters(ClassElement typeElement) {
    for (TypeParameterElement typeParameter : typeElement.getTypeParameters()) {
      define(typeParameter);
    }
  }
}
