/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.engine.internal.element.angular;

import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.angular.AngularPropertyElement;
import com.google.dart.engine.element.angular.AngularPropertyKind;

/**
 * Implementation of {@code AngularPropertyElement}.
 * 
 * @coverage dart.engine.element
 */
public class AngularPropertyElementImpl extends AngularElementImpl implements
    AngularPropertyElement {
  /**
   * The {@link FieldElement} to which this property is bound.
   */
  private FieldElement field;

  /**
   * The offset of the field name in the property map.
   */
  private int fieldNameOffset = -1;

  private AngularPropertyKind propertyKind;

  /**
   * Initialize a newly created Angular property to have the given name.
   * 
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  public AngularPropertyElementImpl(String name, int nameOffset) {
    super(name, nameOffset);
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitAngularPropertyElement(this);
  }

  @Override
  public FieldElement getField() {
    return field;
  }

  @Override
  public int getFieldNameOffset() {
    return fieldNameOffset;
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.ANGULAR_PROPERTY;
  }

  @Override
  public AngularPropertyKind getPropertyKind() {
    return propertyKind;
  }

  /**
   * Set field to which this property is bound.
   * 
   * @param field the field to set
   */
  public void setField(FieldElement field) {
    this.field = field;
  }

  /**
   * Set the offset of the field name in the property map
   * 
   * @param fieldNameOffset the field name offset to set
   */
  public void setFieldNameOffset(int fieldNameOffset) {
    this.fieldNameOffset = fieldNameOffset;
  }

  /**
   * Set the kind of this property.
   * 
   * @param propertyKind the kind to set
   */
  public void setPropertyKind(AngularPropertyKind propertyKind) {
    this.propertyKind = propertyKind;
  }
}
