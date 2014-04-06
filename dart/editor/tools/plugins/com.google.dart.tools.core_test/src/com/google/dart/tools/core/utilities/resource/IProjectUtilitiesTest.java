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
package com.google.dart.tools.core.utilities.resource;

import com.google.dart.tools.core.mock.MockWorkspaceRoot;

import junit.framework.TestCase;

import org.eclipse.core.resources.FileInfoMatcherDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class IProjectUtilitiesTest extends TestCase {

  public void test_configurePackagesFilter() throws Exception {
    IProject project = mock(IProject.class);
    IProjectUtilities.configurePackagesFilter(project);
    verify(project).createFilter(
        anyInt(),
        any(FileInfoMatcherDescription.class),
        anyInt(),
        any(IProgressMonitor.class));
  }

  public void test_newProjectDescription() throws Exception {
    MockWorkspaceRoot root = new MockWorkspaceRoot();
    IProjectDescription description = IProjectUtilities.newDartProjectDescription(root, "foo", null);
    assertNotNull(description);
    assertEquals("foo", description.getName());
    assertNull(description.getLocation());
  }

  @SuppressWarnings("deprecation")
  public void test_newProjectDescription_outsideWorkspace() throws Exception {
    MockWorkspaceRoot root = new MockWorkspaceRoot();
    IPath loc = new Path("/some/other/place");
    IProjectDescription description = IProjectUtilities.newDartProjectDescription(root, "foo", loc);
    assertNotNull(description);
    assertEquals("foo", description.getName());
    assertFalse(root.getLocation().isPrefixOf(description.getLocation()));
  }
}
