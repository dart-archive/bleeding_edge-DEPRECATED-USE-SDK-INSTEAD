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

import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.dart2js.ProcessRunner;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.core.DartLaunchConfigurationDelegate;
import com.google.dart.tools.debug.core.util.ResourceServer;
import com.google.dart.tools.debug.core.util.ResourceServerManager;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Launches the Dart application (compiled to js) in the browser.
 */
public class BrowserLaunchConfigurationDelegate extends DartLaunchConfigurationDelegate {

  public static void openBrowser(String url) throws CoreException {
    IWebBrowser browser = null;
    try {
      browser = PlatformUI.getWorkbench().getBrowserSupport().createBrowser(
          IWorkbenchBrowserSupport.AS_EXTERNAL,
          "defaultBrowser",
          "Default Browser",
          "Browser");
      if (browser != null) {
        browser.openURL(new URL(url));
      } else {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            Messages.BrowserLaunchConfigurationDelegate_DefaultBrowserNotFound));
      }
    } catch (PartInitException e1) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          Messages.BrowserLaunchConfigurationDelegate_DefaultBrowserNotFound));
    } catch (MalformedURLException e) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          Messages.BrowserLaunchConfigurationDelegate_UrlError));
    }
  }

  /**
   * Match both the input and id, so that different types of editor can be opened on the same input.
   */
  @Override
  public void doLaunch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor, InstrumentationBuilder instrumentation) throws CoreException {

    mode = ILaunchManager.RUN_MODE;

    DartLaunchConfigWrapper launchConfig = new DartLaunchConfigWrapper(configuration);
    launchConfig.markAsLaunched();

    String url;

    if (launchConfig.getShouldLaunchFile()) {
      IResource resource = launchConfig.getApplicationResource();

      if (resource == null) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            Messages.BrowserLaunchConfigurationDelegate_HtmlFileNotFound));
      }

//      try {
//        // Our embedded web server will automatically recompile the dart.js file if any of its
//        // dependencies have changed. There's no need to compile it here (esp. on every launch).
//        compileJavascript(resource, monitor);
//      } catch (OperationCanceledException ex) {
//        // The user cancelled the launch.
//
//        return;
//      }

      try {
        // This returns just a plain file: url.
        ResourceServer server = ResourceServerManager.getServer();

        url = server.getUrlForResource(resource);

        url = launchConfig.appendQueryParams(url);
      } catch (IOException ioe) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            "Could not launch browser - unable to start embedded server",
            ioe));
      }
    } else {
      url = launchConfig.getUrl();

      try {
        String scheme = new URI(url).getScheme();

        if (scheme == null) { // add scheme else browser will not launch
          url = "http://" + url;
        }
      } catch (URISyntaxException e) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            Messages.BrowserLaunchConfigurationDelegate_UrlError));
      }
    }

    if (launchConfig.getUseDefaultBrowser()) {
      openBrowser(url);
    } else {
      launchInExternalBrowser(launchConfig, url);
    }

    DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);

  }

  private void launchInExternalBrowser(DartLaunchConfigWrapper launchConfig, final String url)
      throws CoreException {
    String browserName = launchConfig.getBrowserName();
    List<String> cmd = new ArrayList<String>();

    if (DartCore.isMac()) {
      // use open command on mac
      cmd.add("/usr/bin/open");
      cmd.add("-a");
    }
    cmd.add(browserName);
    cmd.add(url);

    if (launchConfig.getArguments().length() != 0) {
      if (DartCore.isMac()) {
        cmd.add("--args");
        cmd.add(launchConfig.getArguments());
      } else {
        cmd.addAll(Arrays.asList(launchConfig.getArgumentsAsArray()));
      }
    }

    try {
      ProcessBuilder builder = new ProcessBuilder(cmd);
      ProcessRunner runner = new ProcessRunner(builder);

      runner.runAsync();
      runner.await(new NullProgressMonitor(), 500);

      if (runner.getExitCode() != 0) {
        if (DartCore.isWindows()) {
          if (browserName.toLowerCase().indexOf("firefox") != -1) {
            if (runner.getExitCode() == 1) {
              // In this case, the application was opened in a new tab successfully.
              // Don't throw an exception.

              return;
            }
          }
        }

        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugUIPlugin.PLUGIN_ID,
            "Could not launch browser \"" + browserName + "\" : \n\n" + runner.getStdErr()));
      }
    } catch (IOException e) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          Messages.BrowserLaunchConfigurationDelegate_BrowserNotFound,
          e));
    }
  }

}
