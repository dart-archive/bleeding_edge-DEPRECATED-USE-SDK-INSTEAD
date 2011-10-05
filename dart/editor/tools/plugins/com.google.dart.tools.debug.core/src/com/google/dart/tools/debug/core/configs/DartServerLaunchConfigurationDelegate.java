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

import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Dart Server Application launch configuration.
 */
public class DartServerLaunchConfigurationDelegate extends LaunchConfigurationDelegate {
  private static final String RHINO_PLUGIN_PATH = "rhino/js.jar";

  private static IPath rhinoPath;

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

    String serverRunner = launchConfig.getServerRunner();

    if (DartLaunchConfigWrapper.SERVER_RUNNER_NODEJS.equals(serverRunner)) {
      launchNode(launch, launchConfig, monitor, mode.equals(ILaunchManager.RUN_MODE));
    } else if (DartLaunchConfigWrapper.SERVER_RUNNER_RHINO.equals(serverRunner)) {
      if (mode.equals(ILaunchManager.RUN_MODE)) {
        launchRhinoRunMode(launch, launchConfig, monitor);
      } else {
        throw new CoreException(
            DartDebugCorePlugin.createErrorStatus("Rhino is not a supported debug target"));
      }
    } else {
      throw new CoreException(DartDebugCorePlugin.createErrorStatus("Server runner '"
          + serverRunner + "' not supported."));
    }
  }

  protected void launchNode(ILaunch launch, DartLaunchConfigWrapper launchConfig,
      IProgressMonitor monitor, boolean runMode) throws CoreException {
    if (!runMode) {
      // TODO(devoncarew): implement this
      throw new CoreException(
          DartDebugCorePlugin.createErrorStatus("Node launch config does not yet support debugging."));
    }

    // Usage: node [options] script.js [arguments]
    //        node debug script.js [arguments]

    DartProject project = launchConfig.getProject();

    File outputDirectory = project.getOutputLocation().makeAbsolute().toFile();

    String scriptPath = getFileName(launchConfig.getApplicationName()) + ".js";

    String nodeExecPath = DartDebugCorePlugin.getPlugin().getNodeExecutablePath();

    if (nodeExecPath.length() == 0) {
      throw new CoreException(
          DartDebugCorePlugin.createErrorStatus("The node executable path has not been set"));
    }

    // /usr/local/bin/node
    String[] commands = new String[] {nodeExecPath, scriptPath};

    commands = combineArrays(commands, launchConfig.getArgumentsAsArray());

    ProcessBuilder processBuilder = new ProcessBuilder(Arrays.asList(commands));

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

    String programName = "node";
    processAttributes.put(IProcess.ATTR_PROCESS_TYPE, programName);

    if (runtimeProcess != null) {
      monitor.beginTask("Node.js", IProgressMonitor.UNKNOWN);

      eclipseProcess = DebugPlugin.newProcess(launch, runtimeProcess,
          launchConfig.getApplicationName(), processAttributes);
    }

    if (runtimeProcess == null || eclipseProcess == null) {
      if (runtimeProcess != null) {
        runtimeProcess.destroy();
      }

      throw new CoreException(DartDebugCorePlugin.createErrorStatus("Error starting node process"));
    }

    eclipseProcess.setAttribute(IProcess.ATTR_CMDLINE, generateCommandLine(commands));

    // wait for process to exit
    while (!eclipseProcess.isTerminated()) {
      try {
        if (monitor.isCanceled()) {
          eclipseProcess.terminate();
          break;
        }
        Thread.sleep(50);
      } catch (InterruptedException e) {
      }
    }
  }

  protected void launchRhinoRunMode(ILaunch launch, DartLaunchConfigWrapper launchConfig,
      IProgressMonitor monitor) throws CoreException {
    if (rhinoPath == null) {
      try {
        rhinoPath = getRhinoLibPath();
      } catch (IOException ioe) {
        throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
            "Error loading Rhino jar", ioe));
      }
    }

    // {"javaw", "javaw.exe", "java", "java.exe", "j9w", "j9w.exe", "j9", "j9.exe"}

    // java -cp path org.mozilla.javascript.tools.shell.Main filename.js scriptarg0 scriptarg1 scriptarg2

    DartProject project = launchConfig.getProject();

    File outputDirectory = project.getOutputLocation().makeAbsolute().toFile();

    //project.getOutputLocation()
    String scriptPath = getFileName(launchConfig.getApplicationName()) + ".js";

    // /usr/bin/java
    String javaExecPath = DartDebugCorePlugin.getPlugin().getJreExecutablePath();

    if (javaExecPath.length() == 0) {
      throw new CoreException(
          DartDebugCorePlugin.createErrorStatus("The java executable path has not been set"));
    }

    String[] commands = new String[] {
        javaExecPath, "-cp", rhinoPath.toOSString(), "org.mozilla.javascript.tools.shell.Main",
        scriptPath};

    commands = combineArrays(commands, launchConfig.getArgumentsAsArray());

    ProcessBuilder processBuilder = new ProcessBuilder(Arrays.asList(commands));

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

    String programName = "rhino";
    processAttributes.put(IProcess.ATTR_PROCESS_TYPE, programName);

    if (runtimeProcess != null) {
      monitor.beginTask("Rhino", IProgressMonitor.UNKNOWN);

      eclipseProcess = DebugPlugin.newProcess(launch, runtimeProcess,
          launchConfig.getApplicationName(), processAttributes);
    }

    if (runtimeProcess == null || eclipseProcess == null) {
      if (runtimeProcess != null) {
        runtimeProcess.destroy();
      }

      throw new CoreException(DartDebugCorePlugin.createErrorStatus("Error starting rhino process"));
    }

    eclipseProcess.setAttribute(IProcess.ATTR_CMDLINE, generateCommandLine(commands));

    // wait for process to exit
    while (!eclipseProcess.isTerminated()) {
      try {
        if (monitor.isCanceled()) {
          eclipseProcess.terminate();
          break;
        }
        Thread.sleep(50);
      } catch (InterruptedException e) {
      }
    }
  }

  private String[] combineArrays(String[] strs1, String[] strs2) {
    List<String> strs = new ArrayList<String>();

    strs.addAll(Arrays.asList(strs1));
    strs.addAll(Arrays.asList(strs2));

    return strs.toArray(new String[strs.size()]);
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

  private String getFileName(String path) {
    int index = path.lastIndexOf('/');

    if (index != -1) {
      path = path.substring(index + 1);
    }

    return path;
  }

  private IPath getRhinoLibPath() throws IOException {
    InputStream in = FileLocator.openStream(DartDebugCorePlugin.getPlugin().getBundle(), new Path(
        RHINO_PLUGIN_PATH), false);

    IPath stateLocation = DartDebugCorePlugin.getPlugin().getStateLocation();
    IPath outPath = stateLocation.append("rhino.jar");

    FileOutputStream out = new FileOutputStream(outPath.toFile());
    byte[] buffer = new byte[10240];

    int count = in.read(buffer);

    while (count != -1) {
      out.write(buffer, 0, count);
      count = in.read(buffer);
    }

    try {
      out.close();
      in.close();
    } catch (IOException ioe) {
      // ignore any exceptions from the close methods - we've already copied the library

    }

    return outPath;
  }
}
