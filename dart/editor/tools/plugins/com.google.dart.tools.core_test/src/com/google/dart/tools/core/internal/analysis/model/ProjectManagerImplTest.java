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

import com.google.dart.engine.index.Index;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.ProjectManager;
import com.google.dart.tools.core.internal.builder.TestProjects;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.mock.MockWorkspaceRoot;

import junit.framework.TestCase;

import org.eclipse.core.resources.IResource;

import static org.mockito.Mockito.mock;

import java.io.File;

public class ProjectManagerImplTest extends TestCase {

  private MockWorkspaceRoot rootContainer;
  private MockProject projectContainer;
  private ProjectManager manager;
  private DartSdk expectedSdk;
  private DartIgnoreManager ignoreManager = new DartIgnoreManager();

  public void test_getIgnoreManager() throws Exception {
    assertSame(ignoreManager, manager.getIgnoreManager());
  }

  public void test_getIndex() throws Exception {
    Index index = manager.getIndex();
    assertNotNull(index);
    assertSame(index, manager.getIndex());
  }

  public void test_getProject() {
    Project actual = manager.getProject(projectContainer);
    assertNotNull(actual);
    assertSame(projectContainer, actual.getResource());
  }

  public void test_getProjects() {
    Project[] actual = manager.getProjects();
    assertNotNull(actual);
    assertEquals(1, actual.length);
    assertNotNull(actual[0]);
    assertSame(manager.getProject(projectContainer), actual[0]);
  }

  public void test_getResource() {
    assertSame(rootContainer, manager.getResource());
  }

  public void test_getResourceFor() {
    IResource resource = projectContainer.getFolder("web").getFile("other.dart");
    File file = resource.getLocation().toFile();
    Project project = manager.getProject(projectContainer);
    Source source = project.getDefaultContext().getSourceFactory().forFile(file);
    assertSame(resource, manager.getResourceFor(source));
  }

  public void test_getResourceFor_null() {
    assertNull(manager.getResourceFor(null));
  }

  public void test_getResourceFor_outside_resource() {
    File file = new File("/does/not/exist.dart");
    Project project = manager.getProject(projectContainer);
    Source source = project.getDefaultContext().getSourceFactory().forFile(file);
    assertNull(manager.getResourceFor(source));
  }

  public void test_getSdk() throws Exception {
    final DartSdk sdk = manager.getSdk();
    assertNotNull(sdk);
    assertSame(expectedSdk, sdk);
  }

  public void test_newSearchEngine() throws Exception {
    assertNotNull(manager.newSearchEngine());
  }

  @Override
  protected void setUp() throws Exception {
    rootContainer = new MockWorkspaceRoot();
    projectContainer = TestProjects.newPubProject3(rootContainer);
    rootContainer.add(projectContainer);
    expectedSdk = mock(DartSdk.class);
    manager = new ProjectManagerImpl(rootContainer, expectedSdk, ignoreManager);
  }
}
