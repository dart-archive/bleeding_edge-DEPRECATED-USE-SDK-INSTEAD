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

import com.google.dart.tools.core.DartCore;
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
import java.util.List;
import java.util.Map;

/**
 * A manager that launches and manages configured browsers.
 */
public class BrowserManager {

  private static final int DEVTOOLS_PORT_NUMBER = 9322;

  private static BrowserManager manager = new BrowserManager();

  private static Process browserProcess = null;

  private static boolean firstLaunch = true;

  public static BrowserManager getManager() {
    return manager;
  }

  private StringBuilder stdout;

  private StringBuilder stderr;

  private IResourceResolver resourceResolver;

  private int devToolsPortNumber;

  private BrowserManager() {

  }

  public void dispose() {
    if (!isProcessTerminated(browserProcess)) {
      browserProcess.destroy();
    }
  }

  /**
   * Launch the browser and open the given file. If debug mode also connect to browser.
   */
  public void launchBrowser(ILaunch launch, DartLaunchConfigWrapper launchConfig, IFile file,
      IProgressMonitor monitor, boolean enableDebugging) throws CoreException {
    launchBrowser(launch, launchConfig, file, null, monitor, enableDebugging);
  }

  /**
   * Launch the browser and open the given url. If debug mode also connect to browser.
   */
  public void launchBrowser(ILaunch launch, DartLaunchConfigWrapper launchConfig, String url,
      IProgressMonitor monitor, boolean enableDebugging) throws CoreException {
    launchBrowser(launch, launchConfig, null, url, monitor, enableDebugging);
  }

