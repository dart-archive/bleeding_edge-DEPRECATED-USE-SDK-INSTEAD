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
package com.google.dart.tools.debug.core.util;

import com.google.dart.tools.core.model.DartSdk;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.core.dartium.DartiumDebugTarget;
import com.google.dart.tools.debug.core.webkit.ChromiumConnector;
import com.google.dart.tools.debug.core.webkit.ChromiumTabInfo;
import com.google.dart.tools.debug.core.webkit.WebkitConnection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A manager that launches and manages configured browsers.
 */
public class BrowserManager {
  private static final int PORT_NUMBER = 9222;

  private static BrowserManager manager = new BrowserManager();

  private static HashMap<String, Process> browserProcesses = new HashMap<String, Process>();

  public static BrowserManager getManager() {
    return manager;
  }

  private BrowserManager() {

  }

  public void dispose() {
    for (Process process : browserProcesses.values()) {
      if (!isProcessTerminated(process)) {
        process.destroy();
      }
    }
  }

  /**
   * Launch the browser and open the given file. If debug mode also connect to browser.
   */
  public void launchBrowser(ILaunch launch, DartLaunchConfigWrapper launchConfig, IFile file,
      IProgressMonitor monitor, boolean debug) throws CoreException {
    launchBrowser(launch, launchConfig, file, null, monitor, debug);
  }

  /**
   * Launch the browser and open the given url. If debug mode also connect to browser.
   */
  public void launchBrowser(ILaunch launch, DartLaunchConfigWrapper launchConfig, String url,
      IProgressMonitor monitor, boolean debug) throws CoreException {
    launchBrowser(launch, launchConfig, null, url, monitor, debug);
  }

