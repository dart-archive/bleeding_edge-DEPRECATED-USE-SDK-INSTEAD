/*
 * Copyright 2013 Dart project authors.
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
package com.google.dart.tools.core.internal.analysis.model;

import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.internal.builder.TestProjects;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.mock.MockWorkspaceRoot;

import junit.framework.TestCase;

public class ProjectManagerImplTest extends TestCase {

  private MockWorkspaceRoot mockRoot;
  private MockProject mockProject;
  private ProjectManagerImpl manager;

  public void test_getProject() {
    Project actual = manager.getProject(mockProject);
    assertNotNull(actual);
    assertSame(mockProject, actual.getResource());
  }

  public void test_getProjects() {
    Project[] actual = manager.getProjects();
    assertNotNull(actual);
    assertEquals(1, actual.length);
    assertNotNull(actual[0]);
    assertSame(manager.getProject(mockProject), actual[0]);
  }

  public void test_getResource() {
    assertSame(mockRoot, manager.getResource());
  }

  @Override
  protected void setUp() throws Exception {
    mockRoot = new MockWorkspaceRoot();
    mockProject = TestProjects.newPubProject3(mockRoot);
    mockRoot.add(mockProject);
    manager = new ProjectManagerImpl(mockRoot);
  }
}
