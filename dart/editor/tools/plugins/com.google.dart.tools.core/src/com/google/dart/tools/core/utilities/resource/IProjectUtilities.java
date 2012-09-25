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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.DartProjectNature;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.refresh.DartPackagesFolderMatcher;

import org.eclipse.core.resources.FileInfoMatcherDescription;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceFilterDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import java.io.File;
import java.net.URI;

/**
 * The class <code>IProjectUtilities</code> defines utility methods used to work with projects.
 */
public final class IProjectUtilities {
  /**
   * Add to the given project a file that is linked to the given file.
   * 
   * @param project the project to which the linked file should be added
   * @param file the file that the linked file should be linked with
   * @param monitor the progress monitor used to provide feedback to the user, or <code>null</code>
   *          if no feedback is desired
   * @return the file that is linked to the given file
   * @throws CoreException if the link could not be created for some reason
   */
  public static IFile addLinkToProject(IProject project, File file, IProgressMonitor monitor)
      throws CoreException {
    IFile newFile = computeLinkPoint(project, file.getName());
    newFile.createLink(new Path(file.getAbsolutePath()), 0, monitor);
    if (!newFile.exists()) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          DartCore.PLUGIN_ID,
          IStatus.ERROR,
          "Failed to create a link to " + file.getAbsolutePath() + " in " + project.getLocation(),
          null));
    }
    return newFile;
  }

  public static void configurePackagesFilter(IProject project) throws CoreException {
    FileInfoMatcherDescription matcher = new FileInfoMatcherDescription(
        DartPackagesFolderMatcher.MATCHER_ID,
        null);

    project.createFilter(IResourceFilterDescription.EXCLUDE_ALL
        | IResourceFilterDescription.FOLDERS | IResourceFilterDescription.FILES
        | IResourceFilterDescription.INHERITABLE, matcher, 0, new NullProgressMonitor());
  }

  /**
   * Create or open the project in the given directory (or the directory containing the given file
   * if the file is not a directory).
   * 
   * @param file the file or directory indicating which directory should be the root of the project
   * @param monitor the progress monitor used to provide feedback to the user, or <code>null</code>
   *          if no feedback is desired
   * @return the resource associated with the file that was passed in
   * @throws CoreException if the project cannot be opened or created
   */
  public static IResource createOrOpenProject(File file, IProgressMonitor monitor)
      throws CoreException {
    IResource[] existingResources = ResourceUtil.getResources(file);
    if (existingResources.length == 1) {
      return existingResources[0];
    } else if (existingResources.length > 1) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          DartCore.PLUGIN_ID,
          "Too many files representing " + file.getAbsolutePath()));
    }
    final File projectDirectory;
    if (file.isDirectory()) {
      projectDirectory = file;
    } else {
      projectDirectory = file.getParentFile();
    }
    final IWorkspace workspace = ResourcesPlugin.getWorkspace();
    workspace.run(new IWorkspaceRunnable() {
      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        String projectName = projectDirectory.getName();
        IProject project = getProject(workspace, projectName);
        IProjectDescription description = createProjectDescription(
            project,
            projectDirectory.toURI());

        monitor.beginTask("Create project " + projectName, 300); //$NON-NLS-1$
        project.create(description, new SubProgressMonitor(monitor, 100));
        if (monitor.isCanceled()) {
          throw new OperationCanceledException();
        }
        project.open(IResource.NONE, new SubProgressMonitor(monitor, 100));
        if (monitor.isCanceled()) {
          throw new OperationCanceledException();
        }

        DartProjectNature nature = new DartProjectNature();
        nature.setProject(project);
        nature.configure();

        IProjectUtilities.configurePackagesFilter(project);

        monitor.done();
      }
    },
        monitor);
    IResource[] newResources = ResourceUtil.getResources(file);
    if (newResources.length == 1) {
      return newResources[0];
    } else if (newResources.length == 0) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          DartCore.PLUGIN_ID,
          "No files representing " + file.getAbsolutePath()));
    }
    throw new CoreException(new Status(
        IStatus.ERROR,
        DartCore.PLUGIN_ID,
        "Too many files representing " + file.getAbsolutePath()));
  }

  /**
   * Return a file in the given project whose name is derived from the given base name that does not
   * already exist.
   * 
   * @param project the project containing the file that is returned
   * @param baseName the base of the name of the returned file
   * @return a file that can be used to link to the given file
   */
  private static IFile computeLinkPoint(IProject project, String fileName) {
    IFile newFile = project.getFile(fileName);
    if (!newFile.exists() && !hasSimilarChild(project, fileName)) {
      return newFile;
    }
    int dotIndex = fileName.lastIndexOf('.');
    String extension;
    String baseName;
    if (dotIndex < 0) {
      extension = "";
      baseName = fileName;
    } else {
      extension = fileName.substring(dotIndex);
      baseName = fileName.substring(0, dotIndex);
    }
    int index = 2;
    while (newFile.exists() || hasSimilarChild(project, newFile.getName())) {
      newFile = project.getFile(baseName + index++ + extension);
    }
    return newFile;
  }

  /**
   * Return a project description for the
   * 
   * @param project
   * @param location
   * @return
   */
  private static IProjectDescription createProjectDescription(IProject project, URI location) {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IProjectDescription description = workspace.newProjectDescription(project.getName());
    description.setLocationURI(location);
    description.setNatureIds(new String[] {DartCore.DART_PROJECT_NATURE});
    ICommand command = description.newCommand();
    command.setBuilderName(DartCore.DART_BUILDER_ID);
    description.setBuildSpec(new ICommand[] {command});
    return description;
  }

  /**
   * Return the project being created, or <code>null</code> if the name is not a valid project name.
   * 
   * @return the project being created
   */
  private static IProject getProject(IWorkspace workspace, String name) {
    if (!workspace.validateName(name, IResource.PROJECT).isOK()) {
      return null;
    }
    return workspace.getRoot().getProject(name);
  }

  /**
   * Projects do not allow a link to be created if there is another resource (linked or not) whose
   * name is the same as the new name with only case differences. Return <code>true</code> if the
   * project has a member whose name is the same when case is ignored.
   * 
   * @param project the project containing the members
   * @param fileName the name being checked for
   * @return <code>true</code> if the project has a member whose name is the same when case is
   *         ignored
   * @throws
   */
  private static boolean hasSimilarChild(IProject project, String fileName) {
    try {
      IResource[] members = project.members();
      if (members == null) {
        return false;
      }
      for (IResource member : members) {
        if (member.getName().equalsIgnoreCase(fileName)) {
          return true;
        }
      }
    } catch (CoreException exception) {
      DartCore.logInformation(
          "Could not get members of project " + project.getLocation(),
          exception);
    }
    return false;
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private IProjectUtilities() {
    super();
  }
}