  protected void launchBrowser(ILaunch launch, DartLaunchConfigWrapper launchConfig, IFile file,
      String url, IProgressMonitor monitor, boolean debug) throws CoreException {

    monitor.beginTask("Launching Chromium...", debug ? 7 : 2);

    File dartium = DartSdk.getInstance().getDartiumExecutable();

    if (dartium == null) {
      throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
          "Could not find Chromium"));
    }

    IPath browserLocation = new Path(dartium.getAbsolutePath());

    String browserName = dartium.getName();

    // avg: 0.434 sec (old: 0.597)
    LogTimer timer = new LogTimer("Dartium debug startup");

    // avg: 55ms
    timer.startTask(browserName + " startup");

    // for now, check if browser is open, if so, exit and restart again
    if (browserProcesses.containsKey(browserName)) {
      Process process = browserProcesses.get(browserName);

      if (!isProcessTerminated(process)) {
        process.destroy();

        // The process needs time to exit.
        waitForProcessToTerminate(process, 200);
      }

      browserProcesses.remove(browserName);
    }

    Process runtimeProcess = null;
    monitor.worked(1);

    ProcessBuilder builder = new ProcessBuilder();
    Map<String, String> env = builder.environment();
    // Due to differences in 32bit and 64 bit environments, dartium 32bit launch does not work on
    // linux with this property.
    env.remove("LD_LIBRARY_PATH");

    // Add the environment variable DART_FLAGS="--enable_asserts --enable_type_checks".
    if (launchConfig.getCheckedMode()) {
      env.put("DART_FLAGS", "--enable_asserts --enable_type_checks");
    }

    IResourceResolver resourceResolver = null;

    if (DartDebugCorePlugin.ENABLE_DEBUGGING) {
      // Start the embedded web server. It is used to serve files from our workspace.
      if (file != null) {
        try {
          ResourceServer server = ResourceServerManager.getServer();

          url = server.getUrlForResource(file);

          resourceResolver = server;
        } catch (IOException exception) {
          throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
              "Could not launch browser - unable to start embedded server", exception));
        }
      }
    } else {
      if (file != null) {
        url = file.getLocationURI().toString();
      }
    }

    List<String> arguments = buildArgumentsList(browserLocation, url, debug);
    builder.command(arguments);
    builder.directory(new File(DartSdk.getInstance().getDartiumWorkingDirectory()));

    try {
      runtimeProcess = builder.start();
    } catch (IOException e) {
      DebugPlugin.logMessage("Exception while starting browser", e);

      throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
          "Could not launch browser"));
    }

    browserProcesses.put(browserName, runtimeProcess);

    readFromProcessPipes(browserName, runtimeProcess.getInputStream());
    readFromProcessPipes(browserName, runtimeProcess.getErrorStream());

    monitor.worked(1);

    if (isProcessTerminated(runtimeProcess)) {
      throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
          "Could not launch browser"));
    }

    timer.stopTask();

    connectToChromiumDebug(browserName, launch, launchConfig, url, monitor, runtimeProcess,
        resourceResolver, timer);

    timer.stopTimer();
    monitor.done();
  }

  /**
   * Launch browser and open file url. If debug mode also connect to browser.
   */
  void connectToChromiumDebug(String browserName, ILaunch launch,
      DartLaunchConfigWrapper launchConfig, String url, IProgressMonitor monitor,
      Process runtimeProcess, IResourceResolver resourceResolver, LogTimer timer)
      throws CoreException {
    monitor.worked(1);

    try {
      // avg: 383ms
      timer.startTask("get chromium tabs");

      List<ChromiumTabInfo> tabs = getChromiumTabs(runtimeProcess);

      monitor.worked(2);

      timer.stopTask();

      // avg: 46ms
      timer.startTask("open WIP connection");

      if (tabs.size() == 0 || tabs.get(0).getWebSocketDebuggerUrl() == null) {
        throw new DebugException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
            "Unable to connect to Dartium"));
      }

      WebkitConnection connection = new WebkitConnection(tabs.get(0).getWebSocketDebuggerUrl());

      DartiumDebugTarget debugTarget = new DartiumDebugTarget(browserName, connection, launch,
          runtimeProcess, resourceResolver);

      monitor.worked(1);

      if (DartDebugCorePlugin.ENABLE_DEBUGGING) {
        launch.addDebugTarget(debugTarget);
      }
      launch.addProcess(debugTarget.getProcess());

      debugTarget.openConnection(url);

      timer.stopTask();
    } catch (IOException e) {
      DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);

      throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
          e.toString(), e));
    }

    monitor.worked(1);
  }

  private List<String> buildArgumentsList(IPath browserLocation, String url, boolean debug) {
    List<String> arguments = new ArrayList<String>();

    arguments.add(browserLocation.toOSString());

    // Enable remote debug over HTTP on the specified port.
    arguments.add("--remote-debugging-port=" + PORT_NUMBER);

    // In order to start up multiple Chrome processes, we need to specify a different user dir.
    arguments.add("--user-data-dir=" + getUserDataDirectoryPath());

    //arguments.add("--disable-breakpad");

    // Indicates that the browser is in "browse without sign-in" (Guest session) mode. Should 
    // completely disable extensions, sync and bookmarks.
    arguments.add("--bwsi");

    // On ChromeOS, file:// access is disabled except for certain whitelisted directories. This
    // switch re-enables file:// for testing.
    //arguments.add("--allow-file-access");

    // By default, file:// URIs cannot read other file:// URIs. This is an override for developers
    // who need the old behavior for testing
    //arguments.add("--allow-file-access-from-files");

    // Whether or not it's actually the first run.
    arguments.add("--no-first-run");

    // Disables the default browser check.
    arguments.add("--no-default-browser-check");

    // Bypass the error dialog when the profile lock couldn't be attained.
    arguments.add("--no-process-singleton-dialog");

    // Causes the browser to launch directly into incognito mode.
    // We use this to prevent the previous session's tabs from re-opening.
    arguments.add("--incognito");

    if (DartDebugCorePlugin.ENABLE_DEBUGGING) {
      // Start up with a blank page.
      arguments.add("--homepage=about:blank");
    } else {
      arguments.add(url);
    }

    return arguments;
  }

  private List<ChromiumTabInfo> getChromiumTabs(Process runtimeProcess) throws IOException,
      CoreException {
    // Give Chromium a maximum of 10 seconds to start up.
    final int maxStartupDelay = 10 * 1000;

    long startTime = System.currentTimeMillis();

    while (true) {
      if (isProcessTerminated(runtimeProcess)) {
        throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
            "Could not launch browser"));
      }

      try {
        return ChromiumConnector.getAvailableTabs(PORT_NUMBER);
      } catch (IOException exception) {
        if ((System.currentTimeMillis() - startTime) > maxStartupDelay) {
          throw exception;
        } else {
          sleep(25);
        }
      }
    }
  }

  /**
   * Create a Chrome user data directory, and return the path to that directory.
   * 
   * @return the user data directory path
   */
  private String getUserDataDirectoryPath() {
    String dataDirPath = System.getProperty("user.home") + File.separator + ".dartChromeSettings";

    File dataDir = new File(dataDirPath);

    if (!dataDir.exists()) {
      dataDir.mkdir();
    }

    return dataDirPath;
  }

  private boolean isProcessTerminated(Process process) {
    try {
      process.exitValue();

      return true;
    } catch (IllegalThreadStateException ex) {
      return false;
    }
  }

  private void readFromProcessPipes(final String processName, final InputStream in) {
    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        byte[] buffer = new byte[2048];

        try {
          int count = in.read(buffer);

          while (count != -1) {
            if (count > 0) {
              String str = new String(buffer, 0, count);

              // Log any browser process output to the debug log.
              DartDebugCorePlugin.logInfo(processName + ": " + str.trim());
            }

            count = in.read(buffer);
          }

          in.close();
        } catch (IOException ioe) {
          // When the process closes, we do not want to print any errors.
        }
      }
    }, "Read from " + processName);

    thread.start();
  }

  private void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (Exception exception) {
    }
  }

  private void waitForProcessToTerminate(Process process, int maxWaitTimeMs) {
    long startTime = System.currentTimeMillis();

    while ((System.currentTimeMillis() - startTime) < maxWaitTimeMs) {
      if (isProcessTerminated(process)) {
        return;
      }

      sleep(10);
    }
  }

}
