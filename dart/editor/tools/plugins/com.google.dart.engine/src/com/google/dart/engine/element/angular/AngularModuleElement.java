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

import com.google.dart.engine.element.ClassElement;

/**
 * The interface {@code AngularModuleElement} defines a single DI <code>Module</code>.
 * 
 * @coverage dart.engine.element
 */
public interface AngularModuleElement extends AngularElement {
  /**
   * An empty array of module elements.
   */
  AngularModuleElement[] EMPTY_ARRAY = {};

  /**
   * Returns the child modules installed into this module using <code>install</code>.
   * 
   * @return the installed child modules
   */
  AngularModuleElement[] getChildModules();

  /**
   * Returns the keys injected into this module using <code>type()</code> and <code>value()</code>
   * invocations.
   * 
   * @return the injected types
   */
  ClassElement[] getKeyTypes();
}
