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
package com.google.dart.engine.type;

import com.google.dart.engine.element.Element;

/**
 * The interface <code>Type</code> defines the behavior of objects representing the declared type of
 * elements in the element model.
 */
public interface Type {
  /**
   * Return the element representing the declaration of this type, or <code>null</code> if the type
   * has not, or cannot, be associated with an element. The former case will occur if the element
   * model is not yet complete; the latter case will occur if an undefined type is referenced.
   * 
   * @return the element representing the declaration of this type
   */
  public Element getElement();

  /**
   * Return the name of this type.
   * 
   * @return the name of this type
   */
  public String getName();
}
