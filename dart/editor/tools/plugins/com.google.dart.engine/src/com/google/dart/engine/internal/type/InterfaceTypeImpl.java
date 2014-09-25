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
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TypeParameterElement;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.ElementPair;
import com.google.dart.engine.internal.element.member.ConstructorMember;
import com.google.dart.engine.internal.element.member.MethodMember;
import com.google.dart.engine.internal.element.member.PropertyAccessorMember;
import com.google.dart.engine.internal.resolver.InheritanceManager;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.type.TypeParameterType;
import com.google.dart.engine.type.UnionType;
import com.google.dart.engine.utilities.general.ObjectUtilities;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Instances of the class {@code InterfaceTypeImpl} defines the behavior common to objects
 * representing the type introduced by either a class or an interface, or a reference to such a
 * type.
 * 
 * @coverage dart.engine.type
 */
public class InterfaceTypeImpl extends TypeImpl implements InterfaceType {
  /**
   * This method computes the longest inheritance path from some passed {@link Type} to Object.
   * 
   * @param type the {@link Type} to compute the longest inheritance path of from the passed
   *          {@link Type} to Object
   * @return the computed longest inheritance path to Object
   * @see InterfaceType#getLeastUpperBound(Type)
   */
  @VisibleForTesting
  public static int computeLongestInheritancePathToObject(InterfaceType type) {
    return computeLongestInheritancePathToObject(type, 0, new HashSet<ClassElement>());
  }

