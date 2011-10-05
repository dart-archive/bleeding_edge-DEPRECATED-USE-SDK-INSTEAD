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
package com.google.dart.tools.ui;

import com.google.dart.compiler.ast.Modifiers;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Field;

/**
 * TODO(brianwilkerson): This is a temporary interface, used to resolve compilation errors.
 */
public class GetterSetterUtil {

  public static String getGetterName(Field field, Object object) {
    String fieldName = field.getElementName();
    return "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
  }

  public static String getGetterStub(Field field, String getterName, boolean addComments,
      Modifiers modifiers) {
    String fieldType = null;
    try {
      fieldType = field.getTypeName();
    } catch (DartModelException exception) {
      // Fall through
    }
    StringBuilder builder = new StringBuilder();
    builder.append(fieldType == null ? "var" : fieldType);
    builder.append(' ');
    builder.append(getterName);
    builder.append("() { return ");
    builder.append(field.getElementName());
    builder.append("; }");
    return builder.toString();
  }

  public static String getSetterName(Field field, Object object) {
    String fieldName = field.getElementName();
    return "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
  }

  public static String getSetterStub(Field field, String setterName, boolean addComments,
      Modifiers modifiers) {
    String fieldType = null;
    try {
      fieldType = field.getTypeName();
    } catch (DartModelException exception) {
      // Fall through
    }
    StringBuilder builder = new StringBuilder();
    builder.append("void ");
    builder.append(setterName);
    builder.append("(");
    builder.append(fieldType == null ? "var" : fieldType);
    builder.append(" value) { ");
    builder.append(field.getElementName());
    builder.append(" = value; }");
    return builder.toString();
  }

}
