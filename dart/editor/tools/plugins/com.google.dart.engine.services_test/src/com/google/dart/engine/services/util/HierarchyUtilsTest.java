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
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.services.internal.correction.CorrectionUtils;
import com.google.dart.engine.services.internal.refactoring.RefactoringImplTest;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;
import java.util.Set;

public class HierarchyUtilsTest extends RefactoringImplTest {
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
    CompilationUnitElement unitElement = testUnit.getElement();
    ClassElement classA = (ClassElement) CorrectionUtils.getChildren(unitElement, "A").get(0);
    ClassElement classB = (ClassElement) CorrectionUtils.getChildren(unitElement, "B").get(0);
    // A
    {
      List<Element> members = HierarchyUtils.getDirectMembers(classA, false);
      assertThat(members).hasSize(2);
      assertThat(members.get(0)).isInstanceOf(FieldElement.class);
      assertEquals("ma1", members.get(0).getName());
      assertThat(members.get(1)).isInstanceOf(MethodElement.class);
      assertEquals("ma2", members.get(1).getName());
    }
    // B
    {
      List<Element> members = HierarchyUtils.getDirectMembers(classB, false);
      assertThat(members).hasSize(2);
      assertThat(members.get(0)).isInstanceOf(FieldElement.class);
      assertEquals("mb1", members.get(0).getName());
      assertThat(members.get(1)).isInstanceOf(MethodElement.class);
      assertEquals("mb2", members.get(1).getName());
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
    CompilationUnitElement unitElement = testUnit.getElement();
    ClassElement classA = (ClassElement) CorrectionUtils.getChildren(unitElement, "A").get(0);
    ClassElement classB = (ClassElement) CorrectionUtils.getChildren(unitElement, "B").get(0);
    // A
    {
      List<Element> members = HierarchyUtils.getMembers(classA, false);
      assertThat(members).hasSize(2);
      assertThat(members.get(0)).isInstanceOf(FieldElement.class);
      assertEquals("ma1", members.get(0).getName());
      assertThat(members.get(1)).isInstanceOf(MethodElement.class);
      assertEquals("ma2", members.get(1).getName());
    }
    // B
    {
      List<Element> members = HierarchyUtils.getMembers(classB, false);
      assertThat(members).hasSize(4);
      // mb1
      assertThat(members.get(0)).isInstanceOf(FieldElement.class);
      assertEquals("mb1", members.get(0).getName());
      // mb2
      assertThat(members.get(1)).isInstanceOf(MethodElement.class);
      assertEquals("mb2", members.get(1).getName());
      // ma1
      assertThat(members.get(2)).isInstanceOf(FieldElement.class);
      assertEquals("ma1", members.get(2).getName());
      // ma2
      assertThat(members.get(3)).isInstanceOf(MethodElement.class);
      assertEquals("ma2", members.get(3).getName());
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
    CompilationUnitElement unitElement = testUnit.getElement();
    ClassElement classA = (ClassElement) CorrectionUtils.getChildren(unitElement, "A").get(0);
    ClassElement classB = (ClassElement) CorrectionUtils.getChildren(unitElement, "B").get(0);
    ClassElement classC = (ClassElement) CorrectionUtils.getChildren(unitElement, "C").get(0);
    ClassElement classD = (ClassElement) CorrectionUtils.getChildren(unitElement, "D").get(0);
    ClassElement classE = (ClassElement) CorrectionUtils.getChildren(unitElement, "E").get(0);
    ClassElement classM = (ClassElement) CorrectionUtils.getChildren(unitElement, "M").get(0);
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
        "");
    CompilationUnitElement unitElement = testUnit.getElement();
    ClassElement classA = (ClassElement) CorrectionUtils.getChildren(unitElement, "A").get(0);
    ClassElement classB = (ClassElement) CorrectionUtils.getChildren(unitElement, "B").get(0);
    ClassElement classC = (ClassElement) CorrectionUtils.getChildren(unitElement, "C").get(0);
    ClassElement classD = (ClassElement) CorrectionUtils.getChildren(unitElement, "D").get(0);
    ClassElement classE = (ClassElement) CorrectionUtils.getChildren(unitElement, "E").get(0);
    // Object
    {
      ClassElement objectElement = classA.getSupertype().getElement();
      List<ClassElement> supers = HierarchyUtils.getSuperClasses(objectElement);
      assertThat(supers).containsOnly();
    }
    // A
    {
      List<ClassElement> supers = HierarchyUtils.getSuperClasses(classA);
      assertThat(supers).containsOnly();
    }
    // B
    {
      List<ClassElement> supers = HierarchyUtils.getSuperClasses(classB);
      assertThat(supers).containsOnly(classA);
    }
    // C
    {
      List<ClassElement> supers = HierarchyUtils.getSuperClasses(classC);
      assertThat(supers).containsOnly(classA, classB);
    }
    // D
    {
      List<ClassElement> supers = HierarchyUtils.getSuperClasses(classD);
      assertThat(supers).containsOnly(classA, classB);
    }
    // E
    {
      List<ClassElement> supers = HierarchyUtils.getSuperClasses(classE);
      assertThat(supers).containsOnly(classA);
    }
  }
}
