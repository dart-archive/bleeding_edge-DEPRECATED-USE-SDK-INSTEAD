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
package com.google.dart.tools.core.generator;

import com.google.dart.tools.core.DartCore;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Common behavior for generating files within projects
 */
public abstract class DartFileGenerator extends DartElementGenerator {

  protected static boolean containsWhitespace(String str) {
    for (int i = 0; i < str.length(); i++) {
      if (Character.isWhitespace(str.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Workspace root
   */
  IWorkspaceRoot root = DartCore.create(ResourcesPlugin.getWorkspace().getRoot()).getWorkspace().getRoot();

  /**
   * If <code>true</code> then the container must exist for the {@link #validateContainer()} method
   * to return <code>true</code>.
   */
  private final boolean containerMustExist;

  /**
   * The path to the Container within which the library is created
   */
  private IPath containerPath;

  /**
   * Construct a new instance.
   * 
   * @param containerMustExist If <code>true</code> then the container must exist for the
   *          {@link #validateContainer()} method to return <code>true</code>.
   */
  public DartFileGenerator(boolean containerMustExist) {
    this.containerMustExist = containerMustExist;
  }

  /**
   * Answer the container within which the File is created
   * 
   * @return the container
   */
  public IContainer getContainer() {
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    return (IContainer) DartCore.create(root).getWorkspace().getRoot().findMember(containerPath);
  }

  /**
   * Answer the containerPath within which the File is created
   * 
   * @return the String corresponding to the path of the chosen container
   */
  public IPath getContainerPath() {
    return containerPath;
  }

  /**
   * Set the container within which the library is created
   * 
   * @param container the container
   */
  public void setContainer(IContainer container) {
    if (container != null) {
      setContainerPath(container.getFullPath());
    } else {
      setContainerPath(""); //$NON-NLS-1$
    }
  }

  public void setContainerPath(IPath path) {
    this.containerPath = path;
  }

  /**
   * Set the container within which the library is created
   * 
   * @param container the container
   */
  public void setContainerPath(String containerPath) {
    setContainerPath(new Path(containerPath));
  }

  /**
   * Validate the settings to determine if the operation can be performed.
   * 
   * @return {@link Status#OK_STATUS} if the operation can be performed, or a status indicating an
   *         error or warning.
   */
  public abstract IStatus validate();

  /**
   * Create the specified folder and its parent folders if it does not already exist.
   * 
   * @param path the folder location
   * @param monitor a progress monitor or <code>null</code>
   */
  protected void createFolderIfNecessary(IPath path, IProgressMonitor monitor) throws CoreException {
    if (path == null || path.segmentCount() == 0) {
      return;
    }
    if (path.segmentCount() == 1) {
      createProjectIfNecessary(path.lastSegment(), monitor);
      return;
    }
    IFolder folder = workspace.getRoot().getFolder(path);
    if (folder.exists()) {
      return;
    }
    createFolderIfNecessary(path.removeLastSegments(1), monitor);
    folder.create(false, true, monitor);
  }

  /**
   * Create the specified project if it does not already exist.
   * 
   * @param projName the name of the project (not <code>null</code>)
   * @param monitor a progress monitor or <code>null</code>
   */
  protected void createProjectIfNecessary(String projName, IProgressMonitor monitor) {
    IProject project = workspace.getRoot().getProject(projName);
    if (project.exists()) {
      return;
    }
    IProjectDescription description = workspace.newProjectDescription(getName());
    description.setNatureIds(new String[] {DartCore.DART_PROJECT_NATURE});
    try {
      project.create(description, monitor);
    } catch (CoreException e) {
      e.printStackTrace();
    }
    try {
      project.open(monitor);
    } catch (CoreException e) {
      e.printStackTrace();
    }
    // TODO (danrubel/mmay): add Dart nature to project
  }

  /**
   * Generate the new file from the content in the specified file and performing the specified
   * substitutions. Anyplace in the raw file content where %key% appears will be replaced by a value
   * from the substitutions map. All instances of %% will be replaced by %.
   * 
   * @param contentPath the path to the file to be read relative to the class
   * @param file The file into which the content will be written
   * @param substitutions a mapping of keys that may appear in the raw content to values that should
   *          be substituted.
   * @param monitor the progress monitor (not <code>null</code>)
   */
  protected void execute(final String contentPath, final IFile file,
      final HashMap<String, String> substitutions, IProgressMonitor monitor) throws CoreException {

    workspace.run(new IWorkspaceRunnable() {

      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        monitor.beginTask("", 2); //$NON-NLS-1$
        String source;
        try {
          source = readExpectedContent(contentPath, substitutions);
        } catch (IOException e) {
          throw new CoreException(new Status(
              IStatus.ERROR,
              DartCore.PLUGIN_ID,
              "Failed to generate source",
              e));
        }
        if (!file.getParent().exists()) {
          IPath path = file.getFullPath().removeLastSegments(1);
          IFolder folder = workspace.getRoot().getFolder(path);
          folder.create(false, true, new SubProgressMonitor(monitor, 1));
        }
        InputStream stream = new ByteArrayInputStream(source.getBytes());
        file.create(stream, false, new SubProgressMonitor(monitor, 1));
        monitor.done();
      }

    }, monitor);
  }

  /**
   * Read content from the specified file while performing the specified text substitutions.
   * Anyplace in the raw file content where %key% appears will be replaced by a value from the
   * substitutions map. All instances of %% will be replaced by %.
   * 
   * @param fileName the path to the file to be read relative to the class
   * @param substitutions a mapping of keys that may appear in the raw content to values that should
   *          be substituted.
   * @return the file content after substitution has been performed
   */
  protected String readExpectedContent(String fileName, Map<String, String> substitutions)
      throws IOException {

    // Read content from the specified file

    InputStream stream = getClass().getResourceAsStream(fileName);
    StringBuilder result = new StringBuilder(2000);
    try {
      InputStreamReader reader = new InputStreamReader(stream);
      while (true) {
        int ch = reader.read();
        if (ch == -1) {
          break;
        }
        if (ch != '%') {
          result.append((char) ch);
          continue;
        }

        // If % is detected, the extract the key and perform a substitution

        StringBuilder key = new StringBuilder(20);
        while (true) {
          ch = reader.read();
          if (ch == -1) {
            throw new RuntimeException("Expected '%' but found EOF in " + fileName);
          }
          if (ch == '%') {
            break;
          }
          key.append((char) ch);
        }

        // If %% is detected, then substitute %
        // Otherwise lookup the value in the substitutions map

        if (key.length() == 0) {
          result.append("%"); //$NON-NLS-1$
        } else {
          String value = substitutions.get(key.toString());
          if (value == null) {
            throw new RuntimeException("Failed to find value for key " + key + " in " + fileName);
          }
          result.append(value);
        }
      }
    } finally {
      stream.close();
    }
    return result.toString();
  }

  /**
   * Validate the container for the operation
   * 
   * @return {@link Status#OK_STATUS} if the container is valid, or a status indicating an error or
   *         warning.
   */
  protected IStatus validateContainer() {
    if (containerPath == null) {
      return error(GeneratorMessages.DartFileGenerator_selectContainer);
    }
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IContainer container = getContainer();
    if (containerMustExist && (container == null || !container.exists())) {
      return error(GeneratorMessages.DartFileGenerator_containerDoesNotExist);
    } else {
      String projectName = getContainerPath().segment(0);
      String folderName = getContainerPath().removeFirstSegments(1).toOSString();
      if (root.findMember(getContainerPath()) != null) {
        return Status.OK_STATUS;
      }
      if (root.findMember(new Path("/" + projectName)) != null) { //$NON-NLS-1$
        return new Status(IStatus.WARNING, DartCore.PLUGIN_ID, MessageFormat.format(
            GeneratorMessages.DartFileGenerator_folderWillBeCreated,
            new Object[] {folderName, projectName}));
      } else if (!folderName.equals("/")) { //$NON-NLS-1$
        return new Status(IStatus.WARNING, DartCore.PLUGIN_ID, MessageFormat.format(
            GeneratorMessages.DartFileGenerator_projectFolderWillBeCreated,
            new Object[] {folderName, projectName}));
      } else {
        return new Status(IStatus.WARNING, DartCore.PLUGIN_ID, MessageFormat.format(
            GeneratorMessages.DartFileGenerator_projectWillBeCreated,
            new Object[] {projectName}));
      }
    }
  }
}
