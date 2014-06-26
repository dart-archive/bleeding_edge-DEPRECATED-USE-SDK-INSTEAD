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
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.TypeParameterElement;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.type.TypeParameterType;
import com.google.dart.engine.utilities.translation.DartName;

/**
 * Instances of the class {@code TypeParameterElementImpl} implement a {@link TypeParameterElement}.
 * 
 * @coverage dart.engine.element
 */
public class TypeParameterElementImpl extends ElementImpl implements TypeParameterElement {
  /**
   * The type defined by this type parameter.
   */
  private TypeParameterType type;

  /**
   * The type representing the bound associated with this parameter, or {@code null} if this
   * parameter does not have an explicit bound.
   */
  private Type bound;

  /**
   * An empty array of type parameter elements.
   */
  public static final TypeParameterElement[] EMPTY_ARRAY = new TypeParameterElement[0];

  /**
   * Initialize a newly created type parameter element to have the given name.
   * 
   * @param name the name of this element
   */
  @DartName("forNode")
  public TypeParameterElementImpl(Identifier name) {
    super(name);
  }

  /**
   * Initialize a newly created method element to have the given name.
   * 
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  public TypeParameterElementImpl(String name, int nameOffset) {
    super(name, nameOffset);
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitTypeParameterElement(this);
  }

  @Override
  public Type getBound() {
    return bound;
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.TYPE_PARAMETER;
  }

  @Override
  public TypeParameterType getType() {
    return type;
  }

  /**
   * Set the type representing the bound associated with this parameter to the given type.
   * 
   * @param bound the type representing the bound associated with this parameter
   */
  public void setBound(Type bound) {
    this.bound = bound;
  }

  /**
   * Set the type defined by this type parameter to the given type
   * 
   * @param type the type defined by this type parameter
   */
  public void setType(TypeParameterType type) {
    this.type = type;
  }

  @Override
  protected void appendTo(StringBuilder builder) {
    builder.append(getDisplayName());
    if (bound != null) {
      builder.append(" extends ");
      builder.append(bound);
    }
  }
}
