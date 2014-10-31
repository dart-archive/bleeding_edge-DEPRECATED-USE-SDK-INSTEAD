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

import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.core.DartLaunchConfigurationDelegate;
import com.google.dart.tools.debug.core.DebugUIHelper;
import com.google.dart.tools.debug.core.pubserve.PubCallback;
import com.google.dart.tools.debug.core.pubserve.PubResult;
import com.google.dart.tools.debug.core.pubserve.PubServeManager;
import com.google.dart.tools.debug.core.pubserve.PubServeResourceResolver;
import com.google.dart.tools.debug.core.util.BrowserManager;
import com.google.dart.tools.debug.core.util.IRemoteConnectionDelegate;
import com.google.dart.tools.debug.core.util.IResourceResolver;
import com.google.dart.tools.debug.core.util.LaunchConfigResourceResolver;
import com.google.dart.tools.debug.core.util.ResourceServer;
import com.google.dart.tools.debug.core.util.ResourceServerManager;
import com.google.dart.tools.debug.core.webkit.DefaultChromiumTabChooser;
import com.google.dart.tools.debug.core.webkit.IChromiumTabChooser;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;

import java.io.IOException;
import java.util.concurrent.Semaphore;

/**
 * The launch configuration delegate for the com.google.dart.tools.debug.core.dartiumLaunchConfig
 * launch config.
 */
public class DartiumLaunchConfigurationDelegate extends DartLaunchConfigurationDelegate implements
    IRemoteConnectionDelegate {
  private static Semaphore launchSemaphore = new Semaphore(1);

  private IChromiumTabChooser tabChooser;

  protected static ILaunch launch;

  protected static DartLaunchConfigWrapper launchConfig;

  private PubCallback<String> pubConnectionCallback = new PubCallback<String>() {
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
        DebugUIHelper.getHelper().showError("Dartium Launch Error", e.getMessage());
      }
    }
  };

  private boolean enableDebugging;

  /**
   * Create a new DartChromiumLaunchConfigurationDelegate.
   */
  public DartiumLaunchConfigurationDelegate() {
    this(new DefaultChromiumTabChooser());
  }

  public DartiumLaunchConfigurationDelegate(IChromiumTabChooser tabChooser) {
    this.tabChooser = tabChooser;
  }

  @Override
  public void doLaunch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor, InstrumentationBuilder instrumentation) throws CoreException {

    if (!ILaunchManager.RUN_MODE.equals(mode) && !ILaunchManager.DEBUG_MODE.equals(mode)) {
      throw new CoreException(DartDebugCorePlugin.createErrorStatus("Execution mode '" + mode
          + "' is not supported."));
    }

    DartiumLaunchConfigurationDelegate.launch = launch;
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

  @Override
  public IDebugTarget performRemoteConnection(String host, int port, IFile file,
      IProgressMonitor monitor, boolean usePubServe) throws CoreException {
    BrowserManager browserManager = new BrowserManager();

    IResourceResolver resolver = null;
    try {
      resolver = usePubServe ? new PubServeResourceResolver() : ResourceServerManager.getServer();
    } catch (IOException e) {
      return null;
    }

    return browserManager.performRemoteConnection(tabChooser, host, port, monitor, resolver);

  }

  private ResourceServer getResourceServer() throws CoreException {

    ResourceServer resourceResolver;
    try {
      resourceResolver = ResourceServerManager.getServer();
    } catch (IOException ioe) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          ioe.getMessage(),
          ioe));
    }

    return resourceResolver;
  }

  private void launchImpl(String mode, IProgressMonitor monitor) throws CoreException {
    launchConfig.markAsLaunched();

    enableDebugging = ILaunchManager.DEBUG_MODE.equals(mode)
        && !DartCoreDebug.DISABLE_DARTIUM_DEBUGGER;

    // Launch the browser - show errors if we couldn't.
    IResource resource = null;
    String url;

    if (launchConfig.getShouldLaunchFile()) {
      resource = launchConfig.getApplicationResource();
      if (resource == null) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            "HTML file could not be found"));
      }

      if (launchConfig.getUsePubServe()) {

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
      } else { // use editor resource server

        BrowserManager.getManager().launchBrowser(
            launch,
            launchConfig,
            (IFile) resource,
            monitor,
            enableDebugging,
            getResourceServer());
      }
    } else {
      // launch url
      url = launchConfig.getUrl();

      BrowserManager.getManager().launchBrowser(
          launch,
          launchConfig,
          url,
          monitor,
          enableDebugging,
          new LaunchConfigResourceResolver(launchConfig));
    }
  }

  private void launchInDartium(final String url, ILaunch launch,
      DartLaunchConfigWrapper launchConfig) throws CoreException {
    BrowserManager manager = BrowserManager.getManager();

    // TODO(keertip): refactor resolver for use with different pub serves
    manager.launchBrowser(
        launch,
        launchConfig,
        url,
        new NullProgressMonitor(),
        enableDebugging,
        new PubServeResourceResolver());

  }
}
