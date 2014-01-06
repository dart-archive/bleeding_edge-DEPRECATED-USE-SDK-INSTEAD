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
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.InterfaceType;

import static com.google.dart.engine.element.ElementFactory.classElement;
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

  public void test_lookUpGetter_declared() {
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    String getterName = "g";
    PropertyAccessorElement getter = getterElement(getterName, false, null);
    classA.setAccessors(new PropertyAccessorElement[] {getter});
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {classA});
    assertSame(getter, classA.lookUpGetter(getterName, library));
  }

  public void test_lookUpGetter_inherited() {
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
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {classA});
    assertNull(classA.lookUpGetter("g", library));
  }

  public void test_lookUpGetter_undeclared_recursive() {
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B", classA.getType());
    classA.setSupertype(classB.getType());
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {
        classA, classB});
    assertNull(classA.lookUpGetter("g", library));
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
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    String setterName = "s";
    PropertyAccessorElement setter = setterElement(setterName, false, null);
    classA.setAccessors(new PropertyAccessorElement[] {setter});
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {classA});
    assertSame(setter, classA.lookUpSetter(setterName, library));
  }

  public void test_lookUpSetter_inherited() {
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
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {classA});
    assertNull(classA.lookUpSetter("s", library));
  }

  public void test_lookUpSetter_undeclared_recursive() {
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B", classA.getType());
    classA.setSupertype(classB.getType());
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {
        classA, classB});
    assertNull(classA.lookUpSetter("s", library));
  }
}
