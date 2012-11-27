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
package com.google.dart.tools.ui.theme.preferences;

import com.google.common.io.CharStreams;
import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.AnalysisServer;
import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.deploy.Activator;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;

/**
 * Helper for creating, manipulating and disposing a temporary {@link DartProject}. This exists in
 * the workspace for a short time but the user should never see it.
 */
public class TemporaryProject {

  private static final String DEFAULT_NAME = "CodeColoringSample";

  static void deleteProject(IProject project) throws CoreException {
    final int MAX_FAILURES = 10;

    int failureCount = 0;

    while (true) {
      try {
        project.delete(true, true, null);
        return;
      } catch (CoreException ce) {
        failureCount++;

        if (failureCount >= MAX_FAILURES) {
          throw ce;
        }
        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();
      }
    }
  }

  private final IProject project;
  private final DartProject dartProject;

  /**
   * Creates new {@link DartProject} with default name.
   */
  public TemporaryProject() throws CoreException {
    this(DEFAULT_NAME);
  }

  /**
   * Creates new {@link DartProject} with given name.
   */
  public TemporaryProject(final String name) throws CoreException {
    final IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IWorkspaceRoot root = workspace.getRoot();
    final String projectName = findUniqueName(root, name);
    project = root.getProject(projectName);
    workspace.run(new IWorkspaceRunnable() {
      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        // delete project
        if (project.exists()) {
          deleteProject(project);
        }
        // create project
        project.create(null);
        project.open(null);
        // set description
        IProjectDescription description = createProjectDescription(project);
        project.setDescription(description, IResource.FORCE, null);
      }
    }, null);
    // remember DartProject
    dartProject = DartCore.create(project);
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
  public void dispose() throws CoreException {
    // TODO(messick): Do we need to notify AnalysisServer?
    {
      IPath location = project.getLocation();
      if (location != null) {
        AnalysisServer server = PackageLibraryManagerProvider.getDefaultAnalysisServer();
        server.discard(location.toFile());
      }
    }
    try {
      if (project.exists()) {
        project.close(null);
      }
    } catch (Throwable e) {
    }
    // do dispose
    deleteProject(project);
  }

  /**
   * @return the {@link DartProject}.
   */
  public DartProject getDartProject() {
    return dartProject;
  }

  /**
   * @return the {@link IFile} on given path, not <code>null</code>, but may not exist.
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
  public IFile setFileContent(String path, InputStream stream) throws CoreException {
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
  public IFile setFileContent(String path, String content) throws IOException, CoreException {
    byte[] bytes = content.getBytes("UTF-8");
    InputStream stream = new ByteArrayInputStream(bytes);
    return setFileContent(path, stream);
  }

  /**
   * Creates or updates {@link CompilationUnit} at given path.
   */
  public CompilationUnit setUnitContent(String path, String content) throws IOException,
      CoreException {
    IFile file = setFileContent(path, content);
    CompilationUnit unit = (CompilationUnit) DartCore.create(file);
    return unit;
  }

  private IProjectDescription createProjectDescription(IProject project) {
    URI location = null;
    try {
      File tempFile = File.createTempFile("DartEditor", null);
      File tempDir = tempFile.getParentFile();
      tempFile.delete();
      tempDir.mkdirs();
      location = tempDir.toURI();
    } catch (IOException ex) {
      Activator.logError(ex);
    }
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IProjectDescription description = workspace.newProjectDescription(project.getName());
    description.setLocationURI(location);
    description.setNatureIds(new String[] {DartCore.DART_PROJECT_NATURE});
    ICommand command = description.newCommand();
    command.setBuilderName(DartCore.DART_BUILDER_ID);
    description.setBuildSpec(new ICommand[] {command});
    return description;
  }

  private String findUniqueName(IWorkspaceRoot root, String initialName) {
    String name = initialName;
    int n = 0;
    while (true) {
      IProject p = root.getProject(name);
      if (!p.exists()) {
        return name;
      }
      n += 1;
      name = initialName + String.valueOf(n);
    }
  }
}
