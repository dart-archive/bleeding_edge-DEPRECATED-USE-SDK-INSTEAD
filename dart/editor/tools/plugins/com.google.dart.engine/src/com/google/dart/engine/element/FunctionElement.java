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

import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.context.AnalysisException;

/**
 * The interface {@code FunctionElement} defines the behavior of elements representing a function.
 * 
 * @coverage dart.engine.element
 */
public interface FunctionElement extends ExecutableElement, LocalElement {
  /**
   * The name of the method that can be implemented by a class to allow its instances to be invoked
   * as if they were a function.
   */
  public static final String CALL_METHOD_NAME = "call"; //$NON-NLS-1$

  /**
   * The name of the method that will be invoked if an attempt is made to invoke an undefined method
   * on an object.
   */
  public static final String NO_SUCH_METHOD_METHOD_NAME = "noSuchMethod"; //$NON-NLS-1$

  /**
   * The name of the synthetic function defined for libraries that are deferred.
   */
  public static final String LOAD_LIBRARY_NAME = "loadLibrary"; //$NON-NLS-1$

  /**
   * Return the resolved {@link FunctionDeclaration} node that declares this {@link FunctionElement}
   * .
   * <p>
   * This method is expensive, because resolved AST might be evicted from cache, so parsing and
   * resolving will be performed.
   * 
   * @return the resolved {@link FunctionDeclaration}, not {@code null}.
   */
  @Override
  public FunctionDeclaration getNode() throws AnalysisException;
}
