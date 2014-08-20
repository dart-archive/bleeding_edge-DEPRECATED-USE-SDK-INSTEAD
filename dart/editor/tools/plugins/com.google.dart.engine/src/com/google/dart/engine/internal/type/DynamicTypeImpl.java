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
import com.google.dart.engine.internal.element.ElementPair;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.type.UnionType;

import org.apache.commons.lang3.NotImplementedException;

import java.util.Set;

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
    return object == this;
  }

  @Override
  public int hashCode() {
    return 1;
  }

  @Override
  public boolean isDynamic() {
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

  @Override
  protected boolean internalEquals(Object object, Set<ElementPair> visitedElementPairs) {
    return object == this;
  }

  @Override
  protected boolean internalIsMoreSpecificThan(Type type, boolean withDynamic,
      Set<TypePair> visitedTypePairs) {
    // T is S
    if (this == type) {
      return true;
    } else if ((type instanceof UnionType)) {
      throw new NotImplementedException("No known use case");
    }

    // else
    return withDynamic;
  }

  @Override
  protected boolean internalIsSubtypeOf(Type type, Set<TypePair> visitedTypePairs) {
    //dynamic is a subtype of all types
    return true;
  }
}
