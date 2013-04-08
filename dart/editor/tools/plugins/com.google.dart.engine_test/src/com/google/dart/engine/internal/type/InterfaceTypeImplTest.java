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
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ElementFactory;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.FunctionElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.MethodElementImpl;
import com.google.dart.engine.internal.element.TypeVariableElementImpl;
import com.google.dart.engine.internal.resolver.TestTypeProvider;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

import static com.google.dart.engine.ast.ASTFactory.identifier;
import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.getterElement;
import static com.google.dart.engine.element.ElementFactory.library;
import static com.google.dart.engine.element.ElementFactory.methodElement;
import static com.google.dart.engine.element.ElementFactory.setterElement;

import java.util.Set;

public class InterfaceTypeImplTest extends EngineTestCase {

  /**
   * The type provider used to access the types.
   */
  private TestTypeProvider typeProvider;

  @Override
  public void setUp() throws Exception {
    typeProvider = new TestTypeProvider();
  }

  public void test_computeLongestInheritancePathToObject_multipleInterfacePaths() {
    //
    //   Object
    //     |
    //     A
    //    / \
    //   B   C
    //   |   |
    //   |   D
    //    \ /
    //     E
    //
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B");
    ClassElementImpl classC = classElement("C");
    ClassElementImpl classD = classElement("D");
    ClassElementImpl classE = classElement("E");
    classB.setInterfaces(new InterfaceType[] {classA.getType()});
    classC.setInterfaces(new InterfaceType[] {classA.getType()});
    classD.setInterfaces(new InterfaceType[] {classC.getType()});
    classE.setInterfaces(new InterfaceType[] {classB.getType(), classD.getType()});

    // assertion: even though the longest path to Object for typeB is 2, and typeE implements typeB,
    // the longest path for typeE is 4 since it also implements typeD
    assertEquals(2, InterfaceTypeImpl.computeLongestInheritancePathToObject(classB.getType()));
    assertEquals(4, InterfaceTypeImpl.computeLongestInheritancePathToObject(classE.getType()));
  }

  public void test_computeLongestInheritancePathToObject_multipleSuperclassPaths() {
    //
    //   Object
    //     |
    //     A
    //    / \
    //   B   C
    //   |   |
    //   |   D
    //    \ /
    //     E
    //
    ClassElement classA = classElement("A");
    ClassElement classB = classElement("B", classA.getType());
    ClassElement classC = classElement("C", classA.getType());
    ClassElement classD = classElement("D", classC.getType());
    ClassElementImpl classE = classElement("E", classB.getType());
    classE.setInterfaces(new InterfaceType[] {classD.getType()});

    // assertion: even though the longest path to Object for typeB is 2, and typeE extends typeB,
    // the longest path for typeE is 4 since it also implements typeD
    assertEquals(2, InterfaceTypeImpl.computeLongestInheritancePathToObject(classB.getType()));
    assertEquals(4, InterfaceTypeImpl.computeLongestInheritancePathToObject(classE.getType()));
  }

  public void test_computeLongestInheritancePathToObject_object() {
    //
    //   Object
    //     |
    //     A
    //
    ClassElement classA = classElement("A");
    InterfaceType object = classA.getSupertype();

    assertEquals(0, InterfaceTypeImpl.computeLongestInheritancePathToObject(object));
  }

  public void test_computeLongestInheritancePathToObject_singleInterfacePath() {
    //
    //   Object
    //     |
    //     A
    //     |
    //     B
    //     |
    //     C
    //
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B");
    ClassElementImpl classC = classElement("C");
    classB.setInterfaces(new InterfaceType[] {classA.getType()});
    classC.setInterfaces(new InterfaceType[] {classB.getType()});

    assertEquals(1, InterfaceTypeImpl.computeLongestInheritancePathToObject(classA.getType()));
    assertEquals(2, InterfaceTypeImpl.computeLongestInheritancePathToObject(classB.getType()));
    assertEquals(3, InterfaceTypeImpl.computeLongestInheritancePathToObject(classC.getType()));
  }

  public void test_computeLongestInheritancePathToObject_singleSuperclassPath() {
    //
    //   Object
    //     |
    //     A
    //     |
    //     B
    //     |
    //     C
    //
    ClassElement classA = classElement("A");
    ClassElement classB = classElement("B", classA.getType());
    ClassElement classC = classElement("C", classB.getType());

    assertEquals(1, InterfaceTypeImpl.computeLongestInheritancePathToObject(classA.getType()));
    assertEquals(2, InterfaceTypeImpl.computeLongestInheritancePathToObject(classB.getType()));
    assertEquals(3, InterfaceTypeImpl.computeLongestInheritancePathToObject(classC.getType()));
  }

  public void test_computeSuperinterfaceSet_multipleInterfacePaths() {
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B");
    ClassElementImpl classC = classElement("C");
    ClassElementImpl classD = classElement("D");
    ClassElementImpl classE = classElement("E");
    classB.setInterfaces(new InterfaceType[] {classA.getType()});
    classC.setInterfaces(new InterfaceType[] {classA.getType()});
    classD.setInterfaces(new InterfaceType[] {classC.getType()});
    classE.setInterfaces(new InterfaceType[] {classB.getType(), classD.getType()});
    // D
    Set<InterfaceType> superinterfacesOfD = InterfaceTypeImpl.computeSuperinterfaceSet(classD.getType());
    assertNotNull(superinterfacesOfD);
    assertTrue(superinterfacesOfD.contains(ElementFactory.getObject().getType()));
    assertTrue(superinterfacesOfD.contains(classA.getType()));
    assertTrue(superinterfacesOfD.contains(classC.getType()));
    assertEquals(3, superinterfacesOfD.size());
    // E
    Set<InterfaceType> superinterfacesOfE = InterfaceTypeImpl.computeSuperinterfaceSet(classE.getType());
    assertNotNull(superinterfacesOfE);
    assertTrue(superinterfacesOfE.contains(ElementFactory.getObject().getType()));
    assertTrue(superinterfacesOfE.contains(classA.getType()));
    assertTrue(superinterfacesOfE.contains(classB.getType()));
    assertTrue(superinterfacesOfE.contains(classC.getType()));
    assertTrue(superinterfacesOfE.contains(classD.getType()));
    assertEquals(5, superinterfacesOfE.size());
  }

