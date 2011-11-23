/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.debug.ui.internal.server;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.ui.internal.DartUtil;
import com.google.dart.tools.debug.ui.internal.util.AbstractLaunchShortcut;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;

/**
 * The launch shortcut for the Dart Server Application launch configuration.
 */
public class DartServerLaunchShortcut extends AbstractLaunchShortcut {

  /**
   * Create a new DartServerLaunchShortcut.
   */
  public DartServerLaunchShortcut() {
    super("Server");
  }

  @Override
  protected ILaunchConfigurationType getConfigurationType() {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(DartDebugCorePlugin.SERVER_LAUNCH_CONFIG_ID);

    return type;
  }

  @Override
  protected IResource getLaunchableResource(Object originalResource) throws DartModelException {
    if (originalResource instanceof DartElement) {
      DartLibrary parentLibrary = ((DartElement) originalResource).getAncestor(DartLibrary.class);
      return parentLibrary.getCorrespondingResource();
    }
    if (originalResource instanceof IResource) {
      return (IResource) originalResource;
    }
    return null;
  }

  /**
   * Launch the Dart application associated with the specified resource.
   * 
   * @param resource the resource
   * @param mode the launch mode ("run", "debug", ...)
   */
  @Override
  protected void launch(IResource resource, String mode) {
    if (resource == null) {
      return;
    }

    // Launch an existing configuration if one exists
    ILaunchConfiguration config = findConfig(resource);
    if (config != null) {
      DebugUITools.launch(config, mode);
      return;
    }

    // Create and launch a new configuration
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(DartDebugCorePlugin.SERVER_LAUNCH_CONFIG_ID);
    ILaunchConfigurationWorkingCopy launchConfig = null;
    try {
      launchConfig = type.newInstance(null, resource.getName());
    } catch (CoreException ce) {
      DartUtil.logError(ce);
      return;
    }

    DartLaunchConfigWrapper launchWrapper = new DartLaunchConfigWrapper(launchConfig);

    launchWrapper.setProjectName(resource.getProject().getName());
    launchWrapper.setApplicationName(resource.getLocation().toOSString());
    launchWrapper.setLibraryLocation(resource.getLocation().removeLastSegments(1).toOSString());

    launchConfig.setMappedResources(new IResource[] {resource});

    try {
      config = launchConfig.doSave();
    } catch (CoreException e) {
      DartUtil.logError(e);
      return;
    }

    DebugUITools.launch(config, mode);
  }

  @Override
  protected boolean testSimilar(IResource resource, ILaunchConfiguration config) {
    DartLaunchConfigWrapper launchWrapper = new DartLaunchConfigWrapper(config);

    if (!resource.getProject().getName().equals(launchWrapper.getProjectName())) {
      return false;
    }

    String resourcePath = resource.getLocation().toOSString();
    String applicationPath = launchWrapper.getApplicationName();

    return resourcePath.equals(applicationPath);
  }

}
