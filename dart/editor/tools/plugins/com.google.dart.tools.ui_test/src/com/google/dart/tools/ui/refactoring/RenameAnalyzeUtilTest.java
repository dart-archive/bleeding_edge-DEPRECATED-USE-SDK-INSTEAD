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
package com.google.dart.tools.ui.refactoring;

import com.google.common.collect.Sets;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.test.util.TestProject;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameAnalyzeUtil;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;
import java.util.Set;

/**
 * Test for {@link RenameAnalyzeUtil}.
 */
public final class RenameAnalyzeUtilTest extends RefactoringTest {

  public void test_getSubTypes() throws Exception {
    setUnitContent(
        "Lib.dart",
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Lib');",
        "class A {}");
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Test');",
        "#import('Lib.dart');",
        "class B extends A {}",
        "class C extends B {}",
        "");
    TestProject.waitForAutoBuild();
    Type typeA = getTopLevelElementNamed("A");
    Type typeB = getTopLevelElementNamed("B");
    Type typeC = getTopLevelElementNamed("C");
    // A
    {
      List<Type> subTypes = RenameAnalyzeUtil.getSubTypes(typeA);
      assertThat(subTypes).containsOnly(typeB, typeC);
    }
    // B
    {
      List<Type> subTypes = RenameAnalyzeUtil.getSubTypes(typeB);
      assertThat(subTypes).containsOnly(typeC);
    }
    // C
    {
      List<Type> subTypes = RenameAnalyzeUtil.getSubTypes(typeC);
      assertThat(subTypes).isEmpty();
    }
  }

  public void test_getSuperTypes_classesOnly() throws Exception {
    setUnitContent(
        "Lib.dart",
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Lib');",
        "class A {}");
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Test');",
        "#import('Lib.dart');",
        "class B extends A {}",
        "class C extends B {}",
        "");
    TestProject.waitForAutoBuild();
    Type typeA = getTopLevelElementNamed("A");
    Type typeB = getTopLevelElementNamed("B");
    Type typeC = getTopLevelElementNamed("C");
    // A
    {
      Set<Type> superTypes = RenameAnalyzeUtil.getSuperTypes(typeA);
      assertThat(superTypes).isEmpty();
    }
    // B
    {
      Set<Type> superTypes = RenameAnalyzeUtil.getSuperTypes(typeB);
      assertThat(superTypes).containsOnly(typeA);
    }
    // C
    {
      Set<Type> superTypes = RenameAnalyzeUtil.getSuperTypes(typeC);
      assertThat(superTypes).containsOnly(typeA, typeB);
    }
  }

  public void test_getSuperTypes_withInterfaces() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Test');",
        "#import('Lib.dart');",
        "interface IA {}",
        "interface IB {}",
        "interface IC {}",
        "interface IAB extends IA, IB {}",
        "interface IBC extends IB, IC {}",
        "class CA implements IAB, IBC {}",
        "");
    TestProject.waitForAutoBuild();
    Type typeIA = getTopLevelElementNamed("IA");
    Type typeIB = getTopLevelElementNamed("IB");
    Type typeIC = getTopLevelElementNamed("IC");
    Type typeIAB = getTopLevelElementNamed("IAB");
    Type typeIBC = getTopLevelElementNamed("IBC");
    Type typeCA = getTopLevelElementNamed("CA");
    // CA
    {
      Set<Type> superTypes = RenameAnalyzeUtil.getSuperTypes(typeCA);
      assertThat(superTypes).containsOnly(typeIAB, typeIBC, typeIA, typeIB, typeIC);
    }
  }

  public void test_getTopLevelElementNamed_class() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class MyClass {}",
        "");
    TestProject.waitForAutoBuild();
    assertNotNull(getTopLevelElementNamed("MyClass"));
    assertNull(getTopLevelElementNamed("noSuchName"));
  }

  public void test_getTopLevelElementNamed_interface() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "interface MyInterface {}",
        "");
    TestProject.waitForAutoBuild();
    assertNotNull(getTopLevelElementNamed("MyInterface"));
    assertNull(getTopLevelElementNamed("noSuchName"));
  }

  public void test_getTopLevelElementNamed_typedef() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef MyFunctionTypeAlias();",
        "");
    TestProject.waitForAutoBuild();
    assertNotNull(getTopLevelElementNamed("MyFunctionTypeAlias"));
    assertNull(getTopLevelElementNamed("noSuchName"));
  }

  public void test_getTopLevelElementNamed_variable() throws Exception {
    setUnitContent(
        "Lib.dart",
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Lib');",
        "var libVar;");
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Test');",
        "#import('Lib.dart');",
        "var testVar;",
        "");
    TestProject.waitForAutoBuild();
    assertNotNull(getTopLevelElementNamed("testVar"));
    assertNotNull(getTopLevelElementNamed("libVar"));
    assertNull(getTopLevelElementNamed("noSuchName"));
  }

  /**
   * @return the {@link DartElement} in library of {@link #testUnit}.
   */
  @SuppressWarnings("unchecked")
  private <T extends DartElement> T getTopLevelElementNamed(String name) throws DartModelException {
    return (T) RenameAnalyzeUtil.getTopLevelElementNamed(
        Sets.<DartLibrary>newHashSet(),
        testUnit,
        name);
  }
}