  public void test_computeSuperinterfaceSet_multipleSuperclassPaths() {
    ClassElement classA = classElement("A");
    ClassElement classB = classElement("B", classA.getType());
    ClassElement classC = classElement("C", classA.getType());
    ClassElement classD = classElement("D", classC.getType());
    ClassElementImpl classE = classElement("E", classB.getType());
    classE.setInterfaces(new InterfaceType[] {classD.getType()});
    // D
    Set<InterfaceType> superinterfacesOfD = InterfaceTypeImpl.computeSuperinterfaceSet(classD.getType());
    assertNotNull(superinterfacesOfD);
    assertTrue(superinterfacesOfD.contains(ElementFactory.getObject().getType()));
    assertTrue(superinterfacesOfD.contains(classA.getType()));
    assertTrue(superinterfacesOfD.contains(classC.getType()));
    assertEquals(3, superinterfacesOfD.size());
    // E
    Set<InterfaceType> superinterfacesOfE = InterfaceTypeImpl.computeSuperinterfaceSet(classE.getType());
    assertNotNull(superinterfacesOfE);
    assertTrue(superinterfacesOfE.contains(ElementFactory.getObject().getType()));
    assertTrue(superinterfacesOfE.contains(classA.getType()));
    assertTrue(superinterfacesOfE.contains(classB.getType()));
    assertTrue(superinterfacesOfE.contains(classC.getType()));
    assertTrue(superinterfacesOfE.contains(classD.getType()));
    assertEquals(5, superinterfacesOfE.size());
  }

  public void test_computeSuperinterfaceSet_singleInterfacePath() {
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B");
    ClassElementImpl classC = classElement("C");
    classB.setInterfaces(new InterfaceType[] {classA.getType()});
    classC.setInterfaces(new InterfaceType[] {classB.getType()});
    // A
    Set<InterfaceType> superinterfacesOfA = InterfaceTypeImpl.computeSuperinterfaceSet(classA.getType());
    assertNotNull(superinterfacesOfA);
    assertTrue(superinterfacesOfA.contains(ElementFactory.getObject().getType()));
    assertEquals(1, superinterfacesOfA.size());
    // B
    Set<InterfaceType> superinterfacesOfB = InterfaceTypeImpl.computeSuperinterfaceSet(classB.getType());
    assertNotNull(superinterfacesOfB);
    assertTrue(superinterfacesOfB.contains(ElementFactory.getObject().getType()));
    assertTrue(superinterfacesOfB.contains(classA.getType()));
    assertEquals(2, superinterfacesOfB.size());
    // C
    Set<InterfaceType> superinterfacesOfC = InterfaceTypeImpl.computeSuperinterfaceSet(classC.getType());
    assertNotNull(superinterfacesOfC);
    assertTrue(superinterfacesOfC.contains(ElementFactory.getObject().getType()));
    assertTrue(superinterfacesOfC.contains(classA.getType()));
    assertTrue(superinterfacesOfC.contains(classB.getType()));
    assertEquals(3, superinterfacesOfC.size());
  }

  public void test_computeSuperinterfaceSet_singleSuperclassPath() {
    ClassElement classA = classElement("A");
    ClassElement classB = classElement("B", classA.getType());
    ClassElement classC = classElement("C", classB.getType());
    // A
    Set<InterfaceType> superinterfacesOfA = InterfaceTypeImpl.computeSuperinterfaceSet(classA.getType());
    assertNotNull(superinterfacesOfA);
    assertTrue(superinterfacesOfA.contains(ElementFactory.getObject().getType()));
    assertEquals(1, superinterfacesOfA.size());
    // B
    Set<InterfaceType> superinterfacesOfB = InterfaceTypeImpl.computeSuperinterfaceSet(classB.getType());
    assertNotNull(superinterfacesOfB);
    assertTrue(superinterfacesOfB.contains(ElementFactory.getObject().getType()));
    assertTrue(superinterfacesOfB.contains(classA.getType()));
    assertEquals(2, superinterfacesOfB.size());
    // C
    Set<InterfaceType> superinterfacesOfC = InterfaceTypeImpl.computeSuperinterfaceSet(classC.getType());
    assertNotNull(superinterfacesOfC);
    assertTrue(superinterfacesOfC.contains(ElementFactory.getObject().getType()));
    assertTrue(superinterfacesOfC.contains(classA.getType()));
    assertTrue(superinterfacesOfC.contains(classB.getType()));
    assertEquals(3, superinterfacesOfC.size());
  }

  public void test_creation() {
    assertNotNull(new InterfaceTypeImpl(classElement("A")));
  }

  public void test_getElement() {
    ClassElementImpl typeElement = classElement("A");
    InterfaceTypeImpl type = new InterfaceTypeImpl(typeElement);

    assertEquals(typeElement, type.getElement());
  }

  public void test_getGetter_implemented() {
    //
    // class A { g {} }
    //
    ClassElementImpl classA = classElement("A");
    String getterName = "g";
    PropertyAccessorElement getterG = getterElement(getterName, false, null);
    classA.setAccessors(new PropertyAccessorElement[] {getterG});
    InterfaceType typeA = classA.getType();

    assertSame(getterG, typeA.getGetter(getterName));
  }

  public void test_getGetter_parameterized() {
    //
    // class A<E> { E g {} }
    //
    ClassElementImpl classA = classElement("A", "E");
    Type typeE = classA.getType().getTypeArguments()[0];
    String getterName = "g";
    PropertyAccessorElement getterG = getterElement(getterName, false, typeE);
    classA.setAccessors(new PropertyAccessorElement[] {getterG});
    //
    // A<I>
    //
    InterfaceType typeI = classElement("I").getType();
    InterfaceTypeImpl typeAI = new InterfaceTypeImpl(classA);
    typeAI.setTypeArguments(new Type[] {typeI});

    PropertyAccessorElement getter = typeAI.getGetter(getterName);
    assertNotNull(getter);
    FunctionType getterType = getter.getType();
    assertSame(typeI, getterType.getReturnType());
  }

