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

import com.google.dart.engine.type.FunctionType;

/**
 * The interface {@code ExecutableElement} defines the behavior of elements representing a
 * executable object, including functions, methods, constructors, getters, and setters.
 */
public interface ExecutableElement extends Element {
  /**
   * Return an array containing all of the functions defined within this executable element.
   * 
   * @return the functions defined within this executable element
   */
  public ExecutableElement[] getFunctions();

  /**
   * Return an array containing all of the labels defined within this executable element.
   * 
   * @return the labels defined within this executable element
   */
  public LabelElement[] getLabels();

  /**
   * Return an array containing all of the local variables defined within this executable element.
   * 
   * @return the local variables defined within this executable element
   */
  public VariableElement[] getLocalVariables();

  /**
   * Return an array containing all of the parameters defined by this executable element.
   * 
   * @return the parameters defined by this executable element
   */
  public VariableElement[] getParameters();

  /**
   * Return the type of function defined by this executable element.
   * 
   * @return the type of function defined by this executable element
   */
  public FunctionType getType();
}
