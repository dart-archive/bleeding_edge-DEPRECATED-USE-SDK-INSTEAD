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
package com.google.dart.tools.internal.corext.refactoring.util;

import org.eclipse.core.runtime.Assert;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Simple reflection utilities.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class ReflectionUtils {

  /**
   * @return the name of the {@link Class} for signature.
   */
  public static String getClassName(Class<?> clazz) {
    return clazz.getName();
  }

  /**
   * @return the declared {@link Method} with given signature, may be private.
   */
  public static Method getMethod(Object target, String signature) {
    Assert.isNotNull(target);
    Assert.isNotNull(signature);
    Class<?> targetClass = getTargetClass(target);
    while (targetClass != null) {
      for (Method method : targetClass.getDeclaredMethods()) {
        if (getMethodSignature(method).equals(signature)) {
          method.setAccessible(true);
          return method;
        }
      }
      targetClass = targetClass.getSuperclass();
    }
    return null;
  }

  /**
   * Invokes method declared in <code>target</code>, may be private. Re-throws any {@link Exception}
   * as is, without declaring or wrapping them.
   */
  @SuppressWarnings("unchecked")
  public static <T> T invokeMethod(Object target, String signature, Object... args) {
    Assert.isNotNull(target);
    Assert.isNotNull(signature);
    Method method = getMethod(target, signature);
    if (method == null) {
      Class<?> targetClass = getTargetClass(target);
      throw new IllegalArgumentException(signature + " in " + targetClass);
    }
    try {
      return (T) method.invoke(target, args);
    } catch (Throwable e) {
      if (e instanceof InvocationTargetException) {
        e = e.getCause();
      }
      throw ExecutionUtils.propagate(e);
    }
  }

  /**
   * @return the signature of the given {@link Method}, not JVM signature however, just some
   *         reasonable signature to write manually.
   */
  private static String getMethodSignature(Method method) {
    StringBuilder sb = new StringBuilder();
    sb.append(method.getName());
    sb.append('(');
    boolean firstParameter = true;
    for (Class<?> parameterType : method.getParameterTypes()) {
      if (!firstParameter) {
        sb.append(',');
      }
      firstParameter = false;
      sb.append(getClassName(parameterType));
    }
    sb.append(')');
    return sb.toString();
  }

  /**
   * @return the {@link Class} or the given "target" - "target" itself if it is {@link Class} or its
   *         {@link Class} if just some object.
   */
  private static Class<?> getTargetClass(Object target) {
    if (target instanceof Class) {
      return (Class<?>) target;
    }
    return target.getClass();
  }
}
