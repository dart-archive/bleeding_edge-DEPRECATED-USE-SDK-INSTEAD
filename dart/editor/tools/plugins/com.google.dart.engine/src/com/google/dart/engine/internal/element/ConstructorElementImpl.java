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
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ClassElement;

/**
 * Instances of the class {@code ConstructorElementImpl} implement a {@code ConstructorElement}.
 */
public class ConstructorElementImpl extends ExecutableElementImpl implements ConstructorElement {
  /**
   * An empty array of constructor elements.
   */
  public static final ConstructorElement[] EMPTY_ARRAY = new ConstructorElement[0];

  /**
   * Initialize a newly created constructor element to have the given name.
   * 
   * @param name the name of this element
   */
  public ConstructorElementImpl(Identifier name) {
    super(name);
  }

  @Override
  public ClassElement getEnclosingElement() {
    return (ClassElement) super.getEnclosingElement();
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.CONSTRUCTOR;
  }

  @Override
  public boolean isConst() {
    return hasModifier(Modifier.CONST);
  }

  @Override
  public boolean isFactory() {
    return hasModifier(Modifier.FACTORY);
  }

  /**
   * Set whether this constructor represents a factory method to the given value.
   * 
   * @param isFactory {@code true} if this constructor represents a factory method
   */
  public void setFactory(boolean isFactory) {
    setModifier(Modifier.FACTORY, isFactory);
  }
}
