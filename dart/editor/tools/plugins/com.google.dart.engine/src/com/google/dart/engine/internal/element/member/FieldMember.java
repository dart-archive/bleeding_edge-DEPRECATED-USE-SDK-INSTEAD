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
package com.google.dart.engine.internal.element.member;

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

/**
 * Instances of the class {@code FieldMember} represent a field element defined in a parameterized
 * type where the values of the type parameters are known.
 */
public class FieldMember extends VariableMember implements FieldElement {
  /**
   * If the given field's type is different when any type parameters from the defining type's
   * declaration are replaced with the actual type arguments from the defining type, create a field
   * member representing the given field. Return the member that was created, or the base field if
   * no member was created.
   * 
   * @param baseField the base field for which a member might be created
   * @param definingType the type defining the parameters and arguments to be used in the
   *          substitution
   * @return the field element that will return the correctly substituted types
   */
  public static FieldElement from(FieldElement baseField, InterfaceType definingType) {
    if (baseField == null || definingType.getTypeArguments().length == 0) {
      return baseField;
    }
    Type baseType = baseField.getType();
    if (baseType == null) {
      return baseField;
    }
    Type[] argumentTypes = definingType.getTypeArguments();
    Type[] parameterTypes = definingType.getElement().getType().getTypeArguments();
    Type substitutedType = baseType.substitute(argumentTypes, parameterTypes);
    if (baseType.equals(substitutedType)) {
      return baseField;
    }
    // TODO(brianwilkerson) Consider caching the substituted type in the instance. It would use more
    // memory but speed up some operations. We need to see how often the type is being re-computed.
    return new FieldMember(baseField, definingType);
  }

  /**
   * Initialize a newly created element to represent a field of the given parameterized type.
   * 
   * @param baseElement the element on which the parameterized element was created
   * @param definingType the type in which the element is defined
   */
  public FieldMember(FieldElement baseElement, InterfaceType definingType) {
    super(baseElement, definingType);
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitFieldElement(this);
  }

  @Override
  public FieldElement getBaseElement() {
    return (FieldElement) super.getBaseElement();
  }

  @Override
  public ClassElement getEnclosingElement() {
    return getBaseElement().getEnclosingElement();
  }

  @Override
  public PropertyAccessorElement getGetter() {
    return PropertyAccessorMember.from(getBaseElement().getGetter(), getDefiningType());
  }

  @Override
  public PropertyAccessorElement getSetter() {
    return PropertyAccessorMember.from(getBaseElement().getSetter(), getDefiningType());
  }

  @Override
  public boolean isStatic() {
    return getBaseElement().isStatic();
  }

  @Override
  protected InterfaceType getDefiningType() {
    return (InterfaceType) super.getDefiningType();
  }
}
