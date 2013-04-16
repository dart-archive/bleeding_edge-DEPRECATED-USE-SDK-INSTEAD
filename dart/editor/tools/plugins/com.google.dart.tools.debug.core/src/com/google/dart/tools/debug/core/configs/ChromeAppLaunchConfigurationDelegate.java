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

package com.google.dart.tools.debug.core.configs;

import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.core.DartLaunchConfigurationDelegate;
import com.google.dart.tools.debug.core.DebugUIHelper;
import com.google.dart.tools.debug.core.util.BrowserManager;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//[ {
//  "devtoolsFrontendUrl": "/devtools/devtools.html?ws=localhost:1234/devtools/page/1",
//  "faviconUrl": "",
//  "id": "1",
//  "thumbnailUrl": "/thumb/chrome://newtab/",
//  "title": "New Tab",
//  "type": "page",
//  "url": "chrome://newtab/",
//  "webSocketDebuggerUrl": "ws://localhost:1234/devtools/page/1"
//}, {
//  "devtoolsFrontendUrl": "/devtools/devtools.html?ws=localhost:1234/devtools/page/2",
//  "faviconUrl": "",
//  "id": "2",
//  "thumbnailUrl": "/thumb/chrome-extension://becjelbpddbpmopbobpojhgneicbhlgj/_generated_background_page.html",
//  "title": "chrome-extension://becjelbpddbpmopbobpojhgneicbhlgj/_generated_background_page.html",
//  "type": "other",
//  "url": "chrome-extension://becjelbpddbpmopbobpojhgneicbhlgj/_generated_background_page.html",
//  "webSocketDebuggerUrl": "ws://localhost:1234/devtools/page/2"
//}, {
//  "devtoolsFrontendUrl": "/devtools/devtools.html?ws=localhost:1234/devtools/page/3",
//  "faviconUrl": "",
//  "id": "3",
//  "thumbnailUrl": "/thumb/chrome-extension://becjelbpddbpmopbobpojhgneicbhlgj/packy.html",
//  "title": "Packy",
//  "type": "other",
//  "url": "chrome-extension://becjelbpddbpmopbobpojhgneicbhlgj/packy.html",
//  "webSocketDebuggerUrl": "ws://localhost:1234/devtools/page/3"
//} ]

// TODO(devoncarew): connect debugger to chrome-extension://becj... * ...bhlgj/_generated_background_page.html ?
// will we get console.log output, even though it starts running before we connect?

/**
 * A ILaunchConfigurationDelegate implementation that can launch Chrome applications. We
 * conceptually launch the manifest.json file which specifies a Chrome app. We currently send
 * Dartium the path to the manifest file's parent directory via the --load-extension flag.
 */
public class ChromeAppLaunchConfigurationDelegate extends DartLaunchConfigurationDelegate {

  private static Process chromeAppBrowserProcess;

  /**
   * Create a new ChromeAppLaunchConfigurationDelegate.
   */
  public ChromeAppLaunchConfigurationDelegate() {

  }

  @Override
  public void doLaunch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor, InstrumentationBuilder instrumentation) throws CoreException {

    if (!ILaunchManager.RUN_MODE.equals(mode) && !ILaunchManager.DEBUG_MODE.equals(mode)) {
      throw new CoreException(DartDebugCorePlugin.createErrorStatus("Execution mode '" + mode
          + "' is not supported."));
    }

    boolean enableDebugging = ILaunchManager.DEBUG_MODE.equals(mode);

    File dartium = DartSdkManager.getManager().getSdk().getDartiumExecutable();

    if (dartium == null) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          "Could not find Dartium"));
    }

    DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(configuration);
    wrapper.markAsLaunched();

    IResource jsonResource = wrapper.getApplicationResource();

    if (jsonResource == null) {
      throw newDebugException("No file specified to launch");
    }

    File cwd = getWorkingDirectory(jsonResource);
    String extensionPath = jsonResource.getParent().getLocation().toFile().getAbsolutePath();

    List<String> commandsList = new ArrayList<String>();

    commandsList.add(dartium.getAbsolutePath());
    commandsList.add("--enable-udd-profiles");
    commandsList.add("--user-data-dir="
        + BrowserManager.getCreateUserDataDirectoryPath("chrome-apps"));
    commandsList.add("--profile-directory=editor");
    commandsList.add("--no-first-run");
    commandsList.add("--no-default-browser-check");
    commandsList.add("--enable-extension-activity-logging");
    commandsList.add("--enable-extension-activity-ui");
    //commandsList.add("--load-extension=" + extensionPath);
    commandsList.add("--load-and-launch-app=" + extensionPath);
    //commandsList.add("--remote-debugging-port=1234");

    if (enableDebugging) {
      // TODO(devoncarew):

    }

    monitor.beginTask("Dartium", IProgressMonitor.UNKNOWN);

    terminatePreviousLaunch();

    String[] commands = commandsList.toArray(new String[commandsList.size()]);
    ProcessBuilder processBuilder = new ProcessBuilder(commands);
    processBuilder.directory(cwd);

    Process runtimeProcess = null;

    try {
      runtimeProcess = processBuilder.start();
    } catch (IOException ioe) {
      throw newDebugException(ioe);
    }

    Map<String, String> processAttributes = new HashMap<String, String>();

    processAttributes.put(IProcess.ATTR_PROCESS_TYPE, "Dartium");

    IProcess eclipseProcess = DebugPlugin.newProcess(
        launch,
        runtimeProcess,
        configuration.getName(),
        processAttributes);

    if (eclipseProcess == null) {
      throw newDebugException("Error starting Dartium");
    }

    saveLaunchedProcess(runtimeProcess);

    if (enableDebugging) {
      // TODO(devoncarew):

    }

    // TODO(devoncarew): we need to wait until the process is started before we can try and activate
    // the window. We need to find a better way to do this then just a fixed delay.
    sleep(1000);

    DebugUIHelper.getHelper().activateApplication(dartium, "Chromium");

    monitor.done();
  }

  /**
   * Return the parent of the Chrome app directory. The Chrome app directory contains the given
   * manifest.json file.
   * 
   * @param jsonResource
   * @return
   */
  private File getWorkingDirectory(IResource jsonResource) {
    IContainer containingDir = jsonResource.getParent();
    File containingFile = containingDir.getLocation().toFile();

    // Return the parent of this directory.
    return containingFile.getParentFile();
  }

  private DebugException newDebugException(String message) {
    return new DebugException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID, message));
  }

  private DebugException newDebugException(Throwable t) {
    return new DebugException(new Status(
        IStatus.ERROR,
        DartDebugCorePlugin.PLUGIN_ID,
        t.toString(),
        t));
  }

  /**
   * Store the successfully launched process into a static variable;
   * 
   * @param process
   */
  private void saveLaunchedProcess(Process process) {
    chromeAppBrowserProcess = process;
  }

  private void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (Exception exception) {

    }
  }

  private void terminatePreviousLaunch() {
    if (chromeAppBrowserProcess != null) {
      try {
        chromeAppBrowserProcess.exitValue();
        chromeAppBrowserProcess = null;
      } catch (IllegalThreadStateException ex) {
        // exitValue() will throw if the process has not yet stopped. In that case, we ask it to.
        chromeAppBrowserProcess.destroy();
        chromeAppBrowserProcess = null;

        // Delay a bit.
        sleep(100);
      }
    }
  }

}
