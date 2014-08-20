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

import com.google.dart.engine.internal.element.ElementPair;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.type.UnionType;
import com.google.dart.engine.type.VoidType;

import org.apache.commons.lang3.NotImplementedException;

import java.util.Set;

/**
 * The unique instance of the class {@code VoidTypeImpl} implements the type {@code void}.
 * 
 * @coverage dart.engine.type
 */
public class VoidTypeImpl extends TypeImpl implements VoidType {
  /**
   * The unique instance of this class.
   */
  private static final VoidTypeImpl INSTANCE = new VoidTypeImpl();

  /**
   * Return the unique instance of this class.
   * 
   * @return the unique instance of this class
   */
  public static VoidTypeImpl getInstance() {
    return INSTANCE;
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private VoidTypeImpl() {
    super(null, Keyword.VOID.getSyntax());
  }

  @Override
  public boolean equals(Object object) {
    return object == this;
  }

  @Override
  public int hashCode() {
    return 2;
  }

  @Override
  public boolean isVoid() {
    return true;
  }

  @Override
  public VoidTypeImpl substitute(Type[] argumentTypes, Type[] parameterTypes) {
    return this;
  }

  @Override
  protected boolean internalEquals(Object object, Set<ElementPair> visitedElementPairs) {
    return object == this;
  }

  @Override
  protected boolean internalIsMoreSpecificThan(Type type, boolean withDynamic,
      Set<TypePair> visitedTypePairs) {
    return isSubtypeOf(type);
  }

  @Override
  protected boolean internalIsSubtypeOf(Type type, Set<TypePair> visitedTypePairs) {
    if ((type instanceof UnionType)) {
      throw new NotImplementedException("No known use case");
    }
    // The only subtype relations that pertain to void are therefore:
    // void <: void (by reflexivity)
    // bottom <: void (as bottom is a subtype of all types).
    // void <: dynamic (as dynamic is a supertype of all types)
    return type == this || type == DynamicTypeImpl.getInstance();
  }
}
