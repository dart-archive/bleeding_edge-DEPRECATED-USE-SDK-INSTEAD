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

import com.google.dart.engine.element.ToolkitObjectElement;

/**
 * Instances of the class {@code ToolkitObjectElementImpl} implement a {@code ToolkitObjectElement}.
 * 
 * @coverage dart.engine.element
 */
public abstract class ToolkitObjectElementImpl extends ElementImpl implements ToolkitObjectElement {
  /**
   * Initialize a newly created toolkit object element to have the given name.
   * 
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  public ToolkitObjectElementImpl(String name, int nameOffset) {
    super(name, nameOffset);
  }
}
