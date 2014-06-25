/*
 * Copyright (c) 2014, the Dart project authors.
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

import com.google.dart.server.MockAnalysisServer;
import com.google.dart.tools.core.internal.builder.TestProjects;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;
import com.google.dart.tools.core.internal.model.MockIgnoreFile;
import com.google.dart.tools.core.mock.MockDelta;
import com.google.dart.tools.core.mock.MockFolder;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.mock.MockWorkspace;
import com.google.dart.tools.core.mock.MockWorkspaceRoot;

import junit.framework.TestCase;

import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;

import java.util.List;

public class DartProjectManagerTest extends TestCase {

  private class MockAnalysisServer_testRoots extends MockAnalysisServer {
    private int callCount = 0;
    private List<String> includedPaths;
    private List<String> excludedPaths;

    public void assertRoots(int expectedCallCount, MockProject[] expectedProjects,
        IResource[] expectedExcludes) {
      assertEquals(expectedCallCount, callCount);
      if (callCount > 0) {
        assertEquals(expectedProjects.length, includedPaths.size());
        for (MockProject proj : expectedProjects) {
          assertTrue(includedPaths.contains(proj.getLocation().toOSString()));
        }
        assertEquals(expectedExcludes.length, excludedPaths.size());
        for (IResource res : expectedExcludes) {
          assertTrue(excludedPaths.contains(res.getLocation().toOSString()));
        }
      }
    }

    @Override
    public void setAnalysisRoots(List<String> includedPaths, List<String> excludedPaths) {
      this.includedPaths = includedPaths;
      this.excludedPaths = excludedPaths;
      callCount++;
    }
  }

  private MockWorkspace workspace;
  private MockWorkspaceRoot root;
  private DartIgnoreManager ignoreManager;
  private MockProject dartProj;
  private MockAnalysisServer_testRoots server;
  private DartProjectManager manager;

  public void test_ignoreManagerChanges() throws Exception {
    manager.start();
    MockFolder libFolder = dartProj.getMockFolder("myapp/lib");
    ignoreManager.addToIgnores(libFolder);
    server.assertRoots(2, new MockProject[] {dartProj}, new IResource[] {libFolder});
    ignoreManager.removeFromIgnores(libFolder);
    server.assertRoots(3, new MockProject[] {dartProj}, new IResource[] {});
  }

  public void test_projectChanges() throws Exception {
    manager.start();
    addJavaProject();
    server.assertRoots(2, new MockProject[] {dartProj}, new IResource[] {});
    MockProject dartProj2 = addDartProject();
    server.assertRoots(3, new MockProject[] {dartProj, dartProj2}, new IResource[] {});
    MockProject proj = dartProj;
    deleteProject(proj);
    server.assertRoots(4, new MockProject[] {dartProj2}, new IResource[] {});
  }

  public void test_setAnalysisRoots() throws Exception {
    MockFolder libFolder = dartProj.getMockFolder("myapp/lib");
    ignoreManager.addToIgnores(libFolder);
    addJavaProject();
    server.assertRoots(0, new MockProject[] {}, new IResource[] {});
    manager.setAnalysisRoots();
    server.assertRoots(1, new MockProject[] {dartProj}, new IResource[] {libFolder});
  }

  public void test_start() throws Exception {
    server.assertRoots(0, new MockProject[] {}, new IResource[] {});
    manager.start();
    server.assertRoots(1, new MockProject[] {dartProj}, new IResource[] {});
  }

  @Override
  protected void setUp() throws Exception {
    workspace = new MockWorkspace();
    root = workspace.getRoot();
    dartProj = TestProjects.newPubProject3(root);
    server = new MockAnalysisServer_testRoots();
    ignoreManager = new DartIgnoreManager(new MockIgnoreFile());
    manager = new DartProjectManager(root, server, ignoreManager);
  }

  private MockProject addDartProject() {
    MockProject proj = root.addProject("new_Dart_Project");
    MockDelta delta = new MockDelta(root);
    delta.add(proj, IResourceDelta.ADDED);
    workspace.notifyResourceChange(delta, IResourceChangeEvent.POST_CHANGE);
    return proj;
  }

  private MockProject addJavaProject() throws Exception {
    MockProject proj = root.addProject("JavaProject");
    IProjectDescription description = proj.getDescription();
    description.setNatureIds(new String[] {"java.nature.id"});
    proj.setDescription(description, null);
    MockDelta delta = new MockDelta(root);
    delta.add(proj, IResourceDelta.ADDED);
    workspace.notifyResourceChange(delta, IResourceChangeEvent.POST_CHANGE);
    return proj;
  }

  private void deleteProject(MockProject proj) {
    root.remove(proj.getFullPath());
    MockDelta delta = new MockDelta(root);
    delta.add(proj, IResourceDelta.REMOVED);
    workspace.notifyResourceChange(delta, IResourceChangeEvent.POST_CHANGE);
  }
}
