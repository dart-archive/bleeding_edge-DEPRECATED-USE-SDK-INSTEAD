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

package com.google.dart.engine.internal.type;

import com.google.dart.engine.internal.element.ElementPair;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.type.UnionType;

import org.apache.commons.lang3.NotImplementedException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * In addition to the methods of the {@code UnionType} interface we add a factory method
 * {@code union} for building unions.
 */
public class UnionTypeImpl extends TypeImpl implements UnionType {
  /**
   * Any unions in the {@code types} will be flattened in the returned union. If there is only one
   * type after flattening then it will be returned directly, instead of a singleton union.
   * 
   * @param types the {@code Type}s to union
   * @return a {@code Type} comprising the {@code Type}s in {@code types}
   */
  public static Type union(Type... types) {
    Set<Type> set = new HashSet<Type>();
    for (Type t : types) {
      if (t instanceof UnionType) {
        set.addAll(((UnionType) t).getElements());
      } else {
        set.add(t);
      }
    }
    if (set.size() == 0) {
      throw new IllegalArgumentException("No known use case for empty unions.");
    } else if (set.size() == 1) {
      return set.iterator().next();
    } else {
      return new UnionTypeImpl(set);
    }
  }

  /**
   * The types in this union.
   */
  private Set<Type> types;

  /**
   * This constructor should only be called by the {@code union} factory: it does not check that its
   * argument {@code types} contains no union types.
   * 
   * @param types
   */
  private UnionTypeImpl(Set<Type> types) {
    // TODO(collinsn): a union type does not correspond to an element, but it may have a name.
    // I'm not sure what the "name" means. If it's the same as {@code toString()} here, then
    // I should define it.
    super(null, null);
    this.types = types;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof UnionType)) {
      return false;
    } else if (this == other) {
      return true;
    } else {
      return this.types.equals(((UnionType) other).getElements());
    }
  }

  @Override
  public Set<Type> getElements() {
    return Collections.unmodifiableSet(this.types);
  }

  @Override
  public int hashCode() {
    return this.types.hashCode();
  }

  @Override
  public Type substitute(Type[] argumentTypes, Type[] parameterTypes) {
    // I can't think of any reason to substitute into a union type, since
    // they should only appear at the top level and not be be nested inside
    // other types.
    //
    // If there were a reason, then the implementation is to form a new union type
    // by mapping the substitution over the elements of this union type.
    throw new NotImplementedException("No known use case.");
  }

  @Override
  protected boolean internalEquals(Object object, Set<ElementPair> visitedElementPairs) {
    // TODO(collinsn): I understand why we have the [visitedElementPairs] 
    // in subtyping definitions: the user code could have inheritance loops, e.g.
    //
    //   class A extends B {}
    //   class B extends A {}
    //
    // However, I don't see how a type equality comparison could cause a loop, since type
    // equality should be structural. For example, we have
    //
    //   G<X1,...,Xm> = H<Y1,...,Yn>
    //
    // when [G = H /\ m = n /\ for all i. Xi = Yi]. Assuming there is no way to build
    // loopy generics (which would break [toString()]), each of the equality comparisons
    // above are on something structurally smaller.
    throw new NotImplementedException(
        "I don't believe there is any concern about infinite loops in type equality comparisons.");
  }

  @Override
  protected boolean internalIsMoreSpecificThan(Type type, boolean withDynamic,
      Set<TypePair> visitedTypePairs) {
    // I can't think of any reason to use [isMoreSpecificThan] for union types.
    throw new NotImplementedException("No known use case.");
  }

  @Override
  protected boolean internalIsSubtypeOf(Type type, Set<TypePair> visitedTypePairs) {
    // Premature optimization opportunity: if [type] is also a union type, we could instead
    // do a subset test on the underlying element tests.

    // TODO(collinsn): what version of subtyping do we want?
    //
    // The more unsound version: any.
    /*
    for (Type t : this.types) {
      if (((TypeImpl) t).internalIsSubtypeOf(type, visitedTypePairs)) {
        return true;
      }
    }
    return false;
    */
    // The less unsound version: all.
    for (Type t : this.types) {
      if (!((TypeImpl) t).internalIsSubtypeOf(type, visitedTypePairs)) {
        return false;
      }
    }
    return true;
  }

  /**
   * The super type test for union types is uniform in non-union subtypes. So, other
   * {@code TypeImpl}s can call this method to implement @ internalIsSubtypeOf} for union types.
   * 
   * @param type
   * @param visitedTypePairs
   * @return true if this union type is a super type of {@code type}
   */
  // TODO(collinsn): call this in all other [TypeImpls] when RHS type is a union type.
  protected boolean internalIsSuperTypeOf(Type type, Set<TypePair> visitedTypePairs) {
    // This implementation does not make sense when [type] is a union type, at least
    // for the "less unsound" version of [internalIsSubtypeOf] above.
    if (type instanceof UnionType) {
      throw new IllegalArgumentException("Only non-union types are supported.");
    }

    for (Type t : this.types) {
      if (((TypeImpl) type).internalIsSubtypeOf(t, visitedTypePairs)) {
        return true;
      }
    }
    return false;
  }
}
