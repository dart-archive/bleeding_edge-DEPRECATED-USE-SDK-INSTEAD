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
package com.google.dart.tools.debug.core.configs;

import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.core.util.BrowserManager;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;

/**
 * The launch configuration delegate for the com.google.dart.tools.debug.core.dartiumLaunchConfig
 * launch config.
 */
public class DartiumLaunchConfigurationDelegate extends LaunchConfigurationDelegate {

  /**
   * Create a new DartChromiumLaunchConfigurationDelegate.
   */
  public DartiumLaunchConfigurationDelegate() {

  }

  @Override
  public boolean buildForLaunch(ILaunchConfiguration configuration, String mode,
      IProgressMonitor monitor) throws CoreException {
    return false;
  }

  @Override
  public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor) throws CoreException {
    if (!ILaunchManager.RUN_MODE.equals(mode) && !ILaunchManager.DEBUG_MODE.equals(mode)) {
      throw new CoreException(DartDebugCorePlugin.createErrorStatus("Execution mode '" + mode
          + "' is not supported."));
    }

    DartLaunchConfigWrapper launchConfig = new DartLaunchConfigWrapper(configuration);

    launchConfig.markAsLaunched();

    boolean enableDebugging = launchConfig.getEnableDebugging();

    // Launch the browser - show errors if we couldn't.
    IResource resource = null;
    String url;

    if (launchConfig.getShouldLaunchFile()) {
      resource = launchConfig.getApplicationResource();
      if (resource == null) {
        throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
            "HTML file could not be found"));
      }
      url = resource.getLocationURI().toString();
    } else {
      url = launchConfig.getUrl();
    }

    BrowserManager manager = BrowserManager.getManager();

    if (resource instanceof IFile) {
      manager.launchBrowser(launch, launchConfig, (IFile) resource, monitor, enableDebugging);
    } else {
      manager.launchBrowser(launch, launchConfig, url, monitor, enableDebugging);
    }
  }

}
