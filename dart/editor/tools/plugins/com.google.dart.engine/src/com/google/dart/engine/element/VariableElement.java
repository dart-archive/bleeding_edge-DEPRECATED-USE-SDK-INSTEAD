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
import com.google.dart.engine.utilities.source.SourceRange;

/**
 * The interface {@code VariableElement} defines the behavior common to elements that represent a
 * variable.
 */
public interface VariableElement extends Element {
  /**
   * Return a synthetic function representing this variable's initializer, or {@code null} if this
   * variable does not have an initializer. The function will have no parameters. The return type of
   * the function will be the compile-time type of the initialization expression.
   * 
   * @return a synthetic function representing this variable's initializer
   */
  public FunctionElement getInitializer();

  /**
   * Return the declared type of this variable, or {@code null} if the variable did not have a
   * declared type (such as if it was declared using the keyword 'var').
   * 
   * @return the declared type of this variable
   */
  public Type getType();

  /**
   * Return a source range that covers the approximate portion of the source in which the name of
   * this variable is visible, or {@code null} if there is no single range of characters within
   * which the variable's name is visible.
   * <ul>
   * <li>For a local variable, this includes everything from the end of the variable's initializer
   * to the end of the block that encloses the variable declaration.</li>
   * <li>For a parameter, this includes the body of the method or function that declares the
   * parameter.</li>
   * <li>For fields and top-level variables, {@code null} will be returned because they are
   * potentially visible in multiple sources.</li>
   * </ul>
   * 
   * @return the range of characters in which the name of this variable is visible
   */
  public SourceRange getVisibleRange();

  /**
   * Return {@code true} if this variable is a const variable. Variables are const if they have been
   * marked as being const using the {@code const} modifier.
   * 
   * @return {@code true} if this variable is a const variable
   */
  public boolean isConst();

  /**
   * Return {@code true} if this variable is a final variable. Variables are final if they have been
   * marked as being final using either the {@code final} or {@code const} modifiers.
   * 
   * @return {@code true} if this variable is a final variable
   */
  public boolean isFinal();
}
