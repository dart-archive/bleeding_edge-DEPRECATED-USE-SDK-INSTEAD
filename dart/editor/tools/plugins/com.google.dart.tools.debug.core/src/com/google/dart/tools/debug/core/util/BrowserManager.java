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
import com.google.dart.tools.core.utilities.net.NetUtils;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.core.DebugUIHelper;
import com.google.dart.tools.debug.core.dartium.DartiumDebugTarget;
import com.google.dart.tools.debug.core.util.ListeningStream.StreamListener;
import com.google.dart.tools.debug.core.webkit.ChromiumConnector;
import com.google.dart.tools.debug.core.webkit.ChromiumTabInfo;
import com.google.dart.tools.debug.core.webkit.DefaultChromiumTabChooser;
import com.google.dart.tools.debug.core.webkit.IChromiumTabChooser;
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
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;

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
  /** The initial page to navigate to. */
  private static final String INITIAL_PAGE = "chrome://version/";

  private static final int DEVTOOLS_PORT_NUMBER = 9322;

  private static BrowserManager manager = new BrowserManager();

  private static Process browserProcess = null;

  /**
   * Create a Chrome user data directory, and return the path to that directory.
   * 
   * @return the user data directory path
   */
  public static String getCreateUserDataDirectoryPath(String baseName) {
    String dataDirPath = System.getProperty("user.home") + File.separator + "." + baseName;

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

  public static BrowserManager getManager() {
    return manager;
  }

  private int devToolsPortNumber;

  private IChromiumTabChooser tabChooser;

  public BrowserManager() {
    this.tabChooser = new DefaultChromiumTabChooser();
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
      IProgressMonitor monitor, boolean enableDebugging, IResourceResolver resolver)
      throws CoreException {
    launchBrowser(launch, launchConfig, file, null, monitor, enableDebugging, resolver);
  }

  /**
   * Launch the browser and open the given url. If debug mode also connect to browser.
   */
  public void launchBrowser(ILaunch launch, DartLaunchConfigWrapper launchConfig, String url,
      IProgressMonitor monitor, boolean enableDebugging, IResourceResolver resolver)
      throws CoreException {
    launchBrowser(launch, launchConfig, null, url, monitor, enableDebugging, resolver);
  }

  public IDebugTarget performRemoteConnection(IChromiumTabChooser tabChooser, String host,
      int port, IProgressMonitor monitor, IResourceResolver resourceResolver) throws CoreException {

    ILaunch launch = null;

    monitor.beginTask("Opening Connection...", IProgressMonitor.UNKNOWN);

    try {
      List<ChromiumTabInfo> tabs = ChromiumConnector.getAvailableTabs(host, port);

      ChromiumTabInfo tab = findTargetTab(tabChooser, tabs);

      if (tab == null || tab.getWebSocketDebuggerUrl() == null) {
        throw new DebugException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            "Unable to connect to debugger in Chromium, make sure browser is open."));
      }

      monitor.worked(1);

      launch = CoreLaunchUtils.createTemporaryLaunch(
          DartDebugCorePlugin.DARTIUM_LAUNCH_CONFIG_ID,
          host + "[" + port + "]");

      CoreLaunchUtils.addLaunch(launch);

      WebkitConnection connection = new WebkitConnection(
          tab.getHost(),
          tab.getPort(),
          tab.getWebSocketDebuggerFile());

      final DartiumDebugTarget debugTarget = new DartiumDebugTarget(
          "Remote",
          connection,
          launch,
          null,
          resourceResolver,
          true,
          true);

      launch.setAttribute(DebugPlugin.ATTR_CONSOLE_ENCODING, "UTF-8");
      launch.addDebugTarget(debugTarget);
      launch.addProcess(debugTarget.getProcess());

      debugTarget.openConnection();

      monitor.worked(1);

      return debugTarget;
    } catch (IOException e) {
      if (launch != null) {
        CoreLaunchUtils.removeLaunch(launch);
      }

      throw new CoreException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          "Could not connect to remote browser \n" + e.toString(),
          e));
    } finally {
      monitor.done();
    }
  }

  protected void launchBrowser(ILaunch launch, DartLaunchConfigWrapper launchConfig, IFile file,
      String url, IProgressMonitor monitor, boolean enableDebugging, IResourceResolver resolver)
      throws CoreException {

    // For now, we always start a debugging connection, even when we're not really debugging.
    boolean enableBreakpoints = enableDebugging;

    monitor.beginTask("Launching Dartium...", enableDebugging ? 7 : 2);

    File dartium = DartSdkManager.getManager().getSdk().getDartiumExecutable();

    if (dartium == null) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          "Could not find Dartium executable in "
              + DartSdkManager.getManager().getSdk().getDartiumWorkingDirectory()
              + ". Download and install Dartium from http://www.dartlang.org/tools/dartium/."));
    }

    IPath browserLocation = new Path(dartium.getAbsolutePath());

    String browserName = dartium.getName();

    // avg: 0.434 sec (old: 0.597)
    LogTimer timer = new LogTimer("Dartium debug startup");

    // avg: 55ms
    timer.startTask(browserName + " startup");

    if (!launchConfig.getUsePubServe()) {
      // TODO(keertip): if file is passed in, url is null. Modify the method
      // to return a url that makes sense in this case.
      url = resolveLaunchUrl(file, url, resolver);
    }

    url = launchConfig.appendQueryParams(url);

    // for now, check if browser is open, and connection is alive
    boolean restart = browserProcess == null || isProcessTerminated(browserProcess)
        || DartiumDebugTarget.getActiveTarget() == null
        || !DartiumDebugTarget.getActiveTarget().canTerminate();

    // we only re-cycle the debug connection if we're launching the same launch configuration
    if (!restart) {
      if (!DartiumDebugTarget.getActiveTarget().getLaunch().getLaunchConfiguration().equals(
          launch.getLaunchConfiguration())) {
        restart = true;
      }
    }

    if (!restart) {
      if (enableDebugging != DartiumDebugTarget.getActiveTarget().getEnableBreakpoints()) {
        restart = true;
      }
    }

    CoreLaunchUtils.removeTerminatedLaunches();

    if (!restart) {
      DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);

      try {
        DartiumDebugTarget.getActiveTarget().navigateToUrl(
            launch.getLaunchConfiguration(),
            url,
            enableBreakpoints,
            resolver);
      } catch (IOException e) {
        DartDebugCorePlugin.logError(e);
      }
    } else {
      terminateExistingBrowserProcess();

      StringBuilder processDescription = new StringBuilder();

      ListeningStream dartiumOutput = startNewBrowserProcess(
          launchConfig,
          url,
          monitor,
          enableDebugging,
          browserLocation,
          browserName,
          processDescription);

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
          dartiumOutput,
          processDescription.toString(),
          resolver);
    }

    DebugUIHelper.getHelper().activateApplication(dartium, "Chromium");

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
      ListeningStream dartiumOutput, String processDescription, IResourceResolver resolver)
      throws CoreException {
    monitor.worked(1);

    try {
      // avg: 383ms
      timer.startTask("get chromium tabs");

      ChromiumTabInfo tab = getChromiumTab(runtimeProcess, devToolsPortNumber, dartiumOutput);

      monitor.worked(2);

      timer.stopTask();

      // avg: 46ms
      timer.startTask("open WIP connection");

      if (tab == null || tab.getWebSocketDebuggerUrl() == null) {
        throw new DebugException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            "Unable to connect to Chromium"));
      }

      // Even when Dartium has reported all the debuggable tabs to us, the debug server
      // may not yet have started up. Delay a small fixed amount of time.
      sleep(100);

      WebkitConnection connection = new WebkitConnection(
          tab.getHost(),
          tab.getPort(),
          tab.getWebSocketDebuggerFile());

      final DartiumDebugTarget debugTarget = new DartiumDebugTarget(
          browserName,
          connection,
          launch,
          runtimeProcess,
          resolver,
          enableBreakpoints,
          false);

      monitor.worked(1);

      launch.setAttribute(DebugPlugin.ATTR_CONSOLE_ENCODING, "UTF-8");
      launch.addDebugTarget(debugTarget);
      launch.addProcess(debugTarget.getProcess());
      debugTarget.getProcess().setAttribute(IProcess.ATTR_CMDLINE, processDescription);

      if (launchConfig.getShowLaunchOutput()) {
        dartiumOutput.setListener(new StreamListener() {
          @Override
          public void handleStreamData(String data) {
            debugTarget.writeToStdout(data);
          }
        });
      }

      debugTarget.openConnection(url, true);

      if (DartDebugCorePlugin.LOGGING) {
        System.out.println("Connected to WIP debug agent on port " + devToolsPortNumber);
      }

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

    monitor.worked(1);
  }

  private List<String> buildArgumentsList(DartLaunchConfigWrapper launchConfig,
      IPath browserLocation, String url, boolean enableDebugging, int devToolsPortNumber) {
    List<String> arguments = new ArrayList<String>();

    arguments.add(browserLocation.toOSString());

    // Enable remote debug over HTTP on the specified port.
    arguments.add("--remote-debugging-port=" + devToolsPortNumber);

    // In order to start up multiple Chrome processes, we need to specify a different user dir.
    arguments.add("--user-data-dir=" + getCreateUserDataDirectoryPath("dartium"));

    if (launchConfig.getUseWebComponents()) {
      arguments.add("--enable-experimental-web-platform-features");
      arguments.add("--enable-html-imports");
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

    if (enableDebugging) {
      // Start up with a blank page.
      arguments.add(INITIAL_PAGE);
    } else {
      arguments.add(url);
    }

    return arguments;
  }

  private void describe(List<String> arguments, StringBuilder builder) {
    for (int i = 0; i < arguments.size(); i++) {
      if (i > 0) {
        builder.append(" ");
      }
      builder.append(arguments.get(i));
    }
  }

  private ChromiumTabInfo findTargetTab(IChromiumTabChooser tabChooser, List<ChromiumTabInfo> tabs) {
    ChromiumTabInfo chromeTab = tabChooser.chooseTab(tabs);

    if (chromeTab != null) {
      for (ChromiumTabInfo tab : tabs) {
        DartDebugCorePlugin.log("Found: " + tab.toString());
      }

      DartDebugCorePlugin.log("Choosing: " + chromeTab);

      return chromeTab;
    }

    StringBuilder builder = new StringBuilder("unable to locate target dartium tab [" + tabs.size()
        + " tabs]\n");

    for (ChromiumTabInfo tab : tabs) {
      builder.append("  " + tab.getUrl() + " [" + tab.getTitle() + "]\n");
    }

    DartDebugCorePlugin.logError(builder.toString().trim());

    return null;
  }

  private ChromiumTabInfo getChromiumTab(Process runtimeProcess, int port,
      ListeningStream dartiumOutput) throws IOException, CoreException {
    // Give Chromium 20 seconds to start up.
    final int maxStartupDelay = 20 * 1000;

    long endTime = System.currentTimeMillis() + maxStartupDelay;

    while (true) {
      if (isProcessTerminated(runtimeProcess)) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            "Could not launch browser - process terminated while trying to connect. "
                + "Try closing any running Dartium instances."
                + getProcessStreamMessage(dartiumOutput.toString())));
      }

      try {
        List<ChromiumTabInfo> tabs = ChromiumConnector.getAvailableTabs(port);

        ChromiumTabInfo targetTab = findTargetTab(tabChooser, tabs);

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
        msg.append("https://www.dartlang.org/tools/dartium/");
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
  private String resolveLaunchUrl(IFile file, String url, IResourceResolver resolver)
      throws CoreException {
    if (file != null) {
      return resolver.getUrlForResource(file);
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
      IProgressMonitor monitor, boolean enableDebugging, IPath browserLocation, String browserName,
      StringBuilder argDescription) throws CoreException {

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
        true,
        devToolsPortNumber);
    builder.command(arguments);
    builder.redirectErrorStream(true);

    describe(arguments, argDescription);

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
