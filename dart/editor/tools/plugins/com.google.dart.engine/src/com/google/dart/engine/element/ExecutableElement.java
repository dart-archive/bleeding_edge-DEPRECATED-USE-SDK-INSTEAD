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
import com.google.dart.engine.type.Type;

/**
 * The interface {@code ExecutableElement} defines the behavior of elements representing an
 * executable object, including functions, methods, constructors, getters, and setters.
 * 
 * @coverage dart.engine.element
 */
public interface ExecutableElement extends Element {
  /**
   * Return an array containing all of the functions defined within this executable element.
   * 
   * @return the functions defined within this executable element
   */
  public FunctionElement[] getFunctions();

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
  public LocalVariableElement[] getLocalVariables();

  /**
   * Return an array containing all of the parameters defined by this executable element.
   * 
   * @return the parameters defined by this executable element
   */
  public ParameterElement[] getParameters();

  /**
   * Return the return type defined by this executable element.
   * 
   * @return the return type defined by this executable element
   */
  public Type getReturnType();

  /**
   * Return the type of function defined by this executable element.
   * 
   * @return the type of function defined by this executable element
   */
  public FunctionType getType();

  /**
   * Return {@code true} if this executable element has body marked as being asynchronous.
   * 
   * @return {@code true} if this executable element has body marked as being asynchronous
   */
  public boolean isAsynchronous();

  /**
   * Return {@code true} if this executable element has a body marked as being a generator.
   * 
   * @return {@code true} if this executable element has a body marked as being a generator
   */
  public boolean isGenerator();

  /**
   * Return {@code true} if this executable element is an operator. The test may be based on the
   * name of the executable element, in which case the result will be correct when the name is
   * legal.
   * 
   * @return {@code true} if this executable element is an operator
   */
  public boolean isOperator();

  /**
   * Return {@code true} if this element is a static element. A static element is an element that is
   * not associated with a particular instance, but rather with an entire library or class.
   * 
   * @return {@code true} if this executable element is a static element
   */
  public boolean isStatic();

  /**
   * Return {@code true} if this executable element has a body marked as being synchronous.
   * 
   * @return {@code true} if this executable element has a body marked as being synchronous
   */
  public boolean isSynchronous();
}
