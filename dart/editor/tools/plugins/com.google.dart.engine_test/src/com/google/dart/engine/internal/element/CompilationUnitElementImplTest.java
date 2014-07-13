/*
 * Copyright (c) 2014, the Dart project authors.
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
import com.google.dart.engine.internal.resolver.TestTypeProvider;

import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.compilationUnit;
import static com.google.dart.engine.element.ElementFactory.enumElement;

public class CompilationUnitElementImplTest extends EngineTestCase {
  public void test_getEnum_declared() {
    TestTypeProvider typeProvider = new TestTypeProvider();
    CompilationUnitElementImpl unit = compilationUnit("/lib.dart");
    String enumName = "E";
    ClassElement enumElement = enumElement(typeProvider, enumName);
    unit.setEnums(new ClassElement[] {enumElement});
    assertSame(enumElement, unit.getEnum(enumName));
  }

  public void test_getEnum_undeclared() {
    TestTypeProvider typeProvider = new TestTypeProvider();
    CompilationUnitElementImpl unit = compilationUnit("/lib.dart");
    String enumName = "E";
    ClassElement enumElement = enumElement(typeProvider, enumName);
    unit.setEnums(new ClassElement[] {enumElement});
    assertNull(unit.getEnum(enumName + "x"));
  }

  public void test_getType_declared() {
    CompilationUnitElementImpl unit = compilationUnit("/lib.dart");
    String className = "C";
    ClassElement classElement = classElement(className);
    unit.setTypes(new ClassElement[] {classElement});
    assertSame(classElement, unit.getType(className));
  }

  public void test_getType_undeclared() {
    CompilationUnitElementImpl unit = compilationUnit("/lib.dart");
    String className = "C";
    ClassElement classElement = classElement(className);
    unit.setTypes(new ClassElement[] {classElement});
    assertNull(unit.getType(className + "x"));
  }
}
