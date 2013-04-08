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
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.internal.context.AnalysisContextImpl;

import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.fieldElement;
import static com.google.dart.engine.element.ElementFactory.library;

public class ElementImplTest extends EngineTestCase {
  public void test_equals() {
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElementImpl classElement = classElement("C");
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {classElement});
    FieldElement field = fieldElement("next", false, false, false, classElement.getType());
    classElement.setFields(new FieldElement[] {field});
    assertTrue(field.equals(field));
    assertFalse(field.equals(field.getGetter()));
    assertFalse(field.equals(field.getSetter()));
    assertFalse(field.getGetter().equals(field.getSetter()));
  }

  public void test_isAccessibleIn_private_differentLibrary() {
    AnalysisContextImpl context = createAnalysisContext();
    LibraryElementImpl library1 = library(context, "lib1");
    ClassElement classElement = classElement("_C");
    ((CompilationUnitElementImpl) library1.getDefiningCompilationUnit()).setTypes(new ClassElement[] {classElement});
    LibraryElementImpl library2 = library(context, "lib2");
    assertFalse(classElement.isAccessibleIn(library2));
  }

  public void test_isAccessibleIn_private_sameLibrary() {
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElement classElement = classElement("_C");
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {classElement});
    assertTrue(classElement.isAccessibleIn(library));
  }

  public void test_isAccessibleIn_public_differentLibrary() {
    AnalysisContextImpl context = createAnalysisContext();
    LibraryElementImpl library1 = library(context, "lib1");
    ClassElement classElement = classElement("C");
    ((CompilationUnitElementImpl) library1.getDefiningCompilationUnit()).setTypes(new ClassElement[] {classElement});
    LibraryElementImpl library2 = library(context, "lib2");
    assertTrue(classElement.isAccessibleIn(library2));
  }

  public void test_isAccessibleIn_public_sameLibrary() {
    LibraryElementImpl library = library(createAnalysisContext(), "lib");
    ClassElement classElement = classElement("C");
    ((CompilationUnitElementImpl) library.getDefiningCompilationUnit()).setTypes(new ClassElement[] {classElement});
    assertTrue(classElement.isAccessibleIn(library));
  }
}
