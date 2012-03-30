/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.model;

import com.google.dart.tools.core.DartCore;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * Instances of the class <code>DartProjectNature</code> implement the nature of a Dart project.
 */
public class DartProjectNature implements IProjectNature {
  /**
   * Return <code>true</code> if the given project has the Dart project nature, or
   * <code>false</code> if it either doesn't have the Dart nature or if we cannot determine whether
   * or not it has the Dart nature. This is a convenience method that handles
   * <code>CoreException</code>s by returning <code>false</code>.
   * 
   * @param project the project being tested
   * @return <code>true</code> if the given project has the Dart project nature
   */
  public static boolean hasDartNature(IProject project) {
    try {
      if (project == null) {
        return false;
      }

      return project.hasNature(DartCore.DART_PROJECT_NATURE);
    } catch (CoreException exception) {
      return false;
    }
  }

  /**
   * Return <code>true</code> if the given resource's project has the Dart project nature, or
   * <code>false</code> if it either doesn't have the Dart nature or if we cannot determine whether
   * or not it has the Dart nature.
   * 
   * @param resource the resource being tested
   * @return <code>true</code> if the given resource's project has the Dart nature
   */
  public static boolean hasDartNature(IResource resource) {
    if (resource == null) {
      return false;
    }

    return hasDartNature(resource.getProject());
  }

  /**
   * The project being represented by this object.
   */
  private IProject project;

  /**
   * Initialize a newly created project nature to represent the nature for a yet unspecified
   * project.
   */
  public DartProjectNature() {
    super();
  }

  @Override
  public void configure() throws CoreException {
    addBuilderToBuildSpec();
  }

  @Override
  public void deconfigure() throws CoreException {
    removeBuilderFromBuildSpec();
  }

  @Override
  public IProject getProject() {
    return project;
  }

  @Override
  public void setProject(IProject project) {
    this.project = project;
  }

  /**
   * Add the Dart builder to the build specification of the underlying project.
   * 
   * @throws CoreException if the builder could not be added for some reason
   */
  private void addBuilderToBuildSpec() throws CoreException {
    IProjectDescription description = project.getDescription();
    int index = getDartCommandIndex(description.getBuildSpec());
    if (index < 0) {
      ICommand command = description.newCommand();
      command.setBuilderName(DartCore.DART_BUILDER_ID);
      setDartCommand(description, command);
    }
  }

  private int getDartCommandIndex(ICommand[] buildSpec) {
    for (int i = 0; i < buildSpec.length; i++) {
      if (buildSpec[i].getBuilderName().equals(DartCore.DART_BUILDER_ID)) {
        return i;
      }
    }
    return -1;
  }

  private void removeBuilderFromBuildSpec() throws CoreException {
    IProjectDescription description = project.getDescription();
    ICommand[] oldCommands = description.getBuildSpec();
    int length = oldCommands.length;
    for (int i = 0; i < length; i++) {
      if (oldCommands[i].getBuilderName().equals(DartCore.DART_BUILDER_ID)) {
        ICommand[] newCommands = new ICommand[length - 1];
        System.arraycopy(oldCommands, 0, newCommands, 0, i);
        System.arraycopy(oldCommands, i + 1, newCommands, i, length - i - 1);
        description.setBuildSpec(newCommands);
        project.setDescription(description, null);
        return;
      }
    }
  }

  private void setDartCommand(IProjectDescription description, ICommand command)
      throws CoreException {
    ICommand[] oldCommands = description.getBuildSpec();
    int length = oldCommands.length;
    ICommand[] newCommands = new ICommand[length + 1];
    System.arraycopy(oldCommands, 0, newCommands, 0, length);
    newCommands[length] = command;
    description.setBuildSpec(newCommands);
    project.setDescription(description, null);
  }
}
