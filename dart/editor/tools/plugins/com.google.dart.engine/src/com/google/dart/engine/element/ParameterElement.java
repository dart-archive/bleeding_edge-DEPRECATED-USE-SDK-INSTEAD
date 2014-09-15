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

import com.google.dart.engine.utilities.dart.ParameterKind;

/**
 * The interface {@code ParameterElement} defines the behavior of elements representing a parameter
 * defined within an executable element.
 * 
 * @coverage dart.engine.element
 */
public interface ParameterElement extends LocalElement, VariableElement {
  /**
   * Return the Dart code of the default value, or {@code null} if no default value.
   * 
   * @return the Dart code of the default value
   */
  public String getDefaultValueCode();

  /**
   * Return the kind of this parameter.
   * 
   * @return the kind of this parameter
   */
  public ParameterKind getParameterKind();

  /**
   * Return an array containing all of the parameters defined by this parameter. A parameter will
   * only define other parameters if it is a function typed parameter.
   * 
   * @return the parameters defined by this parameter element
   */
  public ParameterElement[] getParameters();

  /**
   * Return {@code true} if this parameter is an initializing formal parameter.
   * 
   * @return {@code true} if this parameter is an initializing formal parameter
   */
  public boolean isInitializingFormal();
}
