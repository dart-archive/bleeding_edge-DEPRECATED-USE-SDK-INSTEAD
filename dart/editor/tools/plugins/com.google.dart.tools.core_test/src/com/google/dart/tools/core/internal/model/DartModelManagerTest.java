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

import com.google.dart.compiler.ast.DartDirective;
import com.google.dart.compiler.ast.DartLibraryDirective;
import com.google.dart.compiler.ast.DartSourceDirective;
import com.google.dart.compiler.ast.DartStringLiteral;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.internal.compiler.TestDartSource;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.DartResource;
import com.google.dart.tools.core.test.util.FileOperation;
import com.google.dart.tools.core.test.util.MoneyProjectUtilities;
import com.google.dart.tools.core.test.util.TestUtilities;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

public class DartModelManagerTest extends TestCase {
  public void test_DartModelManager_create_file_exist() throws Exception {
    DartProject moneyProject = MoneyProjectUtilities.getMoneyProject();
    IFile file = moneyProject.getProject().getFile("money.dart");
    DartElementImpl element = DartModelManager.getInstance().create(file);
    assertNotNull(element);
    assertTrue(element instanceof CompilationUnitImpl);
    assertTrue(element.exists());
  }

  public void test_DartModelManager_create_file_nonExist() throws Exception {
    DartProject moneyProject = MoneyProjectUtilities.getMoneyProject();
    IFile file = moneyProject.getProject().getFile("doesNotExist.dart");
    DartElementImpl element = DartModelManager.getInstance().create(file);
    assertNull(element);
  }

  public void test_DartModelManager_getBaseLibraryName_directive() throws Exception {
    String libraryName = "library";
    DartUnit unit = new DartUnit(
        new TestDartSource("test.dart", "#library('" + libraryName + "')"),
        false);
    unit.getDirectives().add(new DartLibraryDirective(DartStringLiteral.get(libraryName)));
    String result = getBaseLibraryName(unit);
    assertEquals(libraryName, result);
  }

  public void test_DartModelManager_getBaseLibraryName_noDirective() throws Exception {
    String baseFileName = "test";
    DartUnit unit = new DartUnit(new TestDartSource(baseFileName + ".dart", ""), false);
    String result = getBaseLibraryName(unit);
    assertEquals(baseFileName, result);
  }

  public void test_DartModelManager_getDartModel() {
    assertNotNull(DartModelManager.getInstance().getDartModel());
  }

  public void test_DartModelManager_getFilesForLibrary() throws Exception {
    File libraryFile = TestUtilities.getPluginRelativePath(
        "com.google.dart.tools.core_test",
        new Path("test_data/Geometry/geometry.dart")).toFile();
    DartUnit libraryUnit = parseLibraryFile(libraryFile);
    Set<File> fileSet = getFilesForLibrary(libraryFile, libraryUnit);
    assertNotNull(fileSet);
    assertEquals(3, fileSet.size());
    assertContainsFile(libraryFile, fileSet);
    assertContainsFile(new File(libraryFile.getParentFile(), "point.dart"), fileSet);
    assertContainsFile(new File(libraryFile.getParentFile(), "license.txt"), fileSet);
  }

