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

import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.ParameterElement;

/**
 * Instances of the class {@code FunctionScope} implement the scope defined by a function.
 * 
 * @coverage dart.engine.resolver
 */
public class FunctionScope extends EnclosedScope {
  private final ExecutableElement functionElement;
  private boolean parametersDefined;

  /**
   * Initialize a newly created scope enclosed within another scope.
   * 
   * @param enclosingScope the scope in which this scope is lexically enclosed
   * @param functionElement the element representing the type represented by this scope
   */
  public FunctionScope(Scope enclosingScope, ExecutableElement functionElement) {
    super(new EnclosedScope(enclosingScope));
    if (functionElement == null) {
      throw new IllegalArgumentException("function element cannot be null");
    }
    this.functionElement = functionElement;
  }

  /**
   * Define the parameters for the given function in the scope that encloses this function.
   */
  public void defineParameters() {
    if (parametersDefined) {
      return;
    }
    parametersDefined = true;
    Scope parameterScope = getEnclosingScope();
    if (functionElement.getEnclosingElement() instanceof ExecutableElement) {
      String name = functionElement.getName();
      if (name != null && !name.isEmpty()) {
        parameterScope.define(functionElement);
      }
    }
    for (ParameterElement parameter : functionElement.getParameters()) {
      if (!parameter.isInitializingFormal()) {
        parameterScope.define(parameter);
      }
    }
  }
}
