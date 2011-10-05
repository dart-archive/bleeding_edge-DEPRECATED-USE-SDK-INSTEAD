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
package com.google.dart.tools.debug.ui.internal.client;

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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

/**
 * This class interprets the current selection or the currently active editor and launches a Dart
 * application. This involves running an existing launch configuration or creating a new one if an
 * appropriate launch configuration does not already exist.
 */
public class DartJsApplicationLaunchShortcut extends AbstractLaunchShortcut {

  /**
   * Create a new DartJsApplicationLaunchShortcut.
   */
  public DartJsApplicationLaunchShortcut() {
    super("Application");
  }

  @Override
  protected ILaunchConfigurationType getConfigurationType() {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(ILaunchConstants.LAUNCH_CONFIG_TYPE);

    return type;
  }

  /**
   * Launch the Dart application associated with the specified resource or open in a browser if the
   * resource is a web page.
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
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(ILaunchConstants.LAUNCH_CONFIG_TYPE);
    ILaunchConfigurationWorkingCopy copy = null;
    try {
      copy = type.newInstance(null, resource.getName());
    } catch (CoreException ce) {
      DartUtil.logError(ce);
      return;
    }

    int launchType = ILaunchConstants.LAUNCH_TYPE_WEB_CLIENT;
    copy.setAttribute(ILaunchConstants.ATTR_LAUNCH_TYPE, launchType);

    String resPath = resource.getFullPath().toString();
    copy.setAttribute(ILaunchConstants.ATTR_RESOURCE_PATH, resPath);

    IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
    boolean internalBrowser = browserSupport.isInternalWebBrowserAvailable();
    copy.setAttribute(ILaunchConstants.ATTR_EXTERNAL_BROWSER, !internalBrowser);

    copy.setMappedResources(new IResource[] {resource});
    try {
      config = copy.doSave();
    } catch (CoreException e) {
      DartUtil.logError(e);
      return;
    }
    DebugUITools.launch(config, mode);
  }

  @Override
  protected boolean testSimilar(IResource resource, ILaunchConfiguration config) {
    try {
      String resourcePath = resource.getFullPath().toPortableString();
      String applicationPath = config.getAttribute(ILaunchConstants.ATTR_RESOURCE_PATH, "");

      return resourcePath.equals(applicationPath);
    } catch (CoreException ce) {
      DartUtil.logError(ce);

      return false;
    }
  }

}
