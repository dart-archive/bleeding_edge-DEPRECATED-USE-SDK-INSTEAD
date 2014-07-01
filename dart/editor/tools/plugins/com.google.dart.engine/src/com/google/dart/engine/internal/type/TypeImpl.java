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
import com.google.dart.engine.internal.element.ElementPair;
import com.google.dart.engine.type.Type;

import java.util.HashSet;
import java.util.Set;

/**
 * The abstract class {@code TypeImpl} implements the behavior common to objects representing the
 * declared type of elements in the element model.
 * 
 * @coverage dart.engine.type
 */
public abstract class TypeImpl implements Type {
  // TODO (jwren) Move this class to "com.google.dart.engine.utilities.collection"
  public class TypePair {
    private Type firstType;
    private Type secondType;
    private int cachedHashCode;

    TypePair(Type firstType, Type secondType) {
      this.firstType = firstType;
      this.secondType = secondType;
    }

    @Override
    public boolean equals(Object object) {
      if (object == this) {
        return true;
      }
      if (object instanceof TypePair) {
        TypePair typePair = (TypePair) object;
        return firstType.equals(typePair.firstType) && secondType != null
            && secondType.equals(typePair.secondType);
      }
      return false;
    }

    // TODO(jwren) Revisit the hashCode distribution.
    // TODO(jwren) For equals() & hashCode() could we use the implementations in ObjectUtilities or
    // Guava's Objects class?
    @Override
    public int hashCode() {
      if (cachedHashCode == 0) {
        int firstHashCode = 0;
        if (firstType != null) {
          Element firstElement = firstType.getElement();
          firstHashCode = firstElement == null ? 0 : firstElement.hashCode();
        }
        int secondHashCode = 0;
        if (secondType != null) {
          Element secondElement = secondType.getElement();
          secondHashCode = secondElement == null ? 0 : secondElement.hashCode();
        }
        cachedHashCode = firstHashCode + secondHashCode;
      }
      return cachedHashCode;
    }
  }

  protected static boolean equalArrays(Type[] typeArgs1, Type[] typeArgs2,
      Set<ElementPair> visitedElementPairs) {
    if (typeArgs1.length != typeArgs2.length) {
      return false;
    }
    for (int i = 0; i < typeArgs1.length; i++) {
      if (!((TypeImpl) typeArgs1[i]).internalEquals(typeArgs2[i], visitedElementPairs)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Return an array containing the results of using the given argument types and parameter types to
   * perform a substitution on all of the given types.
   * 
   * @param types the types on which a substitution is to be performed
   * @param argumentTypes the argument types for the substitution
   * @param parameterTypes the parameter types for the substitution
   * @return the result of performing the substitution on each of the types
   */
  protected static Type[] substitute(Type[] types, Type[] argumentTypes, Type[] parameterTypes) {
    int length = types.length;
    if (length == 0) {
      return types;
    }
    Type[] newTypes = new Type[length];
    for (int i = 0; i < length; i++) {
      newTypes[i] = types[i].substitute(argumentTypes, parameterTypes);
    }
    return newTypes;
  }

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
  public String getDisplayName() {
    return getName();
  }

  @Override
  public Element getElement() {
    return element;
  }

  @Override
  public Type getLeastUpperBound(Type type) {
    return null;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean isAssignableTo(Type type) {
    return isAssignableTo(type, new HashSet<TypePair>());
  }

  /**
   * Return {@code true} if this type is assignable to the given type. A type <i>T</i> may be
   * assigned to a type <i>S</i>, written <i>T</i> &hArr; <i>S</i>, iff either <i>T</i> <: <i>S</i>
   * or <i>S</i> <: <i>T</i> (Interface Types section of spec).
   * <p>
   * The given set of pairs of types (T1, T2), where each pair indicates that we invoked this method
   * because we are in the process of answering the question of whether T1 is a subtype of T2, is
   * used to prevent infinite loops.
   * 
   * @param type the type being compared with this type
   * @param visitedTypePairs the set of pairs of types used to prevent infinite loops
   * @return {@code true} if this type is assignable to the given type
   */
  public final boolean isAssignableTo(Type type, Set<TypePair> visitedTypePairs) {
    return isSubtypeOf(type, visitedTypePairs)
        || ((TypeImpl) type).isSubtypeOf(this, visitedTypePairs);
  }

  @Override
  public boolean isBottom() {
    return false;
  }

  @Override
  public boolean isDartCoreFunction() {
    return false;
  }

  @Override
  public boolean isDynamic() {
    return false;
  }

  @Override
  public final boolean isMoreSpecificThan(Type type) {
    return isMoreSpecificThan(type, false, new HashSet<TypePair>());
  }

  /**
   * Return {@code true} if this type is more specific than the given type.
   * <p>
   * The given set of pairs of types (T1, T2), where each pair indicates that we invoked this method
   * because we are in the process of answering the question of whether T1 is a subtype of T2, is
   * used to prevent infinite loops.
   * 
   * @param type the type being compared with this type
   * @param withDynamic {@code true} if "dynamic" should be considered as a subtype of any type
   * @param visitedTypePairs the set of pairs of types used to prevent infinite loops
   * @return {@code true} if this type is more specific than the given type
   */
  public final boolean isMoreSpecificThan(Type type, boolean withDynamic,
      Set<TypePair> visitedTypePairs) {
    // If the visitedTypePairs already has the pair (this, type), return false
    TypePair typePair = new TypePair(this, type);
    if (!visitedTypePairs.add(typePair)) {
      return false;
    }
    boolean result = internalIsMoreSpecificThan(type, withDynamic, visitedTypePairs);
    visitedTypePairs.remove(typePair);
    return result;
  }

  @Override
  public boolean isObject() {
    return false;
  }

  @Override
  public final boolean isSubtypeOf(Type type) {
    return isSubtypeOf(type, new HashSet<TypePair>());
  }

  /**
   * Return {@code true} if this type is a subtype of the given type.
   * <p>
   * The given set of pairs of types (T1, T2), where each pair indicates that we invoked this method
   * because we are in the process of answering the question of whether T1 is a subtype of T2, is
   * used to prevent infinite loops.
   * 
   * @param type the type being compared with this type
   * @param visitedTypePairs the set of pairs of types used to prevent infinite loops
   * @return {@code true} if this type is a subtype of the given type
   */
  public final boolean isSubtypeOf(Type type, Set<TypePair> visitedTypePairs) {
    // If the visitedTypePairs already has the pair (this, type), return false
    TypePair typePair = new TypePair(this, type);
    if (!visitedTypePairs.add(typePair)) {
      return false;
    }
    boolean result = internalIsSubtypeOf(type, visitedTypePairs);
    visitedTypePairs.remove(typePair);
    return result;
  }

  @Override
  public boolean isSupertypeOf(Type type) {
    return type.isSubtypeOf(this);
  }

  @Override
  public boolean isVoid() {
    return false;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    appendTo(builder);
    return builder.toString();
  }

  /**
   * Append a textual representation of this type to the given builder.
   * 
   * @param builder the builder to which the text is to be appended
   */
  protected void appendTo(StringBuilder builder) {
    if (name == null) {
      builder.append("<unnamed type>");
    } else {
      builder.append(name);
    }
  }

  protected abstract boolean internalEquals(Object object, Set<ElementPair> visitedElementPairs);

  protected abstract boolean internalIsMoreSpecificThan(Type type, boolean withDynamic,
      Set<TypePair> visitedTypePairs);

  protected abstract boolean internalIsSubtypeOf(Type type, Set<TypePair> visitedTypePairs);
}
