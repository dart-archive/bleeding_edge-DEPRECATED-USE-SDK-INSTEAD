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

import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FieldFormalParameterElement;
import com.google.dart.engine.type.ParameterizedType;

/**
 * Instances of the class {@code FieldFormalParameterMember} represent a parameter element defined
 * in a parameterized type where the values of the type parameters are known.
 */
public class FieldFormalParameterMember extends ParameterMember implements
    FieldFormalParameterElement {
  /**
   * Initialize a newly created element to represent a parameter of the given parameterized type.
   * 
   * @param baseElement the element on which the parameterized element was created
   * @param definingType the type in which the element is defined
   */
  public FieldFormalParameterMember(FieldFormalParameterElement baseElement,
      ParameterizedType definingType) {
    super(baseElement, definingType);
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitFieldFormalParameterElement(this);
  }

  @Override
  public FieldElement getField() {
    return ((FieldFormalParameterElement) getBaseElement()).getField();
  }
}
