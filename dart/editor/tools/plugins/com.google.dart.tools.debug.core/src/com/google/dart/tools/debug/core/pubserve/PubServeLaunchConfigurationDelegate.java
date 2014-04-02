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
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.core.DartLaunchConfigurationDelegate;
import com.google.dart.tools.debug.core.util.BrowserManager;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.RuntimeProcess;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * A launch configuration delegate to launch application in Dartium and serve files using pub serve.
 */
public class PubServeLaunchConfigurationDelegate extends DartLaunchConfigurationDelegate {

  private static Semaphore launchSemaphore = new Semaphore(1);

  private static RuntimeProcess eclipseProcess;

  @Override
  public void doLaunch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor, InstrumentationBuilder instrumentation) throws CoreException {

    if (!ILaunchManager.RUN_MODE.equals(mode)) {
      throw new CoreException(DartDebugCorePlugin.createErrorStatus("Execution mode '" + mode
          + "' is not supported."));
    }

    DartLaunchConfigWrapper launchConfig = new DartLaunchConfigWrapper(configuration);

    // If we're in the process of launching Dartium, don't allow a second launch to occur.
    if (launchSemaphore.tryAcquire()) {
      try {
        launchImpl(launchConfig, mode, launch, monitor);
      } finally {
        launchSemaphore.release();
      }
    }
  }

  private void dispose() {
    if (eclipseProcess != null) {
      try {
        eclipseProcess.terminate();
      } catch (DebugException e) {

      }
      eclipseProcess = null;
    }
  }

  private void launchImpl(DartLaunchConfigWrapper launchConfig, String mode, ILaunch launch,
      IProgressMonitor monitor) throws CoreException {

    launchConfig.markAsLaunched();

    // Launch the browser - show errors if we couldn't.
    IResource resource = null;

    resource = launchConfig.getApplicationResource();
    if (resource == null) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          "HTML file could not be found"));
    }

    // launch pub serve
    PubServeManager manager = PubServeManager.getManager();

    if (!manager.startPubServe(launchConfig)) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          "Could not start pub serve\n" + manager.getStdErrorString()));
    }

    String url = "http://localhost:" + PubServeManager.PORT_NUMBER;

    launchInDartium(url + "/" + resource.getName(), launch, launchConfig, monitor);

  }

  private void launchInDartium(final String url, ILaunch launch,
      DartLaunchConfigWrapper launchConfig, IProgressMonitor monitor) throws CoreException {

    // close a running instance of Dartium, if any
    dispose();

    File dartium = DartSdkManager.getManager().getSdk().getDartiumExecutable();

    List<String> cmd = new ArrayList<String>();
    cmd.add(dartium.getAbsolutePath());
    // In order to start up multiple Chrome processes, we need to specify a different user dir.
    cmd.add("--user-data-dir=" + BrowserManager.getCreateUserDataDirectoryPath("pubserve"));

    if (launchConfig.getUseWebComponents()) {
      cmd.add("--enable-experimental-web-platform-features");
      cmd.add("--enable-html-imports");
    }
    // Disables the default browser check.
    cmd.add("--no-default-browser-check");
    // Bypass the error dialog when the profile lock couldn't be attained.
    cmd.add("--no-process-singleton-dialog");
    for (String arg : launchConfig.getArgumentsAsArray()) {
      cmd.add(arg);
    }
    cmd.add(url);

    ProcessBuilder processBuilder = new ProcessBuilder(cmd);
    Map<String, String> env = processBuilder.environment();
    // Due to differences in 32bit and 64 bit environments, dartium 32bit launch does not work on
    // linux with this property.
    env.remove("LD_LIBRARY_PATH");
    if (launchConfig.getCheckedMode()) {
      env.put("DART_FLAGS", "--enable-checked-mode");
    }

    Process javaProcess = null;

    try {
      javaProcess = processBuilder.start();
    } catch (IOException ioe) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          ioe.getMessage(),
          ioe));
    }

    eclipseProcess = null;

    Map<String, String> processAttributes = new HashMap<String, String>();
    String programName = "dartium";
    processAttributes.put(IProcess.ATTR_PROCESS_TYPE, programName);

    if (javaProcess != null) {
      monitor.beginTask("Dartium", IProgressMonitor.UNKNOWN);

      eclipseProcess = new RuntimeProcess(launch, javaProcess, launchConfig.getApplicationName()
          + " (" + new Date() + ")", processAttributes);

    }

    if (javaProcess == null || eclipseProcess == null) {
      if (javaProcess != null) {
        javaProcess.destroy();
      }

      throw new CoreException(
          DartDebugCorePlugin.createErrorStatus("Error starting Dartium browser"));
    }

  }
}
