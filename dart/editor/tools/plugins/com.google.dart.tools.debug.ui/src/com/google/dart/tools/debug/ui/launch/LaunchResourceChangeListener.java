/*
 * Copyright 2012 Dart project authors.
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

package com.google.dart.tools.debug.ui.launch;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.debug.ui.internal.util.LaunchUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;

import java.util.List;

/**
 * Resource change listener that looks for project delete /remove from editor events and deletes the
 * associated launch configurations
 */
public class LaunchResourceChangeListener implements IResourceChangeListener, IResourceVisitor {

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    try {
      event.getResource().accept(this);
    } catch (Exception e) {
      DartCore.logError("Failed to process resource changes for " + event.getResource(), e);
    }
  }

  @Override
  public boolean visit(IResource resource) throws CoreException {
    switch (resource.getType()) {
      case IResource.PROJECT:
        deleteLaunches((IProject) resource);
        return false;
    }
    return false;
  }

  private void deleteLaunches(IProject project) throws CoreException {
    List<ILaunchConfiguration> launchConfigs = LaunchUtils.getLaunchesFor(project);
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunch[] launches = manager.getLaunches();
    for (ILaunch launch : launches) {
      if (launchConfigs.contains(launch.getLaunchConfiguration())) {
        manager.removeLaunch(launch);
      }
    }
    for (ILaunchConfiguration launchConfig : launchConfigs) {
      launchConfig.delete();
    }
  }

}
