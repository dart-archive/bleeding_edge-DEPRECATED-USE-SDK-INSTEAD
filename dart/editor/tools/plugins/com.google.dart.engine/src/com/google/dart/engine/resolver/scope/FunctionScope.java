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

import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.VariableElement;

/**
 * Instances of the class {@code FunctionScope} implement the scope defined by a function.
 */
public class FunctionScope extends EnclosedScope {
  /**
   * Initialize a newly created scope enclosed within another scope.
   * 
   * @param enclosingScope the scope in which this scope is lexically enclosed
   * @param functionElement the element representing the type represented by this scope
   */
  public FunctionScope(Scope enclosingScope, ExecutableElement functionElement) {
    super(new EnclosedScope(enclosingScope));
    defineParameters(functionElement);
  }

  /**
   * Define the parameters for the given function in the scope that encloses this function.
   * 
   * @param functionElement the element representing the function represented by this scope
   */
  private void defineParameters(ExecutableElement functionElement) {
    Scope parameterScope = getEnclosingScope();
    for (VariableElement parameter : functionElement.getParameters()) {
      parameterScope.define(parameter);
    }
  }
}
