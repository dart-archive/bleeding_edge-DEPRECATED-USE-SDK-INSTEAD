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
import com.google.dart.engine.element.angular.AngularScopePropertyElement;
import com.google.dart.engine.type.Type;

/**
 * Implementation of {@code AngularScopePropertyElement}.
 * 
 * @coverage dart.engine.element
 */
public class AngularScopePropertyElementImpl extends AngularElementImpl implements
    AngularScopePropertyElement {
  /**
   * The type of the property
   */
  private final Type type;

  /**
   * Initialize a newly created Angular scope property to have the given name.
   * 
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  public AngularScopePropertyElementImpl(String name, int nameOffset, Type type) {
    super(name, nameOffset);
    this.type = type;
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitAngularScopePropertyElement(this);
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.ANGULAR_SCOPE_PROPERTY;
  }

  @Override
  public Type getType() {
    return type;
  }
}
