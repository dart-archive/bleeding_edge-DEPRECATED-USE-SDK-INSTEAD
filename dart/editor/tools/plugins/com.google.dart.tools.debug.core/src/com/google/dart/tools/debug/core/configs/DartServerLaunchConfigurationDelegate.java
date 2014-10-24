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
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.core.pub.IPackageRootProvider;
import com.google.dart.tools.core.utilities.net.NetUtils;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.core.DartLaunchConfigurationDelegate;
import com.google.dart.tools.debug.core.coverage.CoverageManager;
import com.google.dart.tools.debug.core.server.ServerDebugTarget;
import com.google.dart.tools.debug.core.server.ServerRemoteProcess;
import com.google.dart.tools.debug.core.util.CoreLaunchUtils;
import com.google.dart.tools.debug.core.util.IRemoteConnectionDelegate;

import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Dart Server Application launch configuration.
 */
public class DartServerLaunchConfigurationDelegate extends DartLaunchConfigurationDelegate
    implements IRemoteConnectionDelegate {
  private static final int DEFAULT_PORT_NUMBER = 5858;

  private IPackageRootProvider packageRootProvider;

  private int observatoryPort = -1;

  /**
   * Create a new DartServerLaunchConfigurationDelegate.
   */
  public DartServerLaunchConfigurationDelegate() {
    this(IPackageRootProvider.DEFAULT);
  }

  public DartServerLaunchConfigurationDelegate(IPackageRootProvider packageRootProvider) {
    this.packageRootProvider = packageRootProvider;
  }

  @Override
  public void doLaunch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor, InstrumentationBuilder instrumentation) throws CoreException {

    DartLaunchConfigWrapper launchConfig = new DartLaunchConfigWrapper(
        configuration.getWorkingCopy());

    launchConfig.markAsLaunched();

    boolean enableDebugging = ILaunchManager.DEBUG_MODE.equals(mode)
        && !DartCoreDebug.DISABLE_CLI_DEBUGGER;

    terminateSameLaunches(launch);

    launchVM(launch, launchConfig, enableDebugging, monitor);

  }

  @Override
  public IDebugTarget performRemoteConnection(String host, int port, IProgressMonitor monitor,
      boolean usePubServe) throws CoreException {
    if (monitor == null) {
      monitor = new NullProgressMonitor();
    }

    ILaunch launch = CoreLaunchUtils.createTemporaryLaunch(
        DartDebugCorePlugin.SERVER_LAUNCH_CONFIG_ID,
        host + "[" + port + "]");

    monitor.beginTask("Opening Connection...", 1);

    try {
      CoreLaunchUtils.addLaunch(launch);

      ServerRemoteProcess process = new ServerRemoteProcess(launch);

      ServerDebugTarget debugTarget = new ServerDebugTarget(launch, process, host, port);

      process.setTarget(debugTarget);

      process.fireCreateEvent();

      debugTarget.connect();

      monitor.worked(1);

      launch.addDebugTarget(debugTarget);

      return debugTarget;
    } catch (CoreException ce) {
      CoreLaunchUtils.removeLaunch(launch);

      throw ce;
    } finally {
      monitor.done();
    }
  }

  protected void launchVM(ILaunch launch, DartLaunchConfigWrapper launchConfig,
      boolean enableDebugging, IProgressMonitor monitor) throws CoreException {
    // Usage: dart [options] script.dart [arguments]

    File currentWorkingDirectory = getCurrentWorkingDirectory(launchConfig);

    String scriptPath = launchConfig.getApplicationName();

    scriptPath = translateToFilePath(currentWorkingDirectory, scriptPath);

    String vmExecPath = "";

    if (DartSdkManager.getManager().hasSdk()) {
      File vmExec = DartSdkManager.getManager().getSdk().getVmExecutable();

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

    if (enableDebugging) {
      commandsList.add("--debug:" + connectionPort);
    }

    observatoryPort = NetUtils.findUnusedPort(0);

    launchConfig.setObservatoryPort(observatoryPort);
    launchConfig.save();

    commandsList.add("--enable-vm-service:" + observatoryPort);
    commandsList.add("--trace_service_pause_events");

    if (launchConfig.getPauseIsolateOnExit()) {
      commandsList.add("--pause-isolates-on-exit");
    }

    if (launchConfig.getPauseIsolateOnStart()) {
      commandsList.add("--pause-isolates-on-start");
    }

    // This lets us debug isolates.
    commandsList.add("--break-at-isolate-spawn");

    if (DartCoreDebug.ENABLE_ASYNC) {
      commandsList.add("--enable_async");
    }

    String coverageTempDir = null;
    if (DartCoreDebug.ENABLE_COVERAGE) {
      coverageTempDir = CoverageManager.createTempDir();
      commandsList.add("--coverage_dir=" + coverageTempDir);
    }

    File packageRoot = packageRootProvider.getPackageRoot(launchConfig.getProject());
    if (packageRoot != null) {
      String packageRootString = packageRoot.getAbsolutePath();
      String fileSeparator = System.getProperty("file.separator");
      if (!packageRootString.endsWith(fileSeparator)) {
        packageRootString += fileSeparator;
      }
      commandsList.add("--package-root=" + packageRootString);
    }

    commandsList.add(scriptPath);
    commandsList.addAll(Arrays.asList(launchConfig.getArgumentsAsArray()));
    String[] commands = commandsList.toArray(new String[commandsList.size()]);
    ProcessBuilder processBuilder = new ProcessBuilder(commands);

    if (currentWorkingDirectory != null) {
      processBuilder.directory(currentWorkingDirectory);
    }

    Process runtimeProcess = null;

    try {
      runtimeProcess = processBuilder.start();
      if (coverageTempDir != null) {
        CoverageManager.registerProcess(
            coverageTempDir,
            launchConfig.getApplicationName(),
            runtimeProcess);
      }
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
    processAttributes.put(IProcess.ATTR_CMDLINE, describe(processBuilder));

    if (runtimeProcess != null) {
      monitor.beginTask("Dart", IProgressMonitor.UNKNOWN);

      eclipseProcess = DebugPlugin.newProcess(
          launch,
          runtimeProcess,
          launchConfig.getApplicationName() + " (" + new Date() + ")",
          processAttributes);
    }

    if (runtimeProcess == null || eclipseProcess == null) {
      if (runtimeProcess != null) {
        runtimeProcess.destroy();
      }

      throw new CoreException(
          DartDebugCorePlugin.createErrorStatus("Error starting Dart VM process"));
    }

    eclipseProcess.setAttribute(IProcess.ATTR_CMDLINE, describe(processBuilder));

    if (enableDebugging) {
      ServerDebugTarget debugTarget = new ServerDebugTarget(launch, eclipseProcess, connectionPort);

      try {
        debugTarget.connect();

        launch.addDebugTarget(debugTarget);
      } catch (DebugException ex) {
        // We don't throw an exception if the process died before we could connect.
        if (!isProcessDead(runtimeProcess)) {
          throw ex;
        }
      }
    }

    monitor.done();
  }

  private String describe(ProcessBuilder processBuilder) {
    StringBuilder builder = new StringBuilder();

    for (String arg : processBuilder.command()) {
      builder.append(arg);
      builder.append(" ");
    }

    return builder.toString().trim();
  }

  private File getCurrentWorkingDirectory(DartLaunchConfigWrapper launchConfig) {
    if (launchConfig.getWorkingDirectory().length() > 0) {
      String cwd = launchConfig.getWorkingDirectory();

      return new File(cwd);
    } else {
      IResource resource = launchConfig.getApplicationResource();

      if (resource == null) {
        if (launchConfig.getProject() != null) {
          return launchConfig.getProject().getLocation().toFile();
        } else {
          return null;
        }
      } else {
        if (resource.isLinked()) {
          // If the resource is linked, set the cwd to the parent directory of the resolved resource.
          return resource.getLocation().toFile().getParentFile();
        } else {
          // If the resource is not linked, set the cwd to the resource's parent directory.
          return resource.getParent().getLocation().toFile();
        }
      }
    }
  }

  private boolean isProcessDead(Process process) {
    try {
      process.exitValue();

      return true;
    } catch (IllegalThreadStateException ex) {
      return false;
    }
  }

  private void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {

    }
  }

  @SuppressWarnings("deprecation")
  private void terminateSameLaunches(ILaunch currentLaunch) {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();

    boolean launchTerminated = false;

    for (ILaunch launch : manager.getLaunches()) {
      if (ObjectUtils.equals(
          launch.getLaunchConfiguration(),
          currentLaunch.getLaunchConfiguration())) {
        try {
          launchTerminated = true;
          launch.terminate();
        } catch (DebugException e) {
          DartDebugCorePlugin.logError(e);
        }
      }
    }

    if (launchTerminated) {
      // Wait a while for processes to shutdown.
      sleep(100);
    }
  }

  /**
   * Return either a path relative to the cwd, if possible, or an absolute path to the given script.
   * 
   * @param cwd the current working directory for the launch
   * @param scriptPath the path to the script (a workspace path)
   * @return either a cwd relative path or an absolute path
   */
  private String translateToFilePath(File cwd, String scriptPath) {
    IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(scriptPath);

    if (resource != null) {
      String path = resource.getLocation().toFile().getAbsolutePath();

      if (cwd != null) {
        String cwdPath = cwd.getAbsolutePath();

        if (!cwdPath.endsWith(File.separator)) {
          cwdPath = cwdPath + File.separator;
        }

        if (path.startsWith(cwdPath)) {
          path = path.substring(cwdPath.length());
        }
      }

      return path;
    } else {
      return scriptPath;
    }
  }

}