  public void test_getGetter_unimplemented() {
    //
    // class A {}
    //
    ClassElementImpl classA = classElement("A");
    InterfaceType typeA = classA.getType();

    assertNull(typeA.getGetter("g"));
  }

  public void test_getInterfaces_nonParameterized() {
    //
    // class C implements A, B
    //
    ClassElementImpl classA = classElement("A");
    InterfaceType typeA = classA.getType();
    ClassElementImpl classB = classElement("B");
    InterfaceType typeB = classB.getType();
    ClassElementImpl classC = classElement("C");
    classC.setInterfaces(new InterfaceType[] {typeA, typeB});

    InterfaceType[] interfaces = classC.getType().getInterfaces();
    assertLength(2, interfaces);
    if (interfaces[0] == typeA) {
      assertSame(typeB, interfaces[1]);
    } else {
      assertSame(typeB, interfaces[0]);
      assertSame(typeA, interfaces[1]);
    }
  }

  public void test_getInterfaces_parameterized() {
    //
    // class A<E>
    // class B<F> implements A<F>
    //
    ClassElementImpl classA = classElement("A", "E");
    ClassElementImpl classB = classElement("B", "F");
    InterfaceType typeB = classB.getType();
    InterfaceTypeImpl typeAF = new InterfaceTypeImpl(classA);
    typeAF.setTypeArguments(new Type[] {typeB.getTypeArguments()[0]});
    classB.setInterfaces(new InterfaceType[] {typeAF});
    //
    // B<I>
    //
    InterfaceType typeI = classElement("I").getType();
    InterfaceTypeImpl typeBI = new InterfaceTypeImpl(classB);
    typeBI.setTypeArguments(new Type[] {typeI});

    InterfaceType[] interfaces = typeBI.getInterfaces();
    assertLength(1, interfaces);
    InterfaceType result = interfaces[0];
    assertSame(classA, result.getElement());
    assertSame(typeI, result.getTypeArguments()[0]);
  }

  public void test_getLeastUpperBound_directInterfaceCase() {
    //
    // class A
    // class B implements A
    // class C implements B
    //
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B");
    ClassElementImpl classC = classElement("C");
    InterfaceType typeA = classA.getType();
    InterfaceType typeB = classB.getType();
    InterfaceType typeC = classC.getType();
    classB.setInterfaces(new InterfaceType[] {typeA});
    classC.setInterfaces(new InterfaceType[] {typeB});

    assertEquals(typeB, typeB.getLeastUpperBound(typeC));
    assertEquals(typeB, typeC.getLeastUpperBound(typeB));
  }

  public void test_getLeastUpperBound_directSubclassCase() {
    //
    // class A
    // class B extends A
    // class C extends B
    //
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B", classA.getType());
    ClassElementImpl classC = classElement("C", classB.getType());
    InterfaceType typeB = classB.getType();
    InterfaceType typeC = classC.getType();

    assertEquals(typeB, typeB.getLeastUpperBound(typeC));
    assertEquals(typeB, typeC.getLeastUpperBound(typeB));
  }

  public void test_getLeastUpperBound_functionType() {
    Type interfaceType = classElement("A").getType();
    FunctionTypeImpl functionType = new FunctionTypeImpl(new FunctionElementImpl(identifier("f")));

    assertNull(interfaceType.getLeastUpperBound(functionType));
  }

  public void test_getLeastUpperBound_ignoreTypeParameters() {
    //
    // class List<int>
    // class List<double>
    //
    InterfaceType listType = typeProvider.getListType();
    InterfaceType intType = typeProvider.getIntType();
    InterfaceType doubleType = typeProvider.getDoubleType();
    InterfaceType listOfIntType = listType.substitute(new Type[] {intType});
    InterfaceType listOfDoubleType = listType.substitute(new Type[] {doubleType});
    assertEquals(listType, listOfIntType.getLeastUpperBound(listOfDoubleType));
  }

  public void test_getLeastUpperBound_mixinCase() {
    //
    // class A
    // class B extends A
    // class C extends A
    // class D extends B with M, N, O, P
    //
    ClassElement classA = classElement("A");
    ClassElement classB = classElement("B", classA.getType());
    ClassElement classC = classElement("C", classA.getType());
    ClassElementImpl classD = classElement("D", classB.getType());
    InterfaceType typeA = classA.getType();
    InterfaceType typeC = classC.getType();
    InterfaceType typeD = classD.getType();
    classD.setMixins(new InterfaceType[] {
        classElement("M").getType(), classElement("N").getType(), classElement("O").getType(),
        classElement("P").getType()});

    assertEquals(typeA, typeD.getLeastUpperBound(typeC));
    assertEquals(typeA, typeC.getLeastUpperBound(typeD));
  }

  public void test_getLeastUpperBound_null() {
    Type interfaceType = classElement("A").getType();

    assertNull(interfaceType.getLeastUpperBound(null));
  }

  public void test_getLeastUpperBound_object() {
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B");
    InterfaceType typeA = classA.getType();
    InterfaceType typeB = classB.getType();
    Type typeObject = typeA.getElement().getSupertype();

    // assert that object does not have a super type
    assertNull(((ClassElement) typeObject.getElement()).getSupertype());

    // assert that both A and B have the same super type of Object
    assertEquals(typeObject, typeB.getElement().getSupertype());

    // finally, assert that the only least upper bound of A and B is Object
    assertEquals(typeObject, typeA.getLeastUpperBound(typeB));
  }

  public void test_getLeastUpperBound_self() {
    ClassElement classA = classElement("A");
    InterfaceType typeA = classA.getType();

    assertEquals(typeA, typeA.getLeastUpperBound(typeA));
  }

