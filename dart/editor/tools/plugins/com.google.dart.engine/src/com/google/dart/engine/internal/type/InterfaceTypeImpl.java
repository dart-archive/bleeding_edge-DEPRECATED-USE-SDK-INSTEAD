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
import com.google.dart.engine.type.TypeVariableType;
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
   * An empty array of types.
   */
  public static final InterfaceType[] EMPTY_ARRAY = new InterfaceType[0];

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
  public static int computeLongestInheritancePathToObject(InterfaceType type) {
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
  public static Set<InterfaceType> computeSuperinterfaceSet(InterfaceType type) {
    return computeSuperinterfaceSet(type, new HashSet<InterfaceType>());
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
  private static int computeLongestInheritancePathToObject(InterfaceType type, int depth) {
    ClassElement classElement = type.getElement();
    // Object case
    if (classElement.getSupertype() == null) {
      return depth;
    }
    InterfaceType[] superinterfaces = classElement.getInterfaces();
    int longestPath = 1;
    int pathLength;
    if (superinterfaces.length > 0) {
      // loop through each of the superinterfaces recursively calling this method and keeping track
      // of the longest path to return
      for (InterfaceType superinterface : superinterfaces) {
        pathLength = computeLongestInheritancePathToObject(superinterface, depth + 1);
        if (pathLength > longestPath) {
          longestPath = pathLength;
        }
      }
    }
    // finally, perform this same check on the super type
    InterfaceType supertype = classElement.getSupertype();
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
  private static Set<InterfaceType> computeSuperinterfaceSet(InterfaceType type,
      HashSet<InterfaceType> set) {
    Element element = type.getElement();
    if (element != null && element instanceof ClassElement) {
      ClassElement classElement = (ClassElement) element;
      InterfaceType[] superinterfaces = classElement.getInterfaces();
      for (InterfaceType superinterface : superinterfaces) {
        set.add(superinterface);
        computeSuperinterfaceSet(superinterface, set);
      }
      InterfaceType supertype = classElement.getSupertype();
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
    Type dynamicType = DynamicTypeImpl.getInstance();
    if (this == dynamicType || type == dynamicType) {
      return dynamicType;
    }
    // TODO (jwren) opportunity here for a better, faster algorithm if this turns out to be a bottle-neck
    if (type == null || !(type instanceof InterfaceType)) {
      return null;
    }
    // new names to match up with the spec
    InterfaceType i = this;
    InterfaceType j = (InterfaceType) type;

    // compute set of supertypes
    Set<InterfaceType> si = computeSuperinterfaceSet(i);
    Set<InterfaceType> sj = computeSuperinterfaceSet(j);

    // union si with i and sj with j
    si.add(i);
    sj.add(j);

    // compute intersection, reference as set 's'
    si.retainAll(sj);
    Set<InterfaceType> s = si;

    // define the list sn, a list containing the elements from set 's'
    //ArrayList<Type> sn = new ArrayList<Type>(s.size());
    InterfaceType[] sn = s.toArray(new InterfaceType[s.size()]);

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
  public Type getSuperclass() {
    ClassElement classElement = getElement();
    return getElement().getSupertype().substitute(
        typeArguments,
        TypeVariableTypeImpl.getTypes(classElement.getTypeVariables()));
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
    // If J has no direct supertype then it is Object, and Object has no direct supertypes.
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
    if (type == DynamicTypeImpl.getInstance()) {
      return true;
    } else if (!(type instanceof InterfaceType)) {
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
    if (s == DynamicTypeImpl.getInstance()) {
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
    if (type == DynamicTypeImpl.getInstance()) {
      return true;
    } else if (type instanceof TypeVariableType) {
      return true;
    } else if (!(type instanceof InterfaceType)) {
      return false;
    } else if (this.equals(type)) {
      return true;
    }
    InterfaceType typeT = this;
    InterfaceType typeS = (InterfaceType) type;
    ClassElement elementT = getElement();
    if (elementT == null) {
      return false;
    }
    typeT = substitute(typeArguments, TypeVariableTypeImpl.getTypes(elementT.getTypeVariables()));
    if (typeT.equals(typeS)) {
      return true;
    } else if (ObjectUtilities.equals(elementT, typeS.getElement())) {
      // For each of the type arguments return true if all type args from T is a subtype of all
      // types from S.
      Type[] typeTArgs = typeT.getTypeArguments();
      Type[] typeSArgs = typeS.getTypeArguments();
      if (typeTArgs.length != typeSArgs.length) {
        // This case covers the case where two objects are being compared that have a different
        // number of parameterized types.
        return false;
      }
      for (int i = 0; i < typeTArgs.length; i++) {
        // Recursively call isSubtypeOf the type arguments and return false if the T argument is not
        // a subtype of the S argument.
        if (!typeTArgs[i].isSubtypeOf(typeSArgs[i])) {
          return false;
        }
      }
      return true;
    }

    Type supertype = elementT.getSupertype();
    // The type is Object, return false.
    if (supertype == null) {
      return false;
    }
    Type[] interfaceTypes = elementT.getInterfaces();
    for (Type interfaceType : interfaceTypes) {
      if (interfaceType.isSubtypeOf(typeS)) {
        return true;
      }
    }
    Type[] mixinTypes = elementT.getMixins();
    for (Type mixinType : mixinTypes) {
      if (mixinType.equals(typeS)) {
        return true;
      }
    }
    return supertype.isSubtypeOf(typeS);
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
