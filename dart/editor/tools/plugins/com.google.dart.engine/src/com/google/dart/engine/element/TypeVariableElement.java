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
 * The interface {@code TypeVariableElement} defines the behavior of elements representing a type
 * variable.
 */
public interface TypeVariableElement extends Element {
  /**
   * Return the type representing the bound associated with this variable, or {@code null} if this
   * variable does not have an explicit bound.
   * 
   * @return the type representing the bound associated with this variable
   */
  public Type getBound();
}
