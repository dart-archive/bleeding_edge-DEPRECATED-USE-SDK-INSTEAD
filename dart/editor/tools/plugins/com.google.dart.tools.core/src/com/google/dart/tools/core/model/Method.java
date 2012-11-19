/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.model;

/**
 * The interface <code>Method</code> defines the behavior of objects representing methods defined in
 * types.
 */
public interface Method extends TypeMember, DartFunction {

  /**
   * Return <code>true</code> if this method is declared abstract.
   * 
   * @return <code>true</code> if this method is declared abstract
   */
  public boolean isAbstract();

  /**
   * Return <code>true</code> if this method represents a constructor.
   * 
   * @return <code>true</code> if this method represents a constructor
   */
  public boolean isConstructor();

  /**
   * Return <code>true</code> if this method is declared as a factory method.
   * 
   * @return <code>true</code> if this method is declared as a factory method
   */
  public boolean isFactory();

  /**
   * Return <code>true</code> if this method represents an implicitly defined method. At the moment
   * the only implicitly defined methods are zero-argument constructors in classes that have no
   * explicitly defined constructors.
   * 
   * @return <code>true</code> if this method represents an implicitly defined method
   */
  public boolean isImplicit();
}
