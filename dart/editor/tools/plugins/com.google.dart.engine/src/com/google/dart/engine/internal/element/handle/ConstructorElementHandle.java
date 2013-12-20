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

import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.ElementKind;

/**
 * Instances of the class {@code ConstructorElementHandle} implement a handle to a
 * {@code ConstructorElement}.
 * 
 * @coverage dart.engine.element
 */
public class ConstructorElementHandle extends ExecutableElementHandle implements ConstructorElement {
  /**
   * Initialize a newly created element handle to represent the given element.
   * 
   * @param element the element being represented
   */
  public ConstructorElementHandle(ConstructorElement element) {
    super(element);
  }

  @Override
  public ClassElement getEnclosingElement() {
    return getActualElement().getEnclosingElement();
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.CONSTRUCTOR;
  }

  @Override
  public ConstructorDeclaration getNode() throws AnalysisException {
    return getActualElement().getNode();
  }

  @Override
  public ConstructorElement getRedirectedConstructor() {
    return getActualElement().getRedirectedConstructor();
  }

  @Override
  public boolean isConst() {
    return getActualElement().isConst();
  }

  @Override
  public boolean isDefaultConstructor() {
    return getActualElement().isDefaultConstructor();
  }

  @Override
  public boolean isFactory() {
    return getActualElement().isFactory();
  }

  @Override
  protected ConstructorElement getActualElement() {
    return (ConstructorElement) super.getActualElement();
  }
}