  public void test_DartModelManager_getFilesForLibrary_duplicate() throws Exception {
    final String libraryFileName = "duplicates.dart";
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDirectory) throws Exception {
        File libraryFile = new File(tempDirectory, libraryFileName);
        String secondFileName = "source.dart";
        String thirdFileName = "unique.dart";
        DartUnit libraryUnit = new DartUnit(new TestDartSource(libraryFileName, ""), false);
        {
          List<DartDirective> libDirectives = libraryUnit.getDirectives();
          libDirectives.add(new DartSourceDirective(DartStringLiteral.get(libraryFileName)));
          libDirectives.add(new DartSourceDirective(DartStringLiteral.get(secondFileName)));
          libDirectives.add(new DartSourceDirective(DartStringLiteral.get(secondFileName)));
          libDirectives.add(new DartSourceDirective(DartStringLiteral.get(thirdFileName)));
        }
        Set<File> fileSet = getFilesForLibrary(libraryFile, libraryUnit);
        assertNotNull(fileSet);
        assertEquals(3, fileSet.size());
        assertContainsFile(libraryFile, fileSet);
        assertContainsFile(new File(tempDirectory, secondFileName), fileSet);
        assertContainsFile(new File(tempDirectory, thirdFileName), fileSet);
      }
    });
  }

  public void test_DartModelManager_getInstance() {
    assertNotNull(DartModelManager.getInstance());
  }

  public void test_DartModelManager_getLibraryName_directive() throws Exception {
    String libraryName = "library";
    DartUnit unit = new DartUnit(
        new TestDartSource("test.dart", "#library('" + libraryName + "')"),
        false);
    unit.getDirectives().add(new DartLibraryDirective(DartStringLiteral.get(libraryName)));
    String result = getLibraryName(unit);
    assertEquals(libraryName, result);
  }

  public void test_DartModelManager_getLibraryName_noDirective() throws Exception {
    String baseFileName = "test";
    DartUnit unit = new DartUnit(new TestDartSource(baseFileName + ".dart", ""), false);
    String result = getLibraryName(unit);
    assertEquals(baseFileName, result);
  }

  public void test_DartModelManager_openLibrary_alreadyOpen() throws Exception {
    final File libraryFile = TestUtilities.getPluginRelativePath(
        "com.google.dart.tools.core_test",
        new Path("test_data/Geometry/geometry.dart")).toFile();
    final File nonLibraryFile = TestUtilities.getPluginRelativePath(
        "com.google.dart.tools.core_test",
        new Path("test_data/Geometry/license.txt")).toFile();
    final DartLibrary[] libraryHolder = new DartLibrary[1];
    ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        libraryHolder[0] = DartModelManager.getInstance().openLibrary(libraryFile, null);
      }
    }, null);
    DartLibrary library = libraryHolder[0];
    assertNotNull(library);
    final DartElement parent = library.getParent();
    try {
      ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
        @Override
        public void run(IProgressMonitor monitor) throws CoreException {
          libraryHolder[0] = DartModelManager.getInstance().openLibrary(nonLibraryFile, null);
        }
      }, null);
      assertEquals(library, libraryHolder[0]);
    } finally {
      ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
        @Override
        public void run(IProgressMonitor monitor) throws CoreException {
          ((DartProject) parent).getProject().delete(true, true, monitor);
        }
      }, null);
    }
  }

  public void test_DartModelManager_openLibrary_library() throws Exception {
    final File libraryFile = TestUtilities.getPluginRelativePath(
        "com.google.dart.tools.core_test",
        new Path("test_data/Geometry/geometry.dart")).toFile();
    assertTrue(libraryFile.exists());
    final DartLibrary[] libraryHolder = new DartLibrary[1];
    ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        libraryHolder[0] = DartModelManager.getInstance().openLibrary(libraryFile, null);
      }
    }, null);
    DartLibrary library = libraryHolder[0];
    assertNotNull(library);
    final DartElement parent = library.getParent();
    try {
      assertValidLibrary(library);
    } finally {
      ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
        @Override
        public void run(IProgressMonitor monitor) throws CoreException {
          ((DartProject) parent).getProject().delete(true, true, monitor);
        }
      }, null);
    }
  }

  public void test_DartModelManager_openLibrary_nonLibrary() throws Exception {
    final File nonLibraryFile = TestUtilities.getPluginRelativePath(
        "com.google.dart.tools.core_test",
        new Path("test_data/Geometry/license.txt")).toFile();
    final DartLibrary[] libraryHolder = new DartLibrary[1];
    ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        libraryHolder[0] = DartModelManager.getInstance().openLibrary(nonLibraryFile, null);
      }
    }, null);
    DartLibrary library = libraryHolder[0];
    assertNotNull(library);
    final DartElement parent = library.getParent();
    try {
      assertValidLibrary(library);
    } finally {
      ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
        @Override
        public void run(IProgressMonitor monitor) throws CoreException {
          ((DartProject) parent).getProject().delete(true, true, monitor);
        }
      }, null);
    }
  }

