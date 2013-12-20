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
package com.google.dart.engine.internal.element.handle;

import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.TypeParameterElement;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.Type;

/**
 * Instances of the class {@code FunctionTypeAliasElementHandle} implement a handle to a
 * {@code FunctionTypeAliasElement}.
 * 
 * @coverage dart.engine.element
 */
public class FunctionTypeAliasElementHandle extends ElementHandle implements
    FunctionTypeAliasElement {
  /**
   * Initialize a newly created element handle to represent the given element.
   * 
   * @param element the element being represented
   */
  public FunctionTypeAliasElementHandle(FunctionTypeAliasElement element) {
    super(element);
  }

  @Override
  public CompilationUnitElement getEnclosingElement() {
    return (CompilationUnitElement) super.getEnclosingElement();
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.FUNCTION_TYPE_ALIAS;
  }

  @Override
  public FunctionTypeAlias getNode() throws AnalysisException {
    return getActualElement().getNode();
  }

  @Override
  public ParameterElement[] getParameters() {
    return getActualElement().getParameters();
  }

  @Override
  public Type getReturnType() {
    return getActualElement().getReturnType();
  }

  @Override
  public FunctionType getType() {
    return getActualElement().getType();
  }

  @Override
  public TypeParameterElement[] getTypeParameters() {
    return getActualElement().getTypeParameters();
  }

  @Override
  protected FunctionTypeAliasElement getActualElement() {
    return (FunctionTypeAliasElement) super.getActualElement();
  }
}
