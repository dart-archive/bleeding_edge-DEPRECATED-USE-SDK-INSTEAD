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
package com.google.dart.engine.internal.type;

import com.google.dart.engine.internal.element.DynamicElementImpl;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.type.Type;

/**
 * The unique instance of the class {@code DynamicTypeImpl} implements the type {@code dynamic}.
 * 
 * @coverage dart.engine.type
 */
public class DynamicTypeImpl extends TypeImpl {
  /**
   * The unique instance of this class.
   */
  private static final DynamicTypeImpl INSTANCE = new DynamicTypeImpl(); //$NON-NLS-1$

  /**
   * Return the unique instance of this class.
   * 
   * @return the unique instance of this class
   */
  public static DynamicTypeImpl getInstance() {
    return INSTANCE;
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private DynamicTypeImpl() {
    super(new DynamicElementImpl(), Keyword.DYNAMIC.getSyntax());
    ((DynamicElementImpl) getElement()).setType(this);
  }

  @Override
  public boolean equals(Object object) {
    return object instanceof DynamicTypeImpl;
  }

  @Override
  public boolean isDynamic() {
    return true;
  }

  @Override
  public boolean isMoreSpecificThan(Type type) {
    // T is S
    if (this == type) {
      return true;
    }
    // else
    return false;
  }

  @Override
  public boolean isSubtypeOf(Type type) {
    // dynamic is a subtype of all types
    return true;
  }

  @Override
  public boolean isSupertypeOf(Type type) {
    // dynamic is a supertype of all types
    return true;
  }

  @Override
  public Type substitute(Type[] argumentTypes, Type[] parameterTypes) {
    int length = parameterTypes.length;
    for (int i = 0; i < length; i++) {
      if (parameterTypes[i].equals(this)) {
        return argumentTypes[i];
      }
    }
    return this;
  }
}