  /**
   * Returns the set of all superinterfaces of the passed {@link Type}.
   * 
   * @param type the {@link Type} to compute the set of superinterfaces of
   * @return the {@link Set} of superinterfaces of the passed {@link Type}
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
   * @param visitedClasses the classes that have already been visited
   * @return the computed longest inheritance path to Object
   * @see #computeLongestInheritancePathToObject(Type)
   * @see #getLeastUpperBound(Type)
   */
  private static int computeLongestInheritancePathToObject(InterfaceType type, int depth,
      HashSet<ClassElement> visitedClasses) {
    ClassElement classElement = type.getElement();
    // Object case
    if (classElement.getSupertype() == null || visitedClasses.contains(classElement)) {
      return depth;
    }
    int longestPath = 1;
    try {
      visitedClasses.add(classElement);
      InterfaceType[] superinterfaces = classElement.getInterfaces();
      int pathLength;
      if (superinterfaces.length > 0) {
        // loop through each of the superinterfaces recursively calling this method and keeping track
        // of the longest path to return
        for (InterfaceType superinterface : superinterfaces) {
          pathLength = computeLongestInheritancePathToObject(
              superinterface,
              depth + 1,
              visitedClasses);
          if (pathLength > longestPath) {
            longestPath = pathLength;
          }
        }
      }
      // finally, perform this same check on the super type
      // TODO(brianwilkerson) Does this also need to add in the number of mixin classes?
      InterfaceType supertype = classElement.getSupertype();
      pathLength = computeLongestInheritancePathToObject(supertype, depth + 1, visitedClasses);
      if (pathLength > longestPath) {
        longestPath = pathLength;
      }
    } finally {
      visitedClasses.remove(classElement);
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
    if (element != null) {
      InterfaceType[] superinterfaces = type.getInterfaces();
      for (InterfaceType superinterface : superinterfaces) {
        if (set.add(superinterface)) {
          computeSuperinterfaceSet(superinterface, set);
        }
      }
      InterfaceType supertype = type.getSuperclass();
      if (supertype != null) {
        if (set.add(supertype)) {
          computeSuperinterfaceSet(supertype, set);
        }
      }
    }
    return set;
  }

  /**
   * Return the intersection of the given sets of types, where intersection is based on the equality
   * of the types themselves.
   * 
   * @param first the first set of types to be intersected
   * @param second the second set of types to be intersected
   * @return the intersection of the given sets of types
   */
  private static InterfaceType[] intersection(Set<InterfaceType> first, Set<InterfaceType> second) {
    Set<InterfaceType> result = new HashSet<InterfaceType>(first);
    result.retainAll(second);
    return result.toArray(new InterfaceType[result.size()]);
  }

  /**
   * Return the "least upper bound" of the given types under the assumption that the types have the
   * same element and differ only in terms of the type arguments. The resulting type is composed by
   * comparing the corresponding type arguments, keeping those that are the same, and using
   * 'dynamic' for those that are different.
   * 
   * @param firstType the first type
   * @param secondType the second type
   * @return the "least upper bound" of the given types
   */
  private static InterfaceType leastUpperBound(InterfaceType firstType, InterfaceType secondType) {
    if (firstType.equals(secondType)) {
      return firstType;
    }
    Type[] firstArguments = firstType.getTypeArguments();
    Type[] secondArguments = secondType.getTypeArguments();
    int argumentCount = firstArguments.length;
    if (argumentCount == 0) {
      return firstType;
    }
    Type[] lubArguments = new Type[argumentCount];
    for (int i = 0; i < argumentCount; i++) {
      //
      // Ideally we would take the least upper bound of the two argument types, but this can cause
      // an infinite recursion (such as when finding the least upper bound of String and num).
      //
      if (firstArguments[i].equals(secondArguments[i])) {
        lubArguments[i] = firstArguments[i];
      }
      if (lubArguments[i] == null) {
        lubArguments[i] = DynamicTypeImpl.getInstance();
      }
    }
    InterfaceTypeImpl lub = new InterfaceTypeImpl(firstType.getElement());
    lub.setTypeArguments(lubArguments);
    return lub;
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
    super(element, element.getDisplayName());
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
    return internalEquals(object, new HashSet<ElementPair>());
  }

  @Override
  public PropertyAccessorElement[] getAccessors() {
    PropertyAccessorElement[] accessors = getElement().getAccessors();
    PropertyAccessorElement[] members = new PropertyAccessorElement[accessors.length];
    for (int i = 0; i < accessors.length; i++) {
      members[i] = PropertyAccessorMember.from(accessors[i], this);
    }
    return members;
  }

  @Override
  public String getDisplayName() {
    String name = getName();
    Type[] typeArguments = getTypeArguments();
    boolean allDynamic = true;
    for (Type type : typeArguments) {
      if (type != null && !type.isDynamic()) {
        allDynamic = false;
        break;
      }
    }
    // If there is at least one non-dynamic type, then list them out
    if (!allDynamic) {
      StringBuilder builder = new StringBuilder();
      builder.append(name);
      builder.append("<");
      for (int i = 0; i < typeArguments.length; i++) {
        if (i != 0) {
          builder.append(", ");
        }
        Type typeArg = typeArguments[i];
        builder.append(typeArg.getDisplayName());
      }
      builder.append(">");
      name = builder.toString();
    }
    return name;
  }

  @Override
  public ClassElement getElement() {
    return (ClassElement) super.getElement();
  }

  @Override
  public PropertyAccessorElement getGetter(String getterName) {
    return PropertyAccessorMember.from(
        ((ClassElementImpl) getElement()).getGetter(getterName),
        this);
  }

  @Override
  public InterfaceType[] getInterfaces() {
    ClassElement classElement = getElement();
    InterfaceType[] interfaces = classElement.getInterfaces();
    TypeParameterElement[] typeParameters = classElement.getTypeParameters();
    Type[] parameterTypes = classElement.getType().getTypeArguments();
    if (typeParameters.length == 0) {
      return interfaces;
    }
    int count = interfaces.length;
    InterfaceType[] typedInterfaces = new InterfaceType[count];
    for (int i = 0; i < count; i++) {
      typedInterfaces[i] = interfaces[i].substitute(typeArguments, parameterTypes);
    }
    return typedInterfaces;
  }

  @Override
  public Type getLeastUpperBound(Type type) {
    // quick check for self
    if (type == this) {
      return this;
    }
    // dynamic
    Type dynamicType = DynamicTypeImpl.getInstance();
    if (this == dynamicType || type == dynamicType) {
      return dynamicType;
    }
    // TODO (jwren) opportunity here for a better, faster algorithm if this turns out to be a bottle-neck
    if (!(type instanceof InterfaceType)) {
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
    InterfaceType[] s = intersection(si, sj);

    // for each element in Set s, compute the largest inheritance path to Object
    int[] depths = new int[s.length];
    int maxDepth = 0;
    for (int n = 0; n < s.length; n++) {
      depths[n] = computeLongestInheritancePathToObject(s[n]);
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
        return s[indexOfLeastUpperBound];
      }
    }

    // illegal state, log and return null- Object at maxDepth == 0 should always return itself as
    // the least upper bound.
    // TODO (jwren) log the error state 
    return null;
  }

  @Override
  public MethodElement getMethod(String methodName) {
    return MethodMember.from(((ClassElementImpl) getElement()).getMethod(methodName), this);
  }

  @Override
  public MethodElement[] getMethods() {
    MethodElement[] methods = getElement().getMethods();
    MethodElement[] members = new MethodElement[methods.length];
    for (int i = 0; i < methods.length; i++) {
      members[i] = MethodMember.from(methods[i], this);
    }
    return members;
  }

  @Override
  public InterfaceType[] getMixins() {
    ClassElement classElement = getElement();
    InterfaceType[] mixins = classElement.getMixins();
    TypeParameterElement[] typeParameters = classElement.getTypeParameters();
    Type[] parameterTypes = classElement.getType().getTypeArguments();
    if (typeParameters.length == 0) {
      return mixins;
    }
    int count = mixins.length;
    InterfaceType[] typedMixins = new InterfaceType[count];
    for (int i = 0; i < count; i++) {
      typedMixins[i] = mixins[i].substitute(typeArguments, parameterTypes);
    }
    return typedMixins;
  }

  @Override
  public PropertyAccessorElement getSetter(String setterName) {
    return PropertyAccessorMember.from(
        ((ClassElementImpl) getElement()).getSetter(setterName),
        this);
  }

  @Override
  public InterfaceType getSuperclass() {
    ClassElement classElement = getElement();
    InterfaceType supertype = classElement.getSupertype();
    if (supertype == null) {
      return null;
    }
    Type[] typeParameters = classElement.getType().getTypeArguments();
    if (typeArguments.length == 0 || typeArguments.length != typeParameters.length) {
      return supertype;
    }
    return supertype.substitute(typeArguments, typeParameters);
  }

  @Override
  public Type[] getTypeArguments() {
    return typeArguments;
  }

  @Override
  public TypeParameterElement[] getTypeParameters() {
    return getElement().getTypeParameters();
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
  public boolean isDartCoreFunction() {
    ClassElement element = getElement();
    if (element == null) {
      return false;
    }
    return element.getName().equals("Function") && element.getLibrary().isDartCore();
  }

  @Override
  public boolean isDirectSupertypeOf(InterfaceType type) {
    InterfaceType i = this;
    InterfaceType j = type;
    ClassElement jElement = j.getElement();
    InterfaceType supertype = jElement.getSupertype();
    //
    // If J has no direct supertype then it is Object, and Object has no direct supertypes.
    //
    if (supertype == null) {
      return false;
    }
    //
    // I is listed in the extends clause of J.
    //
    Type[] jArgs = j.getTypeArguments();
    Type[] jVars = jElement.getType().getTypeArguments();
    supertype = supertype.substitute(jArgs, jVars);
    if (supertype.equals(i)) {
      return true;
    }
    //
    // I is listed in the implements clause of J.
    //
    for (InterfaceType interfaceType : jElement.getInterfaces()) {
      interfaceType = interfaceType.substitute(jArgs, jVars);
      if (interfaceType.equals(i)) {
        return true;
      }
    }
    //
    // I is listed in the with clause of J.
    //
    for (InterfaceType mixinType : jElement.getMixins()) {
      mixinType = mixinType.substitute(jArgs, jVars);
      if (mixinType.equals(i)) {
        return true;
      }
    }
    //
    // J is a mixin application of the mixin of I.
    //
    // TODO(brianwilkerson) Determine whether this needs to be implemented or whether it is covered
    // by the case above.
    return false;
  }

  @Override
  public boolean isObject() {
    return getElement().getSupertype() == null;
  }

  @Override
  public ConstructorElement lookUpConstructor(String constructorName, LibraryElement library) {
    // prepare base ConstructorElement
    ConstructorElement constructorElement;
    if (constructorName == null) {
      constructorElement = getElement().getUnnamedConstructor();
    } else {
      constructorElement = getElement().getNamedConstructor(constructorName);
    }
    // not found or not accessible
    if (constructorElement == null || !constructorElement.isAccessibleIn(library)) {
      return null;
    }
    // return member
    return ConstructorMember.from(constructorElement, this);
  }

  @Override
  public PropertyAccessorElement lookUpGetter(String getterName, LibraryElement library) {
    PropertyAccessorElement element = getGetter(getterName);
    if (element != null && element.isAccessibleIn(library)) {
      return element;
    }
    return lookUpGetterInSuperclass(getterName, library);
  }

  @Override
  public PropertyAccessorElement lookUpGetterInSuperclass(String getterName, LibraryElement library) {
    for (InterfaceType mixin : getMixins()) {
      PropertyAccessorElement element = mixin.getGetter(getterName);
      if (element != null && element.isAccessibleIn(library)) {
        return element;
      }
    }
    HashSet<ClassElement> visitedClasses = new HashSet<ClassElement>();
    InterfaceType supertype = getSuperclass();
    ClassElement supertypeElement = supertype == null ? null : supertype.getElement();
    while (supertype != null && !visitedClasses.contains(supertypeElement)) {
      visitedClasses.add(supertypeElement);
      PropertyAccessorElement element = supertype.getGetter(getterName);
      if (element != null && element.isAccessibleIn(library)) {
        return element;
      }
      for (InterfaceType mixin : supertype.getMixins()) {
        element = mixin.getGetter(getterName);
        if (element != null && element.isAccessibleIn(library)) {
          return element;
        }
      }
      supertype = supertype.getSuperclass();
      supertypeElement = supertype == null ? null : supertype.getElement();
    }
    return null;
  }

  @Override
  public MethodElement lookUpMethod(String methodName, LibraryElement library) {
    MethodElement element = getMethod(methodName);
    if (element != null && element.isAccessibleIn(library)) {
      return element;
    }
    return lookUpMethodInSuperclass(methodName, library);
  }

  @Override
  public MethodElement lookUpMethodInSuperclass(String methodName, LibraryElement library) {
    for (InterfaceType mixin : getMixins()) {
      MethodElement element = mixin.getMethod(methodName);
      if (element != null && element.isAccessibleIn(library)) {
        return element;
      }
    }
    HashSet<ClassElement> visitedClasses = new HashSet<ClassElement>();
    InterfaceType supertype = getSuperclass();
    ClassElement supertypeElement = supertype == null ? null : supertype.getElement();
    while (supertype != null && !visitedClasses.contains(supertypeElement)) {
      visitedClasses.add(supertypeElement);
      MethodElement element = supertype.getMethod(methodName);
      if (element != null && element.isAccessibleIn(library)) {
        return element;
      }
      for (InterfaceType mixin : supertype.getMixins()) {
        element = mixin.getMethod(methodName);
        if (element != null && element.isAccessibleIn(library)) {
          return element;
        }
      }
      supertype = supertype.getSuperclass();
      supertypeElement = supertype == null ? null : supertype.getElement();
    }
    return null;
  }

  @Override
  public PropertyAccessorElement lookUpSetter(String setterName, LibraryElement library) {
    PropertyAccessorElement element = getSetter(setterName);
    if (element != null && element.isAccessibleIn(library)) {
      return element;
    }
    return lookUpSetterInSuperclass(setterName, library);
  }

  @Override
  public PropertyAccessorElement lookUpSetterInSuperclass(String setterName, LibraryElement library) {
    for (InterfaceType mixin : getMixins()) {
      PropertyAccessorElement element = mixin.getSetter(setterName);
      if (element != null && element.isAccessibleIn(library)) {
        return element;
      }
    }
    HashSet<ClassElement> visitedClasses = new HashSet<ClassElement>();
    InterfaceType supertype = getSuperclass();
    ClassElement supertypeElement = supertype == null ? null : supertype.getElement();
    while (supertype != null && !visitedClasses.contains(supertypeElement)) {
      visitedClasses.add(supertypeElement);
      PropertyAccessorElement element = supertype.getSetter(setterName);
      if (element != null && element.isAccessibleIn(library)) {
        return element;
      }
      for (InterfaceType mixin : supertype.getMixins()) {
        element = mixin.getSetter(setterName);
        if (element != null && element.isAccessibleIn(library)) {
          return element;
        }
      }
      supertype = supertype.getSuperclass();
      supertypeElement = supertype == null ? null : supertype.getElement();
    }
    return null;
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
    if (argumentTypes.length == 0 || typeArguments.length == 0) {
      return this;
    }
    Type[] newTypeArguments = substitute(typeArguments, argumentTypes, parameterTypes);
    if (Arrays.equals(newTypeArguments, typeArguments)) {
      return this;
    }
    InterfaceTypeImpl newType = new InterfaceTypeImpl(getElement());
    newType.setTypeArguments(newTypeArguments);
    return newType;
  }

  @Override
  protected void appendTo(StringBuilder builder) {
    builder.append(getName());
    int argumentCount = typeArguments.length;
    if (argumentCount > 0) {
      builder.append("<");
      for (int i = 0; i < argumentCount; i++) {
        if (i > 0) {
          builder.append(", ");
        }
        ((TypeImpl) typeArguments[i]).appendTo(builder);
      }
      builder.append(">");
    }
  }

  @Override
  protected boolean internalEquals(Object object, Set<ElementPair> visitedElementPairs) {
    if (!(object instanceof InterfaceTypeImpl)) {
      return false;
    }
    InterfaceTypeImpl otherType = (InterfaceTypeImpl) object;
    return ObjectUtilities.equals(getElement(), otherType.getElement())
        && TypeImpl.equalArrays(typeArguments, otherType.typeArguments, visitedElementPairs);
  }

  @Override
  protected boolean internalIsMoreSpecificThan(Type type, boolean withDynamic,
      Set<TypePair> visitedTypePairs) {
    //
    // S is dynamic.
    // The test to determine whether S is dynamic is done here because dynamic is not an instance of
    // InterfaceType.
    //
    if (type == DynamicTypeImpl.getInstance()) {
      return true;
    } else if ((type instanceof UnionType)) {
      return ((UnionTypeImpl) type).internalUnionTypeIsLessSpecificThan(
          this,
          withDynamic,
          visitedTypePairs);
    } else if (!(type instanceof InterfaceType)) {
      return false;
    }
    return isMoreSpecificThan(
        (InterfaceType) type,
        new HashSet<ClassElement>(),
        withDynamic,
        visitedTypePairs);
  }

  @Override
  protected boolean internalIsSubtypeOf(Type type, Set<TypePair> visitedTypePairs) {
    //
    // T is a subtype of S, written T <: S, iff [bottom/dynamic]T << S
    //
    if (type.isDynamic()) {
      return true;
    } else if (type instanceof TypeParameterType) {
      return false;
    } else if (type instanceof UnionType) {
      return ((UnionTypeImpl) type).internalUnionTypeIsSuperTypeOf(this, visitedTypePairs);
    } else if (type instanceof FunctionType) {
      // This implementation assumes transitivity
      // for function type subtyping on the RHS, but a literal reading
      // of the spec does not specify this. More precisely: if T <: F1 and F1 <: F2 and
      // F1 and F2 are function types, then we assume T <: F2.
      //
      // From the Function Types section of the spec:
      //
      //   If a type I includes an instance method named call(), and the type of call()
      //   is the function type F, then I is considered to be a subtype of F.
      //
      // However, the section on Interface Types says
      //
      //   T is a subtype of S, written T <: S, iff [bottom/dynamic]T << S.
      //
      // after giving rules for << (pronounced "more specific than"). However, the "only if"
      // direction of the "iff"
      // in the definition of <: seems to be contradicted by the special case <: rule
      // quoted from the Function Types section: I see no rule for << which tells us that
      // I << F if I has call() at type F.
      //
      // After defining <: , the spec then
      // emphasizes that unlike the relation <<, the relation <: is not transitive in general:
      //
      //   Note that <: is not a partial order on types, it is only binary relation on types.
      //   This is because <: is not transitive. If it was, the subtype rule would have a cycle.
      //   For example: List <: List<String> and List<int> <: List, but List<int> is not a subtype
      //   of List<String>. Although <: is not a partial order on types, it does contain a partial
      //   order, namely <<. This means that, barring raw types, intuition about classical subtype
      //   rules does apply.
      //
      // There is no other occurrence of the word "raw" in relation to types in the spec that I can
      // find, but presumably it's a reference to
      //
      //   http://docs.oracle.com/javase/tutorial/java/generics/rawTypes.html
      //
      // so e.g. non-generic types are never raw. As pointed out by paulberry, it's not clear
      // whether a type like T<int, dynamic> should be considered raw or not. On the one hand, it
      // doesn't correspond to a "raw"-in-the-Java-sense occurrence of T, which would instead
      // be T<dynamic, dynamic>; on the other hand, it's treated differently by <: and << when
      // occurring on the left hand side.
      ClassElement element = getElement();
      InheritanceManager manager = new InheritanceManager(element.getLibrary());
      FunctionType callType = manager.lookupMemberType(this, "call");
      if (callType != null) {
        // A more literal reading of the spec would give something like
        //
        //  return callType.equals(type)
        //
        // here, but that causes 101 errors in the external tests
        // (tools/test.py --mode release --compiler dartanalyzer --runtime none).
        return callType.isSubtypeOf(type);
      }
      return false;
    } else if (!(type instanceof InterfaceType)) {
      return false;
    } else if (this.equals(type)) {
      return true;
    }
    return isSubtypeOf((InterfaceType) type, new HashSet<ClassElement>(), visitedTypePairs);
  }

  // TODO(jwren) Remove "visitedClasses" parameter, as the logic for "visitedTypePairs" should
  // prevent a larger set of infinite loops
  private boolean isMoreSpecificThan(InterfaceType s, HashSet<ClassElement> visitedClasses,
      boolean withDynamic, Set<TypePair> visitedTypePairs) {
    //
    // A type T is more specific than a type S, written T << S,  if one of the following conditions
    // is met:
    //
    // Reflexivity: T is S.
    //
    if (this.equals(s)) {
      return true;
    }

    //
    // T is bottom. (This case is handled by the class BottomTypeImpl.)
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
        if (!((TypeImpl) tArguments[i]).isMoreSpecificThan(
            sArguments[i],
            withDynamic,
            visitedTypePairs)) {
          return false;
        }
      }
      return true;
    }

    //
    // Transitivity: T << U and U << S.
    //
    // First check for infinite loops
    ClassElement element = getElement();
    if (element == null || visitedClasses.contains(element)) {
      return false;
    }
    visitedClasses.add(element);
    // Iterate over all of the types U that are more specific than T because they are direct
    // supertypes of T and return true if any of them are more specific than S.
    InterfaceType supertype = getSuperclass();
    if (supertype != null
        && ((InterfaceTypeImpl) supertype).isMoreSpecificThan(
            s,
            visitedClasses,
            withDynamic,
            visitedTypePairs)) {
      return true;
    }
    for (InterfaceType interfaceType : getInterfaces()) {
      if (((InterfaceTypeImpl) interfaceType).isMoreSpecificThan(
          s,
          visitedClasses,
          withDynamic,
          visitedTypePairs)) {
        return true;
      }
    }
    for (InterfaceType mixinType : getMixins()) {
      if (((InterfaceTypeImpl) mixinType).isMoreSpecificThan(
          s,
          visitedClasses,
          withDynamic,
          visitedTypePairs)) {
        return true;
      }
    }
    return false;
  }

