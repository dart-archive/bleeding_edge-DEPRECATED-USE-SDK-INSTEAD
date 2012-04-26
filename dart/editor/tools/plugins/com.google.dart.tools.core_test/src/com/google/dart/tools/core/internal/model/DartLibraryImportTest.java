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
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartLibraryImport;
import com.google.dart.tools.core.test.util.TestProject;

import junit.framework.TestCase;

import org.eclipse.core.resources.IResource;

/**
 * Test for {@link DartLibraryImport}.
 */
public class DartLibraryImportTest extends TestCase {
  private TestProject testProject;
  DartLibrary libraryA;
  DartLibrary libraryB;

  public void test_access() throws Exception {
    DartLibraryImport imp = new DartLibraryImport(libraryA, "aaa");
    assertSame(libraryA, imp.getLibrary());
    assertEquals("aaa", imp.getPrefix());
  }

  public void test_equals() throws Exception {
    DartLibraryImport importA = new DartLibraryImport(libraryA, "aaa");
    DartLibraryImport importB = new DartLibraryImport(libraryB, "bbb");
    // prepared imports
    assertTrue(importA.equals(importA));
    assertTrue(importB.equals(importB));
    assertFalse(importA.equals(importB));
    assertFalse(importB.equals(importA));
    // prepared and new imports
    assertTrue(importA.equals(new DartLibraryImport(libraryA, "aaa")));
    assertFalse(importA.equals(new DartLibraryImport(libraryA, "aaaa")));
    assertFalse(importA.equals(new DartLibraryImport(libraryB, "aaa")));
    assertFalse(importA.equals(new DartLibraryImport(null, "aaa")));
    assertFalse(importA.equals(new DartLibraryImport(null, null)));
    // not imports
    assertFalse(importA.equals(null));
    assertFalse(importA.equals(this));
  }

  public void test_hasCode() throws Exception {
    new DartLibraryImport(libraryA, "aaa").hashCode();
    new DartLibraryImport(libraryA, null).hashCode();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    testProject = new TestProject("Test");
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
    libraryA = testProject.getDartProject().getDartLibrary(libResourceA);
    libraryB = testProject.getDartProject().getDartLibrary(libResourceB);
  }

  @Override
  protected void tearDown() throws Exception {
    testProject.dispose();
    super.tearDown();
  }
}
