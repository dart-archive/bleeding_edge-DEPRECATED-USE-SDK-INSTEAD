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
import com.google.dart.tools.core.internal.model.DartProjectNature;
import com.google.dart.tools.core.internal.util.Extensions;
import com.google.dart.tools.core.utilities.resource.IProjectUtilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

/**
 * Instances of <code>DartProjectGenerator</code> are used to create a new Dart project after
 * validating the name of the new project.
 */
public class DartProjectGenerator extends DartElementGenerator {

  public final static String DEFAULT_DART_PROJECT_NAME = "HelloWorld";

  /**
   * Create and open the project
   * 
   * @param monitor the monitor to which activity is reported
   */
  public void execute(IProgressMonitor monitor) throws CoreException {
    if (!validate().isOK()) {
      throw new IllegalStateException(validate().getMessage());
    }
    workspace.run(new IWorkspaceRunnable() {

      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        IProjectDescription description = workspace.newProjectDescription(getName());
        description.setNatureIds(new String[] {DartCore.DART_PROJECT_NATURE});
        IProject project = getProject();

        monitor.beginTask("", 300); //$NON-NLS-1$
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
    }, monitor);
  }

  /**
   * Answer the project
   * 
   * @return the project or <code>null</code> if the name is not a valid project name
   */
  public IProject getProject() {
    String name = getName();
    if (!workspace.validateName(name, IResource.PROJECT).isOK()) {
      return null;
    }
    return workspace.getRoot().getProject(name);
  }

  /**
   * Answer the suggested library name based upon the current project name, by stripping off
   * characters up to and including the last dot '.' if one exists.
   * 
   * @param isApplication <code>true</code> if the name is for a Dart application file or
   *          <code>false</code> if the name is for a Dart library file
   * @return the suggested library name (not <code>null</code>)
   */
  public String getSuggestedLibraryPath(boolean isApplication) {
    String libName = stripFileExtension(getName());
    if (libName.length() == 0) {
      return libName;
    }
    libName += Extensions.DOT_DART;
    return libName;
  }

  /**
   * Returns a unique name starting with the passed <code>prefix</code>. For instance, if
   * <code>"ProjectName"</code> is passed to this method, <code>"ProjectName"</code> is returned,
   * unless the name <code>"ProjectName"</code> already exists in this workspace. If this is the
   * case, <code>"ProjectName1"</code> is returned unless <code>"ProjectName1"</code> is also in the
   * workspace. This loop continues until unique name can be generated.
   * <p>
   * If an invalid <code>prefix</code> is passed, meaning a non-empty String which also passes the
   * test <code>workspace.validateName(getName(), IResource.PROJECT).isOK()</code>, then the value
   * from {@link #DEFAULT_DART_PROJECT_NAME} is used.
   * 
   * @param prefix the prefix to a unique project name in this workspace
   * @return a unique project name, starting with the passed <code>prefix</code>
   */
  public String getUniqueProjectName(String prefix) {
    if (prefix == null || prefix.length() == 0) {
      prefix = DEFAULT_DART_PROJECT_NAME;
    }
    IStatus status = workspace.validateName(prefix, IResource.PROJECT);
    if (!status.isOK()) {
      prefix = DEFAULT_DART_PROJECT_NAME;
    }
    String uniqueName = prefix;
    int i = 1;
    while (workspace.getRoot().getProject(uniqueName).exists()) {
      uniqueName = prefix + String.valueOf(i);
      i++;
    }
    return uniqueName;
  }

  /**
   * Validate the settings to determine if the operation can be performed. * @return
   * {@link Status#OK_STATUS} if the operation can be performed, or a status indicating an error or
   * warning.
   */
  public IStatus validate() {
    if (getName().length() == 0) {
      return error("Enter project name");
    }

    IStatus status = workspace.validateName(getName(), IResource.PROJECT);
    if (!status.isOK()) {
      return status;
    }

    IProject project = getProject();
    if (project.exists()) {
      return error("A project with this name already exists");
    }

    return Status.OK_STATUS;
  }
}
