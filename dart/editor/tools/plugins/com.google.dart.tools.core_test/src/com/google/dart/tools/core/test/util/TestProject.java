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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
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

  static int testCount = 0;

  /**
   * Creates new {@link DartProject} with name "Test".
   */
  public TestProject() throws Exception {
    this("test_project_" + (testCount++));
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
        // delete the project
        if (project.exists()) {
          TestUtilities.deleteProject(project);
        }

        // create the project
        ProgressMonitorLatch latch = new ProgressMonitorLatch();
        try {
          IProjectDescription description = workspace.newProjectDescription(projectName);
          description.setNatureIds(new String[] {DartCore.DART_PROJECT_NATURE});
          ICommand command = description.newCommand();
          command.setBuilderName(DartCore.DART_BUILDER_ID);
          description.setBuildSpec(new ICommand[] {command});
          project.create(description, latch);
          latch.await();

          latch = new ProgressMonitorLatch();
          project.open(latch);
          latch.await();
        } finally {
          latch.setCanceled(true);
        }
      }
    }, workspace.getRoot(), 0, null);
  }

  public IFolder createFolder(String path) throws Exception {
    String[] parts = StringUtils.split(path, "/");
    IContainer container = project;
    for (String part : parts) {
      IFolder folder = container.getFolder(new Path(part));
      if (!folder.exists()) {
        folder.create(true, true, null);
      }
      container = folder;
    }
    return (IFolder) container;
  }

  /**
   * Disposes allocated resources and deletes project.
   */
  public void dispose() throws Exception {
    try {
      if (project.exists()) {
        // Removed the close, this was causing test failures -test_DartModelImpl_getUnreferencedLibraries(),
        // test_HTMLFileImpl_getElementInfo
        // project.close(null);
      }
    } catch (Throwable e) {
    }

    // do dispose
    try {
      TestUtilities.deleteProject(project);
    } catch (CoreException ce) {
      // still could not delete
      if (project.exists()) {
        throw ce;
      }
    }
  }

  /**
   * @return the {@link CompilationUnit} on given path, not <code>null</code>, but may be not
   *         existing.
   */
  public IFile getFile(String path) {
    return project.getFile(new Path(path));
  }

  /**
   * @return the {@link String} content of the {@link IFile}.
   */
  public String getFileString(IFile file) throws Exception {
    Reader reader = new InputStreamReader(file.getContents(), file.getCharset());
    try {
      return CharStreams.toString(reader);
    } finally {
      reader.close();
    }
  }

  /**
   * @return the {@link String} content of the {@link IFile} with given path.
   */
  public String getFileString(String path) throws Exception {
    IFile file = getFile(path);
    return getFileString(file);
  }

  /**
   * @return the underlying {@link IProject}.
   */
  public IProject getProject() {
    return project;
  }

  /**
   * @return the {@link CompilationUnit} on given path, may be <code>null</code>.
   */
  public CompilationUnit getUnit(String path) throws Exception {
    IFile file = getFile(path);
    return (CompilationUnit) DartCore.create(file);
  }

  /**
   * Creates or updates {@link IFile} with content of the given {@link InputStream}.
   */
  public IFile setFileContent(String path, InputStream stream) throws Exception {
    IFile file = getFile(path);
    if (file.exists()) {
      file.setContents(stream, true, false, null);
    } else {
      file.create(stream, true, null);
      file.setCharset("UTF-8", null);
    }
    return file;
  }

  /**
   * Creates or updates with {@link String} content of the {@link IFile}.
   */
  public IFile setFileContent(String path, String content) throws Exception {
    byte[] bytes = content.getBytes("UTF-8");
    InputStream stream = new ByteArrayInputStream(bytes);
    return setFileContent(path, stream);
  }

  /**
   * Creates or updates {@link CompilationUnit} at given path.
   */
  public CompilationUnit setUnitContent(String path, String content) throws Exception {
    IFile file = setFileContent(path, content);
    CompilationUnit unit = (CompilationUnit) DartCore.create(file);

    return unit;
  }
}
