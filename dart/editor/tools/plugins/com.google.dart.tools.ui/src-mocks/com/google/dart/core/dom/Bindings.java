/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.core.dom;

import com.google.dart.compiler.resolver.ClassElement;

/**
 * TODO(devoncarew): This is a temporary class, used to resolve compilation errors.
 */
public final class Bindings {

  public static IFunctionBinding findMethodInHierarchy(ClassElement binding, String fMethodName,
      String[] fParamTypes) {
    return null;
  }

  public static IFunctionBinding findMethodInHierarchy(ITypeBinding binding, String fMethodName,
      String[] fParamTypes) {
    // TODO:
    return null;
  }

  public static IFunctionBinding findMethodInType(ITypeBinding objectType, String fMethodName,
      String[] fParamTypes) {
    // TODO:
    return null;
  }

  public static IFunctionBinding findOverriddenMethod(IFunctionBinding methodBinding,
      boolean testVisibility) {
    // TODO:

    return null;
  }

  public static ITypeBinding[] getAllSuperTypes(ITypeBinding curr) {
    // TODO:
    return null;
  }

  public static boolean isSuperType(ITypeBinding left, ITypeBinding right) {
    // TODO:
    return false;
  }

  private Bindings() {

  }

}