  public void test_getLeastUpperBound_sharedSuperclass1() {
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B", classA.getType());
    ClassElementImpl classC = classElement("C", classA.getType());
    InterfaceType typeA = classA.getType();
    InterfaceType typeB = classB.getType();
    InterfaceType typeC = classC.getType();

    assertEquals(typeA, typeB.getLeastUpperBound(typeC));
    assertEquals(typeA, typeC.getLeastUpperBound(typeB));
  }

  public void test_getLeastUpperBound_sharedSuperclass2() {
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B", classA.getType());
    ClassElementImpl classC = classElement("C", classA.getType());
    ClassElementImpl classD = classElement("D", classC.getType());
    InterfaceType typeA = classA.getType();
    InterfaceType typeB = classB.getType();
    InterfaceType typeD = classD.getType();

    assertEquals(typeA, typeB.getLeastUpperBound(typeD));
    assertEquals(typeA, typeD.getLeastUpperBound(typeB));
  }

  public void test_getLeastUpperBound_sharedSuperclass3() {
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B", classA.getType());
    ClassElementImpl classC = classElement("C", classB.getType());
    ClassElementImpl classD = classElement("D", classB.getType());
    InterfaceType typeB = classB.getType();
    InterfaceType typeC = classC.getType();
    InterfaceType typeD = classD.getType();

    assertEquals(typeB, typeC.getLeastUpperBound(typeD));
    assertEquals(typeB, typeD.getLeastUpperBound(typeC));
  }

  public void test_getLeastUpperBound_sharedSuperclass4() {
    ClassElement classA = classElement("A");
    ClassElement classA2 = classElement("A2");
    ClassElement classA3 = classElement("A3");
    ClassElementImpl classB = classElement("B", classA.getType());
    ClassElementImpl classC = classElement("C", classA.getType());
    InterfaceType typeA = classA.getType();
    InterfaceType typeA2 = classA2.getType();
    InterfaceType typeA3 = classA3.getType();
    InterfaceType typeB = classB.getType();
    InterfaceType typeC = classC.getType();
    classB.setInterfaces(new InterfaceType[] {typeA2});
    classC.setInterfaces(new InterfaceType[] {typeA3});

    assertEquals(typeA, typeB.getLeastUpperBound(typeC));
    assertEquals(typeA, typeC.getLeastUpperBound(typeB));
  }

  public void test_getLeastUpperBound_sharedSuperinterface1() {
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B");
    ClassElementImpl classC = classElement("C");
    InterfaceType typeA = classA.getType();
    InterfaceType typeB = classB.getType();
    InterfaceType typeC = classC.getType();
    classB.setInterfaces(new InterfaceType[] {typeA});
    classC.setInterfaces(new InterfaceType[] {typeA});

    assertEquals(typeA, typeB.getLeastUpperBound(typeC));
    assertEquals(typeA, typeC.getLeastUpperBound(typeB));
  }

  public void test_getLeastUpperBound_sharedSuperinterface2() {
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B");
    ClassElementImpl classC = classElement("C");
    ClassElementImpl classD = classElement("D");
    InterfaceType typeA = classA.getType();
    InterfaceType typeB = classB.getType();
    InterfaceType typeC = classC.getType();
    InterfaceType typeD = classD.getType();
    classB.setInterfaces(new InterfaceType[] {typeA});
    classC.setInterfaces(new InterfaceType[] {typeA});
    classD.setInterfaces(new InterfaceType[] {typeC});

    assertEquals(typeA, typeB.getLeastUpperBound(typeD));
    assertEquals(typeA, typeD.getLeastUpperBound(typeB));
  }

  public void test_getLeastUpperBound_sharedSuperinterface3() {
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B");
    ClassElementImpl classC = classElement("C");
    ClassElementImpl classD = classElement("D");
    InterfaceType typeA = classA.getType();
    InterfaceType typeB = classB.getType();
    InterfaceType typeC = classC.getType();
    InterfaceType typeD = classD.getType();
    classB.setInterfaces(new InterfaceType[] {typeA});
    classC.setInterfaces(new InterfaceType[] {typeB});
    classD.setInterfaces(new InterfaceType[] {typeB});

    assertEquals(typeB, typeC.getLeastUpperBound(typeD));
    assertEquals(typeB, typeD.getLeastUpperBound(typeC));
  }

  public void test_getLeastUpperBound_sharedSuperinterface4() {
    ClassElement classA = classElement("A");
    ClassElement classA2 = classElement("A2");
    ClassElement classA3 = classElement("A3");
    ClassElementImpl classB = classElement("B");
    ClassElementImpl classC = classElement("C");
    InterfaceType typeA = classA.getType();
    InterfaceType typeA2 = classA2.getType();
    InterfaceType typeA3 = classA3.getType();
    InterfaceType typeB = classB.getType();
    InterfaceType typeC = classC.getType();
    classB.setInterfaces(new InterfaceType[] {typeA, typeA2});
    classC.setInterfaces(new InterfaceType[] {typeA, typeA3});

    assertEquals(typeA, typeB.getLeastUpperBound(typeC));
    assertEquals(typeA, typeC.getLeastUpperBound(typeB));
  }

  public void test_getMethod_implemented() {
    //
    // class A { m() {} }
    //
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElementImpl methodM = methodElement(methodName, null);
    classA.setMethods(new MethodElement[] {methodM});
    InterfaceType typeA = classA.getType();

    assertSame(methodM, typeA.getMethod(methodName));
  }

  public void test_getMethod_parameterized() {
    //
    // class A<E> { E m(E p) {} }
    //
    ClassElementImpl classA = classElement("A", "E");
    Type typeE = classA.getType().getTypeArguments()[0];
    String methodName = "m";
    MethodElementImpl methodM = methodElement(methodName, typeE, typeE);
    classA.setMethods(new MethodElement[] {methodM});
    //
    // A<I>
    //
    InterfaceType typeI = classElement("I").getType();
    InterfaceTypeImpl typeAI = new InterfaceTypeImpl(classA);
    typeAI.setTypeArguments(new Type[] {typeI});

    MethodElement method = typeAI.getMethod(methodName);
    assertNotNull(method);
    FunctionType methodType = method.getType();
    assertSame(typeI, methodType.getReturnType());
    Type[] parameterTypes = methodType.getNormalParameterTypes();
    assertLength(1, parameterTypes);
    assertSame(typeI, parameterTypes[0]);
  }

