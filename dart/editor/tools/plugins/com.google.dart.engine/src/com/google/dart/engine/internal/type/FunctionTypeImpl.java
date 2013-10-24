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

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.TypeParameterElement;
import com.google.dart.engine.internal.element.TypeParameterElementImpl;
import com.google.dart.engine.internal.element.member.ParameterMember;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.dart.ParameterKind;
import com.google.dart.engine.utilities.general.ObjectUtilities;

import java.util.ArrayList;
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
    Iterator<Map.Entry<String, Type>> secondIterator = secondTypes.entrySet().iterator();
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
   * An array containing the actual types of the type arguments.
   */
  private Type[] typeArguments = TypeImpl.EMPTY_ARRAY;

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
        && Arrays.equals(getNormalParameterTypes(), otherType.getNormalParameterTypes())
        && Arrays.equals(getOptionalParameterTypes(), otherType.getOptionalParameterTypes())
        && equals(getNamedParameterTypes(), otherType.getNamedParameterTypes())
        && ObjectUtilities.equals(getReturnType(), otherType.getReturnType());
  }

  @Override
  public String getDisplayName() {
    String name = getName();
    if (name == null || name.length() == 0) {
      // TODO(brianwilkerson) Determine whether function types should ever have an empty name.
      Type[] normalParameterTypes = getNormalParameterTypes();
      Type[] optionalParameterTypes = getOptionalParameterTypes();
      Map<String, Type> namedParameterTypes = getNamedParameterTypes();
      Type returnType = getReturnType();
      StringBuilder builder = new StringBuilder();
      builder.append("(");
      boolean needsComma = false;
      if (normalParameterTypes.length > 0) {
        for (Type type : normalParameterTypes) {
          if (needsComma) {
            builder.append(", ");
          } else {
            needsComma = true;
          }
          builder.append(type.getDisplayName());
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
          builder.append(type.getDisplayName());
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
          builder.append(entry.getValue().getDisplayName());
        }
        builder.append("}");
        needsComma = true;
      }
      builder.append(") -> ");
      if (returnType == null) {
        builder.append("null");
      } else {
        builder.append(returnType.getDisplayName());
      }
      name = builder.toString();
    }
    return name;
  }

  @Override
  public Map<String, Type> getNamedParameterTypes() {
    LinkedHashMap<String, Type> namedParameterTypes = new LinkedHashMap<String, Type>();
    ParameterElement[] parameters = getBaseParameters();
    if (parameters.length == 0) {
      return namedParameterTypes;
    }
    Type[] typeParameters = TypeParameterTypeImpl.getTypes(getTypeParameters());
    for (ParameterElement parameter : parameters) {
      if (parameter.getParameterKind() == ParameterKind.NAMED) {
        namedParameterTypes.put(
            parameter.getName(),
            parameter.getType().substitute(typeArguments, typeParameters));
      }
    }
    return namedParameterTypes;
  }

  @Override
  public Type[] getNormalParameterTypes() {
    ParameterElement[] parameters = getBaseParameters();
    if (parameters.length == 0) {
      return TypeImpl.EMPTY_ARRAY;
    }
    Type[] typeParameters = TypeParameterTypeImpl.getTypes(getTypeParameters());
    ArrayList<Type> types = new ArrayList<Type>();
    for (ParameterElement parameter : parameters) {
      if (parameter.getParameterKind() == ParameterKind.REQUIRED) {
        types.add(parameter.getType().substitute(typeArguments, typeParameters));
      }
    }
    return types.toArray(new Type[types.size()]);
  }

  @Override
  public Type[] getOptionalParameterTypes() {
    ParameterElement[] parameters = getBaseParameters();
    if (parameters.length == 0) {
      return TypeImpl.EMPTY_ARRAY;
    }
    Type[] typeParameters = TypeParameterTypeImpl.getTypes(getTypeParameters());
    ArrayList<Type> types = new ArrayList<Type>();
    for (ParameterElement parameter : parameters) {
      if (parameter.getParameterKind() == ParameterKind.POSITIONAL) {
        types.add(parameter.getType().substitute(typeArguments, typeParameters));
      }
    }
    return types.toArray(new Type[types.size()]);
  }

  @Override
  public ParameterElement[] getParameters() {
    ParameterElement[] baseParameters = getBaseParameters();
    // no parameters, quick return
    int parameterCount = baseParameters.length;
    if (parameterCount == 0) {
      return baseParameters;
    }
    // create specialized parameters
    ParameterElement[] specializedParameters = new ParameterElement[parameterCount];
    for (int i = 0; i < parameterCount; i++) {
      specializedParameters[i] = ParameterMember.from(baseParameters[i], this);
    }
    return specializedParameters;
  }

  @Override
  public Type getReturnType() {
    Type baseReturnType = getBaseReturnType();
    if (baseReturnType == null) {
      // TODO(brianwilkerson) This is a patch. The return type should never be null and we need to
      // understand why it is and fix it.
      return DynamicTypeImpl.getInstance();
    }
    return baseReturnType.substitute(
        typeArguments,
        TypeParameterTypeImpl.getTypes(getTypeParameters()));
  }

  @Override
  public Type[] getTypeArguments() {
    return typeArguments;
  }

  @Override
  public TypeParameterElement[] getTypeParameters() {
    Element element = getElement();
    if (element instanceof FunctionTypeAliasElement) {
      return ((FunctionTypeAliasElement) element).getTypeParameters();
    }
    ClassElement definingClass = element.getAncestor(ClassElement.class);
    if (definingClass != null) {
      return definingClass.getTypeParameters();
    }
    return TypeParameterElementImpl.EMPTY_ARRAY;
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
  public boolean isAssignableTo(Type type) {
    return this.isSubtypeOf(type);
  }

  @Override
  public boolean isMoreSpecificThan(Type type) {
    // trivial base cases
    if (type == null) {
      return false;
    } else if (this == type || type.isDynamic() || type.isDartCoreFunction() || type.isObject()) {
      return true;
    } else if (!(type instanceof FunctionType)) {
      return false;
    } else if (this.equals(type)) {
      return true;
    }
    FunctionType t = this;
    FunctionType s = (FunctionType) type;

    Type[] tTypes = t.getNormalParameterTypes();
    Type[] tOpTypes = t.getOptionalParameterTypes();
    Type[] sTypes = s.getNormalParameterTypes();
    Type[] sOpTypes = s.getOptionalParameterTypes();

    // If one function has positional and the other has named parameters, return false.
    if ((sOpTypes.length > 0 && t.getNamedParameterTypes().size() > 0)
        || (tOpTypes.length > 0 && s.getNamedParameterTypes().size() > 0)) {
      return false;
    }

    // named parameters case
    if (t.getNamedParameterTypes().size() > 0) {
      // check that the number of required parameters are equal, and check that every t_i is
      // more specific than every s_i
      if (t.getNormalParameterTypes().length != s.getNormalParameterTypes().length) {
        return false;
      } else if (t.getNormalParameterTypes().length > 0) {
        for (int i = 0; i < tTypes.length; i++) {
          if (!tTypes[i].isMoreSpecificThan(sTypes[i])) {
            return false;
          }
        }
      }
      Map<String, Type> namedTypesT = t.getNamedParameterTypes();
      Map<String, Type> namedTypesS = s.getNamedParameterTypes();
      // if k >= m is false, return false: the passed function type has more named parameter types than this
      if (namedTypesT.size() < namedTypesS.size()) {
        return false;
      }
      // Loop through each element in S verifying that T has a matching parameter name and that the
      // corresponding type is more specific then the type in S.
      Iterator<Entry<String, Type>> iteratorS = namedTypesS.entrySet().iterator();
      while (iteratorS.hasNext()) {
        Entry<String, Type> entryS = iteratorS.next();
        Type typeT = namedTypesT.get(entryS.getKey());
        if (typeT == null) {
          return false;
        }
        if (!typeT.isMoreSpecificThan(entryS.getValue())) {
          return false;
        }
      }
    } else if (s.getNamedParameterTypes().size() > 0) {
      return false;
    } else {
      // positional parameter case
      int tArgLength = tTypes.length + tOpTypes.length;
      int sArgLength = sTypes.length + sOpTypes.length;
      // Check that the total number of parameters in t is greater than or equal to the number of
      // parameters in s and that the number of required parameters in s is greater than or equal to
      // the number of required parameters in t.
      if (tArgLength < sArgLength || sTypes.length < tTypes.length) {
        return false;
      }
      if (tOpTypes.length == 0 && sOpTypes.length == 0) {
        // No positional arguments, don't copy contents to new array
        for (int i = 0; i < sTypes.length; i++) {
          if (!tTypes[i].isMoreSpecificThan(sTypes[i])) {
            return false;
          }
        }
      } else {
        // Else, we do have positional parameters, copy required and positional parameter types into
        // arrays to do the compare (for loop below).
        Type[] tAllTypes = new Type[sArgLength];
        for (int i = 0; i < tTypes.length; i++) {
          tAllTypes[i] = tTypes[i];
        }
        for (int i = tTypes.length, j = 0; i < sArgLength; i++, j++) {
          tAllTypes[i] = tOpTypes[j];
        }
        Type[] sAllTypes = new Type[sArgLength];
        for (int i = 0; i < sTypes.length; i++) {
          sAllTypes[i] = sTypes[i];
        }
        for (int i = sTypes.length, j = 0; i < sArgLength; i++, j++) {
          sAllTypes[i] = sOpTypes[j];
        }
        for (int i = 0; i < sAllTypes.length; i++) {
          if (!tAllTypes[i].isMoreSpecificThan(sAllTypes[i])) {
            return false;
          }
        }
      }
    }
    Type tRetType = t.getReturnType();
    Type sRetType = s.getReturnType();
    return sRetType.isVoid() || tRetType.isMoreSpecificThan(sRetType);
  }

  @Override
  public boolean isSubtypeOf(Type type) {
    // trivial base cases
    if (type == null) {
      return false;
    } else if (this == type || type.isDynamic() || type.isDartCoreFunction() || type.isObject()) {
      return true;
    } else if (!(type instanceof FunctionType)) {
      return false;
    } else if (this.equals(type)) {
      return true;
    }
    FunctionType t = this;
    FunctionType s = (FunctionType) type;

    Type[] tTypes = t.getNormalParameterTypes();
    Type[] tOpTypes = t.getOptionalParameterTypes();
    Type[] sTypes = s.getNormalParameterTypes();
    Type[] sOpTypes = s.getOptionalParameterTypes();

    // If one function has positional and the other has named parameters, return false.
    if ((sOpTypes.length > 0 && t.getNamedParameterTypes().size() > 0)
        || (tOpTypes.length > 0 && s.getNamedParameterTypes().size() > 0)) {
      return false;
    }

    // named parameters case
    if (t.getNamedParameterTypes().size() > 0) {
      // check that the number of required parameters are equal, and check that every t_i is
      // assignable to every s_i
      if (t.getNormalParameterTypes().length != s.getNormalParameterTypes().length) {
        return false;
      } else if (t.getNormalParameterTypes().length > 0) {
        for (int i = 0; i < tTypes.length; i++) {
          if (!tTypes[i].isAssignableTo(sTypes[i])) {
            return false;
          }
        }
      }
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
        if (!typeT.isAssignableTo(entryS.getValue())) {
          return false;
        }
      }
    } else if (s.getNamedParameterTypes().size() > 0) {
      return false;
    } else {
      // positional parameter case
      int tArgLength = tTypes.length + tOpTypes.length;
      int sArgLength = sTypes.length + sOpTypes.length;
      // Check that the total number of parameters in t is greater than or equal to the number of
      // parameters in s and that the number of required parameters in s is greater than or equal to
      // the number of required parameters in t.
      if (tArgLength < sArgLength || sTypes.length < tTypes.length) {
        return false;
      }
      if (tOpTypes.length == 0 && sOpTypes.length == 0) {
        // No positional arguments, don't copy contents to new array
        for (int i = 0; i < sTypes.length; i++) {
          if (!tTypes[i].isAssignableTo(sTypes[i])) {
            return false;
          }
        }
      } else {
        // Else, we do have positional parameters, copy required and positional parameter types into
        // arrays to do the compare (for loop below).
        Type[] tAllTypes = new Type[sArgLength];
        for (int i = 0; i < tTypes.length; i++) {
          tAllTypes[i] = tTypes[i];
        }
        for (int i = tTypes.length, j = 0; i < sArgLength; i++, j++) {
          tAllTypes[i] = tOpTypes[j];
        }
        Type[] sAllTypes = new Type[sArgLength];
        for (int i = 0; i < sTypes.length; i++) {
          sAllTypes[i] = sTypes[i];
        }
        for (int i = sTypes.length, j = 0; i < sArgLength; i++, j++) {
          sAllTypes[i] = sOpTypes[j];
        }
        for (int i = 0; i < sAllTypes.length; i++) {
          if (!tAllTypes[i].isAssignableTo(sAllTypes[i])) {
            return false;
          }
        }
      }
    }
    Type tRetType = t.getReturnType();
    Type sRetType = s.getReturnType();
    return sRetType.isVoid() || tRetType.isAssignableTo(sRetType);
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
    newType.setTypeArguments(substitute(typeArguments, argumentTypes, parameterTypes));
    return newType;
  }

  @Override
  protected void appendTo(StringBuilder builder) {
    Type[] normalParameterTypes = getNormalParameterTypes();
    Type[] optionalParameterTypes = getOptionalParameterTypes();
    Map<String, Type> namedParameterTypes = getNamedParameterTypes();
    Type returnType = getReturnType();
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

  /**
   * @return the base parameter elements of this function element, not {@code null}.
   */
  protected ParameterElement[] getBaseParameters() {
    Element element = getElement();
    if (element instanceof ExecutableElement) {
      return ((ExecutableElement) element).getParameters();
    } else {
      return ((FunctionTypeAliasElement) element).getParameters();
    }
  }

  /**
   * Return the return type defined by this function's element.
   * 
   * @return the return type defined by this function's element
   */
  private Type getBaseReturnType() {
    Element element = getElement();
    if (element instanceof ExecutableElement) {
      return ((ExecutableElement) element).getReturnType();
    } else {
      return ((FunctionTypeAliasElement) element).getReturnType();
    }
  }
}
