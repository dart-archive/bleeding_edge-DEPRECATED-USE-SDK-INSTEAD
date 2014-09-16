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
import com.google.dart.engine.utilities.translation.DartExpressionBody;

import java.util.ArrayList;
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
   * type after flattening then it will be returned directly, instead of a singleton union. Nulls
   * are discarded, unless all types are null, in which case an exception is raised.
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
        if (t != null) {
          set.add(t);
        }
      }
    }
    if (set.size() == 0) {
      // TODO(collinsn): better to return [null] here? The use case is e.g.
      //
      //   union(null, null) ==> null;
      //
      // instead of raising an exception.
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
    // The element is null because union types are not associated with program elements.
    // We make the name null because that's what [FunctionTypeImpl] uses when the element is null.
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
      return types.equals(((UnionType) other).getElements());
    }
  }

  @Override
  public String getDisplayName() {
    StringBuilder builder = new StringBuilder();
    String prefix = "{";
    for (Type t : types) {
      builder.append(prefix);
      builder.append(t.getDisplayName());
      prefix = ",";
    }
    builder.append("}");
    return builder.toString();
  }

  @DartExpressionBody("_types")
  @Override
  public Set<Type> getElements() {
    return Collections.unmodifiableSet(types);
  }

  @Override
  public int hashCode() {
    return types.hashCode();
  }

  @Override
  public Type substitute(Type[] argumentTypes, Type[] parameterTypes) {
    ArrayList<Type> out = new ArrayList<Type>();
    for (Type t : types) {
      out.add(t.substitute(argumentTypes, parameterTypes));
    }
    return union(out.toArray(new Type[out.size()]));
  }

  @Override
  protected void appendTo(StringBuilder builder) {
    String prefix = "{";
    for (Type t : types) {
      builder.append(prefix);
      ((TypeImpl) t).appendTo(builder);
      prefix = ",";
    }
    builder.append("}");
  }

  @Override
  protected boolean internalEquals(Object object, Set<ElementPair> visitedElementPairs) {
    // Since union types are immutable, I don't think it's
    // possible to construct a self-referential union type. Of course, a self-referential
    // non-union type could intermediate through a union type, but since union types
    // don't occur in user programs this is not a problem we expect to run into any time
    // soon.
    return this.equals(object);
  }

  @Override
  protected boolean internalIsMoreSpecificThan(Type type, boolean withDynamic,
      Set<TypePair> visitedTypePairs) {
    // What version of subtyping do we want? See discussion below in [internalIsSubtypeOf].
    //
    // The more unsound version: any.
    for (Type t : types) {
      if (((TypeImpl) t).internalIsMoreSpecificThan(type, withDynamic, visitedTypePairs)) {
        return true;
      }
    }
    return false;
    // The less unsound version: all.
    /*
    for (Type t : types) {
      if (!((TypeImpl) t).internalIsMoreSpecificThan(type, withDynamic, visitedTypePairs)) {
        return false;
      }
    }
    return true;
    */
  }

  @Override
  protected boolean internalIsSubtypeOf(Type type, Set<TypePair> visitedTypePairs) {
    // Premature optimization opportunity: if [type] is also a union type, we could instead
    // do a subset test on the underlying element sets.

    // What version of union-type subtyping do we want? We choose the more unsound version,
    // motivated by the following example. Suppose classes [A] and [B] are unrelated
    // and that [C] extends [B]:
    //
    //   A  B
    //      |
    //      C
    //
    // If [ab : {A, B}] and [ac : {A, C}] then we'd intuitively expect either
    // both or neither of the assignments
    //
    //   B b1 = ab;
    //   B b2 = ac;
    //
    // to be allowed; they're both allowed under the more unsound subtyping rule.
    //
    // However, under the less unsound subtyping rule, the assignment to [b1] is
    // allowed, but the assignment to [b2] is not. The reason is that assignment
    // compatible [T1 <=> T1] for interface types is defined by [T1 <: T2] or
    // [T2 <: T1]. So, in the [b1 = ab] case, the test [B <: {A, B}] always passes,
    // for both definitions of union-type subtyping.  On the other hand, the test
    // [B <: {A, C}] always fails, leaving only [{A, C} <: B], which only passes
    // for the more unsound rule.
    //
    // An interesting consequence: we usually think of [A <: B] meaning that
    // knowing an expression has type [A] is more informative than knowing it has type [B].
    // Of course, this intuition is already wrong once we have [dynamic], but it becomes
    // more wrong when we define union-type subtyping in the more unsound way.  We now e.g.
    // have [{A, B} <: B], where the LHS is surely less informative than the RHS!

    // The more unsound version: any.
    for (Type t : types) {
      if (((TypeImpl) t).internalIsSubtypeOf(type, visitedTypePairs)) {
        return true;
      }
    }
    return false;
    // The less unsound version: all.
    /*
    for (Type t : types) {
      if (!((TypeImpl) t).internalIsSubtypeOf(type, visitedTypePairs)) {
        return false;
      }
    }
    return true;
    */
  }

  /**
   * The more-specific-than test for union types on the RHS is uniform in non-union LHSs. So, other
   * {@code TypeImpl}s can call this method to implement {@code internalIsMoreSpecificThan} for
   * union types.
   * 
   * @param type
   * @param visitedTypePairs
   * @return true if {@code type} is more specific than this union type
   */
  protected boolean internalUnionTypeIsMoreSpecificThan(Type type, boolean withDynamic,
      Set<TypePair> visitedTypePairs) {
    // This implementation does not make sense when [type] is a union type, at least
    // for the "less unsound" version of [internalIsMoreSpecificThan] above.
    if (type instanceof UnionType) {
      throw new IllegalArgumentException("Only non-union types are supported.");
    }

    for (Type t : types) {
      if (((TypeImpl) type).internalIsMoreSpecificThan(t, withDynamic, visitedTypePairs)) {
        return true;
      }
    }
    return false;
  }

  /**
   * The supertype test for union types is uniform in non-union subtypes. So, other {@code TypeImpl}
   * s can call this method to implement {@code internalIsSubtypeOf} for union types.
   * 
   * @param type
   * @param visitedTypePairs
   * @return true if this union type is a super type of {@code type}
   */
  protected boolean internalUnionTypeIsSuperTypeOf(Type type, Set<TypePair> visitedTypePairs) {
    // This implementation does not make sense when [type] is a union type, at least
    // for the "less unsound" version of [internalIsSubtypeOf] above.
    if (type instanceof UnionType) {
      throw new IllegalArgumentException("Only non-union types are supported.");
    }

    for (Type t : types) {
      if (((TypeImpl) type).internalIsSubtypeOf(t, visitedTypePairs)) {
        return true;
      }
    }
    return false;
  }
}
