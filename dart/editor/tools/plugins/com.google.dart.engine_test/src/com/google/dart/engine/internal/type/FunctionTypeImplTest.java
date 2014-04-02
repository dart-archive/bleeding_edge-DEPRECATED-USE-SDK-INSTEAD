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
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.FunctionElementImpl;
import com.google.dart.engine.internal.element.MethodElementImpl;
import com.google.dart.engine.internal.element.TypeParameterElementImpl;
import com.google.dart.engine.internal.resolver.TestTypeProvider;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.type.TypeParameterType;

import static com.google.dart.engine.ast.AstFactory.identifier;
import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.functionElement;
import static com.google.dart.engine.element.ElementFactory.getObject;
import static com.google.dart.engine.element.ElementFactory.namedParameter;
import static com.google.dart.engine.element.ElementFactory.positionalParameter;
import static com.google.dart.engine.element.ElementFactory.requiredParameter;

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
    assertSizeOfMap(0, types);
  }

  public void test_getNormalParameterTypes() {
    FunctionTypeImpl type = new FunctionTypeImpl(new FunctionElementImpl(identifier("f")));
    Type[] types = type.getNormalParameterTypes();
    assertLength(0, types);
  }

  public void test_getReturnType() {
    Type expectedReturnType = VoidTypeImpl.getInstance();
    FunctionElementImpl functionElement = new FunctionElementImpl(identifier("f"));
    functionElement.setReturnType(expectedReturnType);
    FunctionTypeImpl type = new FunctionTypeImpl(functionElement);
    Type returnType = type.getReturnType();
    assertEquals(expectedReturnType, returnType);
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

  public void test_isAssignableTo_normalAndPositionalArgs() {
    // ([a]) -> void <: (a) -> void
    ClassElement a = classElement("A");
    FunctionType t = functionElement("t", null, new ClassElement[] {a}).getType();
    FunctionType s = functionElement("s", new ClassElement[] {a}).getType();
    assertTrue(t.isSubtypeOf(s));
    assertFalse(s.isSubtypeOf(t));
    // assignable iff subtype
    assertTrue(t.isAssignableTo(s));
    assertFalse(s.isAssignableTo(t));
  }

  public void test_isSubtypeOf_baseCase_classFunction() {
    // () -> void <: Function
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
    // class C
    // ! () -> void <: C
    FunctionType f = functionElement("f").getType();
    InterfaceType t = classElement("C").getType();
    assertFalse(f.isSubtypeOf(t));
  }

  public void test_isSubtypeOf_baseCase_null() {
    // ! () -> void <: null
    FunctionType f = functionElement("f").getType();
    assertFalse(f.isSubtypeOf(null));
  }

  public void test_isSubtypeOf_baseCase_self() {
    // () -> void <: () -> void
    FunctionType f = functionElement("f").getType();
    assertTrue(f.isSubtypeOf(f));
  }

  public void test_isSubtypeOf_namedParameters_isAssignable() {
    // B extends A
    // ({name: A}) -> void <: ({name: B}) -> void
    // ({name: B}) -> void <: ({name: A}) -> void
    ClassElement a = classElement("A");
    ClassElement b = classElement("B", a.getType());
    FunctionType t = functionElement("t", null, null, new String[] {"name"}, new ClassElement[] {a}).getType();
    FunctionType s = functionElement("s", null, null, new String[] {"name"}, new ClassElement[] {b}).getType();
    assertTrue(t.isSubtypeOf(s));
    assertTrue(s.isSubtypeOf(t));
  }

  public void test_isSubtypeOf_namedParameters_isNotAssignable() {
    // ! ({name: A}) -> void <: ({name: B}) -> void
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
    // B extends A
    // void t({A name}) {}
    // void s({A diff}) {}
    // ! t <: s
    // ! s <: t
    ClassElement a = classElement("A");
    ClassElement b = classElement("B", a.getType());
    FunctionType t = functionElement("t", null, null, new String[] {"name"}, new ClassElement[] {a}).getType();
    FunctionType s = functionElement("s", null, null, new String[] {"diff"}, new ClassElement[] {b}).getType();
    assertFalse(t.isSubtypeOf(s));
    assertFalse(s.isSubtypeOf(t));
  }

  public void test_isSubtypeOf_namedParameters_orderOfParams() {
    // B extends A
    // ({A: A, B: B}) -> void <: ({B: B, A: A}) -> void
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
    // B extends A
    // ! ({B: B}) -> void <: ({B: B, A: A}) -> void
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
    // B extends A
    // ({A: A, B: B}) -> void <: ({A: A}) -> void
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
    // B extends A
    // ! ({name: A}) -> void <: ({name: B, name2: B}) -> void
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
    // B extends A
    // ({name: A, name2: A}) -> void <: ({name: B}) -> void
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

  public void test_isSubtypeOf_normalAndPositionalArgs_1() {
    // ([a]) -> void <: (a) -> void
    ClassElement a = classElement("A");
    FunctionType t = functionElement("t", null, new ClassElement[] {a}).getType();
    FunctionType s = functionElement("s", new ClassElement[] {a}).getType();
    assertTrue(t.isSubtypeOf(s));
    assertFalse(s.isSubtypeOf(t));
  }

  public void test_isSubtypeOf_normalAndPositionalArgs_2() {
    // (a, [a]) -> void <: (a) -> void
    ClassElement a = classElement("A");
    FunctionType t = functionElement("t", new ClassElement[] {a}, new ClassElement[] {a}).getType();
    FunctionType s = functionElement("s", new ClassElement[] {a}).getType();
    assertTrue(t.isSubtypeOf(s));
    assertFalse(s.isSubtypeOf(t));
  }

  public void test_isSubtypeOf_normalAndPositionalArgs_3() {
    // ([a]) -> void <: () -> void
    ClassElement a = classElement("A");
    FunctionType t = functionElement("t", null, new ClassElement[] {a}).getType();
    FunctionType s = functionElement("s").getType();
    assertTrue(t.isSubtypeOf(s));
    assertFalse(s.isSubtypeOf(t));
  }

  public void test_isSubtypeOf_normalAndPositionalArgs_4() {
    // (a, b, [c, d, e]) -> void <: (a, b, c, [d]) -> void
    ClassElement a = classElement("A");
    ClassElement b = classElement("B");
    ClassElement c = classElement("C");
    ClassElement d = classElement("D");
    ClassElement e = classElement("E");
    FunctionType t = functionElement("t", new ClassElement[] {a, b}, new ClassElement[] {c, d, e}).getType();
    FunctionType s = functionElement("s", new ClassElement[] {a, b, c}, new ClassElement[] {d}).getType();
    assertTrue(t.isSubtypeOf(s));
    assertFalse(s.isSubtypeOf(t));
  }

  public void test_isSubtypeOf_normalParameters_isAssignable() {
    // B extends A
    // (a) -> void <: (b) -> void
    // (b) -> void <: (a) -> void
    ClassElement a = classElement("A");
    ClassElement b = classElement("B", a.getType());
    FunctionType t = functionElement("t", new ClassElement[] {a}).getType();
    FunctionType s = functionElement("s", new ClassElement[] {b}).getType();
    assertTrue(t.isSubtypeOf(s));
    assertTrue(s.isSubtypeOf(t));
  }

  public void test_isSubtypeOf_normalParameters_isNotAssignable() {
    // ! (a) -> void <: (b) -> void
    FunctionType t = functionElement("t", new ClassElement[] {classElement("A")}).getType();
    FunctionType s = functionElement("s", new ClassElement[] {classElement("B")}).getType();
    assertFalse(t.isSubtypeOf(s));
  }

  public void test_isSubtypeOf_normalParameters_sHasMoreParams() {
    // B extends A
    // ! (a) -> void <: (b, b) -> void
    ClassElement a = classElement("A");
    ClassElement b = classElement("B", a.getType());
    FunctionType t = functionElement("t", new ClassElement[] {a}).getType();
    FunctionType s = functionElement("s", new ClassElement[] {b, b}).getType();
    assertFalse(t.isSubtypeOf(s));
  }

  public void test_isSubtypeOf_normalParameters_tHasMoreParams() {
    // B extends A
    // ! (a, a) -> void <: (a) -> void
    ClassElement a = classElement("A");
    ClassElement b = classElement("B", a.getType());
    FunctionType t = functionElement("t", new ClassElement[] {a, a}).getType();
    FunctionType s = functionElement("s", new ClassElement[] {b}).getType();
    // note, this is a different assertion from the other "tHasMoreParams" tests, this is
    // intentional as it is a difference of the "normal parameters"
    assertFalse(t.isSubtypeOf(s));
  }

  public void test_isSubtypeOf_Object() {
    // () -> void <: Object
    FunctionType f = functionElement("f").getType();
    InterfaceType t = getObject().getType();
    assertTrue(f.isSubtypeOf(t));
  }

  public void test_isSubtypeOf_positionalParameters_isAssignable() {
    // B extends A
    // ([a]) -> void <: ([b]) -> void
    // ([b]) -> void <: ([a]) -> void
    ClassElement a = classElement("A");
    ClassElement b = classElement("B", a.getType());
    FunctionType t = functionElement("t", null, new ClassElement[] {a}).getType();
    FunctionType s = functionElement("s", null, new ClassElement[] {b}).getType();
    assertTrue(t.isSubtypeOf(s));
    assertTrue(s.isSubtypeOf(t));
  }

  public void test_isSubtypeOf_positionalParameters_isNotAssignable() {
    // ! ([a]) -> void <: ([b]) -> void
    FunctionType t = functionElement("t", null, new ClassElement[] {classElement("A")}).getType();
    FunctionType s = functionElement("s", null, new ClassElement[] {classElement("B")}).getType();
    assertFalse(t.isSubtypeOf(s));
  }

  public void test_isSubtypeOf_positionalParameters_sHasMoreParams() {
    // B extends A
    // ! ([a]) -> void <: ([b, b]) -> void
    ClassElement a = classElement("A");
    ClassElement b = classElement("B", a.getType());
    FunctionType t = functionElement("t", null, new ClassElement[] {a}).getType();
    FunctionType s = functionElement("s", null, new ClassElement[] {b, b}).getType();
    assertFalse(t.isSubtypeOf(s));
  }

  public void test_isSubtypeOf_positionalParameters_tHasMoreParams() {
    // B extends A
    // ([a, a]) -> void <: ([b]) -> void
    ClassElement a = classElement("A");
    ClassElement b = classElement("B", a.getType());
    FunctionType t = functionElement("t", null, new ClassElement[] {a, a}).getType();
    FunctionType s = functionElement("s", null, new ClassElement[] {b}).getType();
    assertTrue(t.isSubtypeOf(s));
  }

  public void test_isSubtypeOf_returnType_sIsVoid() {
    // () -> void <: void
    FunctionType t = functionElement("t").getType();
    FunctionType s = functionElement("s").getType();
    // function s has the implicit return type of void, we assert it here
    assertTrue(VoidTypeImpl.getInstance().equals(s.getReturnType()));
    assertTrue(t.isSubtypeOf(s));
  }

  public void test_isSubtypeOf_returnType_tAssignableToS() {
    // B extends A
    // () -> A <: () -> B
    // () -> B <: () -> A
    ClassElement a = classElement("A");
    ClassElement b = classElement("B", a.getType());
    FunctionType t = functionElement("t", a).getType();
    FunctionType s = functionElement("s", b).getType();
    assertTrue(t.isSubtypeOf(s));
    assertTrue(s.isSubtypeOf(t));
  }

  public void test_isSubtypeOf_returnType_tNotAssignableToS() {
    // ! () -> A <: () -> B
    FunctionType t = functionElement("t", classElement("A")).getType();
    FunctionType s = functionElement("s", classElement("B")).getType();
    assertFalse(t.isSubtypeOf(s));
  }

  public void test_isSubtypeOf_typeParameters_matchesBounds() {
    TestTypeProvider provider = new TestTypeProvider();
    InterfaceType boolType = provider.getBoolType();
    InterfaceType stringType = provider.getStringType();

    TypeParameterElementImpl parameterB = new TypeParameterElementImpl(identifier("B"));
    parameterB.setBound(boolType);
    TypeParameterTypeImpl typeB = new TypeParameterTypeImpl(parameterB);

    TypeParameterElementImpl parameterS = new TypeParameterElementImpl(identifier("S"));
    parameterS.setBound(stringType);
    TypeParameterTypeImpl typeS = new TypeParameterTypeImpl(parameterS);

    FunctionElementImpl functionAliasElement = new FunctionElementImpl(identifier("func"));
    functionAliasElement.setParameters(new ParameterElement[] {
        requiredParameter("a", typeB), positionalParameter("b", typeS)});
    functionAliasElement.setReturnType(stringType);
    FunctionTypeImpl functionAliasType = new FunctionTypeImpl(functionAliasElement);
    functionAliasElement.setType(functionAliasType);

    FunctionElementImpl functionElement = new FunctionElementImpl(identifier("f"));
    functionElement.setParameters(new ParameterElement[] {
        requiredParameter("c", boolType), positionalParameter("d", stringType)});
    functionElement.setReturnType(provider.getDynamicType());
    FunctionTypeImpl functionType = new FunctionTypeImpl(functionElement);
    functionElement.setType(functionType);

    assertTrue(functionType.isAssignableTo(functionAliasType));
  }

  public void test_isSubtypeOf_wrongFunctionType_normal_named() {
    // ! (a) -> void <: ({name: A}) -> void
    // ! ({name: A}) -> void <: (a) -> void
    ClassElement a = classElement("A");
    FunctionType t = functionElement("t", new ClassElement[] {a}).getType();
    FunctionType s = functionElement("s", null, new String[] {"name"}, new ClassElement[] {a}).getType();
    assertFalse(t.isSubtypeOf(s));
    assertFalse(s.isSubtypeOf(t));
  }

  public void test_isSubtypeOf_wrongFunctionType_optional_named() {
    // ! ([a]) -> void <: ({name: A}) -> void
    // ! ({name: A}) -> void <: ([a]) -> void
    ClassElement a = classElement("A");
    FunctionType t = functionElement("t", null, new ClassElement[] {a}).getType();
    FunctionType s = functionElement("s", null, new String[] {"name"}, new ClassElement[] {a}).getType();
    assertFalse(t.isSubtypeOf(s));
    assertFalse(s.isSubtypeOf(t));
  }

  public void test_setTypeArguments() {
    ClassElementImpl enclosingClass = classElement("C", "E");
    MethodElementImpl methodElement = new MethodElementImpl(identifier("m"));
    enclosingClass.setMethods(new MethodElement[] {methodElement});
    FunctionTypeImpl type = new FunctionTypeImpl(methodElement);
    Type expectedType = enclosingClass.getTypeParameters()[0].getType();
    type.setTypeArguments(new Type[] {expectedType});
    Type[] arguments = type.getTypeArguments();
    assertLength(1, arguments);
    assertEquals(expectedType, arguments[0]);
  }

  public void test_substitute2_equal() {
    ClassElementImpl definingClass = classElement("C", "E");
    TypeParameterType parameterType = definingClass.getTypeParameters()[0].getType();
    MethodElementImpl functionElement = new MethodElementImpl(identifier("m"));
    String namedParameterName = "c";
    functionElement.setParameters(new ParameterElement[] {
        requiredParameter("a", parameterType), positionalParameter("b", parameterType),
        namedParameter(namedParameterName, parameterType)});
    functionElement.setReturnType(parameterType);
    definingClass.setMethods(new MethodElement[] {functionElement});

    FunctionTypeImpl functionType = new FunctionTypeImpl(functionElement);
    functionType.setTypeArguments(new Type[] {parameterType});

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
    assertSizeOfMap(1, namedParameters);
    assertEquals(argumentType, namedParameters.get(namedParameterName));
  }

  public void test_substitute2_notEqual() {
    Type returnType = new InterfaceTypeImpl(new ClassElementImpl(identifier("R")));
    Type normalParameterType = new InterfaceTypeImpl(new ClassElementImpl(identifier("A")));
    Type optionalParameterType = new InterfaceTypeImpl(new ClassElementImpl(identifier("B")));
    Type namedParameterType = new InterfaceTypeImpl(new ClassElementImpl(identifier("C")));

    FunctionElementImpl functionElement = new FunctionElementImpl(identifier("f"));
    String namedParameterName = "c";
    functionElement.setParameters(new ParameterElement[] {
        requiredParameter("a", normalParameterType),
        positionalParameter("b", optionalParameterType),
        namedParameter(namedParameterName, namedParameterType)});
    functionElement.setReturnType(returnType);

    FunctionTypeImpl functionType = new FunctionTypeImpl(functionElement);

    InterfaceTypeImpl argumentType = new InterfaceTypeImpl(new ClassElementImpl(identifier("D")));
    TypeParameterTypeImpl parameterType = new TypeParameterTypeImpl(new TypeParameterElementImpl(
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
    assertSizeOfMap(1, namedParameters);
    assertEquals(namedParameterType, namedParameters.get(namedParameterName));
  }
}
