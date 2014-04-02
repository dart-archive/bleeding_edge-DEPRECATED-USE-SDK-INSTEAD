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
package com.google.dart.engine.internal.resolver;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.context.AnalysisContextFactory;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.error.StaticTypeWarningCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.MethodElementImpl;
import com.google.dart.engine.internal.element.ParameterElementImpl;
import com.google.dart.engine.internal.element.PropertyAccessorElementImpl;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.utilities.dart.ParameterKind;
import com.google.dart.engine.utilities.io.FileUtilities2;

import static com.google.dart.engine.ast.AstFactory.identifier;
import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.getterElement;
import static com.google.dart.engine.element.ElementFactory.library;
import static com.google.dart.engine.element.ElementFactory.methodElement;
import static com.google.dart.engine.element.ElementFactory.setterElement;

import java.util.HashSet;

public class InheritanceManagerTest extends EngineTestCase {
  /**
   * The type provider used to access the types.
   */
  private TestTypeProvider typeProvider;

  /**
   * The library containing the code being resolved.
   */
  private LibraryElementImpl definingLibrary;

  /**
   * The inheritance manager being tested.
   */
  private InheritanceManager inheritanceManager;

  /**
   * The number of members that Object implements (as determined by {@link TestTypeProvider}).
   */
  private int numOfMembersInObject;

  @Override
  public void setUp() {
    typeProvider = new TestTypeProvider();
    inheritanceManager = createInheritanceManager();
    InterfaceType objectType = typeProvider.getObjectType();
    numOfMembersInObject = objectType.getMethods().length + objectType.getAccessors().length;
  }

