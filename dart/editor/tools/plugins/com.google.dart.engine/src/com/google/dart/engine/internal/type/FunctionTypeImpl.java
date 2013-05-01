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

import com.google.common.collect.ImmutableMap;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.general.ObjectUtilities;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Instances of the class {@code FunctionTypeImpl} defines the behavior common to objects
 * representing the type of a function, method, constructor, getter, or setter.
 * 
 * @coverage dart.engine.type
 */
public class FunctionTypeImpl extends TypeImpl implements FunctionType {
  /**
   * Return {@code true} if all of the name/type pairs in the first map are equal to the
   * corresponding name/type pairs in the second map. The maps are expected to iterate over their
   * entries in the same order in which those entries were added to the map.
   * 
   * @param firstTypes the first map of name/type pairs being compared
   * @param secondTypes the second map of name/type pairs being compared
   * @return {@code true} if all of the name/type pairs in the first map are equal to the
   *         corresponding name/type pairs in the second map
   */
  private static boolean equals(Map<String, Type> firstTypes, Map<String, Type> secondTypes) {
    if (secondTypes.size() != firstTypes.size()) {
      return false;
    }
    Iterator<Map.Entry<String, Type>> firstIterator = firstTypes.entrySet().iterator();
    Iterator<Map.Entry<String, Type>> secondIterator = firstTypes.entrySet().iterator();
    while (firstIterator.hasNext()) {
      Map.Entry<String, Type> firstEntry = firstIterator.next();
      Map.Entry<String, Type> secondEntry = secondIterator.next();
      if (!firstEntry.getKey().equals(secondEntry.getKey())
          || !firstEntry.getValue().equals(secondEntry.getValue())) {
        return false;
      }
    }
    return true;
  }

  /**
   * Return a map containing the results of using the given argument types and parameter types to
   * perform a substitution on all of the values in the given map. The order of the entries will be
   * preserved.
   * 
   * @param types the types on which a substitution is to be performed
   * @param argumentTypes the argument types for the substitution
   * @param parameterTypes the parameter types for the substitution
   * @return the result of performing the substitution on each of the types
   */
  private static Map<String, Type> substitute(Map<String, Type> types, Type[] argumentTypes,
      Type[] parameterTypes) {
    if (types.isEmpty()) {
      return types;
    }
    LinkedHashMap<String, Type> newTypes = new LinkedHashMap<String, Type>();
    for (Map.Entry<String, Type> entry : types.entrySet()) {
      newTypes.put(entry.getKey(), entry.getValue().substitute(argumentTypes, parameterTypes));
    }
    return newTypes;
  }

  /**
   * An array containing the actual types of the type arguments.
   */
  private Type[] typeArguments = TypeImpl.EMPTY_ARRAY;

  /**
   * An array containing the types of the normal parameters of this type of function. The parameter
   * types are in the same order as they appear in the declaration of the function.
   * 
   * @return the types of the normal parameters of this type of function
   */
  private Type[] normalParameterTypes = TypeImpl.EMPTY_ARRAY;

  /**
   * A table mapping the names of optional (positional) parameters to the types of the optional
   * parameters of this type of function.
   */
  private Type[] optionalParameterTypes = TypeImpl.EMPTY_ARRAY;

  /**
   * A table mapping the names of named parameters to the types of the named parameters of this type
   * of function.
   */
  private Map<String, Type> namedParameterTypes = ImmutableMap.of();

  /**
   * The type of object returned by this type of function.
   */
  private Type returnType = VoidTypeImpl.getInstance();

  /**
   * Initialize a newly created function type to be declared by the given element and to have the
   * given name.
   * 
   * @param element the element representing the declaration of the function type
   */
  public FunctionTypeImpl(ExecutableElement element) {
    super(element, element == null ? null : element.getName());
  }

