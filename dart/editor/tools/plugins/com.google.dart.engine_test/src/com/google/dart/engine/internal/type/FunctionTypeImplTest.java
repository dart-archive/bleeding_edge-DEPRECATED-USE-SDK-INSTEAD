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
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.FunctionElementImpl;
import com.google.dart.engine.internal.element.TypeVariableElementImpl;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

import static com.google.dart.engine.ast.ASTFactory.identifier;
import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.functionElement;

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

  public void test_getTypeArguments() {
    FunctionTypeImpl type = new FunctionTypeImpl(new FunctionElementImpl(identifier("f")));
    Type[] types = type.getTypeArguments();
    assertLength(0, types);
  }

  public void test_hashCode_element() {
    FunctionTypeImpl type = new FunctionTypeImpl(new FunctionElementImpl(identifier("f")));
    type.hashCode();
  }

  public void test_hashCode_noElement() {
    FunctionTypeImpl type = new FunctionTypeImpl((ExecutableElement) null);
    type.hashCode();
  }

  public void test_isSubtypeOf_baseCase_classFunction() {
    ClassElementImpl functionElement = classElement("Function");
    InterfaceTypeImpl functionType = new InterfaceTypeImpl(functionElement) {
      @Override
      public boolean isDartCoreFunction() {
        return true;
      }
    };
    FunctionType f = functionElement("f").getType();
    assertTrue(f.isSubtypeOf(functionType));
  }

  public void test_isSubtypeOf_baseCase_notFunctionType() {
    FunctionType f = functionElement("f").getType();
    InterfaceType t = classElement("C").getType();
    assertFalse(f.isSubtypeOf(t));
  }

  public void test_isSubtypeOf_baseCase_null() {
    FunctionType f = functionElement("f").getType();
    assertFalse(f.isSubtypeOf(null));
  }

  public void test_isSubtypeOf_baseCase_self() {
    FunctionType f = functionElement("f").getType();
    assertTrue(f.isSubtypeOf(f));
  }

  public void test_isSubtypeOf_namedParameters_isAssignable() {
    ClassElement a = classElement("A");
    ClassElement b = classElement("B", a.getType());
    FunctionType t = functionElement("t", null, null, new String[] {"name"}, new ClassElement[] {a}).getType();
    FunctionType s = functionElement("s", null, null, new String[] {"name"}, new ClassElement[] {b}).getType();
    assertTrue(t.isSubtypeOf(s));
    assertTrue(s.isSubtypeOf(t));
  }

  public void test_isSubtypeOf_namedParameters_isNotAssignable() {
    FunctionType t = functionElement(
        "t",
        null,
        null,
        new String[] {"name"},
        new ClassElement[] {classElement("A")}).getType();
    FunctionType s = functionElement(
        "s",
        null,
        null,
        new String[] {"name"},
        new ClassElement[] {classElement("B")}).getType();
    assertFalse(t.isSubtypeOf(s));
  }

  public void test_isSubtypeOf_namedParameters_namesDifferent() {
    ClassElement a = classElement("A");
    ClassElement b = classElement("B", a.getType());
    FunctionType t = functionElement("t", null, null, new String[] {"name"}, new ClassElement[] {a}).getType();
    FunctionType s = functionElement("s", null, null, new String[] {"diff"}, new ClassElement[] {b}).getType();
    assertFalse(t.isSubtypeOf(s));
    assertFalse(s.isSubtypeOf(t));
  }

  public void test_isSubtypeOf_namedParameters_orderOfParams() {
    ClassElement a = classElement("A");
    ClassElement b = classElement("B", a.getType());
    FunctionType t = functionElement(
        "t",
        null,
        null,
        new String[] {"A", "B"},
        new ClassElement[] {a, b}).getType();
    FunctionType s = functionElement(
        "s",
        null,
        null,
        new String[] {"B", "A"},
        new ClassElement[] {b, a}).getType();
    assertTrue(t.isSubtypeOf(s));
  }

  public void test_isSubtypeOf_namedParameters_orderOfParams2() {
    ClassElement a = classElement("A");
    ClassElement b = classElement("B", a.getType());
    FunctionType t = functionElement("t", null, null, new String[] {"B"}, new ClassElement[] {b}).getType();
    FunctionType s = functionElement(
        "s",
        null,
        null,
        new String[] {"B", "A"},
        new ClassElement[] {b, a}).getType();
    assertFalse(t.isSubtypeOf(s));
  }

  public void test_isSubtypeOf_namedParameters_orderOfParams3() {
    ClassElement a = classElement("A");
    ClassElement b = classElement("B", a.getType());
    FunctionType t = functionElement(
        "t",
        null,
        null,
        new String[] {"A", "B"},
        new ClassElement[] {a, b}).getType();
    FunctionType s = functionElement("s", null, null, new String[] {"B"}, new ClassElement[] {b}).getType();
    assertTrue(t.isSubtypeOf(s));
  }

  public void test_isSubtypeOf_namedParameters_sHasMoreParams() {
    ClassElement a = classElement("A");
    ClassElement b = classElement("B", a.getType());
    FunctionType t = functionElement("t", null, null, new String[] {"name"}, new ClassElement[] {a}).getType();
    FunctionType s = functionElement(
        "s",
        null,
        null,
        new String[] {"name", "name2"},
        new ClassElement[] {b, b}).getType();
    assertFalse(t.isSubtypeOf(s));
  }

  public void test_isSubtypeOf_namedParameters_tHasMoreParams() {
    ClassElement a = classElement("A");
    ClassElement b = classElement("B", a.getType());
    FunctionType t = functionElement(
        "t",
        null,
        null,
        new String[] {"name", "name2"},
        new ClassElement[] {a, a}).getType();
    FunctionType s = functionElement("s", null, null, new String[] {"name"}, new ClassElement[] {b}).getType();
    assertTrue(t.isSubtypeOf(s));
  }

  public void test_isSubtypeOf_normalParameters_isAssignable() {
    ClassElement a = classElement("A");
    ClassElement b = classElement("B", a.getType());
    FunctionType t = functionElement("t", new ClassElement[] {a}).getType();
    FunctionType s = functionElement("s", new ClassElement[] {b}).getType();
    assertTrue(t.isSubtypeOf(s));
    assertTrue(s.isSubtypeOf(t));
  }

  public void test_isSubtypeOf_normalParameters_isNotAssignable() {
    FunctionType t = functionElement("t", new ClassElement[] {classElement("A")}).getType();
    FunctionType s = functionElement("s", new ClassElement[] {classElement("B")}).getType();
    assertFalse(t.isSubtypeOf(s));
  }

  public void test_isSubtypeOf_normalParameters_sHasMoreParams() {
    ClassElement a = classElement("A");
    ClassElement b = classElement("B", a.getType());
    FunctionType t = functionElement("t", new ClassElement[] {a}).getType();
    FunctionType s = functionElement("s", new ClassElement[] {b, b}).getType();
    assertFalse(t.isSubtypeOf(s));
  }

  public void test_isSubtypeOf_normalParameters_tHasMoreParams() {
    ClassElement a = classElement("A");
    ClassElement b = classElement("B", a.getType());
    FunctionType t = functionElement("t", new ClassElement[] {a, a}).getType();
    FunctionType s = functionElement("s", new ClassElement[] {b}).getType();
    // note, this is a different assertion from the other "tHasMoreParams" tests, this is
    // intentional as it is a difference of the "normal parameters"
    assertFalse(t.isSubtypeOf(s));
  }

  public void test_isSubtypeOf_optionalParameters_isAssignable() {
    ClassElement a = classElement("A");
    ClassElement b = classElement("B", a.getType());
    FunctionType t = functionElement("t", null, new ClassElement[] {a}).getType();
    FunctionType s = functionElement("s", null, new ClassElement[] {b}).getType();
    assertTrue(t.isSubtypeOf(s));
    assertTrue(s.isSubtypeOf(t));
  }

  public void test_isSubtypeOf_optionalParameters_isNotAssignable() {
    FunctionType t = functionElement("t", null, new ClassElement[] {classElement("A")}).getType();
    FunctionType s = functionElement("s", null, new ClassElement[] {classElement("B")}).getType();
    assertFalse(t.isSubtypeOf(s));
  }

  public void test_isSubtypeOf_optionalParameters_sHasMoreParams() {
    ClassElement a = classElement("A");
    ClassElement b = classElement("B", a.getType());
    FunctionType t = functionElement("t", null, new ClassElement[] {a}).getType();
    FunctionType s = functionElement("s", null, new ClassElement[] {b, b}).getType();
    assertFalse(t.isSubtypeOf(s));
  }

  public void test_isSubtypeOf_optionalParameters_tHasMoreParams() {
    ClassElement a = classElement("A");
    ClassElement b = classElement("B", a.getType());
    FunctionType t = functionElement("t", null, new ClassElement[] {a, a}).getType();
    FunctionType s = functionElement("s", null, new ClassElement[] {b}).getType();
    assertTrue(t.isSubtypeOf(s));
  }

  public void test_isSubtypeOf_returnType_sIsVoid() {
    FunctionType t = functionElement("t", classElement("A")).getType();
    FunctionType s = functionElement("s").getType();
    // function s has the implicit return type of void, we assert it here
    assertTrue(VoidTypeImpl.getInstance().equals(s.getReturnType()));
    assertTrue(t.isSubtypeOf(s));
  }

  public void test_isSubtypeOf_returnType_tAssignableToS() {
    ClassElement a = classElement("A");
    ClassElement b = classElement("B", a.getType());
    FunctionType t = functionElement("t", a).getType();
    FunctionType s = functionElement("s", b).getType();
    assertTrue(t.isSubtypeOf(s));
    assertTrue(s.isSubtypeOf(t));
  }

  public void test_isSubtypeOf_returnType_tNotAssignableToS() {
    FunctionType t = functionElement("t", classElement("A")).getType();
    FunctionType s = functionElement("s", classElement("B")).getType();
    assertFalse(t.isSubtypeOf(s));
  }

  public void test_isSubtypeOf_wrongFunctionType_normal_named() {
    ClassElement a = classElement("A");
    FunctionType t = functionElement("t", new ClassElement[] {a}).getType();
    FunctionType s = functionElement("s", null, new String[] {"name"}, new ClassElement[] {a}).getType();
    assertFalse(t.isSubtypeOf(s));
    assertFalse(s.isSubtypeOf(t));
  }

  public void test_isSubtypeOf_wrongFunctionType_normal_optional() {
    ClassElement a = classElement("A");
    FunctionType t = functionElement("t", new ClassElement[] {a}).getType();
    FunctionType s = functionElement("s", null, new ClassElement[] {a}).getType();
    assertFalse(t.isSubtypeOf(s));
    assertFalse(s.isSubtypeOf(t));
  }

  public void test_isSubtypeOf_wrongFunctionType_optional_named() {
    ClassElement a = classElement("A");
    FunctionType t = functionElement("t", null, new ClassElement[] {a}).getType();
    FunctionType s = functionElement("s", null, new String[] {"name"}, new ClassElement[] {a}).getType();
    assertFalse(t.isSubtypeOf(s));
    assertFalse(s.isSubtypeOf(t));
  }

  public void test_setNamedParameterTypes() {
    FunctionTypeImpl type = new FunctionTypeImpl(new FunctionElementImpl(identifier("f")));
    LinkedHashMap<String, Type> expectedTypes = new LinkedHashMap<String, Type>();
    expectedTypes.put("a", new InterfaceTypeImpl(new ClassElementImpl(identifier("C"))));
    type.setNamedParameterTypes(expectedTypes);
    Map<String, Type> types = type.getNamedParameterTypes();
    assertEquals(expectedTypes, types);
  }

  public void test_setNormalParameterTypes() {
    FunctionTypeImpl type = new FunctionTypeImpl(new FunctionElementImpl(identifier("f")));
    Type[] expectedTypes = new Type[] {new InterfaceTypeImpl(new ClassElementImpl(identifier("C")))};
    type.setNormalParameterTypes(expectedTypes);
    Type[] types = type.getNormalParameterTypes();
    assertEquals(expectedTypes, types);
  }

  public void test_setReturnType() {
    FunctionTypeImpl type = new FunctionTypeImpl(new FunctionElementImpl(identifier("f")));
    Type expectedType = new InterfaceTypeImpl(new ClassElementImpl(identifier("C")));
    type.setReturnType(expectedType);
    Type returnType = type.getReturnType();
    assertEquals(expectedType, returnType);
  }

  public void test_setTypeArguments() {
    FunctionTypeImpl type = new FunctionTypeImpl(new FunctionElementImpl(identifier("f")));
    Type expectedType = new TypeVariableTypeImpl(new TypeVariableElementImpl(identifier("C")));
    type.setTypeArguments(new Type[] {expectedType});
    Type[] arguments = type.getTypeArguments();
    assertLength(1, arguments);
    assertEquals(expectedType, arguments[0]);
  }

  public void test_substitute2_equal() {
    FunctionTypeImpl functionType = new FunctionTypeImpl(new FunctionElementImpl(identifier("f")));
    TypeVariableTypeImpl parameterType = new TypeVariableTypeImpl(new TypeVariableElementImpl(
        identifier("E")));

    functionType.setReturnType(parameterType);
    functionType.setNormalParameterTypes(new Type[] {parameterType});
    functionType.setOptionalParameterTypes(new Type[] {parameterType});
    LinkedHashMap<String, Type> namedParameterTypes = new LinkedHashMap<String, Type>();
    String namedParameterName = "c";
    namedParameterTypes.put(namedParameterName, parameterType);
    functionType.setNamedParameterTypes(namedParameterTypes);

    InterfaceTypeImpl argumentType = new InterfaceTypeImpl(new ClassElementImpl(identifier("D")));

    FunctionType result = functionType.substitute(
        new Type[] {argumentType},
        new Type[] {parameterType});
    assertEquals(argumentType, result.getReturnType());
    Type[] normalParameters = result.getNormalParameterTypes();
    assertLength(1, normalParameters);
    assertEquals(argumentType, normalParameters[0]);
    Type[] optionalParameters = result.getOptionalParameterTypes();
    assertLength(1, optionalParameters);
    assertEquals(argumentType, optionalParameters[0]);
    Map<String, Type> namedParameters = result.getNamedParameterTypes();
    assertSize(1, namedParameters);
    assertEquals(argumentType, namedParameters.get(namedParameterName));
  }

  public void test_substitute2_notEqual() {
    FunctionTypeImpl functionType = new FunctionTypeImpl(new FunctionElementImpl(identifier("f")));
    Type returnType = new InterfaceTypeImpl(new ClassElementImpl(identifier("R")));
    Type normalParameterType = new InterfaceTypeImpl(new ClassElementImpl(identifier("A")));
    Type optionalParameterType = new InterfaceTypeImpl(new ClassElementImpl(identifier("B")));
    Type namedParameterType = new InterfaceTypeImpl(new ClassElementImpl(identifier("C")));

    functionType.setReturnType(returnType);
    functionType.setNormalParameterTypes(new Type[] {normalParameterType});
    functionType.setOptionalParameterTypes(new Type[] {optionalParameterType});
    LinkedHashMap<String, Type> namedParameterTypes = new LinkedHashMap<String, Type>();
    String namedParameterName = "c";
    namedParameterTypes.put(namedParameterName, namedParameterType);
    functionType.setNamedParameterTypes(namedParameterTypes);

    InterfaceTypeImpl argumentType = new InterfaceTypeImpl(new ClassElementImpl(identifier("D")));
    TypeVariableTypeImpl parameterType = new TypeVariableTypeImpl(new TypeVariableElementImpl(
        identifier("E")));

    FunctionType result = functionType.substitute(
        new Type[] {argumentType},
        new Type[] {parameterType});
    assertEquals(returnType, result.getReturnType());
    Type[] normalParameters = result.getNormalParameterTypes();
    assertLength(1, normalParameters);
    assertEquals(normalParameterType, normalParameters[0]);
    Type[] optionalParameters = result.getOptionalParameterTypes();
    assertLength(1, optionalParameters);
    assertEquals(optionalParameterType, optionalParameters[0]);
    Map<String, Type> namedParameters = result.getNamedParameterTypes();
    assertSize(1, namedParameters);
    assertEquals(namedParameterType, namedParameters.get(namedParameterName));
  }
}
