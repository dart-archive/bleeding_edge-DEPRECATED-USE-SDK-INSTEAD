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
package com.google.dart.tools.debug.core.configs;

import com.google.dart.tools.debug.core.ChromeBrowserConfig;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.core.internal.util.BrowserManager;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;

/**
 * The launch configuration delegate for the com.google.dart.tools.debug.core.chromeLaunchConfig
 * launch config.
 */
public class DartChromeLaunchConfigurationDelegate extends LaunchConfigurationDelegate {

  /**
   * Create a new DartChromeLaunchConfigurationDelegate.
   */
  public DartChromeLaunchConfigurationDelegate() {

  }

  @Override
  public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor) throws CoreException {
    if (!ILaunchManager.RUN_MODE.equals(mode) && !ILaunchManager.DEBUG_MODE.equals(mode)) {
      throw new CoreException(DartDebugCorePlugin.createErrorStatus("Execution mode '" + mode
          + "' is not supported."));
    }

    boolean debugLaunch = ILaunchManager.DEBUG_MODE.equals(mode);

    DartLaunchConfigWrapper launchConfig = new DartLaunchConfigWrapper(configuration);

    String browserName = launchConfig.getBrowserConfig();

    ChromeBrowserConfig chromeBrowserConfig = DartDebugCorePlugin.getPlugin().getChromeBrowserConfig(
        browserName);

    // launch the browser - show errors if we couldn't

    if (chromeBrowserConfig == null) {
      // TODO: better error handling, no browsers configured
      throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
          "Browser not configured"));
    } else {

      IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(
          new Path(launchConfig.getApplicationName()));
      if (!file.exists()) {
        throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
            "HTML file could not be found"));
      }

      BrowserManager manager = BrowserManager.getManager();

      manager.launchBrowser(launch, launchConfig, file, monitor, debugLaunch);
    }
  }

}