  public void test_getMethod_unimplemented() {
    //
    // class A {}
    //
    ClassElementImpl classA = classElement("A");
    InterfaceType typeA = classA.getType();

    assertNull(typeA.getMethod("m"));
  }

  public void test_getMixins_nonParameterized() {
    //
    // class C extends Object with A, B
    //
    ClassElementImpl classA = classElement("A");
    InterfaceType typeA = classA.getType();
    ClassElementImpl classB = classElement("B");
    InterfaceType typeB = classB.getType();
    ClassElementImpl classC = classElement("C");
    classC.setMixins(new InterfaceType[] {typeA, typeB});

    InterfaceType[] interfaces = classC.getType().getMixins();
    assertLength(2, interfaces);
    if (interfaces[0] == typeA) {
      assertSame(typeB, interfaces[1]);
    } else {
      assertSame(typeB, interfaces[0]);
      assertSame(typeA, interfaces[1]);
    }
  }

  public void test_getMixins_parameterized() {
    //
    // class A<E>
    // class B<F> extends Object with A<F>
    //
    ClassElementImpl classA = classElement("A", "E");
    ClassElementImpl classB = classElement("B", "F");
    InterfaceType typeB = classB.getType();
    InterfaceTypeImpl typeAF = new InterfaceTypeImpl(classA);
    typeAF.setTypeArguments(new Type[] {typeB.getTypeArguments()[0]});
    classB.setMixins(new InterfaceType[] {typeAF});
    //
    // B<I>
    //
    InterfaceType typeI = classElement("I").getType();
    InterfaceTypeImpl typeBI = new InterfaceTypeImpl(classB);
    typeBI.setTypeArguments(new Type[] {typeI});

    InterfaceType[] interfaces = typeBI.getMixins();
    assertLength(1, interfaces);
    InterfaceType result = interfaces[0];
    assertSame(classA, result.getElement());
    assertSame(typeI, result.getTypeArguments()[0]);
  }

  public void test_getSetter_implemented() {
    //
    // class A { s() {} }
    //
    ClassElementImpl classA = classElement("A");
    String setterName = "s";
    PropertyAccessorElement setterS = setterElement(setterName, false, null);
    classA.setAccessors(new PropertyAccessorElement[] {setterS});
    InterfaceType typeA = classA.getType();

    assertSame(setterS, typeA.getSetter(setterName));
  }

  public void test_getSetter_parameterized() {
    //
    // class A<E> { s(E p) {} }
    //
    ClassElementImpl classA = classElement("A", "E");
    Type typeE = classA.getType().getTypeArguments()[0];
    String setterName = "s";
    PropertyAccessorElement setterS = setterElement(setterName, false, typeE);
    classA.setAccessors(new PropertyAccessorElement[] {setterS});
    //
    // A<I>
    //
    InterfaceType typeI = classElement("I").getType();
    InterfaceTypeImpl typeAI = new InterfaceTypeImpl(classA);
    typeAI.setTypeArguments(new Type[] {typeI});

    PropertyAccessorElement setter = typeAI.getSetter(setterName);
    assertNotNull(setter);
    FunctionType setterType = setter.getType();
    Type[] parameterTypes = setterType.getNormalParameterTypes();
    assertLength(1, parameterTypes);
    assertSame(typeI, parameterTypes[0]);
  }

  public void test_getSetter_unimplemented() {
    //
    // class A {}
    //
    ClassElementImpl classA = classElement("A");
    InterfaceType typeA = classA.getType();

    assertNull(typeA.getSetter("s"));
  }

  public void test_getSuperclass_nonParameterized() {
    //
    // class B extends A
    //
    ClassElementImpl classA = classElement("A");
    InterfaceType typeA = classA.getType();
    ClassElementImpl classB = classElement("B", typeA);
    InterfaceType typeB = classB.getType();

    assertSame(typeA, typeB.getSuperclass());
  }

  public void test_getSuperclass_parameterized() {
    //
    // class A<E>
    // class B<F> extends A<F>
    //
    ClassElementImpl classA = classElement("A", "E");
    ClassElementImpl classB = classElement("B", "F");
    InterfaceType typeB = classB.getType();
    InterfaceTypeImpl typeAF = new InterfaceTypeImpl(classA);
    typeAF.setTypeArguments(new Type[] {typeB.getTypeArguments()[0]});
    classB.setSupertype(typeAF);
    //
    // B<I>
    //
    InterfaceType typeI = classElement("I").getType();
    InterfaceTypeImpl typeBI = new InterfaceTypeImpl(classB);
    typeBI.setTypeArguments(new Type[] {typeI});

    InterfaceType superclass = typeBI.getSuperclass();
    assertSame(classA, superclass.getElement());
    assertSame(typeI, superclass.getTypeArguments()[0]);
  }

  public void test_getTypeArguments_empty() {
    InterfaceType type = classElement("A").getType();

    assertLength(0, type.getTypeArguments());
  }

  public void test_isDirectSupertypeOf_extends() {
    ClassElement classA = classElement("A");
    ClassElement classB = classElement("B", classA.getType());
    InterfaceType typeA = classA.getType();
    InterfaceType typeB = classB.getType();

    assertTrue(typeA.isDirectSupertypeOf(typeB));
  }

  public void test_isDirectSupertypeOf_false() {
    ClassElement classA = classElement("A");
    ClassElement classB = classElement("B");
    ClassElement classC = classElement("C", classB.getType());
    InterfaceType typeA = classA.getType();
    InterfaceType typeC = classC.getType();

    assertFalse(typeA.isDirectSupertypeOf(typeC));
  }

