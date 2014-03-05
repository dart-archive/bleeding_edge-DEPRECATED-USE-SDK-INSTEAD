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
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.utilities.io.FileUtilities2;

import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.library;

import java.util.HashSet;

public class SubtypeManagerTest extends EngineTestCase {

  /**
   * The inheritance manager being tested.
   */
  private SubtypeManager subtypeManager;

  /**
   * The compilation unit element containing all of the types setup in each test.
   */
  private CompilationUnitElementImpl definingCompilationUnit;

  public void test_computeAllSubtypes_infiniteLoop() throws Exception {
    //
    // class A extends B
    // class B extends A
    //
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B", classA.getType());
    classA.setSupertype(classB.getType());
    definingCompilationUnit.setTypes(new ClassElement[] {classA, classB});

    HashSet<ClassElement> subtypesOfA = subtypeManager.computeAllSubtypes(classA);
    ClassElement[] arraySubtypesOfA = subtypesOfA.toArray(new ClassElement[subtypesOfA.size()]);

    assertSizeOfSet(2, subtypesOfA);
    assertContains(arraySubtypesOfA, classA, classB);
  }

  public void test_computeAllSubtypes_manyRecursiveSubtypes() throws Exception {
    //
    // class A
    // class B extends A
    // class C extends B
    // class D extends B
    // class E extends B
    //
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B", classA.getType());
    ClassElementImpl classC = classElement("C", classB.getType());
    ClassElementImpl classD = classElement("D", classB.getType());
    ClassElementImpl classE = classElement("E", classB.getType());
    definingCompilationUnit.setTypes(new ClassElement[] {classA, classB, classC, classD, classE});

    HashSet<ClassElement> subtypesOfA = subtypeManager.computeAllSubtypes(classA);
    ClassElement[] arraySubtypesOfA = subtypesOfA.toArray(new ClassElement[subtypesOfA.size()]);

    HashSet<ClassElement> subtypesOfB = subtypeManager.computeAllSubtypes(classB);
    ClassElement[] arraySubtypesOfB = subtypesOfB.toArray(new ClassElement[subtypesOfB.size()]);

    assertSizeOfSet(4, subtypesOfA);
    assertContains(arraySubtypesOfA, classB, classC, classD, classE);

    assertSizeOfSet(3, subtypesOfB);
    assertContains(arraySubtypesOfB, classC, classD, classE);
  }

  public void test_computeAllSubtypes_noSubtypes() throws Exception {
    //
    // class A
    //
    ClassElementImpl classA = classElement("A");
    definingCompilationUnit.setTypes(new ClassElement[] {classA});

    HashSet<ClassElement> subtypesOfA = subtypeManager.computeAllSubtypes(classA);

    assertSizeOfSet(0, subtypesOfA);
  }

  public void test_computeAllSubtypes_oneSubtype() throws Exception {
    //
    // class A
    // class B extends A
    //
    ClassElementImpl classA = classElement("A");
    ClassElementImpl classB = classElement("B", classA.getType());
    definingCompilationUnit.setTypes(new ClassElement[] {classA, classB});

    HashSet<ClassElement> subtypesOfA = subtypeManager.computeAllSubtypes(classA);
    ClassElement[] arraySubtypesOfA = subtypesOfA.toArray(new ClassElement[subtypesOfA.size()]);

    assertSizeOfSet(1, subtypesOfA);
    assertContains(arraySubtypesOfA, classB);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    AnalysisContextImpl context = AnalysisContextFactory.contextWithCore();
    FileBasedSource source = new FileBasedSource(FileUtilities2.createFile("/test.dart"));
    definingCompilationUnit = new CompilationUnitElementImpl("test.dart");
    definingCompilationUnit.setSource(source);
    LibraryElementImpl definingLibrary = library(context, "test");
    definingLibrary.setDefiningCompilationUnit(definingCompilationUnit);
    subtypeManager = new SubtypeManager();
  }

}
