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
import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.AnalysisServer;
import com.google.dart.tools.core.analysis.AnalysisTestUtilities;
import com.google.dart.tools.core.index.NotifyCallback;
import com.google.dart.tools.core.internal.index.impl.InMemoryIndex;
import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartProject;

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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.concurrent.CountDownLatch;

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
    AnalysisTestUtilities.waitForAnalysis();
    try {
      final CountDownLatch latch = new CountDownLatch(1);
      InMemoryIndex.getInstance().notify(new NotifyCallback() {
        @Override
        public void done() {
          latch.countDown();
        }
      });
      latch.await();
    } catch (Throwable e) {
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
    // notify AnalysisServer
    {
      IPath location = project.getLocation();
      if (location != null) {
        AnalysisServer server = PackageLibraryManagerProvider.getDefaultAnalysisServer();
        server.discard(location.toFile());
      }
    }
    // we need to close, because in the other case DelteProcessor for some reason closes it,
    // but at the time when we create (!!!) new project
    try {
      if (project.exists()) {
        project.close(null);
      }
    } catch (Throwable e) {
    }
    // do dispose
    TestUtilities.deleteProject(project);
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
   * @return the {@link DartProject}.
   */
  public DartProject getDartProject() {
    return dartProject;
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
    // notify AnalysisServer
    {
      AnalysisServer server = PackageLibraryManagerProvider.getDefaultAnalysisServer();
      File javaFile = file.getLocation().toFile();
      server.scan(javaFile, 5000);
      server.changed(javaFile);
    }
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

  /**
   * Creates or updates {@link CompilationUnit} at given path.
   */
  public CompilationUnit setUnitContent(String path, String content) throws Exception {
    IFile file = setFileContent(path, content);
    return (CompilationUnit) DartCore.create(file);
  }
}
