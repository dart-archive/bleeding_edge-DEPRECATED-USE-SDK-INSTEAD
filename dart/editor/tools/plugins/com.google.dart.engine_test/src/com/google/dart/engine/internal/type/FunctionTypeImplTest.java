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

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.internal.element.FunctionElementImpl;
import com.google.dart.engine.internal.element.TypeElementImpl;
import com.google.dart.engine.type.Type;

import static com.google.dart.engine.ast.ASTFactory.identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public class FunctionTypeImplTest extends EngineTestCase {
  public void test_creation() {
    assertNotNull(new FunctionTypeImpl(new FunctionElementImpl(identifier("f"))));
  }

  public void test_getElement() {
    FunctionElementImpl typeElement = new FunctionElementImpl(identifier("f"));
    FunctionTypeImpl type = new FunctionTypeImpl(typeElement);
    assertEquals(typeElement, type.getElement());
  }

  public void test_getNamedParameterTypes() {
    FunctionTypeImpl type = new FunctionTypeImpl(new FunctionElementImpl(identifier("f")));
    Map<String, Type> types = type.getNamedParameterTypes();
    assertSize(0, types);
  }

  public void test_getNormalParameterTypes() {
    FunctionTypeImpl type = new FunctionTypeImpl(new FunctionElementImpl(identifier("f")));
    Type[] types = type.getNormalParameterTypes();
    assertLength(0, types);
  }

  public void test_getReturnType() {
    FunctionTypeImpl type = new FunctionTypeImpl(new FunctionElementImpl(identifier("f")));
    Type returnType = type.getReturnType();
    assertEquals(VoidTypeImpl.getInstance(), returnType);
  }

  public void test_setNamedParameterTypes() {
    FunctionTypeImpl type = new FunctionTypeImpl(new FunctionElementImpl(identifier("f")));
    LinkedHashMap<String, Type> expectedTypes = new LinkedHashMap<String, Type>();
    expectedTypes.put("a", new InterfaceTypeImpl(new TypeElementImpl(identifier("C"))));
    type.setNamedParameterTypes(expectedTypes);
    Map<String, Type> types = type.getNamedParameterTypes();
    assertEquals(expectedTypes, types);
  }

  public void test_setNormalParameterTypes() {
    FunctionTypeImpl type = new FunctionTypeImpl(new FunctionElementImpl(identifier("f")));
    Type[] expectedTypes = new Type[] {new InterfaceTypeImpl(new TypeElementImpl(identifier("C")))};
    type.setNormalParameterTypes(expectedTypes);
    Type[] types = type.getNormalParameterTypes();
    assertEquals(expectedTypes, types);
  }

  public void test_setReturnType() {
    FunctionTypeImpl type = new FunctionTypeImpl(new FunctionElementImpl(identifier("f")));
    Type expectedType = new InterfaceTypeImpl(new TypeElementImpl(identifier("C")));
    type.setReturnType(expectedType);
    Type returnType = type.getReturnType();
    assertEquals(expectedType, returnType);
  }
}
