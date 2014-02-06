/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.engine.element.angular;

import com.google.dart.engine.type.Type;

/**
 * The interface {@code AngularScopeVariableElement} defines the Angular <code>Scope</code>
 * property. They are created for every <code>scope['property'] = value;</code> code snippet.
 * 
 * @coverage dart.engine.element
 */
public interface AngularScopePropertyElement extends AngularElement {
  /**
   * An empty array of scope property elements.
   */
  AngularScopePropertyElement[] EMPTY_ARRAY = {};

  /**
   * Returns the type of this property, not {@code null}, maybe <code>dynamic</code>.
   * 
   * @return the type of this property.
   */
  Type getType();
}
