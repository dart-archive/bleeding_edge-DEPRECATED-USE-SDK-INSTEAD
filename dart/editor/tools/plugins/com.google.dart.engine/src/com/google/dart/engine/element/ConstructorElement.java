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

import com.google.dart.engine.type.Type;

/**
 * The interface {@code ConstructorElement} defines the behavior of elements representing a
 * constructor or a factory method defined within a type.
 */
public interface ConstructorElement extends ExecutableElement {
  /**
   * Return the type of the instances created by this constructor. Note that a constructor in a
   * class may be a default implementation of an interface's constructor, in which case this will
   * return the type of the interface.
   * 
   * @return the type of the instances created by this constructor
   */
  public Type getConstructedType();

  /**
   * Return the type in which this constructor is defined.
   * 
   * @return the type in which this constructor is defined
   */
  @Override
  public TypeElement getEnclosingElement();

  /**
   * Return {@code true} if this constructor represents a factory method.
   * 
   * @return {@code true} if this constructor represents a factory method
   */
  public boolean isFactory();
}
