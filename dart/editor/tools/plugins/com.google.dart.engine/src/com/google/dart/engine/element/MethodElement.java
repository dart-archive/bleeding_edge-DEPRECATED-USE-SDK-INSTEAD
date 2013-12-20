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

import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.context.AnalysisException;

/**
 * The interface {@code MethodElement} defines the behavior of elements that represent a method
 * defined within a type.
 * 
 * @coverage dart.engine.element
 */
public interface MethodElement extends ClassMemberElement, ExecutableElement {
  /**
   * Return the resolved {@link MethodDeclaration} node that declares this {@link MethodElement}.
   * <p>
   * This method is expensive, because resolved AST might be evicted from cache, so parsing and
   * resolving will be performed.
   * 
   * @return the resolved {@link MethodDeclaration}, not {@code null}.
   */
  @Override
  public MethodDeclaration getNode() throws AnalysisException;

  /**
   * Return {@code true} if this method is abstract. Methods are abstract if they are not external
   * and have no body.
   * 
   * @return {@code true} if this method is abstract
   */
  public boolean isAbstract();
}