  public void test_getMapOfMembersInheritedFromClasses_accessor_extends() throws Exception {
    // class A { int get g; }
    // class B extends A {}
    ClassElementImpl classA = classElement("A");
    String getterName = "g";
    PropertyAccessorElement getterG = getterElement(getterName, false, typeProvider.getIntType());
    classA.setAccessors(new PropertyAccessorElement[] {getterG});

    ClassElementImpl classB = classElement("B", classA.getType());

    MemberMap mapB = inheritanceManager.getMapOfMembersInheritedFromClasses(classB);
    MemberMap mapA = inheritanceManager.getMapOfMembersInheritedFromClasses(classA);

    assertEquals(numOfMembersInObject, mapA.getSize());
    assertEquals(numOfMembersInObject + 1, mapB.getSize());
    assertSame(getterG, mapB.get(getterName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }

  public void test_getMapOfMembersInheritedFromClasses_accessor_implements() throws Exception {
    // class A { int get g; }
    // class B implements A {}
    ClassElementImpl classA = classElement("A");
    String getterName = "g";
    PropertyAccessorElement getterG = getterElement(getterName, false, typeProvider.getIntType());
    classA.setAccessors(new PropertyAccessorElement[] {getterG});

    ClassElementImpl classB = classElement("B");
    classB.setInterfaces(new InterfaceType[] {classA.getType()});

    MemberMap mapB = inheritanceManager.getMapOfMembersInheritedFromClasses(classB);
    MemberMap mapA = inheritanceManager.getMapOfMembersInheritedFromClasses(classA);

    assertEquals(numOfMembersInObject, mapA.getSize());
    assertEquals(numOfMembersInObject, mapB.getSize());
    assertNull(mapB.get(getterName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }

  public void test_getMapOfMembersInheritedFromClasses_accessor_with() throws Exception {
    // class A { int get g; }
    // class B extends Object with A {}
    ClassElementImpl classA = classElement("A");
    String getterName = "g";
    PropertyAccessorElement getterG = getterElement(getterName, false, typeProvider.getIntType());
    classA.setAccessors(new PropertyAccessorElement[] {getterG});

    ClassElementImpl classB = classElement("B");
    classB.setMixins(new InterfaceType[] {classA.getType()});

    MemberMap mapB = inheritanceManager.getMapOfMembersInheritedFromClasses(classB);
    MemberMap mapA = inheritanceManager.getMapOfMembersInheritedFromClasses(classA);

    assertEquals(numOfMembersInObject, mapA.getSize());
    assertEquals(numOfMembersInObject + 1, mapB.getSize());
    assertSame(getterG, mapB.get(getterName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }

  public void test_getMapOfMembersInheritedFromClasses_implicitExtends() throws Exception {
    // class A {}
    ClassElementImpl classA = classElement("A");

    MemberMap mapA = inheritanceManager.getMapOfMembersInheritedFromClasses(classA);
    assertEquals(numOfMembersInObject, mapA.getSize());
    assertNoErrors(classA);
  }

  public void test_getMapOfMembersInheritedFromClasses_method_extends() throws Exception {
    // class A { int g(); }
    // class B extends A {}
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement methodM = methodElement(methodName, typeProvider.getIntType());
    classA.setMethods(new MethodElement[] {methodM});

    ClassElementImpl classB = classElement("B");
    classB.setSupertype(classA.getType());

    MemberMap mapB = inheritanceManager.getMapOfMembersInheritedFromClasses(classB);
    MemberMap mapA = inheritanceManager.getMapOfMembersInheritedFromClasses(classA);

    assertEquals(numOfMembersInObject, mapA.getSize());
    assertEquals(numOfMembersInObject + 1, mapB.getSize());
    assertSame(methodM, mapB.get(methodName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }

  public void test_getMapOfMembersInheritedFromClasses_method_implements() throws Exception {
    // class A { int g(); }
    // class B implements A {}
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement methodM = methodElement(methodName, typeProvider.getIntType());
    classA.setMethods(new MethodElement[] {methodM});

    ClassElementImpl classB = classElement("B");
    classB.setInterfaces(new InterfaceType[] {classA.getType()});

    MemberMap mapB = inheritanceManager.getMapOfMembersInheritedFromClasses(classB);
    MemberMap mapA = inheritanceManager.getMapOfMembersInheritedFromClasses(classA);

    assertEquals(numOfMembersInObject, mapA.getSize());
    assertEquals(numOfMembersInObject, mapB.getSize());
    assertNull(mapB.get(methodName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }

  public void test_getMapOfMembersInheritedFromClasses_method_with() throws Exception {
    // class A { int g(); }
    // class B extends Object with A {}
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement methodM = methodElement(methodName, typeProvider.getIntType());
    classA.setMethods(new MethodElement[] {methodM});

    ClassElementImpl classB = classElement("B");
    classB.setMixins(new InterfaceType[] {classA.getType()});

    MemberMap mapB = inheritanceManager.getMapOfMembersInheritedFromClasses(classB);
    MemberMap mapA = inheritanceManager.getMapOfMembersInheritedFromClasses(classA);

    assertEquals(numOfMembersInObject, mapA.getSize());
    assertEquals(numOfMembersInObject + 1, mapB.getSize());
    assertSame(methodM, mapB.get(methodName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }

  public void test_getMapOfMembersInheritedFromInterfaces_accessor_extends() throws Exception {
    // class A { int get g; }
    // class B extends A {}
    ClassElementImpl classA = classElement("A");
    String getterName = "g";
    PropertyAccessorElement getterG = getterElement(getterName, false, typeProvider.getIntType());
    classA.setAccessors(new PropertyAccessorElement[] {getterG});

    ClassElementImpl classB = classElement("B", classA.getType());

    MemberMap mapB = inheritanceManager.getMapOfMembersInheritedFromInterfaces(classB);
    MemberMap mapA = inheritanceManager.getMapOfMembersInheritedFromInterfaces(classA);

    assertEquals(numOfMembersInObject, mapA.getSize());
    assertEquals(numOfMembersInObject + 1, mapB.getSize());
    assertSame(getterG, mapB.get(getterName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }

  public void test_getMapOfMembersInheritedFromInterfaces_accessor_implements() throws Exception {
    // class A { int get g; }
    // class B implements A {}
    ClassElementImpl classA = classElement("A");
    String getterName = "g";
    PropertyAccessorElement getterG = getterElement(getterName, false, typeProvider.getIntType());
    classA.setAccessors(new PropertyAccessorElement[] {getterG});

    ClassElementImpl classB = classElement("B");
    classB.setInterfaces(new InterfaceType[] {classA.getType()});

    MemberMap mapB = inheritanceManager.getMapOfMembersInheritedFromInterfaces(classB);
    MemberMap mapA = inheritanceManager.getMapOfMembersInheritedFromInterfaces(classA);

    assertEquals(numOfMembersInObject, mapA.getSize());
    assertEquals(numOfMembersInObject + 1, mapB.getSize());
    assertSame(getterG, mapB.get(getterName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }

  public void test_getMapOfMembersInheritedFromInterfaces_accessor_with() throws Exception {
    // class A { int get g; }
    // class B extends Object with A {}
    ClassElementImpl classA = classElement("A");
    String getterName = "g";
    PropertyAccessorElement getterG = getterElement(getterName, false, typeProvider.getIntType());
    classA.setAccessors(new PropertyAccessorElement[] {getterG});

    ClassElementImpl classB = classElement("B");
    classB.setMixins(new InterfaceType[] {classA.getType()});

    MemberMap mapB = inheritanceManager.getMapOfMembersInheritedFromInterfaces(classB);
    MemberMap mapA = inheritanceManager.getMapOfMembersInheritedFromInterfaces(classA);

    assertEquals(numOfMembersInObject, mapA.getSize());
    assertEquals(numOfMembersInObject + 1, mapB.getSize());
    assertSame(getterG, mapB.get(getterName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }

  public void test_getMapOfMembersInheritedFromInterfaces_implicitExtends() throws Exception {
    // class A {}
    ClassElementImpl classA = classElement("A");

    MemberMap mapA = inheritanceManager.getMapOfMembersInheritedFromInterfaces(classA);
    assertEquals(numOfMembersInObject, mapA.getSize());
    assertNoErrors(classA);
  }

  public void test_getMapOfMembersInheritedFromInterfaces_inconsistentMethodInheritance_getter_method()
      throws Exception {
    // class I1 { int m(); }
    // class I2 { int get m; }
    // class A implements I2, I1 {}
    ClassElementImpl classI1 = classElement("I1");
    String methodName = "m";
    MethodElement methodM = methodElement(methodName, typeProvider.getIntType());
    classI1.setMethods(new MethodElement[] {methodM});

    ClassElementImpl classI2 = classElement("I2");
    PropertyAccessorElement getter = getterElement(methodName, false, typeProvider.getIntType());
    classI2.setAccessors(new PropertyAccessorElement[] {getter});

    ClassElementImpl classA = classElement("A");
    classA.setInterfaces(new InterfaceType[] {classI2.getType(), classI1.getType()});

    MemberMap mapA = inheritanceManager.getMapOfMembersInheritedFromInterfaces(classA);
    assertEquals(numOfMembersInObject, mapA.getSize());
    assertNull(mapA.get(methodName));
    assertErrors(classA, StaticWarningCode.INCONSISTENT_METHOD_INHERITANCE_GETTER_AND_METHOD);
  }

  public void test_getMapOfMembersInheritedFromInterfaces_inconsistentMethodInheritance_int_str()
      throws Exception {
    // class I1 { int m(); }
    // class I2 { String m(); }
    // class A implements I1, I2 {}
    ClassElementImpl classI1 = classElement("I1");
    String methodName = "m";
    MethodElement methodM1 = methodElement(methodName, null, typeProvider.getIntType());
    classI1.setMethods(new MethodElement[] {methodM1});

    ClassElementImpl classI2 = classElement("I2");
    MethodElement methodM2 = methodElement(methodName, null, typeProvider.getStringType());
    classI2.setMethods(new MethodElement[] {methodM2});

    ClassElementImpl classA = classElement("A");
    classA.setInterfaces(new InterfaceType[] {classI1.getType(), classI2.getType()});

    MemberMap mapA = inheritanceManager.getMapOfMembersInheritedFromInterfaces(classA);
    assertEquals(numOfMembersInObject, mapA.getSize());
    assertNull(mapA.get(methodName));
    assertErrors(classA, StaticTypeWarningCode.INCONSISTENT_METHOD_INHERITANCE);
  }

  public void test_getMapOfMembersInheritedFromInterfaces_inconsistentMethodInheritance_method_getter()
      throws Exception {
    // class I1 { int m(); }
    // class I2 { int get m; }
    // class A implements I1, I2 {}
    ClassElementImpl classI1 = classElement("I1");
    String methodName = "m";
    MethodElement methodM = methodElement(methodName, typeProvider.getIntType());
    classI1.setMethods(new MethodElement[] {methodM});

    ClassElementImpl classI2 = classElement("I2");
    PropertyAccessorElement getter = getterElement(methodName, false, typeProvider.getIntType());
    classI2.setAccessors(new PropertyAccessorElement[] {getter});

    ClassElementImpl classA = classElement("A");
    classA.setInterfaces(new InterfaceType[] {classI1.getType(), classI2.getType()});

    MemberMap mapA = inheritanceManager.getMapOfMembersInheritedFromInterfaces(classA);
    assertEquals(numOfMembersInObject, mapA.getSize());
    assertNull(mapA.get(methodName));
    assertErrors(classA, StaticWarningCode.INCONSISTENT_METHOD_INHERITANCE_GETTER_AND_METHOD);
  }

  public void test_getMapOfMembersInheritedFromInterfaces_inconsistentMethodInheritance_numOfRequiredParams()
      throws Exception {
    // class I1 { dynamic m(int, [int]); }
    // class I2 { dynamic m(int, int, int); }
    // class A implements I1, I2 {}
    ClassElementImpl classI1 = classElement("I1");
    String methodName = "m";
    MethodElementImpl methodM1 = methodElement(methodName, typeProvider.getDynamicType());
    ParameterElementImpl parameter1 = new ParameterElementImpl(identifier("a1"));
    parameter1.setType(typeProvider.getIntType());
    parameter1.setParameterKind(ParameterKind.REQUIRED);
    ParameterElementImpl parameter2 = new ParameterElementImpl(identifier("a2"));
    parameter2.setType(typeProvider.getIntType());
    parameter2.setParameterKind(ParameterKind.POSITIONAL);
    methodM1.setParameters(new ParameterElement[] {parameter1, parameter2});
    classI1.setMethods(new MethodElement[] {methodM1});

    ClassElementImpl classI2 = classElement("I2");
    MethodElementImpl methodM2 = methodElement(methodName, typeProvider.getDynamicType());
    ParameterElementImpl parameter3 = new ParameterElementImpl(identifier("a3"));
    parameter3.setType(typeProvider.getIntType());
    parameter3.setParameterKind(ParameterKind.REQUIRED);
    ParameterElementImpl parameter4 = new ParameterElementImpl(identifier("a4"));
    parameter4.setType(typeProvider.getIntType());
    parameter4.setParameterKind(ParameterKind.REQUIRED);
    ParameterElementImpl parameter5 = new ParameterElementImpl(identifier("a5"));
    parameter5.setType(typeProvider.getIntType());
    parameter5.setParameterKind(ParameterKind.REQUIRED);
    methodM2.setParameters(new ParameterElement[] {parameter3, parameter4, parameter5});
    classI2.setMethods(new MethodElement[] {methodM2});

    ClassElementImpl classA = classElement("A");
    classA.setInterfaces(new InterfaceType[] {classI1.getType(), classI2.getType()});

    MemberMap mapA = inheritanceManager.getMapOfMembersInheritedFromInterfaces(classA);
    assertEquals(numOfMembersInObject, mapA.getSize());
    assertNull(mapA.get(methodName));
    assertErrors(classA, StaticTypeWarningCode.INCONSISTENT_METHOD_INHERITANCE);
  }

  public void test_getMapOfMembersInheritedFromInterfaces_inconsistentMethodInheritance_str_int()
      throws Exception {
    // class I1 { int m(); }
    // class I2 { String m(); }
    // class A implements I2, I1 {}
    ClassElementImpl classI1 = classElement("I1");
    String methodName = "m";
    MethodElement methodM1 = methodElement(methodName, null, typeProvider.getStringType());
    classI1.setMethods(new MethodElement[] {methodM1});

    ClassElementImpl classI2 = classElement("I2");
    MethodElement methodM2 = methodElement(methodName, null, typeProvider.getIntType());
    classI2.setMethods(new MethodElement[] {methodM2});

    ClassElementImpl classA = classElement("A");
    classA.setInterfaces(new InterfaceType[] {classI2.getType(), classI1.getType()});

    MemberMap mapA = inheritanceManager.getMapOfMembersInheritedFromInterfaces(classA);
    assertEquals(numOfMembersInObject, mapA.getSize());
    assertNull(mapA.get(methodName));
    assertErrors(classA, StaticTypeWarningCode.INCONSISTENT_METHOD_INHERITANCE);
  }

  public void test_getMapOfMembersInheritedFromInterfaces_method_extends() throws Exception {
    // class A { int g(); }
    // class B extends A {}
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement methodM = methodElement(methodName, typeProvider.getIntType());
    classA.setMethods(new MethodElement[] {methodM});

    ClassElementImpl classB = classElement("B", classA.getType());

    MemberMap mapB = inheritanceManager.getMapOfMembersInheritedFromInterfaces(classB);
    MemberMap mapA = inheritanceManager.getMapOfMembersInheritedFromInterfaces(classA);

    assertEquals(numOfMembersInObject, mapA.getSize());
    assertEquals(numOfMembersInObject + 1, mapB.getSize());
    assertSame(methodM, mapB.get(methodName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }

  public void test_getMapOfMembersInheritedFromInterfaces_method_implements() throws Exception {
    // class A { int g(); }
    // class B implements A {}
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement methodM = methodElement(methodName, typeProvider.getIntType());
    classA.setMethods(new MethodElement[] {methodM});

    ClassElementImpl classB = classElement("B");
    classB.setInterfaces(new InterfaceType[] {classA.getType()});

    MemberMap mapB = inheritanceManager.getMapOfMembersInheritedFromInterfaces(classB);
    MemberMap mapA = inheritanceManager.getMapOfMembersInheritedFromInterfaces(classA);

    assertEquals(numOfMembersInObject, mapA.getSize());
    assertEquals(numOfMembersInObject + 1, mapB.getSize());
    assertSame(methodM, mapB.get(methodName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }

  public void test_getMapOfMembersInheritedFromInterfaces_method_with() throws Exception {
    // class A { int g(); }
    // class B extends Object with A {}
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement methodM = methodElement(methodName, typeProvider.getIntType());
    classA.setMethods(new MethodElement[] {methodM});

    ClassElementImpl classB = classElement("B");
    classB.setMixins(new InterfaceType[] {classA.getType()});

    MemberMap mapB = inheritanceManager.getMapOfMembersInheritedFromInterfaces(classB);
    MemberMap mapA = inheritanceManager.getMapOfMembersInheritedFromInterfaces(classA);

    assertEquals(numOfMembersInObject, mapA.getSize());
    assertEquals(numOfMembersInObject + 1, mapB.getSize());
    assertSame(methodM, mapB.get(methodName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }

  public void test_getMapOfMembersInheritedFromInterfaces_union_differentNames() throws Exception {
    // class I1 { int m1(); }
    // class I2 { int m2(); }
    // class A implements I1, I2 {}
    ClassElementImpl classI1 = classElement("I1");
    String methodName1 = "m1";
    MethodElement methodM1 = methodElement(methodName1, typeProvider.getIntType());
    classI1.setMethods(new MethodElement[] {methodM1});

    ClassElementImpl classI2 = classElement("I2");
    String methodName2 = "m2";
    MethodElement methodM2 = methodElement(methodName2, typeProvider.getIntType());
    classI2.setMethods(new MethodElement[] {methodM2});

    ClassElementImpl classA = classElement("A");
    classA.setInterfaces(new InterfaceType[] {classI1.getType(), classI2.getType()});

    MemberMap mapA = inheritanceManager.getMapOfMembersInheritedFromInterfaces(classA);

    assertEquals(numOfMembersInObject + 2, mapA.getSize());
    assertSame(methodM1, mapA.get(methodName1));
    assertSame(methodM2, mapA.get(methodName2));
    assertNoErrors(classA);
  }

  public void test_getMapOfMembersInheritedFromInterfaces_union_multipleSubtypes_2_getters()
      throws Exception {
    // class I1 { int get g; }
    // class I2 { num get g; }
    // class A implements I1, I2 {}
    ClassElementImpl classI1 = classElement("I1");
    String accessorName = "g";
    PropertyAccessorElement getter1 = getterElement(accessorName, false, typeProvider.getIntType());
    classI1.setAccessors(new PropertyAccessorElement[] {getter1});

    ClassElementImpl classI2 = classElement("I2");
    PropertyAccessorElement getter2 = getterElement(accessorName, false, typeProvider.getNumType());
    classI2.setAccessors(new PropertyAccessorElement[] {getter2});

    ClassElementImpl classA = classElement("A");
    classA.setInterfaces(new InterfaceType[] {classI1.getType(), classI2.getType()});

    MemberMap mapA = inheritanceManager.getMapOfMembersInheritedFromInterfaces(classA);
    assertEquals(numOfMembersInObject + 1, mapA.getSize());
    PropertyAccessorElement syntheticAccessor = getterElement(
        accessorName,
        false,
        typeProvider.getDynamicType());
    assertEquals(syntheticAccessor.getType(), mapA.get(accessorName).getType());
    assertNoErrors(classA);
  }

  public void test_getMapOfMembersInheritedFromInterfaces_union_multipleSubtypes_2_methods()
      throws Exception {
    // class I1 { dynamic m(int); }
    // class I2 { dynamic m(num); }
    // class A implements I1, I2 {}
    ClassElementImpl classI1 = classElement("I1");
    String methodName = "m";
    MethodElementImpl methodM1 = methodElement(methodName, typeProvider.getDynamicType());
    ParameterElementImpl parameter1 = new ParameterElementImpl(identifier("a0"));
    parameter1.setType(typeProvider.getIntType());
    parameter1.setParameterKind(ParameterKind.REQUIRED);
    methodM1.setParameters(new ParameterElement[] {parameter1});
    classI1.setMethods(new MethodElement[] {methodM1});

    ClassElementImpl classI2 = classElement("I2");
    MethodElementImpl methodM2 = methodElement(methodName, typeProvider.getDynamicType());
    ParameterElementImpl parameter2 = new ParameterElementImpl(identifier("a0"));
    parameter2.setType(typeProvider.getNumType());
    parameter2.setParameterKind(ParameterKind.REQUIRED);
    methodM2.setParameters(new ParameterElement[] {parameter2});
    classI2.setMethods(new MethodElement[] {methodM2});

    ClassElementImpl classA = classElement("A");
    classA.setInterfaces(new InterfaceType[] {classI1.getType(), classI2.getType()});

    MemberMap mapA = inheritanceManager.getMapOfMembersInheritedFromInterfaces(classA);
    assertEquals(numOfMembersInObject + 1, mapA.getSize());
    MethodElement syntheticMethod = methodElement(
        methodName,
        typeProvider.getDynamicType(),
        typeProvider.getDynamicType());
    assertEquals(syntheticMethod.getType(), mapA.get(methodName).getType());
    assertNoErrors(classA);
  }

  public void test_getMapOfMembersInheritedFromInterfaces_union_multipleSubtypes_2_setters()
      throws Exception {
    // class I1 { set s(int); }
    // class I2 { set s(num); }
    // class A implements I1, I2 {}
    ClassElementImpl classI1 = classElement("I1");
    String accessorName = "s";
    PropertyAccessorElement setter1 = setterElement(accessorName, false, typeProvider.getIntType());
    classI1.setAccessors(new PropertyAccessorElement[] {setter1});

    ClassElementImpl classI2 = classElement("I2");
    PropertyAccessorElement setter2 = setterElement(accessorName, false, typeProvider.getNumType());
    classI2.setAccessors(new PropertyAccessorElement[] {setter2});

    ClassElementImpl classA = classElement("A");
    classA.setInterfaces(new InterfaceType[] {classI1.getType(), classI2.getType()});

    MemberMap mapA = inheritanceManager.getMapOfMembersInheritedFromInterfaces(classA);
    assertEquals(numOfMembersInObject + 1, mapA.getSize());
    PropertyAccessorElementImpl syntheticAccessor = setterElement(
        accessorName,
        false,
        typeProvider.getDynamicType());
    syntheticAccessor.setReturnType(typeProvider.getDynamicType());
    assertEquals(syntheticAccessor.getType(), mapA.get(accessorName + "=").getType());
    assertNoErrors(classA);
  }

  public void test_getMapOfMembersInheritedFromInterfaces_union_multipleSubtypes_3_getters()
      throws Exception {
    // class A {}
    // class B extends A {}
    // class C extends B {}
    // class I1 { A get g; }
    // class I2 { B get g; }
    // class I3 { C get g; }
    // class D implements I1, I2, I3 {}
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B", classA.getType());
    ClassElementImpl classC = classElement("C", classB.getType());

    ClassElementImpl classI1 = classElement("I1");
    String accessorName = "g";
    PropertyAccessorElement getter1 = getterElement(accessorName, false, classA.getType());
    classI1.setAccessors(new PropertyAccessorElement[] {getter1});

    ClassElementImpl classI2 = classElement("I2");
    PropertyAccessorElement getter2 = getterElement(accessorName, false, classB.getType());
    classI2.setAccessors(new PropertyAccessorElement[] {getter2});

    ClassElementImpl classI3 = classElement("I3");
    PropertyAccessorElement getter3 = getterElement(accessorName, false, classC.getType());
    classI3.setAccessors(new PropertyAccessorElement[] {getter3});

    ClassElementImpl classD = classElement("D");
    classD.setInterfaces(new InterfaceType[] {
        classI1.getType(), classI2.getType(), classI3.getType()});

    MemberMap mapD = inheritanceManager.getMapOfMembersInheritedFromInterfaces(classD);
    assertEquals(numOfMembersInObject + 1, mapD.getSize());
    PropertyAccessorElement syntheticAccessor = getterElement(
        accessorName,
        false,
        typeProvider.getDynamicType());
    assertEquals(syntheticAccessor.getType(), mapD.get(accessorName).getType());
    assertNoErrors(classD);
  }

  public void test_getMapOfMembersInheritedFromInterfaces_union_multipleSubtypes_3_methods()
      throws Exception {
    // class A {}
    // class B extends A {}
    // class C extends B {}
    // class I1 { dynamic m(A a); }
    // class I2 { dynamic m(B b); }
    // class I3 { dynamic m(C c); }
    // class D implements I1, I2, I3 {}
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B", classA.getType());
    ClassElementImpl classC = classElement("C", classB.getType());

    ClassElementImpl classI1 = classElement("I1");
    String methodName = "m";
    MethodElementImpl methodM1 = methodElement(methodName, typeProvider.getDynamicType());
    ParameterElementImpl parameter1 = new ParameterElementImpl(identifier("a0"));
    parameter1.setType(classA.getType());
    parameter1.setParameterKind(ParameterKind.REQUIRED);
    methodM1.setParameters(new ParameterElement[] {parameter1});
    classI1.setMethods(new MethodElement[] {methodM1});

    ClassElementImpl classI2 = classElement("I2");
    MethodElementImpl methodM2 = methodElement(methodName, typeProvider.getDynamicType());
    ParameterElementImpl parameter2 = new ParameterElementImpl(identifier("a0"));
    parameter2.setType(classB.getType());
    parameter2.setParameterKind(ParameterKind.REQUIRED);
    methodM2.setParameters(new ParameterElement[] {parameter2});
    classI2.setMethods(new MethodElement[] {methodM2});

    ClassElementImpl classI3 = classElement("I3");
    MethodElementImpl methodM3 = methodElement(methodName, typeProvider.getDynamicType());
    ParameterElementImpl parameter3 = new ParameterElementImpl(identifier("a0"));
    parameter3.setType(classC.getType());
    parameter3.setParameterKind(ParameterKind.REQUIRED);
    methodM3.setParameters(new ParameterElement[] {parameter3});
    classI3.setMethods(new MethodElement[] {methodM3});

    ClassElementImpl classD = classElement("D");
    classD.setInterfaces(new InterfaceType[] {
        classI1.getType(), classI2.getType(), classI3.getType()});

    MemberMap mapD = inheritanceManager.getMapOfMembersInheritedFromInterfaces(classD);
    assertEquals(numOfMembersInObject + 1, mapD.getSize());
    MethodElement syntheticMethod = methodElement(
        methodName,
        typeProvider.getDynamicType(),
        typeProvider.getDynamicType());
    assertEquals(syntheticMethod.getType(), mapD.get(methodName).getType());
    assertNoErrors(classD);
  }

  public void test_getMapOfMembersInheritedFromInterfaces_union_multipleSubtypes_3_setters()
      throws Exception {
    // class A {}
    // class B extends A {}
    // class C extends B {}
    // class I1 { set s(A); }
    // class I2 { set s(B); }
    // class I3 { set s(C); }
    // class D implements I1, I2, I3 {}
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B", classA.getType());
    ClassElementImpl classC = classElement("C", classB.getType());

    ClassElementImpl classI1 = classElement("I1");
    String accessorName = "s";
    PropertyAccessorElement setter1 = setterElement(accessorName, false, classA.getType());
    classI1.setAccessors(new PropertyAccessorElement[] {setter1});

    ClassElementImpl classI2 = classElement("I2");
    PropertyAccessorElement setter2 = setterElement(accessorName, false, classB.getType());
    classI2.setAccessors(new PropertyAccessorElement[] {setter2});

    ClassElementImpl classI3 = classElement("I3");
    PropertyAccessorElement setter3 = setterElement(accessorName, false, classC.getType());
    classI3.setAccessors(new PropertyAccessorElement[] {setter3});

    ClassElementImpl classD = classElement("D");
    classD.setInterfaces(new InterfaceType[] {
        classI1.getType(), classI2.getType(), classI3.getType()});

    MemberMap mapD = inheritanceManager.getMapOfMembersInheritedFromInterfaces(classD);
    assertEquals(numOfMembersInObject + 1, mapD.getSize());
    PropertyAccessorElementImpl syntheticAccessor = setterElement(
        accessorName,
        false,
        typeProvider.getDynamicType());
    syntheticAccessor.setReturnType(typeProvider.getDynamicType());
    assertEquals(syntheticAccessor.getType(), mapD.get(accessorName + "=").getType());
    assertNoErrors(classD);
  }

  public void test_getMapOfMembersInheritedFromInterfaces_union_oneSubtype_2_methods()
      throws Exception {
    // class I1 { int m(); }
    // class I2 { int m([int]); }
    // class A implements I1, I2 {}
    ClassElementImpl classI1 = classElement("I1");
    String methodName = "m";
    MethodElement methodM1 = methodElement(methodName, typeProvider.getIntType());
    classI1.setMethods(new MethodElement[] {methodM1});

    ClassElementImpl classI2 = classElement("I2");
    MethodElementImpl methodM2 = methodElement(methodName, typeProvider.getIntType());
    ParameterElementImpl parameter1 = new ParameterElementImpl(identifier("a1"));
    parameter1.setType(typeProvider.getIntType());
    parameter1.setParameterKind(ParameterKind.POSITIONAL);
    methodM2.setParameters(new ParameterElement[] {parameter1});
    classI2.setMethods(new MethodElement[] {methodM2});

    ClassElementImpl classA = classElement("A");
    classA.setInterfaces(new InterfaceType[] {classI1.getType(), classI2.getType()});

    MemberMap mapA = inheritanceManager.getMapOfMembersInheritedFromInterfaces(classA);
    assertEquals(numOfMembersInObject + 1, mapA.getSize());
    assertSame(methodM2, mapA.get(methodName));
    assertNoErrors(classA);
  }

  public void test_getMapOfMembersInheritedFromInterfaces_union_oneSubtype_3_methods()
      throws Exception {
    // class I1 { int m(); }
    // class I2 { int m([int]); }
    // class I3 { int m([int, int]); }
    // class A implements I1, I2, I3 {}
    ClassElementImpl classI1 = classElement("I1");
    String methodName = "m";
    MethodElementImpl methodM1 = methodElement(methodName, typeProvider.getIntType());
    classI1.setMethods(new MethodElement[] {methodM1});

    ClassElementImpl classI2 = classElement("I2");
    MethodElementImpl methodM2 = methodElement(methodName, typeProvider.getIntType());
    ParameterElementImpl parameter1 = new ParameterElementImpl(identifier("a1"));
    parameter1.setType(typeProvider.getIntType());
    parameter1.setParameterKind(ParameterKind.POSITIONAL);
    methodM1.setParameters(new ParameterElement[] {parameter1});
    classI2.setMethods(new MethodElement[] {methodM2});

    ClassElementImpl classI3 = classElement("I3");
    MethodElementImpl methodM3 = methodElement(methodName, typeProvider.getIntType());
    ParameterElementImpl parameter2 = new ParameterElementImpl(identifier("a2"));
    parameter2.setType(typeProvider.getIntType());
    parameter2.setParameterKind(ParameterKind.POSITIONAL);
    ParameterElementImpl parameter3 = new ParameterElementImpl(identifier("a3"));
    parameter3.setType(typeProvider.getIntType());
    parameter3.setParameterKind(ParameterKind.POSITIONAL);
    methodM3.setParameters(new ParameterElement[] {parameter2, parameter3});
    classI3.setMethods(new MethodElement[] {methodM3});

    ClassElementImpl classA = classElement("A");
    classA.setInterfaces(new InterfaceType[] {
        classI1.getType(), classI2.getType(), classI3.getType()});

    MemberMap mapA = inheritanceManager.getMapOfMembersInheritedFromInterfaces(classA);
    assertEquals(numOfMembersInObject + 1, mapA.getSize());
    assertSame(methodM3, mapA.get(methodName));
    assertNoErrors(classA);
  }

  public void test_getMapOfMembersInheritedFromInterfaces_union_oneSubtype_4_methods()
      throws Exception {
    // class I1 { int m(); }
    // class I2 { int m(); }
    // class I3 { int m([int]); }
    // class I4 { int m([int, int]); }
    // class A implements I1, I2, I3, I4 {}
    ClassElementImpl classI1 = classElement("I1");
    String methodName = "m";
    MethodElement methodM1 = methodElement(methodName, typeProvider.getIntType());
    classI1.setMethods(new MethodElement[] {methodM1});

    ClassElementImpl classI2 = classElement("I2");
    MethodElement methodM2 = methodElement(methodName, typeProvider.getIntType());
    classI2.setMethods(new MethodElement[] {methodM2});

    ClassElementImpl classI3 = classElement("I3");
    MethodElementImpl methodM3 = methodElement(methodName, typeProvider.getIntType());
    ParameterElementImpl parameter1 = new ParameterElementImpl(identifier("a1"));
    parameter1.setType(typeProvider.getIntType());
    parameter1.setParameterKind(ParameterKind.POSITIONAL);
    methodM3.setParameters(new ParameterElement[] {parameter1});
    classI3.setMethods(new MethodElement[] {methodM3});

    ClassElementImpl classI4 = classElement("I4");
    MethodElementImpl methodM4 = methodElement(methodName, typeProvider.getIntType());
    ParameterElementImpl parameter2 = new ParameterElementImpl(identifier("a2"));
    parameter2.setType(typeProvider.getIntType());
    parameter2.setParameterKind(ParameterKind.POSITIONAL);
    ParameterElementImpl parameter3 = new ParameterElementImpl(identifier("a3"));
    parameter3.setType(typeProvider.getIntType());
    parameter3.setParameterKind(ParameterKind.POSITIONAL);
    methodM4.setParameters(new ParameterElement[] {parameter2, parameter3});
    classI4.setMethods(new MethodElement[] {methodM4});

    ClassElementImpl classA = classElement("A");
    classA.setInterfaces(new InterfaceType[] {
        classI1.getType(), classI2.getType(), classI3.getType(), classI4.getType()});

    MemberMap mapA = inheritanceManager.getMapOfMembersInheritedFromInterfaces(classA);
    assertEquals(numOfMembersInObject + 1, mapA.getSize());
    assertSame(methodM4, mapA.get(methodName));
    assertNoErrors(classA);
  }

  public void test_lookupInheritance_interface_getter() throws Exception {
    ClassElementImpl classA = classElement("A");
    String getterName = "g";
    PropertyAccessorElement getterG = getterElement(getterName, false, typeProvider.getIntType());
    classA.setAccessors(new PropertyAccessorElement[] {getterG});

    ClassElementImpl classB = classElement("B");
    classB.setInterfaces(new InterfaceType[] {classA.getType()});
    assertSame(getterG, inheritanceManager.lookupInheritance(classB, getterName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }

  public void test_lookupInheritance_interface_method() throws Exception {
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement methodM = methodElement(methodName, typeProvider.getIntType());
    classA.setMethods(new MethodElement[] {methodM});

    ClassElementImpl classB = classElement("B");
    classB.setInterfaces(new InterfaceType[] {classA.getType()});
    assertSame(methodM, inheritanceManager.lookupInheritance(classB, methodName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }

  public void test_lookupInheritance_interface_setter() throws Exception {
    ClassElementImpl classA = classElement("A");
    String setterName = "s";
    PropertyAccessorElement setterS = setterElement(setterName, false, typeProvider.getIntType());
    classA.setAccessors(new PropertyAccessorElement[] {setterS});

    ClassElementImpl classB = classElement("B");
    classB.setInterfaces(new InterfaceType[] {classA.getType()});
    assertSame(setterS, inheritanceManager.lookupInheritance(classB, setterName + '='));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }

  public void test_lookupInheritance_interface_staticMember() throws Exception {
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement methodM = methodElement(methodName, typeProvider.getIntType());
    ((MethodElementImpl) methodM).setStatic(true);
    classA.setMethods(new MethodElement[] {methodM});

    ClassElementImpl classB = classElement("B");
    classB.setInterfaces(new InterfaceType[] {classA.getType()});
    assertNull(inheritanceManager.lookupInheritance(classB, methodName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }

  public void test_lookupInheritance_interfaces_infiniteLoop() throws Exception {
    ClassElementImpl classA = classElement("A");
    classA.setInterfaces(new InterfaceType[] {classA.getType()});
    assertNull(inheritanceManager.lookupInheritance(classA, "name"));
    assertNoErrors(classA);
  }

  public void test_lookupInheritance_interfaces_infiniteLoop2() throws Exception {
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B");
    classA.setInterfaces(new InterfaceType[] {classB.getType()});
    classB.setInterfaces(new InterfaceType[] {classA.getType()});
    assertNull(inheritanceManager.lookupInheritance(classA, "name"));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }

  public void test_lookupInheritance_interfaces_union2() throws Exception {
    ClassElementImpl classI1 = classElement("I1");
    String methodName1 = "m1";
    MethodElement methodM1 = methodElement(methodName1, typeProvider.getIntType());
    classI1.setMethods(new MethodElement[] {methodM1});

    ClassElementImpl classI2 = classElement("I2");
    String methodName2 = "m2";
    MethodElement methodM2 = methodElement(methodName2, typeProvider.getIntType());
    classI2.setMethods(new MethodElement[] {methodM2});
    classI2.setInterfaces(new InterfaceType[] {classI1.getType()});

    ClassElementImpl classA = classElement("A");
    classA.setInterfaces(new InterfaceType[] {classI2.getType()});
    assertSame(methodM1, inheritanceManager.lookupInheritance(classA, methodName1));
    assertSame(methodM2, inheritanceManager.lookupInheritance(classA, methodName2));
    assertNoErrors(classI1);
    assertNoErrors(classI2);
    assertNoErrors(classA);
  }

  public void test_lookupInheritance_mixin_getter() throws Exception {
    ClassElementImpl classA = classElement("A");
    String getterName = "g";
    PropertyAccessorElement getterG = getterElement(getterName, false, typeProvider.getIntType());
    classA.setAccessors(new PropertyAccessorElement[] {getterG});

    ClassElementImpl classB = classElement("B");
    classB.setMixins(new InterfaceType[] {classA.getType()});
    assertSame(getterG, inheritanceManager.lookupInheritance(classB, getterName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }

  public void test_lookupInheritance_mixin_method() throws Exception {
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement methodM = methodElement(methodName, typeProvider.getIntType());
    classA.setMethods(new MethodElement[] {methodM});

    ClassElementImpl classB = classElement("B");
    classB.setMixins(new InterfaceType[] {classA.getType()});
    assertSame(methodM, inheritanceManager.lookupInheritance(classB, methodName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }

  public void test_lookupInheritance_mixin_setter() throws Exception {
    ClassElementImpl classA = classElement("A");
    String setterName = "s";
    PropertyAccessorElement setterS = setterElement(setterName, false, typeProvider.getIntType());
    classA.setAccessors(new PropertyAccessorElement[] {setterS});

    ClassElementImpl classB = classElement("B");
    classB.setMixins(new InterfaceType[] {classA.getType()});
    assertSame(setterS, inheritanceManager.lookupInheritance(classB, setterName + '='));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }

  public void test_lookupInheritance_mixin_staticMember() throws Exception {
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement methodM = methodElement(methodName, typeProvider.getIntType());
    ((MethodElementImpl) methodM).setStatic(true);
    classA.setMethods(new MethodElement[] {methodM});

    ClassElementImpl classB = classElement("B");
    classB.setMixins(new InterfaceType[] {classA.getType()});
    assertNull(inheritanceManager.lookupInheritance(classB, methodName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }

  public void test_lookupInheritance_noMember() throws Exception {
    ClassElementImpl classA = classElement("A");
    assertNull(inheritanceManager.lookupInheritance(classA, "a"));
    assertNoErrors(classA);
  }

  public void test_lookupInheritance_superclass_getter() throws Exception {
    ClassElementImpl classA = classElement("A");
    String getterName = "g";
    PropertyAccessorElement getterG = getterElement(getterName, false, typeProvider.getIntType());
    classA.setAccessors(new PropertyAccessorElement[] {getterG});

    ClassElementImpl classB = classElement("B", classA.getType());
    assertSame(getterG, inheritanceManager.lookupInheritance(classB, getterName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }

  public void test_lookupInheritance_superclass_infiniteLoop() throws Exception {
    ClassElementImpl classA = classElement("A");
    classA.setSupertype(classA.getType());
    assertNull(inheritanceManager.lookupInheritance(classA, "name"));
    assertNoErrors(classA);
  }

  public void test_lookupInheritance_superclass_infiniteLoop2() throws Exception {
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B");
    classA.setSupertype(classB.getType());
    classB.setSupertype(classA.getType());
    assertNull(inheritanceManager.lookupInheritance(classA, "name"));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }

  public void test_lookupInheritance_superclass_method() throws Exception {
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement methodM = methodElement(methodName, typeProvider.getIntType());
    classA.setMethods(new MethodElement[] {methodM});

    ClassElementImpl classB = classElement("B", classA.getType());
    assertSame(methodM, inheritanceManager.lookupInheritance(classB, methodName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }

  public void test_lookupInheritance_superclass_setter() throws Exception {
    ClassElementImpl classA = classElement("A");
    String setterName = "s";
    PropertyAccessorElement setterS = setterElement(setterName, false, typeProvider.getIntType());
    classA.setAccessors(new PropertyAccessorElement[] {setterS});

    ClassElementImpl classB = classElement("B", classA.getType());
    assertSame(setterS, inheritanceManager.lookupInheritance(classB, setterName + '='));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }

  public void test_lookupInheritance_superclass_staticMember() throws Exception {
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement methodM = methodElement(methodName, typeProvider.getIntType());
    ((MethodElementImpl) methodM).setStatic(true);
    classA.setMethods(new MethodElement[] {methodM});

    ClassElementImpl classB = classElement("B", classA.getType());
    assertNull(inheritanceManager.lookupInheritance(classB, methodName));
    assertNoErrors(classA);
    assertNoErrors(classB);
  }

  public void test_lookupMember_getter() throws Exception {
    ClassElementImpl classA = classElement("A");
    String getterName = "g";
    PropertyAccessorElement getterG = getterElement(getterName, false, typeProvider.getIntType());
    classA.setAccessors(new PropertyAccessorElement[] {getterG});
    assertSame(getterG, inheritanceManager.lookupMember(classA, getterName));
    assertNoErrors(classA);
  }

  public void test_lookupMember_getter_static() throws Exception {
    ClassElementImpl classA = classElement("A");
    String getterName = "g";
    PropertyAccessorElement getterG = getterElement(getterName, true, typeProvider.getIntType());
    classA.setAccessors(new PropertyAccessorElement[] {getterG});
    assertNull(inheritanceManager.lookupMember(classA, getterName));
    assertNoErrors(classA);
  }

  public void test_lookupMember_method() throws Exception {
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement methodM = methodElement(methodName, typeProvider.getIntType());
    classA.setMethods(new MethodElement[] {methodM});
    assertSame(methodM, inheritanceManager.lookupMember(classA, methodName));
    assertNoErrors(classA);
  }

  public void test_lookupMember_method_static() throws Exception {
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement methodM = methodElement(methodName, typeProvider.getIntType());
    ((MethodElementImpl) methodM).setStatic(true);
    classA.setMethods(new MethodElement[] {methodM});
    assertNull(inheritanceManager.lookupMember(classA, methodName));
    assertNoErrors(classA);
  }

  public void test_lookupMember_noMember() throws Exception {
    ClassElementImpl classA = classElement("A");
    assertNull(inheritanceManager.lookupMember(classA, "a"));
    assertNoErrors(classA);
  }

  public void test_lookupMember_setter() throws Exception {
    ClassElementImpl classA = classElement("A");
    String setterName = "s";
    PropertyAccessorElement setterS = setterElement(setterName, false, typeProvider.getIntType());
    classA.setAccessors(new PropertyAccessorElement[] {setterS});
    assertSame(setterS, inheritanceManager.lookupMember(classA, setterName + '='));
    assertNoErrors(classA);
  }

  public void test_lookupMember_setter_static() throws Exception {
    ClassElementImpl classA = classElement("A");
    String setterName = "s";
    PropertyAccessorElement setterS = setterElement(setterName, true, typeProvider.getIntType());
    classA.setAccessors(new PropertyAccessorElement[] {setterS});
    assertNull(inheritanceManager.lookupMember(classA, setterName));
    assertNoErrors(classA);
  }

  private void assertErrors(ClassElement classElt, ErrorCode... expectedErrorCodes) {
    GatheringErrorListener errorListener = new GatheringErrorListener();
    HashSet<AnalysisError> actualErrors = inheritanceManager.getErrors(classElt);
    if (actualErrors != null) {
      for (AnalysisError error : actualErrors) {
        errorListener.onError(error);
      }
    }
    errorListener.assertErrorsWithCodes(expectedErrorCodes);
  }

  private void assertNoErrors(ClassElement classElt) {
    assertErrors(classElt);
  }

  /**
   * Create the inheritance manager used by the tests.
   * 
   * @return the inheritance manager that was created
   */
  private InheritanceManager createInheritanceManager() {
    AnalysisContextImpl context = AnalysisContextFactory.contextWithCore();
    FileBasedSource source = new FileBasedSource(FileUtilities2.createFile("/test.dart"));
    CompilationUnitElementImpl definingCompilationUnit = new CompilationUnitElementImpl("test.dart");
    definingCompilationUnit.setSource(source);
    definingLibrary = library(context, "test");
    definingLibrary.setDefiningCompilationUnit(definingCompilationUnit);
    return new InheritanceManager(definingLibrary);
  }
}
