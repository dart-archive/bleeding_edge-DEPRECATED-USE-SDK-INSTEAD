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

import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.MethodElement;

/**
 * Instances of the class {@code MethodElementHandle} implement a handle to a {@code MethodElement}.
 * 
 * @coverage dart.engine.element
 */
public class MethodElementHandle extends ExecutableElementHandle implements MethodElement {
  /**
   * Initialize a newly created element handle to represent the given element.
   * 
   * @param element the element being represented
   */
  public MethodElementHandle(MethodElement element) {
    super(element);
  }

  @Override
  public ClassElement getEnclosingElement() {
    return (ClassElement) super.getEnclosingElement();
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.METHOD;
  }

  @Override
  public MethodDeclaration getNode() throws AnalysisException {
    return getActualElement().getNode();
  }

  @Override
  public boolean isAbstract() {
    return getActualElement().isAbstract();
  }

  @Override
  public boolean isStatic() {
    return getActualElement().isStatic();
  }

  @Override
  protected MethodElement getActualElement() {
    return (MethodElement) super.getActualElement();
  }
}
