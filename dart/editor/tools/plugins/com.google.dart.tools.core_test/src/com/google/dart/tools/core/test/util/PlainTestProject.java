/*
 * Copyright (c) 2013, the Dart project authors.
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
import com.google.dart.engine.ast.CompilationUnit;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.concurrent.CountDownLatch;

/**
 * Helper for creating, manipulating and disposing temporary {@link IProject}s. This creates plain
 * IProjects, not Dart natured projects. Creating plain Eclipse projects is useful because it does
 * not cause the Dart builder / pub / build.dart to run.
 */
public class PlainTestProject {

  private final IProject project;

  /**
   * Creates new {@link IProject} with name "Test".
   */
  public PlainTestProject() throws Exception {
    this("Test");
  }

  /**
   * Creates new {@link IProject} with given name.
   */
  public PlainTestProject(final String projectName) throws Exception {
    final IWorkspace workspace = ResourcesPlugin.getWorkspace();

    project = workspace.getRoot().getProject(projectName);

    workspace.run(new IWorkspaceRunnable() {
      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        // delete project
        if (project.exists()) {
          TestUtilities.deleteProject(project);
        }

        ProgressMonitorLatch latch = new ProgressMonitorLatch();

        try {
          IProjectDescription description = workspace.newProjectDescription(projectName);
          project.create(description, latch);
          latch.await();

          latch = new ProgressMonitorLatch();
          project.open(latch);
          latch.await();
        } finally {
          latch.setCanceled(true);
        }
      }
    }, null);
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
    final IWorkspace workspace = ResourcesPlugin.getWorkspace();

    final CountDownLatch latch = new CountDownLatch(1);

    try {
      workspace.run(new IWorkspaceRunnable() {
        @Override
        public void run(IProgressMonitor monitor) throws CoreException {
          TestUtilities.deleteProject(project);
          latch.countDown();
        }
      }, null);
    } finally {
      latch.countDown();
    }

    latch.await();
  }

  /**
   * @return the {@link CompilationUnit} on given path, not <code>null</code>, but may be not
   *         existing.
   */
  public IFile getFile(String path) {
    return project.getFile(new Path(path));
  }

  /**
   * @return the {@link String} content of the {@link IFile} with given path.
   */
  public String getFileString(String path) throws Exception {
    IFile file = getFile(path);
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
    // notify AnalysisServer

    // done
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

}
