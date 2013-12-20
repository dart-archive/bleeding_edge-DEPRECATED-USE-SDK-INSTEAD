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

import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.type.Type;

/**
 * The interface {@code VariableElement} defines the behavior common to elements that represent a
 * variable.
 * 
 * @coverage dart.engine.element
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
   * Return the resolved {@link VariableDeclaration} node that declares this {@link VariableElement}
   * .
   * <p>
   * This method is expensive, because resolved AST might be evicted from cache, so parsing and
   * resolving will be performed.
   * 
   * @return the resolved {@link VariableDeclaration}, not {@code null}.
   */
  @Override
  public VariableDeclaration getNode() throws AnalysisException;

  /**
   * Return the declared type of this variable, or {@code null} if the variable did not have a
   * declared type (such as if it was declared using the keyword 'var').
   * 
   * @return the declared type of this variable
   */
  public Type getType();

  /**
   * Return {@code true} if this variable was declared with the 'const' modifier.
   * 
   * @return {@code true} if this variable was declared with the 'const' modifier
   */
  public boolean isConst();

  /**
   * Return {@code true} if this variable was declared with the 'final' modifier. Variables that are
   * declared with the 'const' modifier will return {@code false} even though they are implicitly
   * final.
   * 
   * @return {@code true} if this variable was declared with the 'final' modifier
   */
  public boolean isFinal();
}
