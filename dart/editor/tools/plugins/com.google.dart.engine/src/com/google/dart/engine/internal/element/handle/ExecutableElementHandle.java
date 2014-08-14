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

import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.LabelElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.Type;

/**
 * The abstract class {@code ExecutableElementHandle} implements the behavior common to objects that
 * implement a handle to an {@link ExecutableElement}.
 * 
 * @coverage dart.engine.element
 */
public abstract class ExecutableElementHandle extends ElementHandle implements ExecutableElement {
  /**
   * Initialize a newly created element handle to represent the given element.
   * 
   * @param element the element being represented
   */
  public ExecutableElementHandle(ExecutableElement element) {
    super(element);
  }

  @Override
  public FunctionElement[] getFunctions() {
    return getActualElement().getFunctions();
  }

  @Override
  public LabelElement[] getLabels() {
    return getActualElement().getLabels();
  }

  @Override
  public LocalVariableElement[] getLocalVariables() {
    return getActualElement().getLocalVariables();
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
  public boolean isAsynchronous() {
    return getActualElement().isAsynchronous();
  }

  @Override
  public boolean isGenerator() {
    return getActualElement().isGenerator();
  }

  @Override
  public boolean isOperator() {
    return getActualElement().isOperator();
  }

  @Override
  public boolean isStatic() {
    return getActualElement().isStatic();
  }

  @Override
  public boolean isSynchronous() {
    return getActualElement().isSynchronous();
  }

  @Override
  protected ExecutableElement getActualElement() {
    return (ExecutableElement) super.getActualElement();
  }
}
