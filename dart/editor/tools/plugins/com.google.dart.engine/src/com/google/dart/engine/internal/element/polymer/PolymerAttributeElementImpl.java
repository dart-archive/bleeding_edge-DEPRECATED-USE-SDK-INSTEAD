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

package com.google.dart.engine.internal.element.polymer;

import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.polymer.PolymerAttributeElement;

/**
 * Implementation of {@code PolymerAttributeElement}.
 * 
 * @coverage dart.engine.element
 */
public class PolymerAttributeElementImpl extends PolymerElementImpl implements
    PolymerAttributeElement {
  /**
   * The {@link FieldElement} associated with this attribute.
   */
  private FieldElement field;

  /**
   * Initialize a newly created Polymer attribute to have the given name.
   * 
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  public PolymerAttributeElementImpl(String name, int nameOffset) {
    super(name, nameOffset);
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitPolymerAttributeElement(this);
  }

  @Override
  public FieldElement getField() {
    return field;
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.POLYMER_ATTRIBUTE;
  }

  /**
   * Set the {@link FieldElement} associated with this attribute.
   * 
   * @param field the {@link FieldElement} to set
   */
  public void setField(FieldElement field) {
    this.field = field;
  }
}
