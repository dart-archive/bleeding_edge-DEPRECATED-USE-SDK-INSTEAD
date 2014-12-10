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
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.utilities.translation.DartName;

/**
 * Instances of the class {@code FieldElementImpl} implement a {@code FieldElement}.
 * 
 * @coverage dart.engine.element
 */
public class FieldElementImpl extends PropertyInducingElementImpl implements FieldElement {
  /**
   * An empty array of field elements.
   */
  public static final FieldElement[] EMPTY_ARRAY = new FieldElement[0];

  /**
   * Initialize a newly created field element to have the given name.
   * 
   * @param name the name of this element
   */
  @DartName("forNode")
  public FieldElementImpl(Identifier name) {
    super(name);
  }

  /**
   * Initialize a newly created synthetic field element to have the given name.
   * 
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  public FieldElementImpl(String name, int nameOffset) {
    super(name, nameOffset);
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitFieldElement(this);
  }

  @Override
  public ClassElement getEnclosingElement() {
    return (ClassElement) super.getEnclosingElement();
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.FIELD;
  }

  @Override
  public boolean isEnumConstant() {
    ClassElement enclosingClassElt = getEnclosingElement();
    return enclosingClassElt != null ? enclosingClassElt.isEnum() : false;
  }

  @Override
  public boolean isStatic() {
    return hasModifier(Modifier.STATIC);
  }

  /**
   * Set whether this field is static to correspond to the given value.
   * 
   * @param isStatic {@code true} if the field is static
   */
  public void setStatic(boolean isStatic) {
    setModifier(Modifier.STATIC, isStatic);
  }
}
