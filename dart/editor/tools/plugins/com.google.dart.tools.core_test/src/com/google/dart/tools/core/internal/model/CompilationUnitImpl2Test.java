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

import com.google.dart.tools.core.internal.workingcopy.DefaultWorkingCopyOwner;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.testutil.TestFileUtil;

import junit.framework.TestCase;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Sanity check the {@link CompilationUnitImpl}
 */
public class CompilationUnitImpl2Test extends TestCase {

  private IProject project;
  private IContainer container;

  public CompilationUnitImpl getCompUnit(Class<?> base, String relPath) throws CoreException,
      IOException, DartModelException {
    DartLibraryImpl lib = getDartApp();
    String expected = TestFileUtil.readResource(base, relPath);
    String fileName = relPath.substring(relPath.lastIndexOf('/') + 1);
    IFile file = getOrCreateFile(fileName, expected);
    DefaultWorkingCopyOwner wcopy = DefaultWorkingCopyOwner.getInstance();
    CompilationUnitImpl unit = new CompilationUnitImpl(lib, file, wcopy);
    String actual = unit.getSource();
    assertEquals(expected, actual);
    return unit;
  }

  public void test_CompilationUnitImpl_getSource_ClassTest() throws Exception {
    getCompUnit("testsource/ClassTest.dart");
  }

  public void test_CompilationUnitImpl_getSource_Clock() throws Exception {
    CompilationUnitImpl unit = getCompUnit("testsource/Clock.dart");
    Type[] types = unit.getTypes();
    assertEquals(7, types.length);
    Type type = unit.getType("Number");
    assertNotNull(type);
    assertEquals(7, type.getFields().length);
    assertEquals(2, type.getMethods().length);
    assertEquals(9, type.getChildren().length);
    assertEquals(unit, type.getParent());
  }

  public void test_CompilationUnitImpl_getSource_CoreRuntimeTypesTest() throws Exception {
    getCompUnit("testsource/CoreRuntimeTypesTest.dart");
  }

  public void test_CompilationUnitImpl_getSource_DefaultInitTest() throws Exception {
    getCompUnit("testsource/DefaultInitTest.dart");
  }

  public void test_CompilationUnitImpl_getSource_ExpressionTest() throws Exception {
    getCompUnit("testsource/ExpressionTest.dart");
  }

  public void test_CompilationUnitImpl_getSource_FunctionTest() throws Exception {
    getCompUnit("testsource/FunctionTest.dart");
  }

  public void test_CompilationUnitImpl_getSource_GettersSettersTest() throws Exception {
    getCompUnit("testsource/GettersSettersTest.dart");
  }

  public void test_CompilationUnitImpl_getSource_NamingTest() throws Exception {
    getCompUnit("testsource/NamingTest.dart");
  }

  public void test_CompilationUnitImpl_getSource_OperatorTest() throws Exception {
    getCompUnit("testsource/OperatorTest.dart");
  }

  public void test_CompilationUnitImpl_getSource_SpreadArgumentTest() throws Exception {
    getCompUnit("testsource/SpreadArgumentTest.dart");
  }

  public void test_CompilationUnitImpl_getSource_StatementTest() throws Exception {
    getCompUnit("testsource/StatementTest.dart");
  }

  public void test_CompilationUnitImpl_getSource_SuperTest() throws Exception {
    getCompUnit("testsource/SuperTest.dart");
  }

  public void test_CompilationUnitImpl_getSource_TryCatchTest() throws Exception {
    getCompUnit("testsource/TryCatchTest.dart");
  }

  public void test_CompilationUnitImpl_getSource_UnaryTest() throws Exception {
    getCompUnit("testsource/UnaryTest.dart");
  }

  private CompilationUnitImpl getCompUnit(String relPath) throws CoreException, IOException,
      DartModelException {
    return getCompUnit(getClass(), relPath);
  }

  private IContainer getContainer() throws CoreException {
    if (container == null) {
      container = TestFileUtil.getOrCreateFolder(getProject(), "src");
    }
    return container;
  }

  private DartLibraryImpl getDartApp() throws CoreException, IOException {
    return new DartLibraryImpl(getDartProject(), getOrCreateFile("MyApp.app"));
  }

  private DartModelImpl getDartModel() {
    return DartModelManager.getInstance().getDartModel();
  }

  private DartProjectImpl getDartProject() throws CoreException {
    return new DartProjectImpl(getDartModel(), getProject());
  }

  private IFile getOrCreateFile(String fileName) throws CoreException, IOException {
    return getOrCreateFile(
        fileName,
        TestFileUtil.readResource(getClass(), "testsource/" + fileName));
  }

  private IFile getOrCreateFile(String fileName, String expected) throws CoreException {
    final IFile file = getContainer().getFile(new Path(fileName));
    if (file.exists()) {
      return file;
    }
    final InputStream stream = new ByteArrayInputStream(expected.getBytes());
    TestFileUtil.run(new IWorkspaceRunnable() {
      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        file.create(stream, 0, new NullProgressMonitor());
      }
    });
    return file;
  }

  private IProject getProject() throws CoreException {
    if (project == null) {
      project = TestFileUtil.getOrCreateDartProject(getClass().getSimpleName());
    }
    return project;
  }

}
