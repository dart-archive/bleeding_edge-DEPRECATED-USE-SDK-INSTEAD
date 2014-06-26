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
package com.google.dart.engine.internal.element;

import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.TopLevelVariableElement;
import com.google.dart.engine.utilities.translation.DartName;

/**
 * Instances of the class {@code TopLevelVariableElementImpl} implement a
 * {@code TopLevelVariableElement}.
 * 
 * @coverage dart.engine.element
 */
public class TopLevelVariableElementImpl extends PropertyInducingElementImpl implements
    TopLevelVariableElement {
  /**
   * An empty array of top-level variable elements.
   */
  public static final TopLevelVariableElement[] EMPTY_ARRAY = new TopLevelVariableElement[0];

  /**
   * Initialize a newly created top-level variable element to have the given name.
   * 
   * @param name the name of this element
   */
  @DartName("forNode")
  public TopLevelVariableElementImpl(Identifier name) {
    super(name);
  }

  /**
   * Initialize a newly created synthetic top-level variable element to have the given name.
   * 
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  public TopLevelVariableElementImpl(String name, int nameOffset) {
    super(name, nameOffset);
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitTopLevelVariableElement(this);
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.TOP_LEVEL_VARIABLE;
  }

  @Override
  public boolean isStatic() {
    return true;
  }
}
