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
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

/**
 * Helper for JDT integration.
 */
public class JavaUtils {
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

  /**
   * @return the JDT method or field signature without return type.
   */
  public static String getShortJdtSignature(String signature) {
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

}
