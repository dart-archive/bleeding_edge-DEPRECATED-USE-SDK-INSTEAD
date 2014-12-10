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
package com.google.dart.engine.internal.element;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisContextHelper;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.internal.resolver.TestTypeProvider;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.InterfaceType;

import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.enumElement;
import static com.google.dart.engine.element.ElementFactory.fieldElement;
import static com.google.dart.engine.element.ElementFactory.getterElement;
import static com.google.dart.engine.element.ElementFactory.library;
import static com.google.dart.engine.element.ElementFactory.methodElement;
import static com.google.dart.engine.element.ElementFactory.setterElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClassElementImplTest extends EngineTestCase {
  public void test_getAllSupertypes_interface() {
    ClassElement classA = classElement("A");
    ClassElement classB = classElement("B", classA.getType());
    ClassElementImpl elementC = classElement("C");
    InterfaceType typeObject = classA.getSupertype();
    InterfaceType typeA = classA.getType();
    InterfaceType typeB = classB.getType();
    InterfaceType typeC = elementC.getType();
    elementC.setInterfaces(new InterfaceType[] {typeB});
    InterfaceType[] supers = elementC.getAllSupertypes();
    List<InterfaceType> types = new ArrayList<InterfaceType>();
    Collections.addAll(types, supers);
    assertTrue(types.contains(typeA));
    assertTrue(types.contains(typeB));
    assertTrue(types.contains(typeObject));
    assertFalse(types.contains(typeC));
  }

  public void test_getAllSupertypes_mixins() {
    ClassElement classA = classElement("A");
    ClassElement classB = classElement("B", classA.getType());
    ClassElementImpl classC = classElement("C");
    InterfaceType typeObject = classA.getSupertype();
    InterfaceType typeA = classA.getType();
    InterfaceType typeB = classB.getType();
    InterfaceType typeC = classC.getType();
    classC.setMixins(new InterfaceType[] {typeB});
    InterfaceType[] supers = classC.getAllSupertypes();
    List<InterfaceType> types = new ArrayList<InterfaceType>();
    Collections.addAll(types, supers);
    assertFalse(types.contains(typeA));
    assertTrue(types.contains(typeB));
    assertTrue(types.contains(typeObject));
    assertFalse(types.contains(typeC));
  }

  public void test_getAllSupertypes_recursive() {
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B", classA.getType());
    classA.setSupertype(classB.getType());
    InterfaceType[] supers = classB.getAllSupertypes();
    assertLength(1, supers);
  }

  public void test_getField() {
    ClassElementImpl classA = classElement("A");
    String fieldName = "f";
    FieldElementImpl field = fieldElement(fieldName, false, false, false, null);
    classA.setFields(new FieldElement[] {field});
    assertSame(field, classA.getField(fieldName));
    assertFalse(field.isEnumConstant());
    // no such field
    assertSame(null, classA.getField("noSuchField"));
  }

  public void test_getMethod_declared() {
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement method = methodElement(methodName, null);
    classA.setMethods(new MethodElement[] {method});
    assertSame(method, classA.getMethod(methodName));
  }

  public void test_getMethod_undeclared() {
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement method = methodElement(methodName, null);
    classA.setMethods(new MethodElement[] {method});
    assertNull(classA.getMethod(methodName + "x"));
  }

  public void test_getNode() throws Exception {
    AnalysisContextHelper contextHelper = new AnalysisContextHelper();
    AnalysisContext context = contextHelper.context;
    Source source = contextHelper.addSource("/test.dart", createSource(//
        "class A {}",
        "class B {}"));
    // prepare CompilationUnitElement
    LibraryElement libraryElement = context.computeLibraryElement(source);
    CompilationUnitElement unitElement = libraryElement.getDefiningCompilationUnit();
    // A
    {
      ClassElement elementA = unitElement.getType("A");
      ClassDeclaration nodeA = elementA.getNode();
      assertNotNull(nodeA);
      assertEquals("A", nodeA.getName().getName());
      assertSame(elementA, nodeA.getElement());
    }
    // B
    {
      ClassElement elementB = unitElement.getType("B");
      ClassDeclaration nodeB = elementB.getNode();
      assertNotNull(nodeB);
      assertEquals("B", nodeB.getName().getName());
      assertSame(elementB, nodeB.getElement());
    }
  }

  public void test_hasNonFinalField_false_const() {
    ClassElementImpl classA = classElement("A");
    classA.setFields(new FieldElement[] {fieldElement("f", false, false, true, classA.getType())});
    assertFalse(classA.hasNonFinalField());
  }

  public void test_hasNonFinalField_false_final() {
    ClassElementImpl classA = classElement("A");
    classA.setFields(new FieldElement[] {fieldElement("f", false, true, false, classA.getType())});
    assertFalse(classA.hasNonFinalField());
  }

  public void test_hasNonFinalField_false_recursive() {
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B", classA.getType());
    classA.setSupertype(classB.getType());
    assertFalse(classA.hasNonFinalField());
  }

  public void test_hasNonFinalField_true_immediate() {
    ClassElementImpl classA = classElement("A");
    classA.setFields(new FieldElement[] {fieldElement("f", false, false, false, classA.getType())});
    assertTrue(classA.hasNonFinalField());
  }

  public void test_hasNonFinalField_true_inherited() {
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B", classA.getType());
    classA.setFields(new FieldElement[] {fieldElement("f", false, false, false, classA.getType())});
    assertTrue(classB.hasNonFinalField());
  }

  public void test_hasStaticMember_false_empty() {
    ClassElementImpl classA = classElement("A");
    // no members
    assertFalse(classA.hasStaticMember());
  }

  public void test_hasStaticMember_false_instanceMethod() {
    ClassElementImpl classA = classElement("A");
    MethodElement method = methodElement("foo", null);
    classA.setMethods(new MethodElement[] {method});
    assertFalse(classA.hasStaticMember());
  }

  public void test_hasStaticMember_instanceGetter() {
    ClassElementImpl classA = classElement("A");
    PropertyAccessorElement getter = getterElement("foo", false, null);
    classA.setAccessors(new PropertyAccessorElement[] {getter});
    assertFalse(classA.hasStaticMember());
  }

  public void test_hasStaticMember_true_getter() {
    ClassElementImpl classA = classElement("A");
    PropertyAccessorElementImpl getter = getterElement("foo", false, null);
    classA.setAccessors(new PropertyAccessorElement[] {getter});
    // "foo" is static
    getter.setStatic(true);
    assertTrue(classA.hasStaticMember());
  }

  public void test_hasStaticMember_true_method() {
    ClassElementImpl classA = classElement("A");
    MethodElementImpl method = methodElement("foo", null);
    classA.setMethods(new MethodElement[] {method});
    // "foo" is static
    method.setStatic(true);
    assertTrue(classA.hasStaticMember());
  }

  public void test_hasStaticMember_true_setter() {
    ClassElementImpl classA = classElement("A");
    PropertyAccessorElementImpl setter = setterElement("foo", false, null);
    classA.setAccessors(new PropertyAccessorElement[] {setter});
    // "foo" is static
    setter.setStatic(true);
    assertTrue(classA.hasStaticMember());
  }

  public void test_isEnum() {
    String firstConst = "A";
    String secondConst = "B";
    ClassElementImpl enumE = enumElement(new TestTypeProvider(), "E", firstConst, secondConst);

    // E is an enum
    assertTrue(enumE.isEnum());

    // A and B are static members
    assertTrue(enumE.getField(firstConst).isEnumConstant());
    assertTrue(enumE.getField(secondConst).isEnumConstant());
  }

  public void test_lookUpConcreteMethod_declared() {
    // class A {
    //   m() {}
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement method = methodElement(methodName, null);
    classA.setMethods(new MethodElement[] {method});
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {classA});
    assertSame(method, classA.lookUpConcreteMethod(methodName, library));
  }

  public void test_lookUpConcreteMethod_declaredAbstract() {
    // class A {
    //   m();
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElementImpl method = methodElement(methodName, null);
    method.setAbstract(true);
    classA.setMethods(new MethodElement[] {method});
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {classA});
    assertNull(classA.lookUpConcreteMethod(methodName, library));
  }

  public void test_lookUpConcreteMethod_declaredAbstractAndInherited() {
    // class A {
    //   m() {}
    // }
    // class B extends A {
    //   m();
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement inheritedMethod = methodElement(methodName, null);
    classA.setMethods(new MethodElement[] {inheritedMethod});
    ClassElementImpl classB = classElement("B", classA.getType());
    MethodElementImpl method = methodElement(methodName, null);
    method.setAbstract(true);
    classB.setMethods(new MethodElement[] {method});
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {
        classA, classB});
    assertSame(inheritedMethod, classB.lookUpConcreteMethod(methodName, library));
  }

  public void test_lookUpConcreteMethod_declaredAndInherited() {
    // class A {
    //   m() {}
    // }
    // class B extends A {
    //   m() {}
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement inheritedMethod = methodElement(methodName, null);
    classA.setMethods(new MethodElement[] {inheritedMethod});
    ClassElementImpl classB = classElement("B", classA.getType());
    MethodElement method = methodElement(methodName, null);
    classB.setMethods(new MethodElement[] {method});
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {
        classA, classB});
    assertSame(method, classB.lookUpConcreteMethod(methodName, library));
  }

  public void test_lookUpConcreteMethod_declaredAndInheritedAbstract() {
    // abstract class A {
    //   m();
    // }
    // class B extends A {
    //   m() {}
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    classA.setAbstract(true);
    String methodName = "m";
    MethodElementImpl inheritedMethod = methodElement(methodName, null);
    inheritedMethod.setAbstract(true);
    classA.setMethods(new MethodElement[] {inheritedMethod});
    ClassElementImpl classB = classElement("B", classA.getType());
    MethodElement method = methodElement(methodName, null);
    classB.setMethods(new MethodElement[] {method});
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {
        classA, classB});
    assertSame(method, classB.lookUpConcreteMethod(methodName, library));
  }

  public void test_lookUpConcreteMethod_inherited() {
    // class A {
    //   m() {}
    // }
    // class B extends A {
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement inheritedMethod = methodElement(methodName, null);
    classA.setMethods(new MethodElement[] {inheritedMethod});
    ClassElementImpl classB = classElement("B", classA.getType());
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {
        classA, classB});
    assertSame(inheritedMethod, classB.lookUpConcreteMethod(methodName, library));
  }

  public void test_lookUpConcreteMethod_undeclared() {
    // class A {
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {classA});
    assertNull(classA.lookUpConcreteMethod("m", library));
  }

  public void test_lookUpGetter_declared() {
    // class A {
    //   get g {}
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    String getterName = "g";
    PropertyAccessorElement getter = getterElement(getterName, false, null);
    classA.setAccessors(new PropertyAccessorElement[] {getter});
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {classA});
    assertSame(getter, classA.lookUpGetter(getterName, library));
  }

  public void test_lookUpGetter_inherited() {
    // class A {
    //   get g {}
    // }
    // class B extends A {
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    String getterName = "g";
    PropertyAccessorElement getter = getterElement(getterName, false, null);
    classA.setAccessors(new PropertyAccessorElement[] {getter});
    ClassElementImpl classB = classElement("B", classA.getType());
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {
        classA, classB});
    assertSame(getter, classB.lookUpGetter(getterName, library));
  }

  public void test_lookUpGetter_undeclared() {
    // class A {
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {classA});
    assertNull(classA.lookUpGetter("g", library));
  }

  public void test_lookUpGetter_undeclared_recursive() {
    // class A extends B {
    // }
    // class B extends A {
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B", classA.getType());
    classA.setSupertype(classB.getType());
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {
        classA, classB});
    assertNull(classA.lookUpGetter("g", library));
  }

  public void test_lookUpInheritedConcreteGetter_declared() {
    // class A {
    //   get g {}
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    String getterName = "g";
    PropertyAccessorElement getter = getterElement(getterName, false, null);
    classA.setAccessors(new PropertyAccessorElement[] {getter});
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {classA});
    assertNull(classA.lookUpInheritedConcreteGetter(getterName, library));
  }

  public void test_lookUpInheritedConcreteGetter_inherited() {
    // class A {
    //   get g {}
    // }
    // class B extends A {
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    String getterName = "g";
    PropertyAccessorElement inheritedGetter = getterElement(getterName, false, null);
    classA.setAccessors(new PropertyAccessorElement[] {inheritedGetter});
    ClassElementImpl classB = classElement("B", classA.getType());
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {
        classA, classB});
    assertSame(inheritedGetter, classB.lookUpInheritedConcreteGetter(getterName, library));
  }

  public void test_lookUpInheritedConcreteGetter_undeclared() {
    // class A {
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {classA});
    assertNull(classA.lookUpInheritedConcreteGetter("g", library));
  }

  public void test_lookUpInheritedConcreteGetter_undeclared_recursive() {
    // class A extends B {
    // }
    // class B extends A {
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B", classA.getType());
    classA.setSupertype(classB.getType());
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {
        classA, classB});
    assertNull(classA.lookUpInheritedConcreteGetter("g", library));
  }

  public void test_lookUpInheritedConcreteMethod_declared() {
    // class A {
    //   m() {}
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement method = methodElement(methodName, null);
    classA.setMethods(new MethodElement[] {method});
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {classA});
    assertNull(classA.lookUpInheritedConcreteMethod(methodName, library));
  }

  public void test_lookUpInheritedConcreteMethod_declaredAbstractAndInherited() {
    // class A {
    //   m() {}
    // }
    // class B extends A {
    //   m();
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement inheritedMethod = methodElement(methodName, null);
    classA.setMethods(new MethodElement[] {inheritedMethod});
    ClassElementImpl classB = classElement("B", classA.getType());
    MethodElementImpl method = methodElement(methodName, null);
    method.setAbstract(true);
    classB.setMethods(new MethodElement[] {method});
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {
        classA, classB});
    assertSame(inheritedMethod, classB.lookUpInheritedConcreteMethod(methodName, library));
  }

  public void test_lookUpInheritedConcreteMethod_declaredAndInherited() {
    // class A {
    //   m() {}
    // }
    // class B extends A {
    //   m() {}
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement inheritedMethod = methodElement(methodName, null);
    classA.setMethods(new MethodElement[] {inheritedMethod});
    ClassElementImpl classB = classElement("B", classA.getType());
    MethodElement method = methodElement(methodName, null);
    classB.setMethods(new MethodElement[] {method});
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {
        classA, classB});
    assertSame(inheritedMethod, classB.lookUpInheritedConcreteMethod(methodName, library));
  }

  public void test_lookUpInheritedConcreteMethod_declaredAndInheritedAbstract() {
    // abstract class A {
    //   m();
    // }
    // class B extends A {
    //   m() {}
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    classA.setAbstract(true);
    String methodName = "m";
    MethodElementImpl inheritedMethod = methodElement(methodName, null);
    inheritedMethod.setAbstract(true);
    classA.setMethods(new MethodElement[] {inheritedMethod});
    ClassElementImpl classB = classElement("B", classA.getType());
    MethodElement method = methodElement(methodName, null);
    classB.setMethods(new MethodElement[] {method});
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {
        classA, classB});
    assertNull(classB.lookUpInheritedConcreteMethod(methodName, library));
  }

  public void test_lookUpInheritedConcreteMethod_declaredAndInheritedWithAbstractBetween() {
    // class A {
    //   m() {}
    // }
    // class B extends A {
    //   m();
    // }
    // class C extends B {
    //   m() {}
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement inheritedMethod = methodElement(methodName, null);
    classA.setMethods(new MethodElement[] {inheritedMethod});
    ClassElementImpl classB = classElement("B", classA.getType());
    MethodElementImpl abstractMethod = methodElement(methodName, null);
    abstractMethod.setAbstract(true);
    classB.setMethods(new MethodElement[] {abstractMethod});
    ClassElementImpl classC = classElement("C", classB.getType());
    MethodElementImpl method = methodElement(methodName, null);
    classC.setMethods(new MethodElement[] {method});
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {
        classA, classB, classC});
    assertSame(inheritedMethod, classC.lookUpInheritedConcreteMethod(methodName, library));
  }

  public void test_lookUpInheritedConcreteMethod_inherited() {
    // class A {
    //   m() {}
    // }
    // class B extends A {
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement inheritedMethod = methodElement(methodName, null);
    classA.setMethods(new MethodElement[] {inheritedMethod});
    ClassElementImpl classB = classElement("B", classA.getType());
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {
        classA, classB});
    assertSame(inheritedMethod, classB.lookUpInheritedConcreteMethod(methodName, library));
  }

  public void test_lookUpInheritedConcreteMethod_undeclared() {
    // class A {
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {classA});
    assertNull(classA.lookUpInheritedConcreteMethod("m", library));
  }

  public void test_lookUpInheritedConcreteSetter_declared() {
    // class A {
    //   set g(x) {}
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    String setterName = "s";
    PropertyAccessorElement setter = setterElement(setterName, false, null);
    classA.setAccessors(new PropertyAccessorElement[] {setter});
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {classA});
    assertNull(classA.lookUpInheritedConcreteSetter(setterName, library));
  }

  public void test_lookUpInheritedConcreteSetter_inherited() {
    // class A {
    //   set g(x) {}
    // }
    // class B extends A {
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    String setterName = "s";
    PropertyAccessorElement setter = setterElement(setterName, false, null);
    classA.setAccessors(new PropertyAccessorElement[] {setter});
    ClassElementImpl classB = classElement("B", classA.getType());
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {
        classA, classB});
    assertSame(setter, classB.lookUpInheritedConcreteSetter(setterName, library));
  }

  public void test_lookUpInheritedConcreteSetter_undeclared() {
    // class A {
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {classA});
    assertNull(classA.lookUpInheritedConcreteSetter("s", library));
  }

  public void test_lookUpInheritedConcreteSetter_undeclared_recursive() {
    // class A extends B {
    // }
    // class B extends A {
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B", classA.getType());
    classA.setSupertype(classB.getType());
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {
        classA, classB});
    assertNull(classA.lookUpInheritedConcreteSetter("s", library));
  }

  public void test_lookUpInheritedMethod_declared() {
    // class A {
    //   m() {}
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement method = methodElement(methodName, null);
    classA.setMethods(new MethodElement[] {method});
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {classA});
    assertNull(classA.lookUpInheritedMethod(methodName, library));
  }

  public void test_lookUpInheritedMethod_declaredAndInherited() {
    // class A {
    //   m() {}
    // }
    // class B extends A {
    //   m() {}
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement inheritedMethod = methodElement(methodName, null);
    classA.setMethods(new MethodElement[] {inheritedMethod});
    ClassElementImpl classB = classElement("B", classA.getType());
    MethodElement method = methodElement(methodName, null);
    classB.setMethods(new MethodElement[] {method});
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {
        classA, classB});
    assertSame(inheritedMethod, classB.lookUpInheritedMethod(methodName, library));
  }

  public void test_lookUpInheritedMethod_inherited() {
    // class A {
    //   m() {}
    // }
    // class B extends A {
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement inheritedMethod = methodElement(methodName, null);
    classA.setMethods(new MethodElement[] {inheritedMethod});
    ClassElementImpl classB = classElement("B", classA.getType());
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {
        classA, classB});
    assertSame(inheritedMethod, classB.lookUpInheritedMethod(methodName, library));
  }

  public void test_lookUpInheritedMethod_undeclared() {
    // class A {
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {classA});
    assertNull(classA.lookUpInheritedMethod("m", library));
  }

  public void test_lookUpMethod_declared() {
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement method = methodElement(methodName, null);
    classA.setMethods(new MethodElement[] {method});
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {classA});
    assertSame(method, classA.lookUpMethod(methodName, library));
  }

  public void test_lookUpMethod_inherited() {
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    String methodName = "m";
    MethodElement method = methodElement(methodName, null);
    classA.setMethods(new MethodElement[] {method});
    ClassElementImpl classB = classElement("B", classA.getType());
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {
        classA, classB});
    assertSame(method, classB.lookUpMethod(methodName, library));
  }

  public void test_lookUpMethod_undeclared() {
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {classA});
    assertNull(classA.lookUpMethod("m", library));
  }

  public void test_lookUpMethod_undeclared_recursive() {
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B", classA.getType());
    classA.setSupertype(classB.getType());
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {
        classA, classB});
    assertNull(classA.lookUpMethod("m", library));
  }

  public void test_lookUpSetter_declared() {
    // class A {
    //   set g(x) {}
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    String setterName = "s";
    PropertyAccessorElement setter = setterElement(setterName, false, null);
    classA.setAccessors(new PropertyAccessorElement[] {setter});
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {classA});
    assertSame(setter, classA.lookUpSetter(setterName, library));
  }

  public void test_lookUpSetter_inherited() {
    // class A {
    //   set g(x) {}
    // }
    // class B extends A {
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    String setterName = "s";
    PropertyAccessorElement setter = setterElement(setterName, false, null);
    classA.setAccessors(new PropertyAccessorElement[] {setter});
    ClassElementImpl classB = classElement("B", classA.getType());
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {
        classA, classB});
    assertSame(setter, classB.lookUpSetter(setterName, library));
  }

  public void test_lookUpSetter_undeclared() {
    // class A {
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {classA});
    assertNull(classA.lookUpSetter("s", library));
  }

  public void test_lookUpSetter_undeclared_recursive() {
    // class A extends B {
    // }
    // class B extends A {
    // }
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B", classA.getType());
    classA.setSupertype(classB.getType());
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {
        classA, classB});
    assertNull(classA.lookUpSetter("s", library));
  }
}
