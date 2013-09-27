/*
 * Copyright (c) 2013, the Dart project authors.
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

import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.TypeParameterElement;

/**
 * Instances of the class {@code FunctionTypeScope} implement the scope defined by a function type
 * alias.
 * 
 * @coverage dart.engine.resolver
 */
public class FunctionTypeScope extends EnclosedScope {
  private final FunctionTypeAliasElement typeElement;
  private boolean parametersDefined;

  /**
   * Initialize a newly created scope enclosed within another scope.
   * 
   * @param enclosingScope the scope in which this scope is lexically enclosed
   * @param typeElement the element representing the type alias represented by this scope
   */
  public FunctionTypeScope(Scope enclosingScope, FunctionTypeAliasElement typeElement) {
    super(new EnclosedScope(enclosingScope));
    this.typeElement = typeElement;
    defineTypeParameters();
  }

  /**
   * Define the parameters for the function type alias.
   * 
   * @param typeElement the element representing the type represented by this scope
   */
  public void defineParameters() {
    if (parametersDefined) {
      return;
    }
    parametersDefined = true;
    for (ParameterElement parameter : typeElement.getParameters()) {
      define(parameter);
    }
  }

  /**
   * Define the type parameters for the function type alias.
   * 
   * @param typeElement the element representing the type represented by this scope
   */
  private void defineTypeParameters() {
    Scope typeParameterScope = getEnclosingScope();
    for (TypeParameterElement typeParameter : typeElement.getTypeParameters()) {
      typeParameterScope.define(typeParameter);
    }
  }
}
