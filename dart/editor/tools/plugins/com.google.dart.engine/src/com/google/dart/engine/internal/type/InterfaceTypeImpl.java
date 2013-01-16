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

import com.google.common.annotations.VisibleForTesting;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.general.ObjectUtilities;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Instances of the class {@code InterfaceTypeImpl} defines the behavior common to objects
 * representing the type introduced by either a class or an interface, or a reference to such a
 * type.
 */
public class InterfaceTypeImpl extends TypeImpl implements InterfaceType {

  /**
   * This method computes the longest inheritance path from some passed {@link Type} to Object.
   * 
   * @param type the {@link Type} to compute the longest inheritance path of from the passed
   *          {@link Type} to Object
   * @return the computed longest inheritance path to Object
   * @see #computeLongestInheritancePathToObject(Type, int)
   * @see InterfaceType#getLeastUpperBound(Type)
   */
  @VisibleForTesting
  public static int computeLongestInheritancePathToObject(Type type) {
    return computeLongestInheritancePathToObject(type, 0);
  }

  /**
   * Returns the set of all superinterfaces of the passed {@link Type}.
   * 
   * @param type the {@link Type} to compute the set of superinterfaces of
   * @return the {@link Set} of superinterfaces of the passed {@link Type}
   * @see #computeSuperinterfaceSet(Type, HashSet)
   * @see #getLeastUpperBound(Type)
   */
  @VisibleForTesting
  public static Set<Type> computeSuperinterfaceSet(Type type) {
    return computeSuperinterfaceSet(type, new HashSet<Type>());
  }

  /**
   * This method computes the longest inheritance path from some passed {@link Type} to Object. This
   * method calls itself recursively, callers should use the public method
   * {@link #computeLongestInheritancePathToObject(Type)}.
   * 
   * @param type the {@link Type} to compute the longest inheritance path of from the passed
   *          {@link Type} to Object
   * @param depth a field used recursively
   * @return the computed longest inheritance path to Object
   * @see #computeLongestInheritancePathToObject(Type)
   * @see #getLeastUpperBound(Type)
   */
  private static int computeLongestInheritancePathToObject(Type type, int depth) {
    if (!(type instanceof InterfaceType)) {
      return 0;
    }
    ClassElement classElement = ((InterfaceType) type).getElement();
    // Object case
    if (classElement.getSupertype() == null) {
      return depth;
    }
    Type[] superinterfaces = classElement.getInterfaces();
    int longestPath = 1;
    int pathLength;
    if (superinterfaces.length > 0) {
      // loop through each of the superinterfaces recursively calling this method and keeping track
      // of the longest path to return
      for (Type superinterface : superinterfaces) {
        pathLength = computeLongestInheritancePathToObject(superinterface, depth + 1);
        if (pathLength > longestPath) {
          longestPath = pathLength;
        }
      }
    }
    // finally, perform this same check on the super type
    Type supertype = classElement.getSupertype();
    pathLength = computeLongestInheritancePathToObject(supertype, depth + 1);
    if (pathLength > longestPath) {
      longestPath = pathLength;
    }
    return longestPath;
  }

  /**
   * Returns the set of all superinterfaces of the passed {@link Type}. This is a recursive method,
   * callers should call the public {@link #computeSuperinterfaceSet(Type)}.
   * 
   * @param type the {@link Type} to compute the set of superinterfaces of
   * @param set a {@link HashSet} used recursively by this method
   * @return the {@link Set} of superinterfaces of the passed {@link Type}
   * @see #computeSuperinterfaceSet(Type)
   * @see #getLeastUpperBound(Type)
   */
  private static Set<Type> computeSuperinterfaceSet(Type type, HashSet<Type> set) {
    Element element = type.getElement();
    if (element != null && element instanceof ClassElement) {
      ClassElement classElement = (ClassElement) element;
      Type[] superinterfaces = classElement.getInterfaces();
      for (Type superinterface : superinterfaces) {
        set.add(superinterface);
        computeSuperinterfaceSet(superinterface, set);
      }
      Type supertype = classElement.getSupertype();
      if (supertype != null) {
        set.add(supertype);
        computeSuperinterfaceSet(supertype, set);
      }
    }
    return set;
  }

