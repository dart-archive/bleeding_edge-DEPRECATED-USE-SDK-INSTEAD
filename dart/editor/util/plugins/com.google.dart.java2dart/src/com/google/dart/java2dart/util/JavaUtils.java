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

package com.google.dart.java2dart.util;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;

/**
 * Helper for JDT integration.
 */
public class JavaUtils {
  /**
   * The constant to return from {@link #getFullyQualifiedName(ITypeBinding, boolean)} when given
   * {@link ITypeBinding} is <code>null</code>, i.e. no type binding information found, for example
   * because of compilation errors.
   */
  public static String NO_TYPE_BINDING_NAME = "__WBP_NO_TYPE_BINDING";

  /**
   * The constant to return from {@link #getMethodSignature(IMethodBinding)} when given
   * {@link IMethodBinding} is <code>null</code>, i.e. no method binding information found, for
   * example because of compilation errors.
   */
  public static String NO_METHOD_BINDING_SIGNATURE = "__WBP_NO_METHOD_BINDING";

  /**
   * Returns the fully qualified name of given {@link ITypeBinding}, or
   * {@link #NO_TYPE_BINDING_NAME} if <code>null</code> binding given.
   * 
   * @param binding the binding representing the type.
   * @param runtime flag <code>true</code> if we need name for class loading, <code>false</code> if
   *          we need name for source generation.
   * @return the fully qualified name of given {@link ITypeBinding}, or
   *         {@link #NO_TYPE_BINDING_NAME} .
   */
  public static String getFullyQualifiedName(ITypeBinding binding, boolean runtime) {
    return getFullyQualifiedName(binding, runtime, false);
  }

  /**
   * @param binding the {@link ITypeBinding} to analyze.
   * @param runtime flag <code>true</code> if we need name for class loading, <code>false</code> if
   *          we need name for source generation.
   * @param withGenerics flag <code>true</code> if generics type arguments should be appended.
   * @return the fully qualified name of given {@link ITypeBinding}, or
   *         {@link #NO_TYPE_BINDING_NAME} .
   */
  public static String getFullyQualifiedName(ITypeBinding binding, boolean runtime,
      boolean withGenerics) {
    // check if no binding
    if (binding == null) {
      return NO_TYPE_BINDING_NAME;
    }
    // check for primitive type
    if (binding.isPrimitive()) {
      return binding.getName();
    }
    // array
    if (binding.isArray()) {
      StringBuilder sb = new StringBuilder();
      // append element type qualified name
      ITypeBinding elementType = binding.getElementType();
      String elementTypeQualifiedName = getFullyQualifiedName(elementType, runtime);
      sb.append(elementTypeQualifiedName);
      // append dimensions
      for (int i = 0; i < binding.getDimensions(); i++) {
        sb.append("[]");
      }
      // done
      return sb.toString();
    }
    // object
    {
      String scope;
      ITypeBinding declaringType = binding.getDeclaringClass();
      if (declaringType == null) {
        IPackageBinding packageBinding = binding.getPackage();
        if (packageBinding == null || packageBinding.isUnnamed()) {
          scope = "";
        } else {
          scope = packageBinding.getName() + ".";
        }
      } else if (binding.isTypeVariable()) {
        return binding.getName();
      } else {
        // use '$', because we use this class name for loading class
        scope = getFullyQualifiedName(declaringType, runtime);
        if (runtime) {
          scope += "$";
        } else {
          scope += ".";
        }
      }
      // prepare "simple" name, without scope
      String jdtName = binding.getName();
      String name = StringUtils.substringBefore(jdtName, "<");
      if (withGenerics) {
        ITypeBinding[] typeArguments = binding.getTypeArguments();
        if (typeArguments.length != 0) {
          StringBuilder sb = new StringBuilder(name);
          sb.append("<");
          for (ITypeBinding typeArgument : typeArguments) {
            if (sb.charAt(sb.length() - 1) != '<') {
              sb.append(",");
            }
            String typeArgumentName = getFullyQualifiedName(typeArgument, runtime, withGenerics);
            sb.append(typeArgumentName);
          }
          sb.append(">");
          name = sb.toString();
        }
      }
      // qualified name is scope plus "simple" name
      return scope + name;
    }
  }