  public void test_isDirectSupertypeOf_implements() {
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B");
    InterfaceType typeA = classA.getType();
    InterfaceType typeB = classB.getType();
    classB.setInterfaces(new InterfaceType[] {typeA});

    assertTrue(typeA.isDirectSupertypeOf(typeB));
  }

  public void test_isDirectSupertypeOf_with() {
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B");
    InterfaceType typeA = classA.getType();
    InterfaceType typeB = classB.getType();
    classB.setMixins(new InterfaceType[] {typeA});

    assertTrue(typeA.isDirectSupertypeOf(typeB));
  }

  public void test_isMoreSpecificThan_bottom() {
    Type type = classElement("A").getType();

    assertTrue(BottomTypeImpl.getInstance().isMoreSpecificThan(type));
  }

  public void test_isMoreSpecificThan_covariance() {
    ClassElement classA = classElement("A", "E");
    ClassElement classI = classElement("I");
    ClassElement classJ = classElement("J", classI.getType());
    InterfaceTypeImpl typeAI = new InterfaceTypeImpl(classA);
    InterfaceTypeImpl typeAJ = new InterfaceTypeImpl(classA);
    typeAI.setTypeArguments(new Type[] {classI.getType()});
    typeAJ.setTypeArguments(new Type[] {classJ.getType()});

    assertTrue(typeAJ.isMoreSpecificThan(typeAI));
    assertFalse(typeAI.isMoreSpecificThan(typeAJ));
  }

  public void test_isMoreSpecificThan_directSupertype() {
    ClassElement classA = classElement("A");
    ClassElement classB = classElement("B", classA.getType());
    InterfaceType typeA = classA.getType();
    InterfaceType typeB = classB.getType();

    assertTrue(typeB.isMoreSpecificThan(typeA));
    // the opposite test tests a different branch in isMoreSpecificThan()
    assertFalse(typeA.isMoreSpecificThan(typeB));
  }

  public void test_isMoreSpecificThan_dynamic() {
    InterfaceType type = classElement("A").getType();

    assertTrue(type.isMoreSpecificThan(DynamicTypeImpl.getInstance()));
  }

  public void test_isMoreSpecificThan_indirectSupertype() {
    ClassElement classA = classElement("A");
    ClassElement classB = classElement("B", classA.getType());
    ClassElement classC = classElement("C", classB.getType());
    InterfaceType typeA = classA.getType();
    InterfaceType typeC = classC.getType();

    assertTrue(typeC.isMoreSpecificThan(typeA));
  }

  public void test_isMoreSpecificThan_self() {
    InterfaceType type = classElement("A").getType();

    assertTrue(type.isMoreSpecificThan(type));
  }

  public void test_isSubtypeOf_directSubtype() {
    ClassElement classA = classElement("A");
    ClassElement classB = classElement("B", classA.getType());
    InterfaceType typeA = classA.getType();
    InterfaceType typeB = classB.getType();

    assertTrue(typeB.isSubtypeOf(typeA));
    assertFalse(typeA.isSubtypeOf(typeB));
  }

  public void test_isSubtypeOf_dynamic() {
    ClassElement classA = classElement("A");
    InterfaceType typeA = classA.getType();
    Type dynamicType = DynamicTypeImpl.getInstance();

    assertFalse(dynamicType.isSubtypeOf(typeA));
    assertTrue(typeA.isSubtypeOf(dynamicType));
  }

  public void test_isSubtypeOf_indirectSubtype() {
    ClassElement classA = classElement("A");
    ClassElement classB = classElement("B", classA.getType());
    ClassElement classC = classElement("C", classB.getType());
    InterfaceType typeA = classA.getType();
    InterfaceType typeC = classC.getType();

    assertTrue(typeC.isSubtypeOf(typeA));
    assertFalse(typeA.isSubtypeOf(typeC));
  }

  public void test_isSubtypeOf_interface() {
    ClassElement classA = classElement("A");
    ClassElement classB = classElement("B", classA.getType());
    ClassElementImpl classC = classElement("C");
    InterfaceType typeObject = classA.getSupertype();
    InterfaceType typeA = classA.getType();
    InterfaceType typeB = classB.getType();
    InterfaceType typeC = classC.getType();
    classC.setInterfaces(new InterfaceType[] {typeB});

    assertTrue(typeC.isSubtypeOf(typeB));
    assertTrue(typeC.isSubtypeOf(typeObject));
    assertTrue(typeC.isSubtypeOf(typeA));
    assertFalse(typeA.isSubtypeOf(typeC));
  }

  public void test_isSubtypeOf_mixins() {
    ClassElement classA = classElement("A");
    ClassElement classB = classElement("B", classA.getType());
    ClassElementImpl classC = classElement("C");
    InterfaceType typeObject = classA.getSupertype();
    InterfaceType typeA = classA.getType();
    InterfaceType typeB = classB.getType();
    InterfaceType typeC = classC.getType();
    classC.setMixins(new InterfaceType[] {typeB});

    assertTrue(typeC.isSubtypeOf(typeB));
    assertTrue(typeC.isSubtypeOf(typeObject));
    assertFalse(typeC.isSubtypeOf(typeA));
    assertFalse(typeA.isSubtypeOf(typeC));
  }

  public void test_isSubtypeOf_object() {
    ClassElement classA = classElement("A");
    InterfaceType typeA = classA.getType();
    InterfaceType typeObject = classA.getSupertype();

    assertTrue(typeA.isSubtypeOf(typeObject));
    assertFalse(typeObject.isSubtypeOf(typeA));
  }

  public void test_isSubtypeOf_self() {
    ClassElement classA = classElement("A");
    InterfaceType typeA = classA.getType();

    assertTrue(typeA.isSubtypeOf(typeA));
  }