  protected void launchBrowser(ILaunch launch, DartLaunchConfigWrapper launchConfig, IFile file,
      String url, IProgressMonitor monitor, boolean enableDebugging) throws CoreException {

    // For now, we always start a debugging connection, even when we're not really debugging.
    boolean enableBreakpoints = enableDebugging;

    monitor.beginTask("Launching Dartium...", enableDebugging ? 7 : 2);

    File dartium = DartSdk.getInstance().getDartiumExecutable();

    if (dartium == null) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          "Could not find Dartium"));
    }

    IPath browserLocation = new Path(dartium.getAbsolutePath());

    String browserName = dartium.getName();

    // avg: 0.434 sec (old: 0.597)
    LogTimer timer = new LogTimer("Dartium debug startup");

    // avg: 55ms
    timer.startTask(browserName + " startup");

    resourceResolver = null;
    url = startEmbeddedServer(file, url);

    // for now, check if browser is open, and connection is alive
    boolean restart = browserProcess == null || isProcessTerminated(browserProcess)
        || DartiumDebugTarget.getActiveTarget() == null
        || !DartiumDebugTarget.getActiveTarget().canTerminate();

    if (!restart) {

      DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);

      try {
        DartiumDebugTarget.getActiveTarget().navigateToUrl(url);
      } catch (IOException e) {
        DartDebugCorePlugin.logError(e);
      }
    } else {

      terminateExistingBrowserProcess();

      startNewBrowserProcess(
          launchConfig, url, monitor, enableDebugging, browserLocation, browserName);

      sleep(100);

      monitor.worked(1);

      if (isProcessTerminated(browserProcess)) {
        DartDebugCorePlugin.logError("Dartium stdout: " + stdout);
        DartDebugCorePlugin.logError("Dartium stderr: " + stderr);

        throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
            "Could not launch browser - process terminated on startup"
            + getProcessStreamMessage()));
      }

      connectToChromiumDebug(browserName, launch, launchConfig, url, monitor, browserProcess,
          resourceResolver, timer, enableBreakpoints, devToolsPortNumber);
    }

    stdout = readFromProcessPipes(browserName, browserProcess.getInputStream());
    stderr = readFromProcessPipes(browserName, browserProcess.getErrorStream());

    BrowserHelper.activateApplication(dartium);

    timer.stopTask();
    timer.stopTimer();
    monitor.done();
  }

  /**
   * Launch browser and open file url. If debug mode also connect to browser.
   */
  void connectToChromiumDebug(String browserName, ILaunch launch,
      DartLaunchConfigWrapper launchConfig, String url, IProgressMonitor monitor,
      Process runtimeProcess, IResourceResolver resourceResolver, LogTimer timer,
      boolean enableBreakpoints, int devToolsPortNumber) throws CoreException {
    monitor.worked(1);

    try {
      // avg: 383ms
      timer.startTask("get chromium tabs");

      List<ChromiumTabInfo> tabs = getChromiumTabs(runtimeProcess, devToolsPortNumber);

      monitor.worked(2);

      timer.stopTask();

      // avg: 46ms
      timer.startTask("open WIP connection");

      if (tabs.size() == 0) {
        throw new DebugException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            "Unable to connect to Dartium"));
      }

      ChromiumTabInfo chromiumTab = tabs.get(0);

      if (chromiumTab == null || chromiumTab.getWebSocketDebuggerUrl() == null) {
        throw new DebugException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            "Unable to connect to Chromium"));
      }

      WebkitConnection connection = new WebkitConnection(chromiumTab.getWebSocketDebuggerUrl());

      DartiumDebugTarget debugTarget = new DartiumDebugTarget(
          browserName,
          connection,
          launch,
          runtimeProcess,
          resourceResolver,
          enableBreakpoints);

      monitor.worked(1);

      launch.addDebugTarget(debugTarget);
      launch.addProcess(debugTarget.getProcess());

      debugTarget.openConnection(url);

      timer.stopTask();
    } catch (IOException e) {
      DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);

      throw new CoreException(new Status(
          IStatus.ERROR, 
          DartDebugCorePlugin.PLUGIN_ID, 
          e.toString(), 
          e));
    }

    if (firstLaunch) {
      firstLaunch = false;
    }

    monitor.worked(1);
  }

  private List<String> buildArgumentsList(IPath browserLocation, String url,
      boolean enableDebugging, int devToolsPortNumber) {
    List<String> arguments = new ArrayList<String>();

    arguments.add(browserLocation.toOSString());

    // Enable remote debug over HTTP on the specified port.
    arguments.add("--remote-debugging-port=" + devToolsPortNumber);

    // In order to start up multiple Chrome processes, we need to specify a different user dir.
    arguments.add("--user-data-dir=" + getCreateUserDataDirectoryPath());

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
    //arguments.add("--incognito");

    if (enableDebugging) {
      // Start up with a blank page.
      arguments.add("about:blank");
    } else {
      arguments.add(url);
    }

    return arguments;
  }

  private ChromiumTabInfo findTargetTab(List<ChromiumTabInfo> tabs) {
    final String aboutBlank = "about:blank";

    for (ChromiumTabInfo tab : tabs) {
      if (tab.getTitle().contains(aboutBlank)) {
        return tab;
      }

      if (tab.getUrl().contains(aboutBlank)) {
        return tab;
      }
    }

    if (tabs.size() == 1) {
      return tabs.get(0);
    }

    return null;
  }

  private List<ChromiumTabInfo> getChromiumTabs(Process runtimeProcess, int devToolsPortNumber)
      throws IOException, CoreException {
    // Give Chromium 25 seconds to start up.
    final int maxStartupDelay = 25 * 1000;

    long endTime = System.currentTimeMillis() + maxStartupDelay;

    while (true) {
      if (isProcessTerminated(runtimeProcess)) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            "Could not launch browser - process terminated while trying to connect"
            + getProcessStreamMessage()));
      }

      try {
        List<ChromiumTabInfo> tabs = ChromiumConnector.getAvailableTabs(devToolsPortNumber);

        if (findTargetTab(tabs) != null) {
          return tabs;
        }
      } catch (IOException exception) {
        if (System.currentTimeMillis() > endTime) {
          throw exception;
        }
      }

      if (System.currentTimeMillis() > endTime) {
        throw new IOException("Timed out trying to connect to Dartium");
      }

      sleep(25);
    }
  }

  /**
   * Create a Chrome user data directory, and return the path to that directory.
   * 
   * @return the user data directory path
   */
  private String getCreateUserDataDirectoryPath() {
    String dataDirPath = System.getProperty("user.home") + File.separator + ".dartiumSettings";

    File dataDir = new File(dataDirPath);

    if (!dataDir.exists()) {
      dataDir.mkdir();
    } else {
      // Remove the "<dataDir>/Default/Current Tabs" file if it exists - it can cause old tabs to
      // restore themselves when we launch the browser.
      File defaultDir = new File(dataDir, "Default");

      if (defaultDir.exists()) {
        File tabInfoFile = new File(defaultDir, "Current Tabs");

        if (tabInfoFile.exists()) {
          tabInfoFile.delete();
        }

        File sessionInfoFile = new File(defaultDir, "Current Session");

        if (sessionInfoFile.exists()) {
          sessionInfoFile.delete();
        }
      }
    }

    return dataDirPath;
  }

  private String getProcessStreamMessage() {
    StringBuilder msg = new StringBuilder();
    if (stdout.length() != 0) {
      msg.append("Dartium stdout: ").append(stdout).append("\n");
    }
    boolean expired = false;
    if (stderr.length() != 0) {
      if (stderr.indexOf("Dartium build has expired") != -1) {
        expired = true;
      }
      if (expired) {
        msg.append("\nThis build of Dartium has expired.\n\n");
        msg.append("Please download a new Dart Editor or Dartium build from \n");
        msg.append("http://www.dartlang.org/downloads.html.");
      } else {
        msg.append("Dartium stderr: ").append(stderr);
      }
    }

    if (DartCore.isLinux() && !expired) {
      msg.append("\nFor information on how to setup your machine to run Dartium visit ");
      msg.append("http://code.google.com/p/dart/wiki/PreparingYourMachine#Linux");
    }
    if (msg.length() != 0) {
      msg.insert(0, ":\n\n");
    } else {
      msg.append(".");
    }
    return msg.toString();
  }

  private boolean isProcessTerminated(Process process) {
    try {
      if (process != null) {
        process.exitValue();
      }
      return true;
    } catch (IllegalThreadStateException ex) {
      return false;
    }
  }

  private StringBuilder readFromProcessPipes(final String processName, final InputStream in) {
    final StringBuilder output = new StringBuilder();

    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        byte[] buffer = new byte[2048];

        try {
          int count = in.read(buffer);

          while (count != -1) {
            if (count > 0) {
              String str = new String(buffer, 0, count);

              // Log any browser process output to stdout.
              if (DartDebugCorePlugin.LOGGING) {
                System.out.print(str);
              }

              output.append(str);
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

    return output;
  }

  private void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (Exception exception) {
    }
  }

  /**
   * @param file
   * @throws CoreException
   */
  private String startEmbeddedServer(IFile file, String url) throws CoreException {
    // Start the embedded web server. It is used to serve files from our workspace.
    if (file != null) {
      String fileUrl = null;
      try {
        resourceResolver = ResourceServerManager.getServer();

        fileUrl = resourceResolver.getUrlForResource(file);
      } catch (IOException exception) {
        throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
            "Could not launch browser - unable to start embedded server", exception));
      }
      return fileUrl;
    }
    return url;
  }

  /**
   * @param launchConfig
   * @param url
   * @param monitor
   * @param enableDebugging
   * @param browserLocation
   * @param browserName
   * @throws CoreException
   */
  private void startNewBrowserProcess(DartLaunchConfigWrapper launchConfig, String url,
      IProgressMonitor monitor, boolean enableDebugging, IPath browserLocation, String browserName)
      throws CoreException {

    Process process = null;
    monitor.worked(1);

    ProcessBuilder builder = new ProcessBuilder();
    Map<String, String> env = builder.environment();
    // Due to differences in 32bit and 64 bit environments, dartium 32bit launch does not work on
    // linux with this property.
    env.remove("LD_LIBRARY_PATH");

    // Add the environment variable DART_FLAGS="--enable-checked-mode"
    // to enable asserts and type checks
    if (launchConfig.getCheckedMode()) {
      env.put("DART_FLAGS", "--enable-checked-mode");
    }

    //pass in pref value for --package-root if set
    String packageRoot = DartCore.getPlugin().getPackageRootPref();
    // TODO (keertip): if using default "packages" directory, do not set env variable
    if (packageRoot != null) {
      String packageRootUri = resourceResolver.getUrlForFile(new Path(packageRoot).toFile());
      env.put("DART_PACKAGE_ROOT", packageRootUri);
    }

    devToolsPortNumber = DEVTOOLS_PORT_NUMBER;

    if (enableDebugging) {
      devToolsPortNumber = NetUtils.findUnusedPort(DEVTOOLS_PORT_NUMBER);

      if (devToolsPortNumber == -1) {
        throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
            "Unable to locate an available port for the Dartium debugger"));
      }
    }

    List<String> arguments = buildArgumentsList(
        browserLocation, url, enableDebugging, devToolsPortNumber);
    builder.command(arguments);
    builder.directory(new File(DartSdk.getInstance().getDartiumWorkingDirectory()));

    try {
      process = builder.start();
    } catch (IOException e) {
      DartDebugCorePlugin.logError("Exception while starting Dartium", e);

      throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
          "Could not launch browser: " + e.toString()));
    }

    browserProcess = process;
  }

  private void terminateExistingBrowserProcess() {
    if (browserProcess != null) {
      if (!isProcessTerminated(browserProcess)) {
        // TODO(devoncarew): try and use an OS mechanism to send it a graceful shutdown request?
        // This could avoid the problem w/ Chrome displaying the crashed message on the next run.

        browserProcess.destroy();

        // The process needs time to exit.
        waitForProcessToTerminate(browserProcess, 200);
        //sleep(100);
      }
      browserProcess = null;
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
