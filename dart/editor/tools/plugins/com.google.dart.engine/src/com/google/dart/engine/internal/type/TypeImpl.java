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
package com.google.dart.engine.internal.type;

import com.google.dart.engine.element.Element;
import com.google.dart.engine.type.Type;

/**
 * The abstract class {@code TypeImpl} implements the behavior common to objects representing the
 * declared type of elements in the element model.
 */
public abstract class TypeImpl implements Type {
  /**
   * The element representing the declaration of this type, or {@code null} if the type has not, or
   * cannot, be associated with an element.
   */
  private Element element;

  /**
   * The name of this type, or {@code null} if the type does not have a name.
   */
  private String name;

  /**
   * An empty array of types.
   */
  public static final Type[] EMPTY_ARRAY = new Type[0];

  /**
   * Initialize a newly created type to be declared by the given element and to have the given name.
   * 
   * @param element the element representing the declaration of the type
   * @param name the name of the type
   */
  public TypeImpl(Element element, String name) {
    this.element = element;
    this.name = name;
  }

  @Override
  public Element getElement() {
    return element;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean isAssignableTo(Type type) {
    return this.isSubtypeOf(type) || type.isSubtypeOf(this);
  }

  @Override
  public boolean isMoreSpecificThan(Type type) {
    return false;
  }

  @Override
  public boolean isSupertypeOf(Type type) {
    return type.isSubtypeOf(this);
  }
}
