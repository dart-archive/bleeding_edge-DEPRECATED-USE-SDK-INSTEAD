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

import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.core.DartLaunchConfigurationDelegate;
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

/**
 * A ILaunchConfigurationDelegate implementation that can launch Chrome applications. We
 * conceptually launch the manifest.json file which specifies a Chrome app. We currently send
 * Dartium the path to the manifest file's parent directory via the --load-extension flag.
 */
public class ChromeAppLaunchConfigurationDelegate extends DartLaunchConfigurationDelegate {

  /**
   * Create a new ChromeAppLaunchConfigurationDelegate.
   */
  public ChromeAppLaunchConfigurationDelegate() {

  }

  @Override
  public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor) throws CoreException {

    long start = System.currentTimeMillis();

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
    commandsList.add("--user-data-dir=" + BrowserManager.getCreateUserDataDirectoryPath());
    commandsList.add("--profile-directory=editor");
    commandsList.add("--no-first-run");
    commandsList.add("--no-default-browser-check");
    commandsList.add("--enable-extension-activity-logging");
    commandsList.add("--enable-extension-activity-ui");
    commandsList.add("--load-extension=" + extensionPath);

    // Use the about:extensions url to see a list of chrome extensions (and their ids).

    // This don't work, but it would be nice if it did.
    //commandsList.add("--app-id=mbadbcdklbggkebhdfmignehollhhdhi");

    // This is a proposed flag, but does not yet exist.
    //commandsList.add("--load-and-launch-app=" + extensionPath);

    if (enableDebugging) {
      // TODO(devoncarew):

    }

    String[] commands = commandsList.toArray(new String[commandsList.size()]);
    ProcessBuilder processBuilder = new ProcessBuilder(commands);
    processBuilder.directory(cwd);

    Process runtimeProcess = null;

    monitor.beginTask("Dartium", IProgressMonitor.UNKNOWN);

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

    if (enableDebugging) {
      // TODO(devoncarew):

    }

    monitor.done();

    long elapsed = System.currentTimeMillis() - start;
    Instrumentation.metric("ChromeAppLaunchLaunchConfiguration-launch", elapsed).with("mode", mode).log();

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

}
