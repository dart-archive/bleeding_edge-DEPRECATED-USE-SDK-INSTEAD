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
package com.google.dart.engine.element;

import com.google.dart.engine.type.Type;

/**
 * The interface {@code MultiplyDefinedElement} defines the behavior of pseudo-elements that
 * represent multiple elements defined within a single scope that have the same name. This situation
 * is not allowed by the language, so objects implementing this interface always represent an error.
 * As a result, most of the normal operations on elements do not make sense and will return useless
 * results.
 * 
 * @coverage dart.engine.element
 */
public interface MultiplyDefinedElement extends Element {
  /**
   * Return an array containing all of the elements that were defined within the scope to have the
   * same name.
   * 
   * @return the elements that were defined with the same name
   */
  public Element[] getConflictingElements();

  /**
   * Return the type of this element as the dynamic type.
   * 
   * @return the type of this element as the dynamic type
   */
  public Type getType();
}
