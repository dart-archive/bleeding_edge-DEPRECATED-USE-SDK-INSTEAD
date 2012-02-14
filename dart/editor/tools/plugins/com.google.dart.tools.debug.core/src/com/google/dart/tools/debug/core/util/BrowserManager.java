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
      if (!processTerminated(process)) {
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

  /**
   * Launch browser and open file url. If debug mode also connect to browser.
   */
  protected void connectToChromiumDebug(String browserName, ILaunch launch,
      DartLaunchConfigWrapper launchConfig, String url, IProgressMonitor monitor,
      Process javaProcess, IResourceResolver resourceResolver) throws CoreException {
    monitor.worked(1);

    try {
      LogTimer timer = new LogTimer("get chromium tabs");

      List<ChromiumTabInfo> tabs = getChromiumTabs();

      monitor.worked(2);

      timer.endTimer();

      timer = new LogTimer("open WIP connection");

      if (tabs.size() == 0 || tabs.get(0).getWebSocketDebuggerUrl() == null) {
        throw new DebugException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
            "Unable to connect to Dartium"));
      }

      WebkitConnection connection = new WebkitConnection(tabs.get(0).getWebSocketDebuggerUrl());

      DartiumDebugTarget debugTarget = new DartiumDebugTarget(browserName, connection, launch,
          javaProcess, resourceResolver);

      monitor.worked(1);

      if (DartDebugCorePlugin.ENABLE_DEBUGGING) {
        launch.addDebugTarget(debugTarget);
      }
      launch.addProcess(debugTarget.getProcess());

      debugTarget.openConnection(url);

      timer.endTimer();
    } catch (IOException e) {
      DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);

      throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
          e.toString(), e));
    }

    monitor.worked(1);
  }

  protected void launchBrowser(ILaunch launch, DartLaunchConfigWrapper launchConfig, IFile file,
      String url, IProgressMonitor monitor, boolean debug) throws CoreException {

    monitor.beginTask("Launching Chromium...", debug ? 7 : 3);

    File dartium = DartSdk.getInstance().getDartiumExecutable();

    if (dartium == null) {
      throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
          "Could not find Chromium"));
    }

    IPath browserLocation = new Path(dartium.getAbsolutePath());

    String browserName = dartium.getName();
    LogTimer timer = new LogTimer(browserName + " startup");

    // for now, check if browser is open, if so, exit and restart again
    if (browserProcesses.containsKey(browserName)) {
      Process process = browserProcesses.get(browserName);

      if (!processTerminated(process)) {
        process.destroy();
        browserProcesses.remove(browserName);

        // The process needs time to exit.
        sleep(100);
      }
    }

    Process javaProcess = null;
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
      javaProcess = builder.start();
    } catch (IOException e) {
      DebugPlugin.logMessage("Exception while starting browser", e);

      throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
          "Could not launch browser"));
    }

    browserProcesses.put(browserName, javaProcess);

    readFromProcessPipes(browserName, javaProcess.getInputStream());
    readFromProcessPipes(browserName, javaProcess.getErrorStream());

    timer.endTimer();

    timer = new LogTimer("chromium startup delay");

    monitor.worked(1);

    // Check to see if the process exits soon after starting up, and if so stop the debug launch
    // process.
    sleep(100);

    monitor.worked(1);

    if (processTerminated(javaProcess)) {
      throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
          "Could not launch browser"));
    }

    timer.endTimer();

    connectToChromiumDebug(browserName, launch, launchConfig, url, monitor, javaProcess,
        resourceResolver);

    monitor.done();
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

    if (DartDebugCorePlugin.ENABLE_DEBUGGING) {
      // Causes the browser to launch directly into incognito mode.
      // We use this to prevent the previous session's tabs from re-opening.
      arguments.add("--incognito");

      // Start up with a blank page.
      arguments.add("--homepage=about:blank");
    } else {
      arguments.add(url);
    }

    return arguments;
  }

  private List<ChromiumTabInfo> getChromiumTabs() throws IOException {
    // Give Chromium 5 seconds to start up.
    final int maxFailureCount = 50;

    int failureCount = 0;

    while (true) {
      try {
        return ChromiumConnector.getAvailableTabs(PORT_NUMBER);
      } catch (IOException exception) {
        failureCount++;

        if (failureCount >= maxFailureCount) {
          throw exception;
        } else {
          sleep(100);
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

  private boolean processTerminated(Process process) {
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

}
