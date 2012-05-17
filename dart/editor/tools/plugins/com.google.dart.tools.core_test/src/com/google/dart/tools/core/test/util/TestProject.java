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

package com.google.dart.tools.core.test.util;

import com.google.common.io.CharStreams;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartProject;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Helper for creating, manipulating and disposing temporary {@link DartProject}.
 */
public class TestProject {

  /**
   * Wait for auto-build notification to occur, that is for the auto-build to finish.
   */
  public static void waitForAutoBuild() {
    while (true) {
      try {
        IJobManager jobManager = Job.getJobManager();
        jobManager.wakeUp(ResourcesPlugin.FAMILY_AUTO_BUILD);
        jobManager.wakeUp(ResourcesPlugin.FAMILY_AUTO_BUILD);
        jobManager.wakeUp(ResourcesPlugin.FAMILY_AUTO_BUILD);
        jobManager.join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
        break;
      } catch (Throwable e) {
      }
    }
  }

  private final IProject project;

  private final DartProject dartProject;

  /**
   * Creates new {@link DartProject} with name "Test".
   */
  public TestProject() throws Exception {
    this("Test");
  }

  /**
   * Creates new {@link DartProject} with given name.
   */
  public TestProject(final String projectName) throws Exception {
    final IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IWorkspaceRoot root = workspace.getRoot();
    project = root.getProject(projectName);
    workspace.run(new IWorkspaceRunnable() {
      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        // delete project
        if (project.exists()) {
          TestUtilities.deleteProject(project);
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
    }, null);
    // remember DartProject
    dartProject = DartCore.create(project);
  }

  /**
   * Disposes allocated resources and deletes project.
   */
  public void dispose() throws Exception {
    TestUtilities.deleteProject(project);
  }

  /**
   * @return the {@link DartProject}.
   */
  public DartProject getDartProject() {
    return dartProject;
  }

  /**
   * @return the {@link String} content of the {@link IFile} with given path.
   */
  public String getFileString(String path) throws Exception {
    IFile file = project.getFile(new Path(path));
    Reader reader = new InputStreamReader(file.getContents(), file.getCharset());
    try {
      return CharStreams.toString(reader);
    } finally {
      reader.close();
    }
  }

  /**
   * @return the underlying {@link IProject}.
   */
  public IProject getProject() {
    return project;
  }

  /**
   * @return the {@link CompilationUnit} on given path.
   */
  public CompilationUnit getUnit(String path) throws Exception {
    IFile file = project.getFile(new Path(path));
    return (CompilationUnit) DartCore.create(file);
  }

  /**
   * Creates or updates {@link IFile} with content of the given {@link InputStream}.
   */
  public IFile setFileContent(String path, InputStream inputStream) throws Exception {
    final IFile file = project.getFile(new Path(path));
    final InputStream stream = inputStream;
    ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        if (file.exists()) {
          file.setContents(stream, true, false, null);
        } else {
          file.create(stream, true, null);
        }
      }
    }, null);
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