  /**
   * An array containing the actual types of the type arguments.
   */
  private Type[] typeArguments = TypeImpl.EMPTY_ARRAY;

  /**
   * The instance representing the type {@code dynamic}.
   */
  private static final InterfaceTypeImpl DYNAMIC_TYPE = new InterfaceTypeImpl("dynamic");

  /**
   * Return a shared instance of this class representing the type {@code dynamic}.
   * 
   * @return an instance of this class representing the type {@code dynamic}
   */
  public static InterfaceTypeImpl getDynamic() {
    return DYNAMIC_TYPE;
  }

  /**
   * Initialize a newly created type to be declared by the given element.
   * 
   * @param element the element representing the declaration of the type
   */
  public InterfaceTypeImpl(ClassElement element) {
    super(element, element.getName());
  }

  /**
   * Initialize a newly created type to have the given name. This constructor should only be used in
   * cases where there is no declaration of the type.
   * 
   * @param name the name of the type
   */
  private InterfaceTypeImpl(String name) {
    super(null, name);
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof InterfaceTypeImpl)) {
      return false;
    }
    InterfaceTypeImpl otherType = (InterfaceTypeImpl) object;
    return ObjectUtilities.equals(getElement(), otherType.getElement())
        && Arrays.equals(typeArguments, otherType.typeArguments);
  }

  @Override
  public ClassElement getElement() {
    return (ClassElement) super.getElement();
  }

  @Override
  public Type getLeastUpperBound(Type type) {
    // TODO (jwren) opportunity here for a better, faster algorithm if this turns out to be a bottle-neck
    if (type == null || !(type instanceof InterfaceType)) {
      return null;
    }
    // new names to match up with the spec
    InterfaceType i = this;
    InterfaceType j = (InterfaceType) type;

    // compute set of supertypes
    Set<Type> si = computeSuperinterfaceSet(i);
    Set<Type> sj = computeSuperinterfaceSet(j);

    // union si with i and sj with j
    si.add(i);
    sj.add(j);

    // compute intersection, reference as set 's'
    si.retainAll(sj);
    Set<Type> s = si;

    // define the list sn, a list containing the elements from set 's'
    //ArrayList<Type> sn = new ArrayList<Type>(s.size());
    Type[] sn = s.toArray(new Type[s.size()]);

    // for each element in Set sn, compute the largest inheritance path to Object
    int[] depths = new int[sn.length];
    int maxDepth = 0;
    for (int n = 0; n < sn.length; n++) {
      depths[n] = computeLongestInheritancePathToObject(sn[n]);
      if (depths[n] > maxDepth) {
        maxDepth = depths[n];
      }
    }

    // ensure that the currently computed maxDepth is unique,
    // otherwise, decrement and test for uniqueness again
    for (; maxDepth >= 0; maxDepth--) {
      int indexOfLeastUpperBound = -1;
      int numberOfTypesAtMaxDepth = 0;
      for (int m = 0; m < depths.length; m++) {
        if (depths[m] == maxDepth) {
          numberOfTypesAtMaxDepth++;
          indexOfLeastUpperBound = m;
        }
      }
      if (numberOfTypesAtMaxDepth == 1) {
        return sn[indexOfLeastUpperBound];
      }
    }

    // illegal state, log and return null- Object at maxDepth == 0 should always return itself as
    // the least upper bound.
    // TODO (jwren) log the error state 
    return null;
  }

  @Override
  public Type[] getTypeArguments() {
    return typeArguments;
  }

  @Override
  public int hashCode() {
    ClassElement element = getElement();
    if (element == null) {
      return 0;
    }
    return element.hashCode();
  }

  @Override
  public boolean isDirectSupertypeOf(InterfaceType type) {
    ClassElement i = getElement();
    ClassElement j = type.getElement();
    Type supertype = j.getSupertype();
    //
    // If J is Object then it has no direct supertype.
    //
    if (supertype == null) {
      return false;
    }
    //
    // I is listed in the extends clause of J.
    //
    ClassElement supertypeElement = (ClassElement) supertype.getElement();
    if (supertypeElement.equals(i)) {
      return true;
    }
    //
    // I is listed in the implements clause of J.
    //
    for (Type interfaceType : j.getInterfaces()) {
      if (interfaceType.equals(i)) {
        return true;
      }
    }
    //
    // I is listed in the with clause of J.
    //
    for (Type mixinType : j.getMixins()) {
      if (mixinType.equals(i)) {
        return true;
      }
    }
    //
    // J is a mixin application of the mixin of I.
    //
    // TODO(brianwilkerson) Implement this.
    return false;
  }

  @Override
  public boolean isMoreSpecificThan(Type type) {
    if (!(type instanceof InterfaceType)) {
      return false;
    }
    InterfaceType s = (InterfaceType) type;
    //
    // A type T is more specific than a type S, written T << S,  if one of the following conditions
    // is met:
    //
    //
    // Reflexivity: T is S.
    //
    if (this.equals(s)) {
      return true;
    }
    //
    // T is bottom.
    //
    // This case is handled by the class BottomTypeImpl.
    //
    // S is dynamic.
    //
    if (s.equals(DYNAMIC_TYPE)) {
      return true;
    }
    //
    // Direct supertype: S is a direct supertype of T.
    //
    if (s.isDirectSupertypeOf(this)) {
      return true;
    }
    //
    // Covariance: T is of the form I<T1, ..., Tn> and S is of the form I<S1, ..., Sn> and Ti << Si, 1 <= i <= n.
    //
    ClassElement tElement = getElement();
    ClassElement sElement = s.getElement();
    if (tElement.equals(sElement)) {
      Type[] tArguments = getTypeArguments();
      Type[] sArguments = s.getTypeArguments();
      if (tArguments.length != sArguments.length) {
        return false;
      }
      for (int i = 0; i < tArguments.length; i++) {
        if (!tArguments[i].isMoreSpecificThan(sArguments[i])) {
          return false;
        }
      }
      return true;
    }
    //
    // Transitivity: T << U and U << S.
    //
    if (getElement().getSupertype() == null) {
      return false;
    }
    return getElement().getSupertype().isMoreSpecificThan(type);
  }

  @Override
  public boolean isSubtypeOf(Type type) {
    //
    // T is a subtype of S, written T <: S, iff [bottom/dynamic]T << S
    //
    // TODO(brianwilkerson) This is an approximation that needs to be fixed once the type
    // substitution operation is implemented.
    if (!(type instanceof InterfaceType)) {
      return false;
    } else if (this.equals(type)) {
      return true;
    }
    ClassElement element = getElement();
    if (element == null) {
      return false;
    }
    Type supertype = element.getSupertype();
    if (supertype == null) {
      // TODO(brianwilkerson) Figure out whether the type is Object and return true if it is.
      return false;
    }
    return supertype.isSubtypeOf(type);
  }

  /**
   * Set the actual types of the type arguments to those in the given array.
   * 
   * @param typeArguments the actual types of the type arguments
   */
  public void setTypeArguments(Type[] typeArguments) {
    this.typeArguments = typeArguments;
  }

  @Override
  public InterfaceTypeImpl substitute(Type[] argumentTypes) {
    return substitute(argumentTypes, getTypeArguments());
  }

  @Override
  public InterfaceTypeImpl substitute(Type[] argumentTypes, Type[] parameterTypes) {
    if (argumentTypes.length != parameterTypes.length) {
      throw new IllegalArgumentException("argumentTypes.length (" + argumentTypes.length
          + ") != parameterTypes.length (" + parameterTypes.length + ")");
    }
    if (argumentTypes.length == 0) {
      return this;
    }
    InterfaceTypeImpl newType = new InterfaceTypeImpl(getElement());
    newType.setTypeArguments(substitute(typeArguments, argumentTypes, parameterTypes));
    return newType;
  }
}
