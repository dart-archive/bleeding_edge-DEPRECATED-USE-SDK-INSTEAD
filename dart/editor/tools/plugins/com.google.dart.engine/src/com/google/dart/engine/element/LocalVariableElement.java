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
package com.google.dart.engine.element;

/**
 * The interface {@code LocalVariableElement} defines the behavior common to elements that represent
 * a local variable.
 * 
 * @coverage dart.engine.element
 */
public interface LocalVariableElement extends LocalElement, VariableElement {
  /**
   * Return an array containing all of the toolkit specific objects attached to this variable.
   * 
   * @return the toolkit objects attached to this variable
   */
  public ToolkitObjectElement[] getToolkitObjects();
}
