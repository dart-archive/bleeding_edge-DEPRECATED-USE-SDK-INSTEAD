/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.element;

import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.MethodElementImpl;
import com.google.dart.engine.internal.element.TypeVariableElementImpl;
import com.google.dart.engine.internal.type.FunctionTypeImpl;
import com.google.dart.engine.internal.type.InterfaceTypeImpl;
import com.google.dart.engine.internal.type.TypeVariableTypeImpl;
import com.google.dart.engine.type.Type;

import static com.google.dart.engine.ast.ASTFactory.identifier;

/**
 * The class {@code ElementFactory} defines utility methods used to create elements for testing
 * purposes.
 */
public final class ElementFactory {
  /**
   * The element representing the class 'Object'.
   */
  private static ClassElement objectElement;

  public static ClassElement classElement(String typeName, String... parameterNames) {
    return classElement(typeName, getObject().getType(), parameterNames);
  }

  public static ClassElement classElement(String typeName, Type superclassType,
      String... parameterNames) {
    ClassElementImpl element = new ClassElementImpl(identifier(typeName));
    element.setSupertype(superclassType);
    InterfaceTypeImpl type = new InterfaceTypeImpl(element);
    element.setType(type);

    int count = parameterNames.length;
    if (count > 0) {
      TypeVariableElementImpl[] typeVariables = new TypeVariableElementImpl[count];
      TypeVariableTypeImpl[] typeArguments = new TypeVariableTypeImpl[count];
      for (int i = 0; i < count; i++) {
        TypeVariableElementImpl variable = new TypeVariableElementImpl(
            identifier(parameterNames[i]));
        typeVariables[i] = variable;
        typeArguments[i] = new TypeVariableTypeImpl(variable);
        variable.setType(typeArguments[i]);
      }
      element.setTypeVariables(typeVariables);
      type.setTypeArguments(typeArguments);
    }

    return element;
  }

  public static ClassElement getObject() {
    if (objectElement == null) {
      objectElement = classElement("Object", (Type) null);
    }
    return objectElement;
  }

  public static MethodElement methodElement(String methodName, Type returnType,
      Type... argumentTypes) {
    MethodElementImpl method = new MethodElementImpl(identifier(methodName));
    FunctionTypeImpl methodType = new FunctionTypeImpl(method);
    methodType.setNormalParameterTypes(argumentTypes);
    methodType.setReturnType(returnType);
    method.setType(methodType);
    return method;
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private ElementFactory() {
  }
}
