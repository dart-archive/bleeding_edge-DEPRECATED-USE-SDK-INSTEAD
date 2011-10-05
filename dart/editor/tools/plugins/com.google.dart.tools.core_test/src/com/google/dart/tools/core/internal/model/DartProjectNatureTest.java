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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.mock.MockProject;

import junit.framework.TestCase;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class DartProjectNatureTest extends TestCase {
  public void test_DartProjectNature_configure_once() throws CoreException {
    IProject project = new MockProject();
    DartProjectNature nature = new DartProjectNature();
    nature.setProject(project);
    nature.configure();
    assertHasDartBuilder(project);
  }

  public void test_DartProjectNature_configure_twice() throws CoreException {
    IProject project = new MockProject();
    DartProjectNature nature = new DartProjectNature();
    nature.setProject(project);
    nature.configure();
    nature.configure();
    assertHasDartBuilder(project);
  }

  public void test_DartProjectNature_deconfigure_with() throws CoreException {
    IProject project = new MockProject();
    DartProjectNature nature = new DartProjectNature();
    nature.setProject(project);
    nature.configure();
    nature.deconfigure();
    assertDoesNotHaveDartBuilder(project);
  }

  public void test_DartProjectNature_deconfigure_without() throws CoreException {
    IProject project = new MockProject();
    DartProjectNature nature = new DartProjectNature();
    nature.setProject(project);
    nature.deconfigure();
    assertDoesNotHaveDartBuilder(project);
  }

  public void test_DartProjectNature_getProject() {
    DartProjectNature nature = new DartProjectNature();
    assertEquals(null, nature.getProject());
  }

  public void test_DartProjectNature_hasProjectNature_exception() {
    IProject project = new MockProject() {
      @Override
      public boolean hasNature(String natureId) throws CoreException {
        throw new CoreException(new Status(IStatus.ERROR, "com.google.dart.tools.core_test", ""));
      }
    };
    assertFalse(DartProjectNature.hasDartNature(project));
  }

  public void test_DartProjectNature_hasProjectNature_false() {
    IProject project = new MockProject();
    assertFalse(DartProjectNature.hasDartNature(project));
  }

  public void test_DartProjectNature_hasProjectNature_true() {
    IProject project = new MockProject() {
      @Override
      public boolean hasNature(String natureId) {
        return natureId.equals(DartCore.DART_PROJECT_NATURE);
      }
    };
    assertTrue(DartProjectNature.hasDartNature(project));
  }

  public void test_DartProjectNature_setProject() {
    DartProjectNature nature = new DartProjectNature();
    IProject project = new MockProject();
    nature.setProject(project);
    assertEquals(project, nature.getProject());
  }

  private void assertDoesNotHaveDartBuilder(IProject project) throws CoreException {
    ICommand[] buildSpec = project.getDescription().getBuildSpec();
    assertNotNull(buildSpec);
    for (ICommand command : buildSpec) {
      if (DartCore.DART_BUILDER_ID.equals(command.getBuilderName())) {
        fail("Build spec found for Dart builder");
      }
    }
  }

  private void assertHasDartBuilder(IProject project) throws CoreException {
    ICommand[] buildSpec = project.getDescription().getBuildSpec();
    assertNotNull(buildSpec);
    for (ICommand command : buildSpec) {
      if (DartCore.DART_BUILDER_ID.equals(command.getBuilderName())) {
        return;
      }
    }
    fail("No build spec found for Dart builder");
  }
}
