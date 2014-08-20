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
import com.google.dart.engine.internal.element.TypeParameterElementImpl;
import com.google.dart.engine.internal.resolver.TestTypeProvider;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.type.TypeParameterType;

import static com.google.dart.engine.ast.AstFactory.identifier;
import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.functionElement;
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

  public void test_computeLongestInheritancePathToObject_recursion() {
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B", classA.getType());
    classA.setSupertype(classB.getType());
    assertEquals(2, InterfaceTypeImpl.computeLongestInheritancePathToObject(classA.getType()));
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

  public void test_computeSuperinterfaceSet_genericInterfacePath() {
    //
    //  A
    //  | implements
    //  B<T>
    //  | implements
    //  C<T>
    //
    //  D
    //
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B", "T");
    ClassElementImpl classC = classElement("C", "T");
    ClassElement classD = classElement("D");
    InterfaceType typeA = classA.getType();
    classB.setInterfaces(new InterfaceType[] {typeA});
    InterfaceTypeImpl typeBT = new InterfaceTypeImpl(classB);
    Type typeT = classC.getType().getTypeArguments()[0];
    typeBT.setTypeArguments(new Type[] {typeT});
    classC.setInterfaces(new InterfaceType[] {typeBT});
    // A
    Set<InterfaceType> superinterfacesOfA = InterfaceTypeImpl.computeSuperinterfaceSet(typeA);
    assertSizeOfSet(1, superinterfacesOfA);
    InterfaceType typeObject = ElementFactory.getObject().getType();
    assertTrue(superinterfacesOfA.contains(typeObject));
    // B<D>
    InterfaceTypeImpl typeBD = new InterfaceTypeImpl(classB);
    typeBD.setTypeArguments(new Type[] {classD.getType()});
    Set<InterfaceType> superinterfacesOfBD = InterfaceTypeImpl.computeSuperinterfaceSet(typeBD);
    assertSizeOfSet(2, superinterfacesOfBD);
    assertTrue(superinterfacesOfBD.contains(typeObject));
    assertTrue(superinterfacesOfBD.contains(typeA));
    // C<D>
    InterfaceTypeImpl typeCD = new InterfaceTypeImpl(classC);
    typeCD.setTypeArguments(new Type[] {classD.getType()});
    Set<InterfaceType> superinterfacesOfCD = InterfaceTypeImpl.computeSuperinterfaceSet(typeCD);
    assertSizeOfSet(3, superinterfacesOfCD);
    assertTrue(superinterfacesOfCD.contains(typeObject));
    assertTrue(superinterfacesOfCD.contains(typeA));
    assertTrue(superinterfacesOfCD.contains(typeBD));
  }

  public void test_computeSuperinterfaceSet_genericSuperclassPath() {
    //
    //  A
    //  |
    //  B<T>
    //  |
    //  C<T>
    //
    //  D
    //
    ClassElement classA = classElement("A");
    InterfaceType typeA = classA.getType();
    ClassElement classB = classElement("B", typeA, "T");
    ClassElementImpl classC = classElement("C", "T");
    InterfaceTypeImpl typeBT = new InterfaceTypeImpl(classB);
    Type typeT = classC.getType().getTypeArguments()[0];
    typeBT.setTypeArguments(new Type[] {typeT});
    classC.setSupertype(typeBT);
    ClassElement classD = classElement("D");
    // A
    Set<InterfaceType> superinterfacesOfA = InterfaceTypeImpl.computeSuperinterfaceSet(typeA);
    assertSizeOfSet(1, superinterfacesOfA);
    InterfaceType typeObject = ElementFactory.getObject().getType();
    assertTrue(superinterfacesOfA.contains(typeObject));
    // B<D>
    InterfaceTypeImpl typeBD = new InterfaceTypeImpl(classB);
    typeBD.setTypeArguments(new Type[] {classD.getType()});
    Set<InterfaceType> superinterfacesOfBD = InterfaceTypeImpl.computeSuperinterfaceSet(typeBD);
    assertSizeOfSet(2, superinterfacesOfBD);
    assertTrue(superinterfacesOfBD.contains(typeObject));
    assertTrue(superinterfacesOfBD.contains(typeA));
    // C<D>
    InterfaceTypeImpl typeCD = new InterfaceTypeImpl(classC);
    typeCD.setTypeArguments(new Type[] {classD.getType()});
    Set<InterfaceType> superinterfacesOfCD = InterfaceTypeImpl.computeSuperinterfaceSet(typeCD);
    assertSizeOfSet(3, superinterfacesOfCD);
    assertTrue(superinterfacesOfCD.contains(typeObject));
    assertTrue(superinterfacesOfCD.contains(typeA));
    assertTrue(superinterfacesOfCD.contains(typeBD));
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
    assertSizeOfSet(3, superinterfacesOfD);
    assertTrue(superinterfacesOfD.contains(ElementFactory.getObject().getType()));
    assertTrue(superinterfacesOfD.contains(classA.getType()));
    assertTrue(superinterfacesOfD.contains(classC.getType()));
    // E
    Set<InterfaceType> superinterfacesOfE = InterfaceTypeImpl.computeSuperinterfaceSet(classE.getType());
    assertSizeOfSet(5, superinterfacesOfE);
    assertTrue(superinterfacesOfE.contains(ElementFactory.getObject().getType()));
    assertTrue(superinterfacesOfE.contains(classA.getType()));
    assertTrue(superinterfacesOfE.contains(classB.getType()));
    assertTrue(superinterfacesOfE.contains(classC.getType()));
    assertTrue(superinterfacesOfE.contains(classD.getType()));
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
    assertSizeOfSet(3, superinterfacesOfD);
    assertTrue(superinterfacesOfD.contains(ElementFactory.getObject().getType()));
    assertTrue(superinterfacesOfD.contains(classA.getType()));
    assertTrue(superinterfacesOfD.contains(classC.getType()));
    // E
    Set<InterfaceType> superinterfacesOfE = InterfaceTypeImpl.computeSuperinterfaceSet(classE.getType());
    assertSizeOfSet(5, superinterfacesOfE);
    assertTrue(superinterfacesOfE.contains(ElementFactory.getObject().getType()));
    assertTrue(superinterfacesOfE.contains(classA.getType()));
    assertTrue(superinterfacesOfE.contains(classB.getType()));
    assertTrue(superinterfacesOfE.contains(classC.getType()));
    assertTrue(superinterfacesOfE.contains(classD.getType()));
  }

  public void test_computeSuperinterfaceSet_recursion() {
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B", classA.getType());
    classA.setSupertype(classB.getType());

    Set<InterfaceType> superinterfacesOfB = InterfaceTypeImpl.computeSuperinterfaceSet(classB.getType());
    assertSizeOfSet(2, superinterfacesOfB);
  }

  public void test_computeSuperinterfaceSet_singleInterfacePath() {
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B");
    ClassElementImpl classC = classElement("C");
    classB.setInterfaces(new InterfaceType[] {classA.getType()});
    classC.setInterfaces(new InterfaceType[] {classB.getType()});
    // A
    Set<InterfaceType> superinterfacesOfA = InterfaceTypeImpl.computeSuperinterfaceSet(classA.getType());
    assertSizeOfSet(1, superinterfacesOfA);
    assertTrue(superinterfacesOfA.contains(ElementFactory.getObject().getType()));
    // B
    Set<InterfaceType> superinterfacesOfB = InterfaceTypeImpl.computeSuperinterfaceSet(classB.getType());
    assertSizeOfSet(2, superinterfacesOfB);
    assertTrue(superinterfacesOfB.contains(ElementFactory.getObject().getType()));
    assertTrue(superinterfacesOfB.contains(classA.getType()));
    // C
    Set<InterfaceType> superinterfacesOfC = InterfaceTypeImpl.computeSuperinterfaceSet(classC.getType());
    assertSizeOfSet(3, superinterfacesOfC);
    assertTrue(superinterfacesOfC.contains(ElementFactory.getObject().getType()));
    assertTrue(superinterfacesOfC.contains(classA.getType()));
    assertTrue(superinterfacesOfC.contains(classB.getType()));
  }

  public void test_computeSuperinterfaceSet_singleSuperclassPath() {
    //
    //  A
    //  |
    //  B
    //  |
    //  C
    //
    ClassElement classA = classElement("A");
    ClassElement classB = classElement("B", classA.getType());
    ClassElement classC = classElement("C", classB.getType());
    // A
    Set<InterfaceType> superinterfacesOfA = InterfaceTypeImpl.computeSuperinterfaceSet(classA.getType());
    assertSizeOfSet(1, superinterfacesOfA);
    assertTrue(superinterfacesOfA.contains(ElementFactory.getObject().getType()));
    // B
    Set<InterfaceType> superinterfacesOfB = InterfaceTypeImpl.computeSuperinterfaceSet(classB.getType());
    assertSizeOfSet(2, superinterfacesOfB);
    assertTrue(superinterfacesOfB.contains(ElementFactory.getObject().getType()));
    assertTrue(superinterfacesOfB.contains(classA.getType()));
    // C
    Set<InterfaceType> superinterfacesOfC = InterfaceTypeImpl.computeSuperinterfaceSet(classC.getType());
    assertSizeOfSet(3, superinterfacesOfC);
    assertTrue(superinterfacesOfC.contains(ElementFactory.getObject().getType()));
    assertTrue(superinterfacesOfC.contains(classA.getType()));
    assertTrue(superinterfacesOfC.contains(classB.getType()));
  }

  public void test_creation() {
    assertNotNull(new InterfaceTypeImpl(classElement("A")));
  }

  public void test_getAccessors() {
    ClassElementImpl typeElement = classElement("A");
    PropertyAccessorElement getterG = getterElement("g", false, null);
    PropertyAccessorElement getterH = getterElement("h", false, null);
    typeElement.setAccessors(new PropertyAccessorElement[] {getterG, getterH});
    InterfaceTypeImpl type = new InterfaceTypeImpl(typeElement);

    assertEquals(2, type.getAccessors().length);
  }

  public void test_getAccessors_empty() {
    ClassElementImpl typeElement = classElement("A");
    InterfaceTypeImpl type = new InterfaceTypeImpl(typeElement);

    assertEquals(0, type.getAccessors().length);
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
    // class A<E> { E get g {} }
    //
    ClassElementImpl classA = classElement("A", "E");
    Type typeE = classA.getType().getTypeArguments()[0];
    String getterName = "g";
    PropertyAccessorElement getterG = getterElement(getterName, false, typeE);
    classA.setAccessors(new PropertyAccessorElement[] {getterG});
    ((FunctionTypeImpl) getterG.getType()).setTypeArguments(classA.getType().getTypeArguments());
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

  public void test_getLeastUpperBound_twoComparables() {
    InterfaceType string = typeProvider.getStringType();
    InterfaceType num = typeProvider.getNumType();

    assertEquals(typeProvider.getObjectType(), string.getLeastUpperBound(num));
  }

  public void test_getLeastUpperBound_typeParameters_different() {
    //
    // class List<int>
    // class List<double>
    //
    InterfaceType listType = typeProvider.getListType();
    InterfaceType intType = typeProvider.getIntType();
    InterfaceType doubleType = typeProvider.getDoubleType();
    InterfaceType listOfIntType = listType.substitute(new Type[] {intType});
    InterfaceType listOfDoubleType = listType.substitute(new Type[] {doubleType});
    assertEquals(typeProvider.getObjectType(), listOfIntType.getLeastUpperBound(listOfDoubleType));
  }

  public void test_getLeastUpperBound_typeParameters_same() {
    //
    // List<int>
    // List<int>
    //
    InterfaceType listType = typeProvider.getListType();
    InterfaceType intType = typeProvider.getIntType();
    InterfaceType listOfIntType = listType.substitute(new Type[] {intType});
    assertEquals(listOfIntType, listOfIntType.getLeastUpperBound(listOfIntType));
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
    ((FunctionTypeImpl) methodM.getType()).setTypeArguments(classA.getType().getTypeArguments());
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

  public void test_getMethods() {
    ClassElementImpl typeElement = classElement("A");
    MethodElementImpl methodOne = methodElement("one", null);
    MethodElementImpl methodTwo = methodElement("two", null);
    typeElement.setMethods(new MethodElement[] {methodOne, methodTwo});
    InterfaceTypeImpl type = new InterfaceTypeImpl(typeElement);

    assertEquals(2, type.getMethods().length);
  }

  public void test_getMethods_empty() {
    ClassElementImpl typeElement = classElement("A");
    InterfaceTypeImpl type = new InterfaceTypeImpl(typeElement);

    assertEquals(0, type.getMethods().length);
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
    // class A<E> { set s(E p) {} }
    //
    ClassElementImpl classA = classElement("A", "E");
    Type typeE = classA.getType().getTypeArguments()[0];
    String setterName = "s";
    PropertyAccessorElement setterS = setterElement(setterName, false, typeE);
    classA.setAccessors(new PropertyAccessorElement[] {setterS});
    ((FunctionTypeImpl) setterS.getType()).setTypeArguments(classA.getType().getTypeArguments());
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

  public void test_hashCode() {
    ClassElement classA = classElement("A");
    InterfaceType typeA = classA.getType();

    assertFalse(0 == typeA.hashCode());
  }

  public void test_isAssignableTo_typeVariables() {
    //
    // class A<E> {}
    // class B<F, G> {
    //   A<F> af;
    //   f (A<G> ag) {
    //     af = ag;
    //   }
    // }
    //
    ClassElement classA = classElement("A", "E");
    ClassElement classB = classElement("B", "F", "G");
    InterfaceTypeImpl typeAF = new InterfaceTypeImpl(classA);
    typeAF.setTypeArguments(new Type[] {classB.getTypeParameters()[0].getType()});
    InterfaceTypeImpl typeAG = new InterfaceTypeImpl(classA);
    typeAG.setTypeArguments(new Type[] {classB.getTypeParameters()[1].getType()});

    assertFalse(typeAG.isAssignableTo(typeAF));
  }

  public void test_isAssignableTo_void() {
    assertFalse(VoidTypeImpl.getInstance().isAssignableTo(typeProvider.getIntType()));
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
    // the opposite test tests a different branch in isMoreSpecificThan()
    assertFalse(typeA.isMoreSpecificThan(typeB));
  }

  public void test_isMoreSpecificThan_dynamic() {
    InterfaceType type = classElement("A").getType();

    assertTrue(type.isMoreSpecificThan(DynamicTypeImpl.getInstance()));
  }

  public void test_isMoreSpecificThan_generic() {
    ClassElement classA = classElement("A", "E");
    ClassElement classB = classElement("B");
    Type dynamicType = DynamicTypeImpl.getInstance();
    InterfaceType typeAOfDynamic = classA.getType().substitute(new Type[] {dynamicType});
    InterfaceType typeAOfB = classA.getType().substitute(new Type[] {classB.getType()});

    assertFalse(typeAOfDynamic.isMoreSpecificThan(typeAOfB));
    assertTrue(typeAOfB.isMoreSpecificThan(typeAOfDynamic));
  }

  public void test_isMoreSpecificThan_self() {
    InterfaceType type = classElement("A").getType();

    assertTrue(type.isMoreSpecificThan(type));
  }

  public void test_isMoreSpecificThan_transitive_interface() {
    //
    //  class A {}
    //  class B extends A {}
    //  class C implements B {}
    //
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B", classA.getType());
    ClassElementImpl classC = classElement("C");
    classC.setInterfaces(new InterfaceType[] {classB.getType()});
    InterfaceType typeA = classA.getType();
    InterfaceType typeC = classC.getType();

    assertTrue(typeC.isMoreSpecificThan(typeA));
  }

  public void test_isMoreSpecificThan_transitive_mixin() {
    //
    //  class A {}
    //  class B extends A {}
    //  class C with B {}
    //
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B", classA.getType());
    ClassElementImpl classC = classElement("C");
    classC.setMixins(new InterfaceType[] {classB.getType()});
    InterfaceType typeA = classA.getType();
    InterfaceType typeC = classC.getType();

    assertTrue(typeC.isMoreSpecificThan(typeA));
  }

  public void test_isMoreSpecificThan_transitive_recursive() {
    //
    //  class A extends B {}
    //  class B extends A {}
    //  class C {}
    //
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B", classA.getType());
    ClassElementImpl classC = classElement("C");
    InterfaceType typeA = classA.getType();
    InterfaceType typeC = classC.getType();
    classA.setSupertype(classB.getType());

    assertFalse(typeA.isMoreSpecificThan(typeC));
  }

  public void test_isMoreSpecificThan_transitive_superclass() {
    //
    //  class A {}
    //  class B extends A {}
    //  class C extends B {}
    //
    ClassElement classA = classElement("A");
    ClassElement classB = classElement("B", classA.getType());
    ClassElement classC = classElement("C", classB.getType());
    InterfaceType typeA = classA.getType();
    InterfaceType typeC = classC.getType();

    assertTrue(typeC.isMoreSpecificThan(typeA));
  }

  public void test_isMoreSpecificThan_typeParameterType() {
    //
    // class A<E> {}
    //
    ClassElement classA = classElement("A", "E");
    InterfaceType typeA = classA.getType();
    TypeParameterType parameterType = classA.getTypeParameters()[0].getType();
    Type objectType = typeProvider.getObjectType();

    assertTrue(parameterType.isMoreSpecificThan(objectType));
    assertFalse(parameterType.isMoreSpecificThan(typeA));
  }

  public void test_isMoreSpecificThan_typeParameterType_withBound() {
    //
    // class A {}
    // class B<E extends A> {}
    //
    ClassElement classA = classElement("A");
    InterfaceType typeA = classA.getType();
    ClassElementImpl classB = classElement("B");
    TypeParameterElementImpl parameterEA = new TypeParameterElementImpl(identifier("E"));
    TypeParameterType parameterAEType = new TypeParameterTypeImpl(parameterEA);
    parameterEA.setBound(typeA);
    parameterEA.setType(parameterAEType);
    classB.setTypeParameters(new TypeParameterElementImpl[] {parameterEA});

    assertTrue(parameterAEType.isMoreSpecificThan(typeA));
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

    assertTrue(dynamicType.isSubtypeOf(typeA));
    assertTrue(typeA.isSubtypeOf(dynamicType));
  }

  public void test_isSubtypeOf_function() throws Exception {
    //
    // void f(String s) {}
    // class A {
    //   void call(String s) {}
    // }
    //
    InterfaceType stringType = typeProvider.getStringType();
    ClassElementImpl classA = classElement("A");
    classA.setMethods(new MethodElement[] {methodElement(
        "call",
        VoidTypeImpl.getInstance(),
        stringType)});

    FunctionType functionType = functionElement("f", new ClassElement[] {stringType.getElement()}).getType();

    assertTrue(classA.getType().isSubtypeOf(functionType));
  }

  public void test_isSubtypeOf_generic() {
    ClassElement classA = classElement("A", "E");
    ClassElement classB = classElement("B");
    Type dynamicType = DynamicTypeImpl.getInstance();
    InterfaceType typeAOfDynamic = classA.getType().substitute(new Type[] {dynamicType});
    InterfaceType typeAOfB = classA.getType().substitute(new Type[] {classB.getType()});

    assertTrue(typeAOfDynamic.isSubtypeOf(typeAOfB));
    assertTrue(typeAOfB.isSubtypeOf(typeAOfDynamic));
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
    //
    // class A {}
    // class B extends A {}
    // class C with B {}
    //
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
    assertTrue(typeC.isSubtypeOf(typeA));
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

  public void test_isSubtypeOf_transitive_recursive() {
    //
    //  class A extends B {}
    //  class B extends A {}
    //  class C {}
    //
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B", classA.getType());
    ClassElementImpl classC = classElement("C");
    InterfaceType typeA = classA.getType();
    InterfaceType typeC = classC.getType();
    classA.setSupertype(classB.getType());

    assertFalse(typeA.isSubtypeOf(typeC));
  }

  public void test_isSubtypeOf_transitive_superclass() {
    ClassElement classA = classElement("A");
    ClassElement classB = classElement("B", classA.getType());
    ClassElement classC = classElement("C", classB.getType());
    InterfaceType typeA = classA.getType();
    InterfaceType typeC = classC.getType();

    assertTrue(typeC.isSubtypeOf(typeA));
    assertFalse(typeA.isSubtypeOf(typeC));
  }

  public void test_isSubtypeOf_typeArguments() {
    Type dynamicType = DynamicTypeImpl.getInstance();
    ClassElement classA = classElement("A", "E");
    ClassElement classI = classElement("I");
    ClassElement classJ = classElement("J", classI.getType());
    ClassElement classK = classElement("K");
    InterfaceType typeA = classA.getType();
    InterfaceType typeA_dynamic = typeA.substitute(new Type[] {dynamicType});
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
    assertTrue(typeA_dynamic.isSubtypeOf(typeAI));
    assertTrue(typeA_dynamic.isSubtypeOf(typeAJ));

    // A<I> <: A and A<J> <: A
    assertTrue(typeAI.isSubtypeOf(typeA_dynamic));
    assertTrue(typeAJ.isSubtypeOf(typeA_dynamic));

    // A<I> !<: A<K> and A<K> !<: A<I>
    assertFalse(typeAI.isSubtypeOf(typeAK));
    assertFalse(typeAK.isSubtypeOf(typeAI));
  }

  public void test_isSubtypeOf_typeParameter() {
    //
    // class A<E> {}
    //
    ClassElement classA = classElement("A", "E");
    InterfaceType typeA = classA.getType();
    TypeParameterType parameterType = classA.getTypeParameters()[0].getType();

    assertFalse(typeA.isSubtypeOf(parameterType));
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
    assertTrue(typeA.isSupertypeOf(dynamicType));
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
    //
    // class A {}
    // class B extends A {}
    // class C with B {}
    //
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
    assertTrue(typeA.isSupertypeOf(typeC));
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

  public void test_lookUpGetter_recursive() {
    //
    // class A extends B {}
    // class B extends A {}
    //
    ClassElementImpl classA = classElement("A");
    InterfaceType typeA = classA.getType();
    ClassElementImpl classB = classElement("B", typeA);
    classA.setSupertype(classB.getType());

    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    ((CompilationUnitElementImpl) unit).setTypes(new ClassElement[] {classA, classB});

    assertNull(typeA.lookUpGetter("g", library));
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
    ((FunctionTypeImpl) methodM.getType()).setTypeArguments(classA.getType().getTypeArguments());
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

  public void test_lookUpMethod_recursive() {
    //
    // class A extends B {}
    // class B extends A {}
    //
    ClassElementImpl classA = classElement("A");
    InterfaceType typeA = classA.getType();
    ClassElementImpl classB = classElement("B", typeA);
    classA.setSupertype(classB.getType());

    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    ((CompilationUnitElementImpl) unit).setTypes(new ClassElement[] {classA, classB});

    assertNull(typeA.lookUpMethod("m", library));
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

  public void test_lookUpSetter_recursive() {
    //
    // class A extends B {}
    // class B extends A {}
    //
    ClassElementImpl classA = classElement("A");
    InterfaceType typeA = classA.getType();
    ClassElementImpl classB = classElement("B", typeA);
    classA.setSupertype(classB.getType());

    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    ((CompilationUnitElementImpl) unit).setTypes(new ClassElement[] {classA, classB});

    assertNull(typeA.lookUpSetter("s", library));
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
    ClassElement classAE = classElement("A", "E");
    InterfaceType typeAE = classAE.getType();
    InterfaceType argumentType = classElement("B").getType();
    Type[] args = {argumentType};
    Type[] params = {classAE.getTypeParameters()[0].getType()};
    InterfaceType typeAESubbed = typeAE.substitute(args, params);

    assertEquals(classAE, typeAESubbed.getElement());
    Type[] resultArguments = typeAESubbed.getTypeArguments();
    assertLength(1, resultArguments);
    assertEquals(argumentType, resultArguments[0]);
  }

  public void test_substitute_exception() {
    try {
      ClassElementImpl classA = classElement("A");
      InterfaceTypeImpl type = new InterfaceTypeImpl(classA);
      InterfaceType argumentType = classElement("B").getType();

      type.substitute(new Type[] {argumentType}, new Type[] {});
      fail("Expected to encounter exception, argument and parameter type array lengths not equal.");
    } catch (Exception e) {
      // Expected result
    }
  }

  public void test_substitute_notEqual() {
    // The [test_substitute_equals] above has a slightly higher level implementation.
    ClassElementImpl classA = classElement("A");
    TypeParameterElementImpl parameterElement = new TypeParameterElementImpl(identifier("E"));

    InterfaceTypeImpl type = new InterfaceTypeImpl(classA);
    TypeParameterTypeImpl parameter = new TypeParameterTypeImpl(parameterElement);
    type.setTypeArguments(new Type[] {parameter});

    InterfaceType argumentType = classElement("B").getType();
    TypeParameterTypeImpl parameterType = new TypeParameterTypeImpl(new TypeParameterElementImpl(
        identifier("F")));

    InterfaceType result = type.substitute(new Type[] {argumentType}, new Type[] {parameterType});
    assertEquals(classA, result.getElement());
    Type[] resultArguments = result.getTypeArguments();
    assertLength(1, resultArguments);
    assertEquals(parameter, resultArguments[0]);
  }
}
