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
package com.google.dart.engine.internal.element;

import com.google.dart.engine.ast.IndexExpression;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.element.ExecutableElement;

/**
 * For AST nodes that could be in both the getter and setter contexts ({@link IndexExpression}s and
 * {@link SimpleIdentifier}s), the additional resolved elements are stored in the AST node, in an
 * {@link AuxiliaryElements}. Since resolved elements are either statically resolved or resolved
 * using propagated type information, this class is a wrapper for a pair of
 * {@link ExecutableElement}s, not just a single {@link ExecutableElement}.
 */
public class AuxiliaryElements {

  /**
   * The element based on propagated type information, or {@code null} if the AST structure has not
   * been resolved or if this identifier could not be resolved.
   */
  private ExecutableElement propagatedElement;

  /**
   * The element associated with this identifier based on static type information, or {@code null}
   * if the AST structure has not been resolved or if this identifier could not be resolved.
   */
  private ExecutableElement staticElement;

  /**
   * Create the {@link AuxiliaryElements} with a static and propagated {@link ExecutableElement}.
   * 
   * @param staticElement the static element
   * @param propagatedElement the propagated element
   */
  public AuxiliaryElements(ExecutableElement staticElement, ExecutableElement propagatedElement) {
    this.staticElement = staticElement;
    this.propagatedElement = propagatedElement;
  }

  /**
   * Get the propagated element.
   */
  public ExecutableElement getPropagatedElement() {
    return propagatedElement;
  }

  /**
   * Get the static element.
   */
  public ExecutableElement getStaticElement() {
    return staticElement;
  }

}