  private boolean isSubtypeOf(InterfaceType type, HashSet<ClassElement> visitedClasses,
      Set<TypePair> visitedTypePairs) {
    InterfaceType typeT = this;
    InterfaceType typeS = type;
    ClassElement elementT = getElement();
    if (elementT == null || visitedClasses.contains(elementT)) {
      return false;
    }
    visitedClasses.add(elementT);

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
        if (!((TypeImpl) typeTArgs[i]).isSubtypeOf(typeSArgs[i], visitedTypePairs)) {
          return false;
        }
      }
      return true;
    } else if (typeS.isDartCoreFunction() && elementT.getMethod("call") != null) {
      return true;
    }

    InterfaceType supertype = getSuperclass();
    // The type is Object, return false.
    if (supertype != null
        && ((InterfaceTypeImpl) supertype).isSubtypeOf(typeS, visitedClasses, visitedTypePairs)) {
      return true;
    }
    InterfaceType[] interfaceTypes = getInterfaces();
    for (InterfaceType interfaceType : interfaceTypes) {
      if (((InterfaceTypeImpl) interfaceType).isSubtypeOf(typeS, visitedClasses, visitedTypePairs)) {
        return true;
      }
    }
    InterfaceType[] mixinTypes = getMixins();
    for (InterfaceType mixinType : mixinTypes) {
      if (((InterfaceTypeImpl) mixinType).isSubtypeOf(typeS, visitedClasses, visitedTypePairs)) {
        return true;
      }
    }
    return false;
  }
}
