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
 * The interface {@code ToolkitObjectElement} defines the behavior of elements that represent a
 * toolkit specific object, such as Angular controller or component. These elements are not based on
 * the Dart syntax, but on some semantic agreement, such as a special annotation.
 * 
 * @coverage dart.engine.element
 */
public interface ToolkitObjectElement extends Element {
  /**
   * An empty array of toolkit object elements.
   */
  ToolkitObjectElement[] EMPTY_ARRAY = new ToolkitObjectElement[0];
}
