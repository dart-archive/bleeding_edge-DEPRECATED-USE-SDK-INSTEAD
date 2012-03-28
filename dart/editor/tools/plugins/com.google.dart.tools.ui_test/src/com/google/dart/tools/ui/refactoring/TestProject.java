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

package com.google.dart.tools.ui.refactoring;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartProject;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Helper for creating, manipulating and disposing temporary {@link DartProject}.
 */
public class TestProject {
  private final IProject project;

  /**
   * Creates new {@link DartProject} with name "Test".
   */
  public TestProject() throws Exception {
    this("Test");
  }

  /**
   * Creates new {@link DartProject} with given name.
   */
  public TestProject(String projectName) throws Exception {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IWorkspaceRoot root = workspace.getRoot();
    project = root.getProject(projectName);
    // delete project
    if (project.exists()) {
      project.delete(true, true, null);
    }
    // create project
    {
      project.create(null);
      project.open(null);
    }
    // set nature
    {
      IProjectDescription description = workspace.newProjectDescription(projectName);
      description.setNatureIds(new String[] {DartCore.DART_PROJECT_NATURE});
      project.setDescription(description, null);
    }
  }

  /**
   * Disposes allocated resources and deletes project.
   */
  public void dispose() throws Exception {
    project.delete(true, true, null);
  }

  /**
   * Creates or updates {@link IFile} with content of the given {@link InputStream}.
   */
  public IFile setFileContent(String path, InputStream inputStream) throws Exception {
    IFile file = project.getFile(new Path(path));
    if (file.exists()) {
      file.setContents(inputStream, true, false, null);
    } else {
      file.create(inputStream, true, null);
    }
    return file;
  }

  /**
   * Creates or updates with {@link String} content of the {@link IFile}.
   */
  public IFile setFileContent(String path, String content) throws Exception {
    InputStream inputStream = new ByteArrayInputStream(content.getBytes());
    return setFileContent(path, inputStream);
  }

  /**
   * Creates or updates {@link CompilationUnit} at given path.
   */
  public CompilationUnit setUnitContent(String path, String content) throws Exception {
    IFile file = setFileContent(path, content);
    return (CompilationUnit) DartCore.create(file);
  }
}
