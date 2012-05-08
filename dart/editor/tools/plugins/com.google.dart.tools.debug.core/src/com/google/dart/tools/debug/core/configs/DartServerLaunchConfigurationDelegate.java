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

import com.google.dart.tools.core.model.DartSdk;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.core.server.ServerDebugTarget;
import com.google.dart.tools.debug.core.util.NetUtils;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
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
  private static final int DEFAULT_PORT_NUMBER = 5858;

  /**
   * Create a new DartServerLaunchConfigurationDelegate.
   */
  public DartServerLaunchConfigurationDelegate() {

  }

  @Override
  public boolean buildForLaunch(ILaunchConfiguration configuration, String mode,
      IProgressMonitor monitor) throws CoreException {
    return false;
  }

  @Override
  public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor) throws CoreException {
    DartLaunchConfigWrapper launchConfig = new DartLaunchConfigWrapper(configuration);

    launchConfig.markAsLaunched();

    launchVM(launch, launchConfig, monitor);
  }

  protected void launchVM(ILaunch launch, DartLaunchConfigWrapper launchConfig,
      IProgressMonitor monitor) throws CoreException {
    boolean enableDebugging = launchConfig.getEnableDebugging()
        && DartDebugCorePlugin.SERVER_DEBUGGING;

    // Usage: dart [options] script.dart [arguments]

    File currentWorkingDirectory = getCurrentWorkingDirectory(launchConfig);

    String scriptPath = launchConfig.getApplicationName();

    scriptPath = translateToFilePath(scriptPath);

    String vmExecPath = "";

    if (DartSdk.isInstalled()) {
      File vmExec = DartSdk.getInstance().getVmExecutable();

      if (vmExec != null) {
        vmExecPath = vmExec.getAbsolutePath().toString();
      }
    } else {
      vmExecPath = DartDebugCorePlugin.getPlugin().getDartVmExecutablePath();
    }

    if (vmExecPath.length() == 0) {
      throw new CoreException(
          DartDebugCorePlugin.createErrorStatus("The executable path for the Dart VM has not been set."));
    }

    List<String> commandsList = new ArrayList<String>();

    int connectionPort = NetUtils.findUnusedPort(DEFAULT_PORT_NUMBER);

    commandsList.add(vmExecPath);
    commandsList.addAll(Arrays.asList(launchConfig.getVmArgumentsAsArray()));
    if (DartDebugCorePlugin.SERVER_DEBUGGING && enableDebugging) {
      commandsList.add("--debug:" + connectionPort);
    }
    commandsList.add(scriptPath);
    commandsList.addAll(Arrays.asList(launchConfig.getArgumentsAsArray()));

    String[] commands = commandsList.toArray(new String[commandsList.size()]);
    ProcessBuilder processBuilder = new ProcessBuilder(commands);

    processBuilder.directory(currentWorkingDirectory);

    Process runtimeProcess = null;

    try {
      runtimeProcess = processBuilder.start();
    } catch (IOException ioe) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          ioe.getMessage(),
          ioe));
    }

    IProcess eclipseProcess = null;

    Map<String, String> processAttributes = new HashMap<String, String>();

    String programName = "dart";
    processAttributes.put(IProcess.ATTR_PROCESS_TYPE, programName);

    if (runtimeProcess != null) {
      monitor.beginTask("Dart", IProgressMonitor.UNKNOWN);

      eclipseProcess = DebugPlugin.newProcess(
          launch,
          runtimeProcess,
          launchConfig.getApplicationName(),
          processAttributes);
    }

    if (runtimeProcess == null || eclipseProcess == null) {
      if (runtimeProcess != null) {
        runtimeProcess.destroy();
      }

      throw new CoreException(
          DartDebugCorePlugin.createErrorStatus("Error starting Dart VM process"));
    }

    eclipseProcess.setAttribute(IProcess.ATTR_CMDLINE, generateCommandLine(commands));

    if (enableDebugging) {
      ServerDebugTarget debugTarget = new ServerDebugTarget(launch, eclipseProcess, connectionPort);

      launch.addDebugTarget(debugTarget);

      debugTarget.connect();
    }

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

  private File getCurrentWorkingDirectory(DartLaunchConfigWrapper launchConfig) {
    IResource resource = launchConfig.getApplicationResource();

    if (resource == null) {
      if (launchConfig.getProject() != null) {
        return launchConfig.getProject().getLocation().toFile();
      }
    } else {
      if (resource.isLinked()) {
        // If the resource is linked, set the cwd to the parent directory of the resolved resource.
        return resource.getLocation().toFile().getParentFile();
      } else {
        // If the resource is not linked, set the cwd to the project's directory.
        return resource.getProject().getLocation().toFile();
      }
    }
    return null;
  }

  private String translateToFilePath(String scriptPath) {
    IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(scriptPath);

    if (resource != null) {
      return resource.getLocation().toFile().getAbsolutePath();
    } else {
      return scriptPath;
    }
  }

}
