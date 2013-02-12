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

import com.google.dart.engine.utilities.source.SourceRange;

/**
 * The interface {@code FunctionElement} defines the behavior of elements representing a function.
 */
public interface FunctionElement extends ExecutableElement {
  /**
   * Return a source range that covers the approximate portion of the source in which the name of
   * this function is visible, or {@code null} if there is no single range of characters within
   * which the variable's name is visible.
   * <ul>
   * <li>For a local function, this includes everything from the beginning of the function's body to
   * the end of the block that encloses the function declaration.</li>
   * <li>For top-level functions, {@code null} will be returned because they are potentially visible
   * in multiple sources.</li>
   * </ul>
   * 
   * @return the range of characters in which the name of this function is visible
   */
  public SourceRange getVisibleRange();
}