  public void test_isSubtypeOf_typeArguments() {
    ClassElement classA = classElement("A", "E");
    ClassElement classI = classElement("I");
    ClassElement classJ = classElement("J", classI.getType());
    ClassElement classK = classElement("K");
    InterfaceType typeA = classA.getType();
    InterfaceTypeImpl typeAI = new InterfaceTypeImpl(classA);
    InterfaceTypeImpl typeAJ = new InterfaceTypeImpl(classA);
    InterfaceTypeImpl typeAK = new InterfaceTypeImpl(classA);
    typeAI.setTypeArguments(new Type[] {classI.getType()});
    typeAJ.setTypeArguments(new Type[] {classJ.getType()});
    typeAK.setTypeArguments(new Type[] {classK.getType()});

    // A<J> <: A<I> since J <: I
    assertTrue(typeAJ.isSubtypeOf(typeAI));
    assertFalse(typeAI.isSubtypeOf(typeAJ));

    // A<I> <: A<I> since I <: I
    assertTrue(typeAI.isSubtypeOf(typeAI));

    // A <: A<I> and A <: A<J>
    assertTrue(typeA.isSubtypeOf(typeAI));
    assertTrue(typeA.isSubtypeOf(typeAJ));

    // A<I> <: A and A<J> <: A
    assertTrue(typeAI.isSubtypeOf(typeA));
    assertTrue(typeAJ.isSubtypeOf(typeA));

    // A<I> !<: A<K> and A<K> !<: A<I>
    assertFalse(typeAI.isSubtypeOf(typeAK));
    assertFalse(typeAK.isSubtypeOf(typeAI));
  }

  public void test_isSupertypeOf_directSupertype() {
    ClassElement classA = classElement("A");
    ClassElement classB = classElement("B", classA.getType());
    InterfaceType typeA = classA.getType();
    InterfaceType typeB = classB.getType();

    assertFalse(typeB.isSupertypeOf(typeA));
    assertTrue(typeA.isSupertypeOf(typeB));
  }

  public void test_isSupertypeOf_dynamic() {
    ClassElement classA = classElement("A");
    InterfaceType typeA = classA.getType();
    Type dynamicType = DynamicTypeImpl.getInstance();

    assertTrue(dynamicType.isSupertypeOf(typeA));
    assertFalse(typeA.isSupertypeOf(dynamicType));
  }

  public void test_isSupertypeOf_indirectSupertype() {
    ClassElement classA = classElement("A");
    ClassElement classB = classElement("B", classA.getType());
    ClassElement classC = classElement("C", classB.getType());
    InterfaceType typeA = classA.getType();
    InterfaceType typeC = classC.getType();

    assertFalse(typeC.isSupertypeOf(typeA));
    assertTrue(typeA.isSupertypeOf(typeC));
  }

  public void test_isSupertypeOf_interface() {
    ClassElement classA = classElement("A");
    ClassElement classB = classElement("B", classA.getType());
    ClassElementImpl classC = classElement("C");
    InterfaceType typeObject = classA.getSupertype();
    InterfaceType typeA = classA.getType();
    InterfaceType typeB = classB.getType();
    InterfaceType typeC = classC.getType();
    classC.setInterfaces(new InterfaceType[] {typeB});

    assertTrue(typeB.isSupertypeOf(typeC));
    assertTrue(typeObject.isSupertypeOf(typeC));
    assertTrue(typeA.isSupertypeOf(typeC));
    assertFalse(typeC.isSupertypeOf(typeA));
  }

  public void test_isSupertypeOf_mixins() {
    ClassElement classA = classElement("A");
    ClassElement classB = classElement("B", classA.getType());
    ClassElementImpl classC = classElement("C");
    InterfaceType typeObject = classA.getSupertype();
    InterfaceType typeA = classA.getType();
    InterfaceType typeB = classB.getType();
    InterfaceType typeC = classC.getType();
    classC.setMixins(new InterfaceType[] {typeB});

    assertTrue(typeB.isSupertypeOf(typeC));
    assertTrue(typeObject.isSupertypeOf(typeC));
    assertFalse(typeA.isSupertypeOf(typeC));
    assertFalse(typeC.isSupertypeOf(typeA));
  }

  public void test_isSupertypeOf_object() {
    ClassElement classA = classElement("A");
    InterfaceType typeA = classA.getType();
    InterfaceType typeObject = classA.getSupertype();

    assertFalse(typeA.isSupertypeOf(typeObject));
    assertTrue(typeObject.isSupertypeOf(typeA));
  }

  public void test_isSupertypeOf_self() {
    ClassElement classA = classElement("A");
    InterfaceType typeA = classA.getType();

    assertTrue(typeA.isSupertypeOf(typeA));
  }

  public void test_lookUpGetter_implemented() {
    //
    // class A { g {} }
    //
    ClassElementImpl classA = classElement("A");
    String getterName = "g";
    PropertyAccessorElement getterG = getterElement(getterName, false, null);
    classA.setAccessors(new PropertyAccessorElement[] {getterG});
    InterfaceType typeA = classA.getType();

    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    ((CompilationUnitElementImpl) unit).setTypes(new ClassElement[] {classA});

    assertSame(getterG, typeA.lookUpGetter(getterName, library));
  }

  public void test_lookUpGetter_inherited() {
    //
    // class A { g {} }
    // class B extends A {}
    //
    ClassElementImpl classA = classElement("A");
    String getterName = "g";
    PropertyAccessorElement getterG = getterElement(getterName, false, null);
    classA.setAccessors(new PropertyAccessorElement[] {getterG});
    ClassElementImpl classB = classElement("B", classA.getType());
    InterfaceType typeB = classB.getType();

    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    ((CompilationUnitElementImpl) unit).setTypes(new ClassElement[] {classA, classB});

    assertSame(getterG, typeB.lookUpGetter(getterName, library));
  }

  public void test_lookUpGetter_unimplemented() {
    //
    // class A {}
    //
    ClassElementImpl classA = classElement("A");
    InterfaceType typeA = classA.getType();

    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    ((CompilationUnitElementImpl) unit).setTypes(new ClassElement[] {classA});

    assertNull(typeA.lookUpGetter("g", library));
  }

