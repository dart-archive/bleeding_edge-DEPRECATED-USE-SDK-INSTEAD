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
package com.google.dart.engine.internal.element;

import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ClassElement;

/**
 * Instances of the class {@code MethodElementImpl} implement a {@code MethodElement}.
 */
public class MethodElementImpl extends ExecutableElementImpl implements MethodElement {
  /**
   * An empty array of method elements.
   */
  public static final MethodElement[] EMPTY_ARRAY = new MethodElement[0];

  /**
   * Initialize a newly created method element to have the given name.
   * 
   * @param name the name of this element
   */
  public MethodElementImpl(Identifier name) {
    super(name);
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
  public boolean isAbstract() {
    return hasModifier(Modifier.ABSTRACT);
  }

  @Override
  public boolean isStatic() {
    return hasModifier(Modifier.STATIC);
  }

  /**
   * Set whether this method is abstract to correspond to the given value.
   * 
   * @param isAbstract {@code true} if the method is abstract
   */
  public void setAbstract(boolean isAbstract) {
    setModifier(Modifier.ABSTRACT, isAbstract);
  }

  /**
   * Set whether this method is static to correspond to the given value.
   * 
   * @param isStatic {@code true} if the method is static
   */
  public void setStatic(boolean isStatic) {
    setModifier(Modifier.STATIC, isStatic);
  }
}