  /**
   * @return the JDT signature of described method.
   */
  public static String getJdtMethodSignature(String className, String methodName,
      String parameterTypes[]) {
    StringBuilder parametersSignature = new StringBuilder();
    for (String parameterType : parameterTypes) {
      parametersSignature.append(getJdtTypeName(parameterType));
    }
    return getJdtTypeName(className) + "." + methodName + "(" + parametersSignature + ")";
  }

  /**
   * @return the JDT method or field signature without return type.
   */
  public static String getJdtSignature(IBinding binding) {
    if (binding != null) {
      String signature = binding.getKey();
      return JavaUtils.getJdtSignature(signature);
    }
    return null;
  }

  /**
   * @return the JDT method or field signature without return type.
   */
  public static String getJdtSignature(String signature) {
    int closeParenIndex = signature.indexOf(')');
    if (closeParenIndex != -1) {
      // field
      if (!signature.contains("(")) {
        return signature.substring(0, closeParenIndex);
      }
      // method or parameter
      int parameterIndex = signature.indexOf('#');
      // method
      if (parameterIndex == -1) {
        return signature.substring(0, closeParenIndex + 1);
      }
      // method parameter
      return signature.substring(0, closeParenIndex + 1) + signature.substring(parameterIndex);
    }
    return signature;
  }

  /**
   * @return the JDT signature type name for given "human" type name.
   */
  public static String getJdtTypeName(String name) {
    if ("boolean".equals(name)) {
      return "Z";
    }
    if ("byte".equals(name)) {
      return "B";
    }
    if ("char".equals(name)) {
      return "C";
    }
    if ("double".equals(name)) {
      return "D";
    }
    if ("float".equals(name)) {
      return "F";
    }
    if ("int".equals(name)) {
      return "I";
    }
    if ("long".equals(name)) {
      return "J";
    }
    if ("short".equals(name)) {
      return "S";
    }
    if ("void".equals(name)) {
      return "V";
    }
    return "L" + StringUtils.replace(name, ".", "/") + ";";
  }

  /**
   * @return signature for given {@link IMethodBinding} with base-declaration types.
   */
  public static String getMethodDeclarationSignature(IMethodBinding methodBinding) {
    if (methodBinding == null) {
      return NO_METHOD_BINDING_SIGNATURE;
    }
    return getMethodSignature(methodBinding.getMethodDeclaration(), true);
  }

  /**
   * @return signature for given {@link IMethodBinding} with generic type names.
   */
  public static String getMethodGenericSignature(IMethodBinding methodBinding) {
    return getMethodSignature(methodBinding.getMethodDeclaration(), false);
  }

  public static IBinding getOriginalBinding(IBinding binding) {
    if (binding instanceof IMethodBinding) {
      IMethodBinding methodBinding = (IMethodBinding) binding;
      methodBinding = methodBinding.getMethodDeclaration();
      while (true) {
        IMethodBinding overriddenMethod = Bindings.findOverriddenMethod(methodBinding, true);
        if (overriddenMethod == null) {
          break;
        }
        methodBinding = overriddenMethod;
      }
      return methodBinding;
    }
    if (binding instanceof IVariableBinding) {
      IVariableBinding varBinding = (IVariableBinding) binding;
      return varBinding.getVariableDeclaration();
    }
    return binding;
  }

  public static String getQualifiedName(ITypeBinding binding) {
    String name = binding.getQualifiedName();
    if (name.contains("<")) {
      name = StringUtils.substringBefore(name, "<");
    }
    return name;
  }

  /**
   * @return the JDT signature with changed name.
   */
  public static String getRenamedJdtSignature(String signature, String newName) {
    // parameter
    {
      int parameterIndex = signature.indexOf('#');
      if (parameterIndex != -1) {
        return signature.substring(0, parameterIndex + 1) + newName;
      }
    }
    // field or method
    int dotIndex = signature.indexOf('.');
    Assert.isLegal(dotIndex != -1, "Cannot find '.' in " + signature);
    // method
    int openParenIndex = signature.indexOf('(');
    if (openParenIndex != -1) {
      return signature.substring(0, dotIndex + 1) + newName + signature.substring(openParenIndex);
    }
    // field
    return signature.substring(0, dotIndex + 1) + newName;
  }