  /**
   * Initialize a newly created function type to be declared by the given element and to have the
   * given name.
   * 
   * @param element the element representing the declaration of the function type
   */
  public FunctionTypeImpl(FunctionTypeAliasElement element) {
    super(element, element == null ? null : element.getName());
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof FunctionTypeImpl)) {
      return false;
    }
    FunctionTypeImpl otherType = (FunctionTypeImpl) object;
    return ObjectUtilities.equals(getElement(), otherType.getElement())
        && Arrays.equals(normalParameterTypes, otherType.normalParameterTypes)
        && Arrays.equals(optionalParameterTypes, otherType.optionalParameterTypes)
        && equals(namedParameterTypes, otherType.namedParameterTypes)
        && ObjectUtilities.equals(returnType, otherType.returnType);
  }

  @Override
  public Map<String, Type> getNamedParameterTypes() {
    return namedParameterTypes;
  }

  @Override
  public Type[] getNormalParameterTypes() {
    return normalParameterTypes;
  }

  @Override
  public Type[] getOptionalParameterTypes() {
    return optionalParameterTypes;
  }

  @Override
  public Type getReturnType() {
    return returnType;
  }

  @Override
  public Type[] getTypeArguments() {
    return typeArguments;
  }

  @Override
  public int hashCode() {
    Element element = getElement();
    if (element == null) {
      return 0;
    }
    return element.hashCode();
  }

  @Override
  public boolean isSubtypeOf(Type type) {
    // trivial base cases
    if (type == null) {
      return false;
    } else if (this == type || type.isDynamic() || type.isDartCoreFunction()) {
      return true;
    } else if (!(type instanceof FunctionType)) {
      return false;
    } else if (this.equals(type)) {
      return true;
    }
    FunctionType t = this;
    FunctionType s = (FunctionType) type;
    // normal parameter types
    if (t.getNormalParameterTypes().length != s.getNormalParameterTypes().length) {
      return false;
    } else if (t.getNormalParameterTypes().length > 0) {
      Type[] tTypes = t.getNormalParameterTypes();
      Type[] sTypes = s.getNormalParameterTypes();
      for (int i = 0; i < tTypes.length; i++) {
        if (!tTypes[i].isAssignableTo(sTypes[i])) {
          return false;
        }
      }
    }

    // optional parameter types
    if (t.getOptionalParameterTypes().length > 0) {
      Type[] tOpTypes = t.getOptionalParameterTypes();
      Type[] sOpTypes = s.getOptionalParameterTypes();
      // if k >= m is false, return false: the passed function type has more optional parameter types than this
      if (tOpTypes.length < sOpTypes.length) {
        return false;
      }
      for (int i = 0; i < sOpTypes.length; i++) {
        if (!tOpTypes[i].isAssignableTo(sOpTypes[i])) {
          return false;
        }
      }
      if (t.getNamedParameterTypes().size() > 0 || s.getNamedParameterTypes().size() > 0) {
        return false;
      }
    } else if (s.getOptionalParameterTypes().length > 0) {
      return false;
    }

    // named parameter types
    if (t.getNamedParameterTypes().size() > 0) {
      Map<String, Type> namedTypesT = t.getNamedParameterTypes();
      Map<String, Type> namedTypesS = s.getNamedParameterTypes();
      // if k >= m is false, return false: the passed function type has more named parameter types than this
      if (namedTypesT.size() < namedTypesS.size()) {
        return false;
      }
      // Loop through each element in S verifying that T has a matching parameter name and that the
      // corresponding type is assignable to the type in S.
      Iterator<Entry<String, Type>> iteratorS = namedTypesS.entrySet().iterator();
      while (iteratorS.hasNext()) {
        Entry<String, Type> entryS = iteratorS.next();
        Type typeT = namedTypesT.get(entryS.getKey());
        if (typeT == null) {
          return false;
        }
        if (!entryS.getValue().isAssignableTo(typeT)) {
          return false;
        }
      }
    } else if (s.getNamedParameterTypes().size() > 0) {
      return false;
    }
    return s.getReturnType().equals(VoidTypeImpl.getInstance())
        || t.getReturnType().isAssignableTo(s.getReturnType());
  }

  /**
   * Set the mapping of the names of named parameters to the types of the named parameters of this
   * type of function to the given mapping.
   * 
   * @param namedParameterTypes the mapping of the names of named parameters to the types of the
   *          named parameters of this type of function
   */
  public void setNamedParameterTypes(LinkedHashMap<String, Type> namedParameterTypes) {
    this.namedParameterTypes = namedParameterTypes;
  }

  /**
   * Set the types of the normal parameters of this type of function to the types in the given
   * array.
   * 
   * @param normalParameterTypes the types of the normal parameters of this type of function
   */
  public void setNormalParameterTypes(Type[] normalParameterTypes) {
    this.normalParameterTypes = normalParameterTypes;
  }

  /**
   * Set the types of the optional parameters of this type of function to the types in the given
   * array.
   * 
   * @param optionalParameterTypes the types of the optional parameters of this type of function
   */
  public void setOptionalParameterTypes(Type[] optionalParameterTypes) {
    this.optionalParameterTypes = optionalParameterTypes;
  }

  /**
   * Set the type of object returned by this type of function to the given type.
   * 
   * @param returnType the type of object returned by this type of function
   */
  public void setReturnType(Type returnType) {
    this.returnType = returnType;
  }

  /**
   * Set the actual types of the type arguments to the given types.
   * 
   * @param typeArguments the actual types of the type arguments
   */
  public void setTypeArguments(Type[] typeArguments) {
    this.typeArguments = typeArguments;
  }

  @Override
  public FunctionTypeImpl substitute(Type[] argumentTypes) {
    return substitute(argumentTypes, getTypeArguments());
  }

  @Override
  public FunctionTypeImpl substitute(Type[] argumentTypes, Type[] parameterTypes) {
    if (argumentTypes.length != parameterTypes.length) {
      throw new IllegalArgumentException("argumentTypes.length (" + argumentTypes.length
          + ") != parameterTypes.length (" + parameterTypes.length + ")");
    }
    if (argumentTypes.length == 0) {
      return this;
    }
    Element element = getElement();
    FunctionTypeImpl newType = (element instanceof ExecutableElement) ? new FunctionTypeImpl(
        (ExecutableElement) element) : new FunctionTypeImpl((FunctionTypeAliasElement) element);
    newType.setReturnType(returnType.substitute(argumentTypes, parameterTypes));
    newType.setNormalParameterTypes(substitute(normalParameterTypes, argumentTypes, parameterTypes));
    newType.setOptionalParameterTypes(substitute(
        optionalParameterTypes,
        argumentTypes,
        parameterTypes));
    newType.namedParameterTypes = substitute(namedParameterTypes, argumentTypes, parameterTypes);
    return newType;
  }

  @Override
  protected void appendTo(StringBuilder builder) {
    builder.append("(");
    boolean needsComma = false;
    if (normalParameterTypes.length > 0) {
      for (Type type : normalParameterTypes) {
        if (needsComma) {
          builder.append(", ");
        } else {
          needsComma = true;
        }
        ((TypeImpl) type).appendTo(builder);
      }
    }
    if (optionalParameterTypes.length > 0) {
      if (needsComma) {
        builder.append(", ");
        needsComma = false;
      }
      builder.append("[");
      for (Type type : optionalParameterTypes) {
        if (needsComma) {
          builder.append(", ");
        } else {
          needsComma = true;
        }
        ((TypeImpl) type).appendTo(builder);
      }
      builder.append("]");
      needsComma = true;
    }
    if (namedParameterTypes.size() > 0) {
      if (needsComma) {
        builder.append(", ");
        needsComma = false;
      }
      builder.append("{");
      for (Map.Entry<String, Type> entry : namedParameterTypes.entrySet()) {
        if (needsComma) {
          builder.append(", ");
        } else {
          needsComma = true;
        }
        builder.append(entry.getKey());
        builder.append(": ");
        ((TypeImpl) entry.getValue()).appendTo(builder);
      }
      builder.append("}");
      needsComma = true;
    }
    builder.append(") -> ");
    if (returnType == null) {
      builder.append("null");
    } else {
      ((TypeImpl) returnType).appendTo(builder);
    }
  }
}
