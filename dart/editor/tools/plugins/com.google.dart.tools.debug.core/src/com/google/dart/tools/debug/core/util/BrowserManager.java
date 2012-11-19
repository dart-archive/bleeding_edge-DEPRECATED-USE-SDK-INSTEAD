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
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.core.dartium.DartiumDebugTarget;
import com.google.dart.tools.debug.core.util.ListeningStream.StreamListener;
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

  private static IResourceResolver resourceResolver;

  /** The initial page to navigate to. */
  private static final String INITIAL_PAGE = "chrome://version/";

  /** A fragment of the initial page, used to search for it in a list of open tabs. */
  private static final String INITIAL_PAGE_FRAGMENT = "chrome://version";

  public static BrowserManager getManager() {
    return manager;
  }

  private static IResourceResolver getResourceServer() throws IOException {
    if (resourceResolver == null) {
      resourceResolver = ResourceServerManager.getServer();
    }

    return resourceResolver;
  }

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

    File dartium = DartSdkManager.getManager().getSdk().getDartiumExecutable();

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

    url = resolveLaunchUrl(file, url);

    // for now, check if browser is open, and connection is alive
    boolean restart = browserProcess == null || isProcessTerminated(browserProcess)
        || DartiumDebugTarget.getActiveTarget() == null
        || !DartiumDebugTarget.getActiveTarget().canTerminate();

    if (!restart) {
      DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);

      try {
        DartiumDebugTarget.getActiveTarget().navigateToUrl(url, enableBreakpoints);
      } catch (IOException e) {
        DartDebugCorePlugin.logError(e);
      }
    } else {
      terminateExistingBrowserProcess();

      ListeningStream dartiumOutput = startNewBrowserProcess(
          launchConfig,
          url,
          monitor,
          enableDebugging,
          browserLocation,
          browserName);

      sleep(100);

      monitor.worked(1);

      if (isProcessTerminated(browserProcess)) {
        DartDebugCorePlugin.logError("Dartium output: " + dartiumOutput.toString());

        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            "Could not launch browser - process terminated on startup"
                + getProcessStreamMessage(dartiumOutput.toString())));
      }

      connectToChromiumDebug(
          browserName,
          launch,
          launchConfig,
          url,
          monitor,
          browserProcess,
          timer,
          enableBreakpoints,
          devToolsPortNumber,
          dartiumOutput);
    }

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
      Process runtimeProcess, LogTimer timer, boolean enableBreakpoints, int devToolsPortNumber,
      ListeningStream dartiumOutput) throws CoreException {
    monitor.worked(1);

    try {
      // avg: 383ms
      timer.startTask("get chromium tabs");

      ChromiumTabInfo chromiumTab = getChromiumTab(
          runtimeProcess,
          devToolsPortNumber,
          dartiumOutput);

      monitor.worked(2);

      timer.stopTask();

      // avg: 46ms
      timer.startTask("open WIP connection");

      if (chromiumTab == null) {
        throw new DebugException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            "Unable to connect to Dartium"));
      }

      if (chromiumTab == null || chromiumTab.getWebSocketDebuggerUrl() == null) {
        throw new DebugException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            "Unable to connect to Chromium"));
      }

      WebkitConnection connection = new WebkitConnection(chromiumTab.getWebSocketDebuggerUrl());

      final DartiumDebugTarget debugTarget = new DartiumDebugTarget(
          browserName,
          connection,
          launch,
          runtimeProcess,
          getResourceServer(),
          enableBreakpoints);

      monitor.worked(1);

      launch.setAttribute(DebugPlugin.ATTR_CONSOLE_ENCODING, "UTF-8");
      launch.addDebugTarget(debugTarget);
      launch.addProcess(debugTarget.getProcess());

      if (launchConfig.getShowLaunchOutput()) {
        dartiumOutput.setListener(new StreamListener() {
          @Override
          public void handleStreamData(String data) {
            debugTarget.writeToStdout(data);
          }
        });
      }

      debugTarget.openConnection(url);

      timer.stopTask();
    } catch (IOException e) {
      DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);

      IStatus status;

      // Clean up the error message on certain connection failures to Dartium.
      // http://code.google.com/p/dart/issues/detail?id=4435
      if (e.toString().indexOf("connection failed: unknown status code 500") != -1) {
        DartDebugCorePlugin.logError(e);

        status = new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            "Unable to connect to Dartium");
      } else {
        status = new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID, e.toString(), e);
      }

      throw new CoreException(status);
    }

    if (firstLaunch) {
      firstLaunch = false;
    }

    monitor.worked(1);
  }

  private List<String> buildArgumentsList(DartLaunchConfigWrapper launchConfig,
      IPath browserLocation, String url, boolean enableDebugging, int devToolsPortNumber) {
    List<String> arguments = new ArrayList<String>();

    arguments.add(browserLocation.toOSString());

    // Enable remote debug over HTTP on the specified port.
    arguments.add("--remote-debugging-port=" + devToolsPortNumber);

    // In order to start up multiple Chrome processes, we need to specify a different user dir.
    arguments.add("--user-data-dir=" + getCreateUserDataDirectoryPath());

    // Indicates that the browser is in "browse without sign-in" (Guest session) mode. Should 
    // completely disable extensions, sync and bookmarks.
    // devoncarew: This only works under _CHROMEOS.
    //arguments.add("--bwsi");

    // Several extensions in the wild have errors that prevent connecting the debugger to Dartium.
    //arguments.add("--disable-extensions");

    // Disables syncing browser data to a Google Account. Do we want to do this?
    //arguments.add("--disable-sync");

    if (launchConfig.getUseWebComponents()) {
      arguments.add("--enable-experimental-webkit-features");
      arguments.add("--enable-devtools-experiments");
    }

    // Whether or not it's actually the first run.
    arguments.add("--no-first-run");

    // Disables the default browser check.
    arguments.add("--no-default-browser-check");

    // Bypass the error dialog when the profile lock couldn't be attained.
    arguments.add("--no-process-singleton-dialog");

    for (String arg : launchConfig.getArgumentsAsArray()) {
      arguments.add(arg);
    }

    // Causes the browser to launch directly into incognito mode.
    // We use this to prevent the previous session's tabs from re-opening.
    //arguments.add("--incognito");

    if (enableDebugging) {
      // Start up with a blank page.
      arguments.add(INITIAL_PAGE);
    } else {
      arguments.add(url);
    }

    return arguments;
  }

  private ChromiumTabInfo findTargetTab(List<ChromiumTabInfo> tabs) {
    for (ChromiumTabInfo tab : tabs) {
      if (tab.getTitle().contains(INITIAL_PAGE_FRAGMENT)) {
        return tab;
      }

      if (tab.getUrl().contains(INITIAL_PAGE_FRAGMENT)) {
        return tab;
      }
    }

    if (tabs.size() == 0) {
      // If no tabs, return null.
      return null;
    } else if (tabs.size() == 1) {
      // If one tab, return that.
      return tabs.get(0);
    } else {
      // If more then one tab, return the first visible, non-Chrome extension tab.
      for (ChromiumTabInfo tab : tabs) {
        if (!tab.isChromeExtension()) {
          return tab;
        }
      }
    }

    StringBuilder builder = new StringBuilder("unable to locate target dartium tab [" + tabs.size()
        + " tabs]\n");

    for (ChromiumTabInfo tab : tabs) {
      builder.append("  " + tab.getUrl() + " [" + tab.getTitle() + "]\n");
    }

    DartDebugCorePlugin.logError(builder.toString().trim());

    return null;
  }

  private ChromiumTabInfo getChromiumTab(Process runtimeProcess, int devToolsPortNumber,
      ListeningStream dartiumOutput) throws IOException, CoreException {
    // Give Chromium 20 seconds to start up.
    final int maxStartupDelay = 20 * 1000;

    long endTime = System.currentTimeMillis() + maxStartupDelay;

    while (true) {
      if (isProcessTerminated(runtimeProcess)) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            "Could not launch browser - process terminated while trying to connect"
                + getProcessStreamMessage(dartiumOutput.toString())));
      }

      try {
        List<ChromiumTabInfo> tabs = ChromiumConnector.getAvailableTabs(devToolsPortNumber);

        ChromiumTabInfo targetTab = findTargetTab(tabs);

        if (targetTab != null) {
          return targetTab;
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
    // TODO(devoncarew): delete old .dartiumSettings, .dartiumPrefs?
    String dataDirPath = System.getProperty("user.home") + File.separator + ".dartium";

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

  private String getProcessStreamMessage(String output) {
    StringBuilder msg = new StringBuilder();

    if (output.length() != 0) {
      msg.append("Dartium stdout: ").append(output).append("\n");
    }

    boolean expired = false;

    if (output.length() != 0) {
      if (output.indexOf("Dartium build has expired") != -1) {
        expired = true;
      }

      if (expired) {
        msg.append("\nThis build of Dartium has expired.\n\n");
        msg.append("Please download a new Dart Editor or Dartium build from \n");
        msg.append("http://www.dartlang.org/downloads.html.");
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

  private ListeningStream readFromProcessPipes(final String processName, final InputStream in) {
    final ListeningStream output = new ListeningStream();

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

              output.appendData(str);
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

  /**
   * @param file
   * @throws CoreException
   */
  private String resolveLaunchUrl(IFile file, String url) throws CoreException {
    try {
      if (file != null) {
        return getResourceServer().getUrlForResource(file);
      }
    } catch (IOException exception) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          "Could not launch browser - unable to start embedded server",
          exception));
    }

    return url;
  }

  private void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (Exception exception) {

    }
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
  private ListeningStream startNewBrowserProcess(DartLaunchConfigWrapper launchConfig, String url,
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

    // Pass in --package-root if the preference is set
    String packageRoot = DartCore.getPlugin().getPackageRootPref();
    // TODO(keertip): if using default "packages" directory, do not set env variable
    // TODO(devoncarew): why are we only passing package root in when launching a file (not a url)?
    if (packageRoot != null && launchConfig.getShouldLaunchFile()) {
      try {
        String packageRootUri = getResourceServer().getUrlForFile(new Path(packageRoot).toFile());

        // Strip a trailing slash off the uri if the user setting didn't have one.
        if (!packageRoot.endsWith("/") && packageRootUri.endsWith("/")) {
          packageRootUri = packageRootUri.substring(0, packageRootUri.length() - 1);
        }

        env.put("DART_PACKAGE_ROOT", packageRootUri);
      } catch (IOException e) {
        DartDebugCorePlugin.logError(e);
      }
    }

    // This flag allows us to retrieve the dart: core sources from Dartium.
    env.put("DART_DEBUG_LIBS", "true");

    devToolsPortNumber = DEVTOOLS_PORT_NUMBER;

    if (enableDebugging) {
      devToolsPortNumber = NetUtils.findUnusedPort(DEVTOOLS_PORT_NUMBER);

      if (devToolsPortNumber == -1) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            "Unable to locate an available port for the Dartium debugger"));
      }
    }

    List<String> arguments = buildArgumentsList(
        launchConfig,
        browserLocation,
        url,
        enableDebugging,
        devToolsPortNumber);
    builder.command(arguments);
    builder.directory(DartSdkManager.getManager().getSdk().getDartiumWorkingDirectory());
    builder.redirectErrorStream(true);

    try {
      process = builder.start();
    } catch (IOException e) {
      DartDebugCorePlugin.logError("Exception while starting Dartium", e);

      throw new CoreException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          "Could not launch browser: " + e.toString()));
    }

    browserProcess = process;

    return readFromProcessPipes(browserName, browserProcess.getInputStream());
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
