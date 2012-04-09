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
package com.google.dart.tools.debug.ui.internal.browser;

import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.ui.internal.DartUtil;
import com.google.dart.tools.debug.ui.internal.util.AbstractLaunchShortcut;
import com.google.dart.tools.debug.ui.internal.util.ILaunchShortcutExt;
import com.google.dart.tools.debug.ui.internal.util.LaunchUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;

/**
 * This class interprets the current selection or the currently active editor and opens html page in
 * the browser as configured. This involves running an existing launch configuration or creating a
 * new one if an appropriate launch configuration does not already exist.
 */
public class BrowserLaunchShortcut extends AbstractLaunchShortcut implements ILaunchShortcutExt {

  /**
   * Create a new BrowserLaunchShortcut.
   */
  public BrowserLaunchShortcut() {
    super("Application"); //$NON-NLS-1$
  }

  @Override
  public boolean canLaunch(IResource resource) {
    if (resource instanceof IFile) {
      if ("html".equalsIgnoreCase(resource.getFileExtension())) {
        return true;
      }
    }

    DartLibrary[] libraries = LaunchUtils.getDartLibraries(resource);

    if (libraries.length > 0) {
      for (DartLibrary library : libraries) {
        if (library instanceof DartLibraryImpl) {
          DartLibraryImpl impl = (DartLibraryImpl) library;
          if (impl.isBrowserApplication()) {
            return true;
          }
        }
      }
    }

    return false;
  }

  @Override
  protected ILaunchConfigurationType getConfigurationType() {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(DartDebugCorePlugin.BROWSER_LAUNCH_CONFIG_ID);

    return type;
  }

  /**
   * Open in a browser the url or page specified
   * 
   * @param resource the resource
   * @param mode the launch mode ("run", "debug", ...)
   */
  @Override
  protected void launch(IResource resource, String mode) {
    mode = ILaunchManager.RUN_MODE;

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
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(DartDebugCorePlugin.BROWSER_LAUNCH_CONFIG_ID);
    ILaunchConfigurationWorkingCopy launchConfig = null;
    try {
      launchConfig = type.newInstance(null, resource.getName());
    } catch (CoreException ce) {
      DartUtil.logError(ce);
      return;
    }

    DartLaunchConfigWrapper launchWrapper = new DartLaunchConfigWrapper(launchConfig);

    launchWrapper.setProjectName(resource.getProject().getName());
    launchWrapper.setApplicationName(resource.getFullPath().toPortableString());
    launchWrapper.setUseDefaultBrowser(true);

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
    return LaunchUtils.isLaunchableWith(resource, config);
  }

}
