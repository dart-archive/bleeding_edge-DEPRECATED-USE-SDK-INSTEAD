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
import com.google.dart.engine.element.TypeVariableElement;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.type.TypeVariableType;

/**
 * Instances of the class {@code TypeVariableElementImpl} implement a {@code TypeVariableElement}.
 */
public class TypeVariableElementImpl extends ElementImpl implements TypeVariableElement {
  /**
   * The type defined by this type variable.
   */
  private TypeVariableType type;

  /**
   * The type representing the bound associated with this variable, or {@code null} if this variable
   * does not have an explicit bound.
   */
  private Type bound;

  /**
   * An empty array of type variable elements.
   */
  public static final TypeVariableElement[] EMPTY_ARRAY = new TypeVariableElement[0];

  /**
   * Initialize a newly created type variable element to have the given name.
   * 
   * @param name the name of this element
   */
  public TypeVariableElementImpl(Identifier name) {
    super(name);
  }

  @Override
  public Type getBound() {
    return bound;
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.TYPE_VARIABLE;
  }

  @Override
  public TypeVariableType getType() {
    return type;
  }

  /**
   * Set the type representing the bound associated with this variable to the given type.
   * 
   * @param bound the type representing the bound associated with this variable
   */
  public void setBound(Type bound) {
    this.bound = bound;
  }

  /**
   * Set the type defined by this type variable to the given type
   * 
   * @param type the type defined by this type variable
   */
  public void setType(TypeVariableType type) {
    this.type = type;
  }
}
