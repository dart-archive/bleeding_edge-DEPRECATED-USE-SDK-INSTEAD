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
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.MethodElementImpl;
import com.google.dart.engine.source.ContentCache;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.utilities.io.FileUtilities2;

import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.getterElement;
import static com.google.dart.engine.element.ElementFactory.library;
import static com.google.dart.engine.element.ElementFactory.methodElement;
import static com.google.dart.engine.element.ElementFactory.setterElement;

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
  InheritanceManager inheritanceManager;

  @Override
  public void setUp() {
    typeProvider = new TestTypeProvider();
    inheritanceManager = createInheritanceManager();
  }

  public void test_lookupInheritance_interface_getter() throws Exception {
    ClassElementImpl classA = classElement("A");
    String getterName = "g";
    PropertyAccessorElement getterG = getterElement(getterName, false, typeProvider.getIntType());
    classA.setAccessors(new PropertyAccessorElement[] {getterG});

    ClassElementImpl classB = classElement("B");
    classB.setInterfaces(new InterfaceType[] {classA.getType()});
    assertSame(getterG, inheritanceManager.lookupInheritance(classB, getterName));
  }

  public void test_lookupInheritance_interface_method() throws Exception {
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement methodM = methodElement(methodName, typeProvider.getIntType());
    classA.setMethods(new MethodElement[] {methodM});

    ClassElementImpl classB = classElement("B");
    classB.setInterfaces(new InterfaceType[] {classA.getType()});
    assertSame(methodM, inheritanceManager.lookupInheritance(classB, methodName));
  }

  public void test_lookupInheritance_interface_setter() throws Exception {
    ClassElementImpl classA = classElement("A");
    String setterName = "s";
    PropertyAccessorElement setterS = setterElement(setterName, false, typeProvider.getIntType());
    classA.setAccessors(new PropertyAccessorElement[] {setterS});

    ClassElementImpl classB = classElement("B");
    classB.setInterfaces(new InterfaceType[] {classA.getType()});
    assertSame(setterS, inheritanceManager.lookupInheritance(classB, setterName + '='));
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
  }

  public void test_lookupInheritance_interfaces_infiniteLoop() throws Exception {
    ClassElementImpl classA = classElement("A");
    classA.setInterfaces(new InterfaceType[] {classA.getType()});
    assertNull(inheritanceManager.lookupInheritance(classA, "name"));
  }

  public void test_lookupInheritance_interfaces_infiniteLoop2() throws Exception {
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B");
    classA.setInterfaces(new InterfaceType[] {classB.getType()});
    classB.setInterfaces(new InterfaceType[] {classA.getType()});
    assertNull(inheritanceManager.lookupInheritance(classA, "name"));
  }

  public void test_lookupInheritance_interfaces_union1() throws Exception {
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
    assertSame(methodM1, inheritanceManager.lookupInheritance(classA, methodName1));
    assertSame(methodM2, inheritanceManager.lookupInheritance(classA, methodName2));
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
  }

  public void test_lookupInheritance_interfaces_union3() throws Exception {
    ClassElementImpl classI1 = classElement("I1");
    String methodName = "m";
    MethodElement methodM = methodElement(methodName, typeProvider.getIntType());
    classI1.setMethods(new MethodElement[] {methodM});

    ClassElementImpl classI2 = classElement("I2");
    PropertyAccessorElement getterG = getterElement(methodName, false, typeProvider.getIntType());
    classI2.setAccessors(new PropertyAccessorElement[] {getterG});

    ClassElementImpl classA = classElement("A");
    classA.setInterfaces(new InterfaceType[] {classI1.getType(), classI2.getType()});
    assertNull(inheritanceManager.lookupInheritance(classA, methodName));
  }

  public void test_lookupInheritance_mixin_getter() throws Exception {
    ClassElementImpl classA = classElement("A");
    String getterName = "g";
    PropertyAccessorElement getterG = getterElement(getterName, false, typeProvider.getIntType());
    classA.setAccessors(new PropertyAccessorElement[] {getterG});

    ClassElementImpl classB = classElement("B");
    classB.setMixins(new InterfaceType[] {classA.getType()});
    assertSame(getterG, inheritanceManager.lookupInheritance(classB, getterName));
  }

  public void test_lookupInheritance_mixin_method() throws Exception {
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement methodM = methodElement(methodName, typeProvider.getIntType());
    classA.setMethods(new MethodElement[] {methodM});

    ClassElementImpl classB = classElement("B");
    classB.setMixins(new InterfaceType[] {classA.getType()});
    assertSame(methodM, inheritanceManager.lookupInheritance(classB, methodName));
  }

  public void test_lookupInheritance_mixin_setter() throws Exception {
    ClassElementImpl classA = classElement("A");
    String setterName = "s";
    PropertyAccessorElement setterS = setterElement(setterName, false, typeProvider.getIntType());
    classA.setAccessors(new PropertyAccessorElement[] {setterS});

    ClassElementImpl classB = classElement("B");
    classB.setMixins(new InterfaceType[] {classA.getType()});
    assertSame(setterS, inheritanceManager.lookupInheritance(classB, setterName + '='));
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
  }

  public void test_lookupInheritance_noMember() throws Exception {
    ClassElementImpl classA = classElement("A");
    assertNull(inheritanceManager.lookupInheritance(classA, "a"));
  }

  public void test_lookupInheritance_superclass_getter() throws Exception {
    ClassElementImpl classA = classElement("A");
    String getterName = "g";
    PropertyAccessorElement getterG = getterElement(getterName, false, typeProvider.getIntType());
    classA.setAccessors(new PropertyAccessorElement[] {getterG});

    ClassElementImpl classB = classElement("B", classA.getType());
    assertSame(getterG, inheritanceManager.lookupInheritance(classB, getterName));
  }

  public void test_lookupInheritance_superclass_infiniteLoop() throws Exception {
    ClassElementImpl classA = classElement("A");
    classA.setSupertype(classA.getType());
    assertNull(inheritanceManager.lookupInheritance(classA, "name"));
  }

  public void test_lookupInheritance_superclass_infiniteLoop2() throws Exception {
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B");
    classA.setSupertype(classB.getType());
    classB.setSupertype(classA.getType());
    assertNull(inheritanceManager.lookupInheritance(classA, "name"));
  }

  public void test_lookupInheritance_superclass_method() throws Exception {
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement methodM = methodElement(methodName, typeProvider.getIntType());
    classA.setMethods(new MethodElement[] {methodM});

    ClassElementImpl classB = classElement("B", classA.getType());
    assertSame(methodM, inheritanceManager.lookupInheritance(classB, methodName));
  }

  public void test_lookupInheritance_superclass_setter() throws Exception {
    ClassElementImpl classA = classElement("A");
    String setterName = "s";
    PropertyAccessorElement setterS = setterElement(setterName, false, typeProvider.getIntType());
    classA.setAccessors(new PropertyAccessorElement[] {setterS});

    ClassElementImpl classB = classElement("B", classA.getType());
    assertSame(setterS, inheritanceManager.lookupInheritance(classB, setterName + '='));
  }

  public void test_lookupInheritance_superclass_staticMember() throws Exception {
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement methodM = methodElement(methodName, typeProvider.getIntType());
    ((MethodElementImpl) methodM).setStatic(true);
    classA.setMethods(new MethodElement[] {methodM});

    ClassElementImpl classB = classElement("B", classA.getType());
    assertNull(inheritanceManager.lookupInheritance(classB, methodName));
  }

  public void test_lookupMember_getter() throws Exception {
    ClassElementImpl classA = classElement("A");
    String getterName = "g";
    PropertyAccessorElement getterG = getterElement(getterName, false, typeProvider.getIntType());
    classA.setAccessors(new PropertyAccessorElement[] {getterG});
    assertSame(getterG, inheritanceManager.lookupMember(classA, getterName));
  }

  public void test_lookupMember_getter_static() throws Exception {
    ClassElementImpl classA = classElement("A");
    String getterName = "g";
    PropertyAccessorElement getterG = getterElement(getterName, true, typeProvider.getIntType());
    classA.setAccessors(new PropertyAccessorElement[] {getterG});
    assertNull(inheritanceManager.lookupMember(classA, getterName));
  }

  public void test_lookupMember_method() throws Exception {
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement methodM = methodElement(methodName, typeProvider.getIntType());
    classA.setMethods(new MethodElement[] {methodM});
    assertSame(methodM, inheritanceManager.lookupMember(classA, methodName));
  }

  public void test_lookupMember_method_static() throws Exception {
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement methodM = methodElement(methodName, typeProvider.getIntType());
    ((MethodElementImpl) methodM).setStatic(true);
    classA.setMethods(new MethodElement[] {methodM});
    assertNull(inheritanceManager.lookupMember(classA, methodName));
  }

  public void test_lookupMember_noMember() throws Exception {
    ClassElementImpl classA = classElement("A");
    assertNull(inheritanceManager.lookupMember(classA, "a"));
  }

  public void test_lookupMember_setter() throws Exception {
    ClassElementImpl classA = classElement("A");
    String setterName = "s";
    PropertyAccessorElement setterS = setterElement(setterName, false, typeProvider.getIntType());
    classA.setAccessors(new PropertyAccessorElement[] {setterS});
    assertSame(setterS, inheritanceManager.lookupMember(classA, setterName + '='));
  }

  public void test_lookupMember_setter_static() throws Exception {
    ClassElementImpl classA = classElement("A");
    String setterName = "s";
    PropertyAccessorElement setterS = setterElement(setterName, true, typeProvider.getIntType());
    classA.setAccessors(new PropertyAccessorElement[] {setterS});
    assertNull(inheritanceManager.lookupMember(classA, setterName));
  }

  /**
   * Create the inheritance manager used by the tests.
   * 
   * @return the inheritance manager that was created
   */
  private InheritanceManager createInheritanceManager() {
    AnalysisContextImpl context = AnalysisContextFactory.contextWithCore();
    FileBasedSource source = new FileBasedSource(
        new ContentCache(),
        FileUtilities2.createFile("/test.dart"));
    CompilationUnitElementImpl definingCompilationUnit = new CompilationUnitElementImpl("test.dart");
    definingCompilationUnit.setSource(source);
    definingLibrary = library(context, "test");
    definingLibrary.setDefiningCompilationUnit(definingCompilationUnit);
    return new InheritanceManager(definingLibrary);
  }
}
