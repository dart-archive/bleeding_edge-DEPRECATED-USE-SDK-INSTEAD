/*
 * Copyright (c) 2011, the Dart project authors.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Dart Server Application launch configuration.
 */
public class DartServerLaunchConfigurationDelegate extends LaunchConfigurationDelegate {

  /**
   * Create a new DartServerLaunchConfigurationDelegate.
   */
  public DartServerLaunchConfigurationDelegate() {

  }

  @Override
  public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor) throws CoreException {

    if (!ILaunchManager.RUN_MODE.equals(mode) && !ILaunchManager.DEBUG_MODE.equals(mode)) {
      throw new CoreException(DartDebugCorePlugin.createErrorStatus("Execution mode '" + mode
          + "' is not supported."));
    }

    DartLaunchConfigWrapper launchConfig = new DartLaunchConfigWrapper(configuration);

    launchVM(launch, launchConfig, monitor, true);

  }

  protected void launchVM(ILaunch launch, DartLaunchConfigWrapper launchConfig,
      IProgressMonitor monitor, boolean runMode) throws CoreException {
    if (!runMode) {
      // TODO(devoncarew): implement this
      throw new CoreException(
          DartDebugCorePlugin.createErrorStatus("Dart Server launch config does not yet support debugging."));
    }

    // Usage: dart [options] script.dart [arguments]

    File outputDirectory = new Path(launchConfig.getLibraryLocation()).makeAbsolute().toFile();

    String scriptPath = launchConfig.getApplicationName();

    String vmExecPath = DartDebugCorePlugin.getPlugin().getDartVmExecutablePath();
    if (vmExecPath.length() == 0) {

      throw new CoreException(
          DartDebugCorePlugin.createErrorStatus("The executable path for the Dart VM has not been set"));
    }

    List<String> commandsList = new ArrayList<String>();

    commandsList.add(vmExecPath);
    commandsList.addAll(Arrays.asList(launchConfig.getVmArgumentsAsArray()));
    // TODO(keertip): move to ui for setting dart vm arguments
    commandsList.add("--new_gen_heap_size=64");
    commandsList.add(scriptPath);
    commandsList.addAll(Arrays.asList(launchConfig.getArgumentsAsArray()));

    String[] commands = commandsList.toArray(new String[commandsList.size()]);
    ProcessBuilder processBuilder = new ProcessBuilder(commands);

    processBuilder.directory(outputDirectory);

    Process runtimeProcess = null;

    try {
      runtimeProcess = processBuilder.start();
    } catch (IOException ioe) {
      throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
          ioe.getMessage(), ioe));
    }

    IProcess eclipseProcess = null;

    Map<String, String> processAttributes = new HashMap<String, String>();

    String programName = "dart";
    processAttributes.put(IProcess.ATTR_PROCESS_TYPE, programName);

    if (runtimeProcess != null) {
      monitor.beginTask("Dart", IProgressMonitor.UNKNOWN);

      eclipseProcess = DebugPlugin.newProcess(launch, runtimeProcess,
          launchConfig.getApplicationName(), processAttributes);
    }

    if (runtimeProcess == null || eclipseProcess == null) {
      if (runtimeProcess != null) {
        runtimeProcess.destroy();
      }

      throw new CoreException(
          DartDebugCorePlugin.createErrorStatus("Error starting Dart VM process"));
    }

    eclipseProcess.setAttribute(IProcess.ATTR_CMDLINE, generateCommandLine(commands));

    monitor.done();

  }

  private String generateCommandLine(String[] commands) {
    StringBuilder builder = new StringBuilder();

    for (String str : commands) {
      if (builder.length() > 0) {
        builder.append(" ");
      }

      builder.append(str);
    }

    return builder.toString();
  }

}
