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

package com.google.dart.engine.services.util;

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ClassMemberElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.services.internal.correction.CorrectionUtils;
import com.google.dart.engine.services.internal.refactoring.RefactoringImplTest;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class HierarchyUtilsTest extends RefactoringImplTest {
  private static void removeObjectMembers(List<Element> members) {
    for (Iterator<Element> iter = members.iterator(); iter.hasNext();) {
      Element element = iter.next();
      if (element.getEnclosingElement() instanceof ClassElement) {
        ClassElement clazz = (ClassElement) element.getEnclosingElement();
        if (clazz.getSupertype() == null) {
          iter.remove();
        }
      }
    }
  }

  public void test_getDirectMembers() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A() {}",
        "  var ma1;",
        "  ma2() {}",
        "}",
        "class B extends A {",
        "  B() {}",
        "  B.named() {}",
        "  var mb1;",
        "  mb2() {}",
        "}",
        "");
    ClassElement classA = getClassElement("A");
    ClassElement classB = getClassElement("B");
    // A
    {
      List<Element> members = HierarchyUtils.getDirectMembers(classA, false);
      assertThat(members).hasSize(2);
      assertThat(members.get(0)).isInstanceOf(FieldElement.class);
      assertEquals("ma1", members.get(0).getDisplayName());
      assertThat(members.get(1)).isInstanceOf(MethodElement.class);
      assertEquals("ma2", members.get(1).getDisplayName());
    }
    // B
    {
      List<Element> members = HierarchyUtils.getDirectMembers(classB, false);
      assertThat(members).hasSize(2);
      assertThat(members.get(0)).isInstanceOf(FieldElement.class);
      assertEquals("mb1", members.get(0).getDisplayName());
      assertThat(members.get(1)).isInstanceOf(MethodElement.class);
      assertEquals("mb2", members.get(1).getDisplayName());
    }
  }

  public void test_getHierarchyMembers_constructors() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A { A() {} }",
        "class B extends A { B() {} }",
        "");
    ConstructorElement methodA = getConstructorElement("A", null);
    ConstructorElement methodB = getConstructorElement("B", null);
    // A
    {
      Set<ClassMemberElement> members = HierarchyUtils.getHierarchyMembers(searchEngine, methodA);
      assertThat(members).containsOnly(methodA);
    }
    // B
    {
      Set<ClassMemberElement> members = HierarchyUtils.getHierarchyMembers(searchEngine, methodB);
      assertThat(members).containsOnly(methodB);
    }
  }

  public void test_getHierarchyMembers_fields() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A { int foo; }",
        "class B extends A { get foo => null; }",
        "class C extends B { set foo(x) {} }",
        "class D { int foo; }",
        "");
    FieldElement fieldA = getFieldElement("A", "foo");
    FieldElement fieldB = getFieldElement("B", "foo");
    FieldElement fieldC = getFieldElement("C", "foo");
    // A
    {
      Set<ClassMemberElement> members = HierarchyUtils.getHierarchyMembers(searchEngine, fieldA);
      assertThat(members).containsOnly(fieldA, fieldB, fieldC);
    }
    // B
    {
      Set<ClassMemberElement> members = HierarchyUtils.getHierarchyMembers(searchEngine, fieldB);
      assertThat(members).containsOnly(fieldA, fieldB, fieldC);
    }
    // C
    {
      Set<ClassMemberElement> members = HierarchyUtils.getHierarchyMembers(searchEngine, fieldC);
      assertThat(members).containsOnly(fieldA, fieldB, fieldC);
    }
  }

  public void test_getHierarchyMembers_methods() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A { foo() {} }",
        "class B extends A { foo() {} }",
        "class C extends B { foo() {} }",
        "class D { foo() {} }",
        "class E extends D { foo() {} }",
        "");
    MethodElement methodA = getMethodElement("A", "foo");
    MethodElement methodB = getMethodElement("B", "foo");
    MethodElement methodC = getMethodElement("C", "foo");
    MethodElement methodD = getMethodElement("D", "foo");
    MethodElement methodE = getMethodElement("E", "foo");
    // A
    {
      Set<ClassMemberElement> members = HierarchyUtils.getHierarchyMembers(searchEngine, methodA);
      assertThat(members).containsOnly(methodA, methodB, methodC);
    }
    // B
    {
      Set<ClassMemberElement> members = HierarchyUtils.getHierarchyMembers(searchEngine, methodB);
      assertThat(members).containsOnly(methodA, methodB, methodC);
    }
    // C
    {
      Set<ClassMemberElement> members = HierarchyUtils.getHierarchyMembers(searchEngine, methodC);
      assertThat(members).containsOnly(methodA, methodB, methodC);
    }
    // D
    {
      Set<ClassMemberElement> members = HierarchyUtils.getHierarchyMembers(searchEngine, methodD);
      assertThat(members).containsOnly(methodD, methodE);
    }
    // E
    {
      Set<ClassMemberElement> members = HierarchyUtils.getHierarchyMembers(searchEngine, methodE);
      assertThat(members).containsOnly(methodD, methodE);
    }
  }

  public void test_getHierarchyMembers_withInterfaces() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A { foo() {} }",
        "class B implements A { foo() {} }",
        "abstract class C implements A { }",
        "class D extends C { foo() {} }",
        "class E { foo() {} }",
        "");
    MethodElement methodA = getMethodElement("A", "foo");
    MethodElement methodB = getMethodElement("B", "foo");
    MethodElement methodD = getMethodElement("D", "foo");
    // A
    {
      Set<ClassMemberElement> members = HierarchyUtils.getHierarchyMembers(searchEngine, methodA);
      assertThat(members).containsOnly(methodA, methodB, methodD);
    }
    // B
    {
      Set<ClassMemberElement> members = HierarchyUtils.getHierarchyMembers(searchEngine, methodB);
      assertThat(members).containsOnly(methodA, methodB, methodD);
    }
    // C
    {
      Set<ClassMemberElement> members = HierarchyUtils.getHierarchyMembers(searchEngine, methodD);
      assertThat(members).containsOnly(methodA, methodB, methodD);
    }
  }

  public void test_getMembers() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A() {}",
        "  var ma1;",
        "  ma2() {}",
        "}",
        "class B extends A {",
        "  B() {}",
        "  B.named() {}",
        "  var mb1;",
        "  mb2() {}",
        "}",
        "");
    ClassElement classA = getClassElement("A");
    ClassElement classB = getClassElement("B");
    // A
    {
      List<Element> members = HierarchyUtils.getMembers(classA, false);
      removeObjectMembers(members);
      assertThat(members).hasSize(2);
      assertThat(members.get(0)).isInstanceOf(FieldElement.class);
      assertEquals("ma1", members.get(0).getDisplayName());
      assertThat(members.get(1)).isInstanceOf(MethodElement.class);
      assertEquals("ma2", members.get(1).getDisplayName());
    }
    // B
    {
      List<Element> members = HierarchyUtils.getMembers(classB, false);
      removeObjectMembers(members);
      assertThat(members).hasSize(4);
      // mb1
      assertThat(members.get(0)).isInstanceOf(FieldElement.class);
      assertEquals("mb1", members.get(0).getDisplayName());
      // mb2
      assertThat(members.get(1)).isInstanceOf(MethodElement.class);
      assertEquals("mb2", members.get(1).getDisplayName());
      // ma1
      assertThat(members.get(2)).isInstanceOf(FieldElement.class);
      assertEquals("ma1", members.get(2).getDisplayName());
      // ma2
      assertThat(members.get(3)).isInstanceOf(MethodElement.class);
      assertEquals("ma2", members.get(3).getDisplayName());
    }
  }

  public void test_getSubClasses() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {}",
        "class B extends A {}",
        "class C extends B {}",
        "class D extends B implements A {}",
        "class M {}",
        "class E extends A with M {}",
        "");
    ClassElement classA = getClassElement("A");
    ClassElement classB = getClassElement("B");
    ClassElement classC = getClassElement("C");
    ClassElement classD = getClassElement("D");
    ClassElement classE = getClassElement("E");
    ClassElement classM = getClassElement("M");
    // A
    {
      Set<ClassElement> subs = HierarchyUtils.getSubClasses(searchEngine, classA);
      assertThat(subs).containsOnly(classB, classC, classD, classE);
    }
    // B
    {
      Set<ClassElement> subs = HierarchyUtils.getSubClasses(searchEngine, classB);
      assertThat(subs).containsOnly(classC, classD);
    }
    // C
    {
      Set<ClassElement> subs = HierarchyUtils.getSubClasses(searchEngine, classC);
      assertThat(subs).containsOnly();
    }
    // M
    {
      Set<ClassElement> subs = HierarchyUtils.getSubClasses(searchEngine, classM);
      assertThat(subs).containsOnly(classE);
    }
  }

  public void test_getSuperClasses() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {}",
        "class B extends A {}",
        "class C extends B {}",
        "class D extends B implements A {}",
        "class M {}",
        "class E extends A with M {}",
        "class F implements A {}",
        "");
    ClassElement classA = getClassElement("A");
    ClassElement classB = getClassElement("B");
    ClassElement classC = getClassElement("C");
    ClassElement classD = getClassElement("D");
    ClassElement classE = getClassElement("E");
    ClassElement classF = getClassElement("F");
    ClassElement objectElement = classA.getSupertype().getElement();
    // Object
    {
      Set<ClassElement> supers = HierarchyUtils.getSuperClasses(objectElement);
      assertThat(supers).containsOnly();
    }
    // A
    {
      Set<ClassElement> supers = HierarchyUtils.getSuperClasses(classA);
      assertThat(supers).containsOnly(objectElement);
    }
    // B
    {
      Set<ClassElement> supers = HierarchyUtils.getSuperClasses(classB);
      assertThat(supers).containsOnly(objectElement, classA);
    }
    // C
    {
      Set<ClassElement> supers = HierarchyUtils.getSuperClasses(classC);
      assertThat(supers).containsOnly(objectElement, classA, classB);
    }
    // D
    {
      Set<ClassElement> supers = HierarchyUtils.getSuperClasses(classD);
      assertThat(supers).containsOnly(objectElement, classA, classB);
    }
    // E
    {
      Set<ClassElement> supers = HierarchyUtils.getSuperClasses(classE);
      assertThat(supers).containsOnly(objectElement, classA);
    }
    // F
    {
      Set<ClassElement> supers = HierarchyUtils.getSuperClasses(classF);
      assertThat(supers).containsOnly(objectElement, classA);
    }
  }

  /**
   * @return the existing {@link ClassElement} form "testUnit".
   */
  private ClassElement getClassElement(String name) {
    CompilationUnitElement unitElement = testUnit.getElement();
    return (ClassElement) CorrectionUtils.getChildren(unitElement, name).get(0);
  }

  /**
   * @return the existing {@link ConstructorElement}.
   */
  private ConstructorElement getConstructorElement(String className, String name) {
    ClassElement classElement = getClassElement(className);
    if (name != null) {
      return classElement.getNamedConstructor(name);
    }
    return classElement.getUnnamedConstructor();
  }

  /**
   * @return the existing {@link FieldElement}.
   */
  private FieldElement getFieldElement(String className, String fieldName) {
    ClassElement classElement = getClassElement(className);
    FieldElement fieldElement = classElement.getField(fieldName);
    if (fieldElement == null) {
      fail("Not found: " + fieldName);
    }
    return fieldElement;
  }

  /**
   * @return the existing {@link MethodElement}.
   */
  private MethodElement getMethodElement(String className, String methodName) {
    ClassElement classElement = getClassElement(className);
    return classElement.getMethod(methodName);
  }
}
