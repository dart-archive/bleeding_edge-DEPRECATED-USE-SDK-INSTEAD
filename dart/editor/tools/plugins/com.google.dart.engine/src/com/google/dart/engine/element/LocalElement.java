/*
 * Copyright (c) 2013, the Dart project authors.
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

import com.google.dart.engine.utilities.source.SourceRange;

/**
 * The interface {@code LocalElement} defines the behavior of elements that can be (but are not
 * required to be) defined within a method or function (an {@link ExecutableElement}).
 * 
 * @coverage dart.engine.element
 */
public interface LocalElement extends Element {
  /**
   * Return a source range that covers the approximate portion of the source in which the name of
   * this element is visible, or {@code null} if there is no single range of characters within which
   * the element name is visible.
   * <ul>
   * <li>For a local variable, this includes everything from the end of the variable's initializer
   * to the end of the block that encloses the variable declaration.</li>
   * <li>For a parameter, this includes the body of the method or function that declares the
   * parameter.</li>
   * <li>For a local function, this includes everything from the beginning of the function's body to
   * the end of the block that encloses the function declaration.</li>
   * <li>For top-level functions, {@code null} will be returned because they are potentially visible
   * in multiple sources.</li>
   * </ul>
   * 
   * @return the range of characters in which the name of this element is visible
   */
  public SourceRange getVisibleRange();
}
