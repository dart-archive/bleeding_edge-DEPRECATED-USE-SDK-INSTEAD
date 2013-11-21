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

import java.util.HashSet;
import java.util.Set;

/**
 * The abstract class {@code TypeImpl} implements the behavior common to objects representing the
 * declared type of elements in the element model.
 * 
 * @coverage dart.engine.type
 */
public abstract class TypeImpl implements Type {
  public class TypePair {

    private Type firstType;

    private Type secondType;

    TypePair(Type firstType, Type secondType) {
      this.firstType = firstType;
      this.secondType = secondType;
    }

    @Override
    public boolean equals(Object object) {
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
      int firstHashCode = 0;
      if (firstType != null) {
        firstHashCode = firstType.getElement() == null ? 0 : firstType.getElement().hashCode();
      }
      int secondHashCode = 0;
      if (secondType != null) {
        secondHashCode = secondType.getElement() == null ? 0 : secondType.getElement().hashCode();
      }
      return firstHashCode + secondHashCode;
    }
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

  @Override
  public boolean isAssignableTo(Type type, Set<TypePair> visitedTypePairs) {
    return this.isSubtypeOf(type, visitedTypePairs) || type.isSubtypeOf(this, visitedTypePairs);
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

  @Override
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

  @Override
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

  protected abstract boolean internalIsMoreSpecificThan(Type type, boolean withDynamic,
      Set<TypePair> visitedTypePairs);

  protected abstract boolean internalIsSubtypeOf(Type type, Set<TypePair> visitedTypePairs);
}
