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

import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.Type;

/**
 * The interface {@code FunctionTypeAliasElement} defines the behavior of elements representing a
 * function type alias ({@code typedef}).
 * 
 * @coverage dart.engine.element
 */
public interface FunctionTypeAliasElement extends Element {
  /**
   * Return the compilation unit in which this type alias is defined.
   * 
   * @return the compilation unit in which this type alias is defined
   */
  @Override
  public CompilationUnitElement getEnclosingElement();

  /**
   * Return the resolved {@link FunctionTypeAlias} node that declares this
   * {@link FunctionTypeAliasElement} .
   * <p>
   * This method is expensive, because resolved AST might be evicted from cache, so parsing and
   * resolving will be performed.
   * 
   * @return the resolved {@link FunctionTypeAlias}, not {@code null}.
   */
  @Override
  public FunctionTypeAlias getNode() throws AnalysisException;

  /**
   * Return an array containing all of the parameters defined by this type alias.
   * 
   * @return the parameters defined by this type alias
   */
  public ParameterElement[] getParameters();

  /**
   * Return the return type defined by this type alias.
   * 
   * @return the return type defined by this type alias
   */
  public Type getReturnType();

  /**
   * Return the type of function defined by this type alias.
   * 
   * @return the type of function defined by this type alias
   */
  public FunctionType getType();

  /**
   * Return an array containing all of the type parameters defined for this type.
   * 
   * @return the type parameters defined for this type
   */
  public TypeParameterElement[] getTypeParameters();
}
