/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.tools.debug.core.pubserve;

import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.core.DartLaunchConfigurationDelegate;
import com.google.dart.tools.debug.core.DebugUIHelper;
import com.google.dart.tools.debug.core.util.BrowserManager;
import com.google.dart.tools.debug.core.util.LaunchConfigResourceResolver;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;

import java.util.concurrent.Semaphore;

/**
 * A launch configuration delegate to launch application in Dartium and serve files using pub serve.
 */
public class PubServeLaunchConfigurationDelegate extends DartLaunchConfigurationDelegate {

  private static Semaphore launchSemaphore = new Semaphore(1);

  protected static ILaunch launch;

  protected static DartLaunchConfigWrapper launchConfig;

  private static PubCallback<String> pubConnectionCallback = new PubCallback<String>() {

    @Override
    public void handleResult(PubResult<String> result) {
      if (result.isError()) {
        DebugUIHelper.getHelper().showError(
            "Launch Error",
            "Pub serve communication error: " + result.getErrorMessage());
        return;
      }

      try {
        String launchUrl = result.getResult();
        launchInDartium(launchUrl, launch, launchConfig);
      } catch (CoreException e) {
        DartDebugCorePlugin.logError(e);
      }
    }
  };

  private static boolean enableDebugging;

  private static void launchInDartium(final String url, ILaunch launch,
      DartLaunchConfigWrapper launchConfig) throws CoreException {

    BrowserManager manager = BrowserManager.getManager();
    manager.launchBrowser(
        launch,
        launchConfig,
        url,
        new NullProgressMonitor(),
        enableDebugging,
        new LaunchConfigResourceResolver(launchConfig));

  }

  @Override
  public void doLaunch(ILaunchConfiguration configuration, String mode, ILaunch rlaunch,
      IProgressMonitor monitor, InstrumentationBuilder instrumentation) throws CoreException {

    if (!ILaunchManager.RUN_MODE.equals(mode) && !ILaunchManager.DEBUG_MODE.equals(mode)) {
      throw new CoreException(DartDebugCorePlugin.createErrorStatus("Execution mode '" + mode
          + "' is not supported."));
    }

    launch = rlaunch;
    launchConfig = new DartLaunchConfigWrapper(configuration);

    // If we're in the process of launching Dartium, don't allow a second launch to occur.
    if (launchSemaphore.tryAcquire()) {
      try {
        launchImpl(mode, monitor);
      } finally {
        launchSemaphore.release();
      }
    }
  }

  private void launchImpl(String mode, IProgressMonitor monitor) throws CoreException {

    launchConfig.markAsLaunched();
    enableDebugging = ILaunchManager.DEBUG_MODE.equals(mode)
        && !DartCoreDebug.DISABLE_DARTIUM_DEBUGGER;

    // Launch the browser - show errors if we couldn't.
    IResource resource = null;

    resource = launchConfig.getApplicationResource();

    if (resource == null) {
      String url = launchConfig.getUrl();

      BrowserManager.getManager().launchBrowser(
          launch,
          launchConfig,
          url,
          monitor,
          enableDebugging,
          new PubServeResourceResolver());

    } else {

      // launch pub serve
      PubServeManager manager = PubServeManager.getManager();

      try {

        manager.serve(launchConfig, pubConnectionCallback);

      } catch (Exception e) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            "Could not start pub serve or connect to pub\n" + manager.getStdErrorString(),
            e));
      }
    }
  }
}