  public void test_lookUpMethod_implemented() {
    //
    // class A { m() {} }
    //
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElementImpl methodM = methodElement(methodName, null);
    classA.setMethods(new MethodElement[] {methodM});
    InterfaceType typeA = classA.getType();

    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    ((CompilationUnitElementImpl) unit).setTypes(new ClassElement[] {classA});

    assertSame(methodM, typeA.lookUpMethod(methodName, library));
  }

  public void test_lookUpMethod_inherited() {
    //
    // class A { m() {} }
    // class B extends A {}
    //
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElementImpl methodM = methodElement(methodName, null);
    classA.setMethods(new MethodElement[] {methodM});
    ClassElementImpl classB = classElement("B", classA.getType());
    InterfaceType typeB = classB.getType();

    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    ((CompilationUnitElementImpl) unit).setTypes(new ClassElement[] {classA, classB});

    assertSame(methodM, typeB.lookUpMethod(methodName, library));
  }

  public void test_lookUpMethod_parameterized() {
    //
    // class A<E> { E m(E p) {} }
    // class B<F> extends A<F> {}
    //
    ClassElementImpl classA = classElement("A", "E");
    Type typeE = classA.getType().getTypeArguments()[0];
    String methodName = "m";
    MethodElementImpl methodM = methodElement(methodName, typeE, typeE);
    classA.setMethods(new MethodElement[] {methodM});
    ClassElementImpl classB = classElement("B", "F");
    InterfaceType typeB = classB.getType();
    InterfaceTypeImpl typeAF = new InterfaceTypeImpl(classA);
    typeAF.setTypeArguments(new Type[] {typeB.getTypeArguments()[0]});
    classB.setSupertype(typeAF);

    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    ((CompilationUnitElementImpl) unit).setTypes(new ClassElement[] {classA});
    //
    // B<I>
    //
    InterfaceType typeI = classElement("I").getType();
    InterfaceTypeImpl typeBI = new InterfaceTypeImpl(classB);
    typeBI.setTypeArguments(new Type[] {typeI});

    MethodElement method = typeBI.lookUpMethod(methodName, library);
    assertNotNull(method);
    FunctionType methodType = method.getType();
    assertSame(typeI, methodType.getReturnType());
    Type[] parameterTypes = methodType.getNormalParameterTypes();
    assertLength(1, parameterTypes);
    assertSame(typeI, parameterTypes[0]);
  }

  public void test_lookUpMethod_unimplemented() {
    //
    // class A {}
    //
    ClassElementImpl classA = classElement("A");
    InterfaceType typeA = classA.getType();

    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    ((CompilationUnitElementImpl) unit).setTypes(new ClassElement[] {classA});

    assertNull(typeA.lookUpMethod("m", library));
  }

  public void test_lookUpSetter_implemented() {
    //
    // class A { s(x) {} }
    //
    ClassElementImpl classA = classElement("A");
    String setterName = "s";
    PropertyAccessorElement setterS = setterElement(setterName, false, null);
    classA.setAccessors(new PropertyAccessorElement[] {setterS});
    InterfaceType typeA = classA.getType();

    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    ((CompilationUnitElementImpl) unit).setTypes(new ClassElement[] {classA});

    assertSame(setterS, typeA.lookUpSetter(setterName, library));
  }

  public void test_lookUpSetter_inherited() {
    //
    // class A { s(x) {} }
    // class B extends A {}
    //
    ClassElementImpl classA = classElement("A");
    String setterName = "g";
    PropertyAccessorElement setterS = setterElement(setterName, false, null);
    classA.setAccessors(new PropertyAccessorElement[] {setterS});
    ClassElementImpl classB = classElement("B", classA.getType());
    InterfaceType typeB = classB.getType();

    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    ((CompilationUnitElementImpl) unit).setTypes(new ClassElement[] {classA, classB});

    assertSame(setterS, typeB.lookUpSetter(setterName, library));
  }

  public void test_lookUpSetter_unimplemented() {
    //
    // class A {}
    //
    ClassElementImpl classA = classElement("A");
    InterfaceType typeA = classA.getType();

    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    ((CompilationUnitElementImpl) unit).setTypes(new ClassElement[] {classA});

    assertNull(typeA.lookUpSetter("s", library));
  }

  public void test_setTypeArguments() {
    InterfaceTypeImpl type = (InterfaceTypeImpl) classElement("A").getType();
    Type[] typeArguments = new Type[] {classElement("B").getType(), classElement("C").getType()};
    type.setTypeArguments(typeArguments);

    assertEquals(typeArguments, type.getTypeArguments());
  }

  public void test_substitute_equal() {
    ClassElementImpl classA = classElement("A");
    TypeVariableElementImpl parameterElement = new TypeVariableElementImpl(identifier("E"));

    InterfaceTypeImpl type = new InterfaceTypeImpl(classA);
    TypeVariableTypeImpl parameter = new TypeVariableTypeImpl(parameterElement);
    type.setTypeArguments(new Type[] {parameter});

    InterfaceType argumentType = classElement("B").getType();

    InterfaceType result = type.substitute(new Type[] {argumentType}, new Type[] {parameter});
    assertEquals(classA, result.getElement());
    Type[] resultArguments = result.getTypeArguments();
    assertLength(1, resultArguments);
    assertEquals(argumentType, resultArguments[0]);
  }

  public void test_substitute_notEqual() {
    ClassElementImpl classA = classElement("A");
    TypeVariableElementImpl parameterElement = new TypeVariableElementImpl(identifier("E"));

    InterfaceTypeImpl type = new InterfaceTypeImpl(classA);
    TypeVariableTypeImpl parameter = new TypeVariableTypeImpl(parameterElement);
    type.setTypeArguments(new Type[] {parameter});

    InterfaceType argumentType = classElement("B").getType();
    TypeVariableTypeImpl parameterType = new TypeVariableTypeImpl(new TypeVariableElementImpl(
        identifier("F")));

    InterfaceType result = type.substitute(new Type[] {argumentType}, new Type[] {parameterType});
    assertEquals(classA, result.getElement());
    Type[] resultArguments = result.getTypeArguments();
    assertLength(1, resultArguments);
    assertEquals(parameter, resultArguments[0]);
  }
}
