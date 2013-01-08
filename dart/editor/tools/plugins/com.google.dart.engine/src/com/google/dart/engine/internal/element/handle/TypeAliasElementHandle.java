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

import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.TypeAliasElement;
import com.google.dart.engine.element.TypeVariableElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.type.FunctionType;

/**
 * Instances of the class {@code TypeAliasElementHandle} implement a handle to a
 * {@code TypeAliasElement}.
 */
public class TypeAliasElementHandle extends ElementHandle implements TypeAliasElement {
  /**
   * Initialize a newly created element handle to represent the given element.
   * 
   * @param element the element being represented
   */
  public TypeAliasElementHandle(TypeAliasElement element) {
    super(element);
  }

  @Override
  public CompilationUnitElement getEnclosingElement() {
    return (CompilationUnitElement) super.getEnclosingElement();
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.TYPE_ALIAS;
  }

  @Override
  public VariableElement[] getParameters() {
    return getActualElement().getParameters();
  }

  @Override
  public FunctionType getType() {
    return getActualElement().getType();
  }

  @Override
  public TypeVariableElement[] getTypeVariables() {
    return getActualElement().getTypeVariables();
  }

  @Override
  protected TypeAliasElement getActualElement() {
    return (TypeAliasElement) super.getActualElement();
  }
}
