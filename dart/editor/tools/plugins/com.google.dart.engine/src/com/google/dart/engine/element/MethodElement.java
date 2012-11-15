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

/**
 * The interface {@code MethodElement} defines the behavior of elements that represent a method
 * defined within a type.
 */
public interface MethodElement extends ExecutableElement {
  /**
   * Return the type in which this method is defined.
   * 
   * @return the type in which this method is defined
   */
  @Override
  public TypeElement getEnclosingElement();

  /**
   * Return {@code true} if this method is abstract. Methods are abstract if they are not external
   * and have no body.
   * 
   * @return {@code true} if this method is abstract
   */
  public boolean isAbstract();

  /**
   * Return {@code true} if this method is static. Methods are static if they have been marked as
   * being static using the {@code static} modifier.
   * 
   * @return {@code true} if this method is static
   */
  public boolean isStatic();
}
