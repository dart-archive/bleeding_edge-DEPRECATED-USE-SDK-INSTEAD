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
package com.google.dart.tools.debug.ui.internal.chrome;

import com.google.dart.tools.debug.core.ChromeBrowserConfig;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.DartUtil;
import com.google.dart.tools.debug.ui.internal.DebugErrorHandler;
import com.google.dart.tools.debug.ui.internal.preferences.DebugPreferencePage;
import com.google.dart.tools.debug.ui.internal.util.AbstractLaunchShortcut;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

import java.util.List;

/**
 * A launch shortcut to allow users to launch Dart applications in Chrome.
 */
public class DartChromeLaunchShortcut extends AbstractLaunchShortcut {

  public DartChromeLaunchShortcut() {
    super("Chrome");
  }

  @Override
  protected ILaunchConfigurationType getConfigurationType() {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(DartDebugCorePlugin.CHROME_LAUNCH_CONFIG_ID);

    return type;
  }

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
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(DartDebugCorePlugin.CHROME_LAUNCH_CONFIG_ID);
    ILaunchConfigurationWorkingCopy launchConfig = null;
    try {
      launchConfig = type.newInstance(null, resource.getName());
    } catch (CoreException ce) {
      DartUtil.logError(ce);
      return;
    }

    DartLaunchConfigWrapper launchWrapper = new DartLaunchConfigWrapper(launchConfig);

    launchWrapper.setApplicationName(resource.getFullPath().toString());

    List<ChromeBrowserConfig> browsers = DartDebugCorePlugin.getPlugin().getConfiguredBrowsers();

    if (browsers.size() == 0) {
      String message = "In order to run a Dart application you first need to configure a Chrome browser.";

      DebugErrorHandler.errorDialog((Shell) null, "Unable to Launch Chrome", message, new Status(
          IStatus.ERROR, DartDebugUIPlugin.PLUGIN_ID, message));

      PreferenceDialog pref = PreferencesUtil.createPreferenceDialogOn(
          PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
          DebugPreferencePage.PAGE_ID, null, null);
      if (pref != null) {
        pref.open();
      }
    } else {
      launchWrapper.setProjectName(resource.getProject().getName());
      launchWrapper.setBrowserConfig(browsers.get(0).getName());

      launchConfig.setMappedResources(new IResource[] {resource});

      try {
        config = launchConfig.doSave();
      } catch (CoreException e) {
        DartUtil.logError(e);
        return;
      }

      DebugUITools.launch(config, mode);
    }
  }

  @Override
  protected boolean testSimilar(IResource resource, ILaunchConfiguration config) {
    DartLaunchConfigWrapper launchWrapper = new DartLaunchConfigWrapper(config);

    String resourcePath = resource.getFullPath().toString();
    String applicationPath = launchWrapper.getApplicationName();

    if (resourcePath.equals(applicationPath)) {
      return true;
    }

    resourcePath = resource.getProjectRelativePath().toString();

    return resourcePath.equals(applicationPath);
  }

}