  public static boolean isMethod(Object nodeBinding, String className, String methodName) {
    if (nodeBinding instanceof IMethodBinding) {
      IMethodBinding binding = (IMethodBinding) nodeBinding;
      return binding.getName().equals(methodName) && isMethodInClass(binding, className);
    }
    return false;
  }

  public static boolean isMethodDeclaredInClass(Object bindingObject, String reqClassName) {
    if (bindingObject instanceof IMethodBinding) {
      IMethodBinding binding = (IMethodBinding) bindingObject;
      binding = (IMethodBinding) getOriginalBinding(binding);
      return getQualifiedName(binding.getDeclaringClass()).equals(reqClassName);
    }
    return false;
  }

  /**
   * @return <code>true</code> if given {@link IMethodBinding} is method defined in the class with
   *         required name.
   */
  public static boolean isMethodInClass(Object bindingObject, String reqClassName) {
    if (bindingObject instanceof IMethodBinding) {
      IMethodBinding binding = (IMethodBinding) bindingObject;
      return isSubtype(binding.getDeclaringClass(), reqClassName);
    }
    return false;
  }

  public static boolean isStaticFieldBinding(Object binding) {
    if (binding instanceof IVariableBinding) {
      IVariableBinding fieldBinding = (IVariableBinding) binding;
      return fieldBinding.isField() && isStatic(fieldBinding);
    }
    return false;
  }

  public static boolean isSubtype(ITypeBinding binding, ITypeBinding superBinding) {
    if (binding != null) {
      if (binding == superBinding) {
        return true;
      }
      for (ITypeBinding intf : binding.getInterfaces()) {
        if (isSubtype(intf, superBinding)) {
          return true;
        }
      }
      if (isSubtype(binding.getSuperclass(), superBinding)) {
        return true;
      }
    }
    return false;
  }

  public static boolean isSubtype(ITypeBinding binding, String reqClassName) {
    if (binding != null) {
      if (getQualifiedName(binding).equals(reqClassName)) {
        return true;
      }
      for (ITypeBinding intf : binding.getInterfaces()) {
        if (isSubtype(intf, reqClassName)) {
          return true;
        }
      }
      if (isSubtype(binding.getSuperclass(), reqClassName)) {
        return true;
      }
    }
    return false;
  }

  public static boolean isTypeNamed(Object binding, String reqName) {
    if (binding instanceof ITypeBinding) {
      ITypeBinding typeBinding = (ITypeBinding) binding;
      return reqName.equals(getQualifiedName(typeBinding));
    }
    return false;
  }

  /**
   * @param methodBinding the method binding.
   * @param declaration set <code>true</code> if need type variables replaced with a base types.
   * @return signature for given {@link IMethodBinding}.
   */
  private static String getMethodSignature(IMethodBinding methodBinding, boolean declaration) {
    // check if no binding
    if (methodBinding == null) {
      return NO_METHOD_BINDING_SIGNATURE;
    }
    // signature
    StringBuilder buffer = new StringBuilder();
    // name
    if (methodBinding.isConstructor()) {
      buffer.append("<init>");
    } else {
      buffer.append(methodBinding.getName());
    }
    // parameters
    buffer.append('(');
    {
      ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();
      for (int i = 0; i < parameterTypes.length; i++) {
        ITypeBinding parameterType = parameterTypes[i];
        if (i != 0) {
          buffer.append(',');
        }
        if (declaration && parameterType.isTypeVariable()) {
          ITypeBinding variableBinding = getTypeVariableBound(parameterType);
          if (variableBinding == null) {
            buffer.append("java.lang.Object");
          } else {
            buffer.append(getFullyQualifiedName(variableBinding, false));
          }
        } else {
          buffer.append(getFullyQualifiedName(parameterType, false));
        }
      }
    }
    buffer.append(')');
    // return result
    return buffer.toString();
  }

  /**
   * @param typeBinding the {@link ITypeBinding} of type variable.
   * @return the declared type bounds, may be <code>null</code> if not specified.
   */
  private static ITypeBinding getTypeVariableBound(ITypeBinding typeBinding) {
    Assert.isLegal(typeBinding.isTypeVariable());
    ITypeBinding[] typeBounds = typeBinding.getTypeBounds();
    if (typeBounds.length != 0) {
      return typeBounds[0];
    } else {
      return null;
    }
  }

  private static boolean isStatic(IBinding binding) {
    return Modifier.isStatic(binding.getModifiers());
  }
}
