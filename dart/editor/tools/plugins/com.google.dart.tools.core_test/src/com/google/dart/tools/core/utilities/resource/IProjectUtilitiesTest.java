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

import com.google.dart.tools.core.mock.MockFile;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.test.util.FileOperation;
import com.google.dart.tools.core.test.util.TestUtilities;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class IProjectUtilitiesTest extends TestCase {
  public void test_IProjectUtilities_addLinkToProject_duplicate() throws CoreException {
    MockProject project = new MockProject("project") {
      @Override
      public IFile getFile(String fileName) {
        return new MockFile(this, fileName) {
          private boolean linkCreated = false;

          @Override
          public void createLink(IPath localLocation, int updateFlags, IProgressMonitor monitor)
              throws CoreException {
            linkCreated = true;
          }

          @Override
          public boolean exists() {
            String name = getName();
            if (!containsDigit(name)) {
              return true;
            }
            return linkCreated;
          }

          private boolean containsDigit(String string) {
            int length = string.length();
            for (int i = 0; i < length; i++) {
              if (Character.isDigit(string.charAt(i))) {
                return true;
              }
            }
            return false;
          }
        };
      }
    };
    IFile newFile = IProjectUtilities.addLinkToProject(project, new File("foo.dart"), null);
    assertNotNull(newFile);
    assertEquals("foo2.dart", newFile.getName());
    assertTrue(newFile.exists());
  }

  public void test_IProjectUtilities_addLinkToProject_noDuplicate() throws CoreException {
    MockProject project = new MockProject("project") {
      @Override
      public IFile getFile(String fileName) {
        return new MockFile(this, fileName) {
          private boolean linkCreated = false;

          @Override
          public void createLink(IPath localLocation, int updateFlags, IProgressMonitor monitor)
              throws CoreException {
            linkCreated = true;
          }

          @Override
          public boolean exists() {
            return linkCreated;
          }
        };
      }
    };
    String fileName = "foo.dart";
    IFile newFile = IProjectUtilities.addLinkToProject(project, new File(fileName), null);
    assertNotNull(newFile);
    assertEquals(fileName, newFile.getName());
    assertTrue(newFile.exists());
  }

  public void test_IProjectUtilities_createOrOpenProject_directory() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDirectory) throws Exception {
        File projectDir = createProjectDir(tempDirectory);
        createDartFile(projectDir);
        IResource result = IProjectUtilities.createOrOpenProject(
            projectDir,
            new NullProgressMonitor());
        assertTrue(result instanceof IProject);
        IProject project = (IProject) result;
        TestUtilities.deleteProject(project);
      }
    });
  }

  public void test_IProjectUtilities_createOrOpenProject_file() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDirectory) throws Exception {
        File projectDir = createProjectDir(tempDirectory);
        File dartFile = createDartFile(projectDir);
        IResource result = IProjectUtilities.createOrOpenProject(
            dartFile,
            new NullProgressMonitor());
        assertTrue(result instanceof IFile);
        IFile file = (IFile) result;
        file.getProject().delete(true, new NullProgressMonitor());
      }
    });
  }

  private File createDartFile(File projectDir) throws IOException {
    File dartFile = new File(projectDir, "test.dart");
    PrintWriter writer = null;
    try {
      writer = new PrintWriter(new FileWriter(dartFile));
      writer.println("class Test {");
      writer.println("}");
      writer.flush();
    } finally {
      if (writer != null) {
        writer.close();
      }
    }
    return dartFile;
  }

  private File createProjectDir(File tempDirectory) {
    File projectDir = new File(tempDirectory, "projectDir");
    projectDir.mkdirs();
    return projectDir;
  }
}
