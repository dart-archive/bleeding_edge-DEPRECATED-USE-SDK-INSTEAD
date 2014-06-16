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

package com.google.dart.tools.debug.core.dartium;

import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.containers.AbstractSourceContainer;

/**
 * A source lookup container while launch/debug from url
 */
public class DartiumUrlScriptSourceContainer extends AbstractSourceContainer {

  public static final String TYPE_ID = DebugPlugin.getUniqueIdentifier()
      + ".containerType.workspace"; //$NON-NLS-1$

  @Override
  public Object[] findSourceElements(String name) throws CoreException {

    if (DartiumDebugTarget.getActiveTarget() == null) {
      return EMPTY;
    }

    ILaunch launch = DartiumDebugTarget.getActiveTarget().getLaunch();
    DartLaunchConfigWrapper launchConfig = new DartLaunchConfigWrapper(
        launch.getLaunchConfiguration());
    IProject project = launchConfig.getProject();

    if (project == null) {
      return EMPTY;
    }

    Object[] resources = findResource(name, project);
    if (resources != EMPTY) {
      return resources;
    }

    // check in a connected project, for e.g a project with the generated files
    // project would likely be named "projectName-gen" or "projectName-1" etc 
    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    for (IProject project2 : projects) {
      String projectName = project2.getName();
      if (projectName.lastIndexOf("-") != -1) {
        projectName = projectName.substring(0, projectName.indexOf("-"));
      }
      if (projectName.equals(project.getName()) && project2 != project) {
        resources = findResource(name, project2);
        if (resources != EMPTY) {
          return resources;
        }
      }
    }

    return EMPTY;
  }

  @Override
  public String getName() {
    return "Remote Url Scripts";
  }

  @Override
  public ISourceContainerType getType() {
    return getSourceContainerType(TYPE_ID);
  }

  private Object[] findResource(String name, IProject project) {
    Path path = new Path(name);
    for (int i = path.segmentCount() - 1; i >= 0; i--) {
      IResource resource = project.findMember(path.removeFirstSegments(i));
      if (resource != null) {
        return new Object[] {resource};
      }
    }
    return EMPTY;
  }

}
