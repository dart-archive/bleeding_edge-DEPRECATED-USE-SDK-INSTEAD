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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.info.DartProjectInfo;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.test.util.FileOperation;
import com.google.dart.tools.core.test.util.MoneyProjectUtilities;
import com.google.dart.tools.core.test.util.TestUtilities;

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class DartProjectImplTest extends TestCase {
  public void test_DartProjectImpl_computeChildPaths_money() throws Exception {
    DartProjectImpl project = (DartProjectImpl) MoneyProjectUtilities.getMoneyProject();
    ArrayList<String> paths = new ArrayList<String>();
    computeChildPaths(project, paths);
    assertEquals(1, paths.size());
    assertEquals("money.dart", paths.get(0));
  }

  public void test_DartProjectImpl_getChildren() {
    // TODO Implement this
  }

  public void test_DartProjectImpl_getCorrespondingResource() throws DartModelException {
    IProject project = new MockProject("testProject") {
      @Override
      public boolean hasNature(String nature) {
        return nature.equals(DartCore.DART_PROJECT_NATURE);
      }
    };
    DartProjectImpl dartProject = new DartProjectImpl(new DartModelImpl(), project);
    assertEquals(project, dartProject.getCorrespondingResource());
  }

  public void test_DartProjectImpl_getDartLibraries() {
    // TODO Implement this
  }

  public void test_DartProjectImpl_getElementName() {
    String projectName = "project";
    IProject project = new MockProject(projectName);
    DartProjectImpl dartProject = new DartProjectImpl(new DartModelImpl(), project);
    assertEquals(projectName, dartProject.getElementName());
  }

  public void test_DartProjectImpl_getNonDartResources() {
    IProject project = new MockProject();
    DartProjectImpl dartProject = new DartProjectImpl(new DartModelImpl(), project);
    try {
      dartProject.getNonDartResources();
      fail("Expected DartModelException");
    } catch (DartModelException exception) {
      // Expected
    }
  }

  public void test_DartProjectImpl_getProject_notNull() {
    IProject project = new MockProject();
    DartProjectImpl dartProject = new DartProjectImpl(new DartModelImpl(), project);
    assertEquals(project, dartProject.getProject());
  }

  public void test_DartProjectImpl_setChildPaths() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(final File file) throws Exception {
        IProject project = new MockProject() {
          @Override
          public IPath getLocation() {
            return new Path(file.getAbsolutePath());
          }
        };
        DartProjectImpl projectImpl = new DartProjectImpl(new DartModelImpl(), project);
        DartProjectInfo projectInfo = new DartProjectInfo();
        String path = "test.dart";
        List<String> paths = new ArrayList<String>();
        paths.add(path);
        setChildPaths(projectImpl, projectInfo, paths);
        // Force the file to be re-read.
        projectInfo = new DartProjectInfo();
        paths = getChildPaths(projectImpl, projectInfo);
        assertNotNull(paths);
        assertEquals(1, paths.size());
        assertEquals(path, paths.get(0));
      }
    });
  }

  /**
   * Invoke the private method {@link DartProjectImpl#computeChildPaths(List<String>)}.
   * 
   * @param impl the project impl on which the method is to be invoked
   * @param paths the list argument to be passed to the method
   * @throws Exception if the method could not be executed or itself throws an exception
   */
  private void computeChildPaths(DartProjectImpl impl, List<String> paths) throws Exception {
    Method method = DartProjectImpl.class.getDeclaredMethod("computeChildPaths", List.class);
    method.setAccessible(true);
    method.invoke(impl, paths);
  }

  /**
   * Invoke the private method {@link DartProjectImpl#getChildPaths(DartProjectInfo)}.
   * 
   * @param impl the project impl on which the method is to be invoked
   * @param info the info argument to be passed to the method
   * @return the result of executing the method
   * @throws Exception if the method could not be executed or itself throws an exception
   */
  @SuppressWarnings("unchecked")
  private List<String> getChildPaths(DartProjectImpl impl, DartProjectInfo info) throws Exception {
    Method method = DartProjectImpl.class.getDeclaredMethod(
        "getChildPaths",
        DartProjectInfo.class,
        boolean.class);
    method.setAccessible(true);
    return (List<String>) method.invoke(impl, info, false);
  }

  /**
   * Invoke the private method {@link DartProjectImpl#getChildPaths(DartProjectInfo)}.
   * 
   * @param impl the project impl on which the method is to be invoked
   * @param info the info argument to be passed to the method
   * @param paths the paths to be passed to the method
   * @return the result of executing the method
   * @throws Exception if the method could not be executed or itself throws an exception
   */
  private void setChildPaths(DartProjectImpl impl, DartProjectInfo info, List<String> paths)
      throws Exception {
    Method method = DartProjectImpl.class.getDeclaredMethod(
        "setChildPaths",
        DartProjectInfo.class,
        List.class);
    method.setAccessible(true);
    method.invoke(impl, info, paths);
  }
}
