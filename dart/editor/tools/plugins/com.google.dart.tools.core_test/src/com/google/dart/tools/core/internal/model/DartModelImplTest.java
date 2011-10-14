/*
 * Copyright (c) 2011, the Dart project authors.
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

import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;

import java.util.List;

public class DartModelImplTest extends TestCase {
  public void test_DartModelImpl_copy() {
    // TODO Implement this
  }

  public void test_DartModelImpl_delete() {
    // TODO Implement this
  }

  public void test_DartModelImpl_getChildren() {
    // TODO Implement this
  }

  public void test_DartModelImpl_getCorrespondingResource() {
    DartModelImpl model = new DartModelImpl();
    IResource resource = model.getCorrespondingResource();
    assertTrue(resource instanceof IWorkspaceRoot);
  }

  public void test_DartModelImpl_getDartProject_project() {
    DartModelImpl model = new DartModelImpl();
    IProject project = new MockProject();
    DartProject dartProject = model.getDartProject(project);
    assertNotNull(dartProject);
    assertEquals(project, dartProject.getProject());
  }

  public void test_DartModelImpl_getDartProject_string() {
    DartModelImpl model = new DartModelImpl();
    String projectName = "testProject";
    DartProject project = model.getDartProject(projectName);
    assertNotNull(project);
    assertEquals(projectName, project.getProject().getName());
  }

  public void test_DartModelImpl_getDartProjects() throws DartModelException {
    DartModelImpl model = new DartModelImpl();
    DartProject[] projects = model.getDartProjects();
    assertNotNull(projects);
    // TODO Some of the other tests are not cleaning up after themselves.
    // assertEquals(0, projects.length);
  }

  public void test_DartModelImpl_getElementName() {
    DartModelImpl model = new DartModelImpl();
    assertEquals("", model.getElementName());
  }

  public void test_DartModelImpl_getNonDartResources() throws DartModelException {
    DartModelImpl model = new DartModelImpl();
    IResource[] resources = model.getNonDartResources();
    assertNotNull(resources);
    assertEquals(0, resources.length);
  }

  public void test_DartModelImpl_getUnreferencedLibraries() throws DartModelException {
    DartModelImpl model = new DartModelImpl();
    List<DartLibrary> result = model.getUnreferencedLibraries();
    assertNotNull(result);
    // TODO(brianwilkerson) Clean up other tests so that we can more fully test the results. Right
    // now other tests are creating libraries in the workspace and leaving them around, but we can't
    // know which tests will run before this one, so we don't have a predictable state.
//    assertEquals(0, result.size());
  }

  public void test_DartModelImpl_getWorkspace() {
    DartModelImpl model = new DartModelImpl();
    assertNotNull(model.getWorkspace());
  }

  public void test_DartModelImpl_move() {
    // TODO Implement this
  }

  public void test_DartModelImpl_rename() {
    // TODO Implement this
  }
}
