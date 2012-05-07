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
package com.google.dart.tools.core.internal.model;

import com.google.common.base.Joiner;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartImport;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.test.util.TestProject;

import junit.framework.TestCase;

import org.eclipse.core.resources.IResource;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for {@link DartImportImpl}.
 */
public class DartImportImplTest extends TestCase {

  /**
   * Test for {@link DartLibrary#getImports()}.
   */
  public void test_duplicateImport() throws Exception {
    TestProject testProject = new TestProject("Test");
    try {
      IResource libResourceA = testProject.setUnitContent(
          "LibA.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "#library('A');",
              "")).getResource();
      IResource resourceTest = testProject.setUnitContent(
          "TestC.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "#library('Test');",
              "#import('LibA.dart');",
              "#import('LibA.dart');",
              "")).getResource();
      DartLibrary libraryA = testProject.getDartProject().getDartLibrary(libResourceA);
      DartLibrary libraryTest = testProject.getDartProject().getDartLibrary(resourceTest);
      //
      DartImport[] imports = libraryTest.getImports();
      assertThat(imports).hasSize(1);
      {
        assertEquals(libraryA, imports[0].getLibrary());
        assertEquals(null, imports[0].getPrefix());
      }
    } finally {
      testProject.dispose();
    }
  }

  /**
   * Test for {@link DartLibrary#getImports()}.
   */
  public void test_oneLibrary_twoPrefixes() throws Exception {
    TestProject testProject = new TestProject("Test");
    try {
      IResource libResourceA = testProject.setUnitContent(
          "LibA.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "#library('A');",
              "")).getResource();
      IResource resourceTest = testProject.setUnitContent(
          "TestC.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "#library('Test');",
              "#import('LibA.dart', prefix: 'aaa');",
              "#import('LibA.dart', prefix: 'bbb');",
              "")).getResource();
      DartLibrary libraryA = testProject.getDartProject().getDartLibrary(libResourceA);
      DartLibrary libraryTest = testProject.getDartProject().getDartLibrary(resourceTest);
      //
      DartImport[] imports = libraryTest.getImports();
      assertThat(imports).hasSize(2);
      {
        assertEquals(libraryA, imports[0].getLibrary());
        assertEquals("aaa", imports[0].getPrefix());
      }
      {
        assertEquals(libraryA, imports[1].getLibrary());
        assertEquals("bbb", imports[1].getPrefix());
      }
    } finally {
      testProject.dispose();
    }
  }

  public void test_twoLibraries_noPrefixes() throws Exception {
    TestProject testProject = new TestProject("Test");
    try {
      IResource libResourceA = testProject.setUnitContent(
          "LibA.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "#library('A');",
              "")).getResource();
      IResource libResourceB = testProject.setUnitContent(
          "LibB.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "#library('B');",
              "")).getResource();
      IResource resourceTest = testProject.setUnitContent(
          "TestC.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "#library('Test');",
              "#import('LibA.dart');",
              "#import('LibB.dart');",
              "")).getResource();
      DartLibrary libraryA = testProject.getDartProject().getDartLibrary(libResourceA);
      DartLibrary libraryB = testProject.getDartProject().getDartLibrary(libResourceB);
      DartLibrary libraryTest = testProject.getDartProject().getDartLibrary(resourceTest);
      CompilationUnit unitTest = libraryTest.getDefiningCompilationUnit();
      String sourceTest = unitTest.getSource();
      //
      DartImport[] imports = libraryTest.getImports();
      assertThat(imports).hasSize(2);
      DartImport importA = imports[0];
      DartImport importB = imports[1];
      // Object methods
      assertTrue(importA.equals(importA));
      assertFalse(importA.equals(importB));
      assertFalse(importA.equals(null));
      assertFalse(importA.equals(this));
      importA.hashCode();
      // "LibA.dart"
      {
        assertEquals(DartElement.IMPORT, importA.getElementType());
        assertEquals(libraryA, importA.getLibrary());
        assertEquals(null, importA.getPrefix());
        assertEquals(null, importA.getElementName());
        assertEquals(null, importA.getNameRange());
        assertEquals(unitTest, importA.getParent());
        assertEquals(unitTest, importA.getCompilationUnit());
        assertEquals(unitTest, importA.getAncestor(CompilationUnit.class));
        {
          String s = "#import('LibA.dart');";
          assertEquals(
              new SourceRangeImpl(sourceTest.indexOf(s), s.length()),
              importA.getSourceRange());
        }
      }
      // "LibB.dart"
      {
        assertEquals(libraryB, importB.getLibrary());
        assertEquals(DartElement.IMPORT, importA.getElementType());
        assertEquals(null, importB.getPrefix());
        assertEquals(null, importB.getElementName());
        assertEquals(null, importB.getNameRange());
        assertEquals(unitTest, importB.getParent());
        assertEquals(unitTest, importB.getCompilationUnit());
        assertEquals(unitTest, importB.getAncestor(CompilationUnit.class));
        {
          String s = "#import('LibB.dart');";
          assertEquals(
              new SourceRangeImpl(sourceTest.indexOf(s), s.length()),
              importB.getSourceRange());
        }
      }
    } finally {
      testProject.dispose();
    }
  }

  /**
   * Test for {@link DartLibrary#getImports()}.
   */
  public void test_twoLibraries_onePrefix() throws Exception {
    TestProject testProject = new TestProject("Test");
    try {
      IResource libResourceA = testProject.setUnitContent(
          "LibA.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "#library('A');",
              "")).getResource();
      IResource libResourceB = testProject.setUnitContent(
          "LibB.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "#library('B');",
              "")).getResource();
      IResource resourceTest = testProject.setUnitContent(
          "TestC.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "#library('Test');",
              "#import('LibA.dart', prefix: 'aaa');",
              "#import('LibB.dart', prefix: 'aaa');",
              "")).getResource();
      DartLibrary libraryA = testProject.getDartProject().getDartLibrary(libResourceA);
      DartLibrary libraryB = testProject.getDartProject().getDartLibrary(libResourceB);
      DartLibrary libraryTest = testProject.getDartProject().getDartLibrary(resourceTest);
      //
      DartImport[] imports = libraryTest.getImports();
      assertThat(imports).hasSize(2);
      {
        assertEquals(libraryA, imports[0].getLibrary());
        assertEquals("aaa", imports[0].getPrefix());
      }
      {
        assertEquals(libraryB, imports[1].getLibrary());
        assertEquals("aaa", imports[1].getPrefix());
      }
    } finally {
      testProject.dispose();
    }
  }

  public void test_twoLibraries_twoPrefixes() throws Exception {
    TestProject testProject = new TestProject("Test");
    try {
      IResource libResourceA = testProject.setUnitContent(
          "LibA.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "#library('A');",
              "")).getResource();
      IResource libResourceB = testProject.setUnitContent(
          "LibB.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "#library('B');",
              "")).getResource();
      IResource resourceTest = testProject.setUnitContent(
          "TestC.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "#library('Test');",
              "#import('LibA.dart', prefix: 'aaa');",
              "#import('LibB.dart', prefix: 'bbb');",
              "")).getResource();
      DartLibrary libraryA = testProject.getDartProject().getDartLibrary(libResourceA);
      DartLibrary libraryB = testProject.getDartProject().getDartLibrary(libResourceB);
      DartLibrary libraryTest = testProject.getDartProject().getDartLibrary(resourceTest);
      CompilationUnit unitTest = libraryTest.getDefiningCompilationUnit();
      String sourceTest = unitTest.getSource();
      //
      DartImport[] imports = libraryTest.getImports();
      assertThat(imports).hasSize(2);
      DartImport importA = imports[0];
      DartImport importB = imports[1];
      // Object methods
      assertTrue(importA.equals(importA));
      assertFalse(importA.equals(importB));
      assertFalse(importA.equals(null));
      assertFalse(importA.equals(this));
      importA.hashCode();
      // "aaa" = "LibA.dart"
      {
        assertEquals(DartElement.IMPORT, importA.getElementType());
        assertEquals(libraryA, importA.getLibrary());
        assertEquals("aaa", importA.getPrefix());
        assertEquals("aaa", importA.getElementName());
        assertEquals(unitTest, importA.getParent());
        assertEquals(unitTest, importA.getCompilationUnit());
        // name range
        assertEquals(new SourceRangeImpl(sourceTest.indexOf("'aaa');"), 5), importA.getNameRange());
        // source range
        {
          String s = "#import('LibA.dart', prefix: 'aaa');";
          assertEquals(
              new SourceRangeImpl(sourceTest.indexOf(s), s.length()),
              importA.getSourceRange());
        }
        // URI range
        {
          String s = "'LibA.dart'";
          assertEquals(
              new SourceRangeImpl(sourceTest.indexOf(s), s.length()),
              importA.getUriRange());
        }
      }
      // "bbb" = "LibB.dart"
      {
        assertEquals(libraryB, importB.getLibrary());
        assertEquals(DartElement.IMPORT, importA.getElementType());
        assertEquals("bbb", importB.getPrefix());
        assertEquals(unitTest, importA.getCompilationUnit());
        // name range
        assertEquals(new SourceRangeImpl(sourceTest.indexOf("'bbb');"), 5), importB.getNameRange());
        // source range
        {
          String s = "#import('LibB.dart', prefix: 'bbb');";
          assertEquals(
              new SourceRangeImpl(sourceTest.indexOf(s), s.length()),
              importB.getSourceRange());
        }
        // URI range
        {
          String s = "'LibB.dart'";
          assertEquals(
              new SourceRangeImpl(sourceTest.indexOf(s), s.length()),
              importB.getUriRange());
        }
      }
    } finally {
      testProject.dispose();
    }
  }
}