//  public void test_DartModelManager_parseLibraryFile_invalid() throws Exception {
//    File libraryFile = TestUtilities.getPluginRelativePath("com.google.dart.tools.core_test",
//        new Path("test_data/Geometry/point.dart")).toFile();
//    DartUnit libraryUnit = parseLibraryFile(libraryFile);
//    assertNull(libraryUnit);
//  }

  public void test_DartModelManager_parseLibraryFile_valid() throws Exception {
    File libraryFile = TestUtilities.getPluginRelativePath(
        "com.google.dart.tools.core_test",
        new Path("test_data/Geometry/geometry.dart")).toFile();
    DartUnit libraryUnit = parseLibraryFile(libraryFile);
    assertNotNull(libraryUnit);
  }

  public void test_DartModelManager_parseLibraryFile_valid2() throws Exception {
    File libraryFile = TestUtilities.getPluginRelativePath(
        "com.google.dart.tools.core_test",
        new Path("test_data/UserLibrary/userLib.dart")).toFile();
    DartUnit libraryUnit = parseLibraryFile(libraryFile);
    assertNotNull(libraryUnit);
  }

  /**
   * Assert that the given set of files contains the given file.
   * 
   * @param expectedFile the file that is expected to be in the list
   * @param fileSet the set of files being tested
   */
  private void assertContainsFile(File expectedFile, Set<File> fileSet) {
    for (File file : fileSet) {
      if (file.equals(expectedFile)) {
        return;
      }
    }
    fail("Missing file '" + expectedFile.getAbsolutePath() + "'");
  }

  /**
   * Assert that the given library is valid.
   * 
   * @param library the library to be tested
   * @throws DartModelException if the state of the library could not be determined
   */
  private void assertValidLibrary(DartLibrary library) throws DartModelException {
    assertNotNull(library);
    final DartElement parent = library.getParent();
    assertNotNull(parent);
    assertTrue(parent instanceof DartProject);
    assertEquals("Geometry", library.getDisplayName());
    assertTrue(library.getElementName().endsWith("test_data/Geometry/geometry.dart"));
    CompilationUnit[] units = library.getCompilationUnits();
    assertNotNull(units);
    assertEquals(2, units.length);
    DartResource[] resources = library.getResources();
    assertNotNull(resources);
    assertEquals(1, resources.length);
  }

  /**
   * Invoke the private method {@link DartModelManager#getBaseLibraryName(DartUnit)}.
   * 
   * @param unit the compilation unit to be passed in to the method
   * @return the result of executing the method
   * @throws Exception if the method could not be executed or itself throws an exception
   */
  private String getBaseLibraryName(DartUnit unit) throws Exception {
    DartModelManager manager = DartModelManager.getInstance();
    Method method = DartModelManager.class.getDeclaredMethod("getBaseLibraryName", DartUnit.class);
    method.setAccessible(true);
    return (String) method.invoke(manager, unit);
  }

  /**
   * Invoke the private method {@link DartModelManager#parseLibraryFile(File)}.
   * 
   * @param libraryFile the library file to be passed in to the method
   * @return the result of executing the method
   * @throws Exception if the method could not be executed or itself throws an exception
   */
  @SuppressWarnings("unchecked")
  private Set<File> getFilesForLibrary(File libraryFile, DartUnit libraryUnit) throws Exception {
    DartModelManager manager = DartModelManager.getInstance();
    Method method = DartModelManager.class.getDeclaredMethod(
        "getFilesForLibrary",
        File.class,
        DartUnit.class);
    method.setAccessible(true);
    return (Set<File>) method.invoke(manager, libraryFile, libraryUnit);
  }

  /**
   * Invoke the private method {@link DartModelManager#getLibraryName(DartUnit)}.
   * 
   * @param unit the compilation unit to be passed in to the method
   * @return the result of executing the method
   * @throws Exception if the method could not be executed or itself throws an exception
   */
  private String getLibraryName(DartUnit unit) throws Exception {
    DartModelManager manager = DartModelManager.getInstance();
    Method method = DartModelManager.class.getDeclaredMethod("getLibraryName", DartUnit.class);
    method.setAccessible(true);
    return (String) method.invoke(manager, unit);
  }

  /**
   * Invoke the private method {@link DartModelManager#parseLibraryFile(File)}.
   * 
   * @param libraryFile the library file to be passed in to the method
   * @return the result of executing the method
   * @throws Exception if the method could not be executed or itself throws an exception
   */
  private DartUnit parseLibraryFile(File libraryFile) throws Exception {
    DartModelManager manager = DartModelManager.getInstance();
    Method method = DartModelManager.class.getDeclaredMethod("parseLibraryFile", File.class);
    method.setAccessible(true);
    return (DartUnit) method.invoke(manager, libraryFile);
  }
}
