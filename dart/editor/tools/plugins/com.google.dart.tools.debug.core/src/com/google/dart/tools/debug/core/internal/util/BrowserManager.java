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
package com.google.dart.tools.debug.core.internal.util;

import com.google.dart.tools.core.model.DartSdk;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.core.configs.DartiumLaunchConfigurationDelegate;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;

import java.io.File;
import java.io.FileOutputStream;
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

  public void dispose() {
    for (Process process : browserProcesses.values()) {
      if (!processTerminated(process)) {
        process.destroy();
      }
    }
  }

  /**
   * Launch browser and open file url. If debug mode also connect to browser.
   */
  public void launchBrowser(ILaunch launch, DartLaunchConfigWrapper launchConfig, String url,
      IProgressMonitor monitor, boolean debug) throws CoreException {
    monitor.beginTask("Launching Chromium...", debug ? 9 : 3);

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

    Process process = null;
    monitor.worked(1);

    ProcessBuilder builder = new ProcessBuilder();
    Map<String, String> env = builder.environment();
    // due to differences in 32bit and 64 bit environments, dartium 32bit launch does not work on linux with 
    // this property
    env.remove("LD_LIBRARY_PATH");

    List<String> arguments = buildArgumentsList(browserLocation, url, debug);
    builder.command(arguments);
    builder.directory(new File(DartSdk.getInstance().getDartiumWorkingDirectory()));

    try {
      process = builder.start();
    } catch (IOException e) {

      DebugPlugin.logMessage("Exception while starting browser", e);
    }

    if (process == null) {
      throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
          "Could not launch browser"));
    }

    browserProcesses.put(browserName, process);

    readFromProcessPipes(browserName, process.getInputStream());
    readFromProcessPipes(browserName, process.getErrorStream());

    timer.endTimer();

    monitor.worked(1);

    // Check to see if the process exits soon after starting up, and if so stop the debug launch
    // process.
    sleep(100);

    monitor.worked(1);

    if (processTerminated(process)) {
      throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
          "Could not launch browser"));
    }

//    if (debug) {
//      connectToChromiumDebug(browserName, launch, launchConfig, monitor);
//    } else {
    // If we don't do this, the launch configurations will keep accumulating in the UI. This was
    // not a problem when we wrapped the runtime process with an IProcess.
    DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);
//    }

    monitor.done();
  }

  private List<String> buildArgumentsList(IPath browserLocation, String url, boolean debug) {
    List<String> arguments = new ArrayList<String>();

    arguments.add(browserLocation.toOSString());

    if (debug) {
      arguments.add("--remote-shell-port=" + PORT_NUMBER);
    }

    // In order to start up multiple Chrome processes, we need to specify a different user dir.
    arguments.add("--user-data-dir=" + getUserDataDirectoryPath());

    //arguments.add("--disable-breakpad");

    // Indicates that the browser is in "browse without sign-in" (Guest session) mode. Should 
    // completely disable extensions, sync and bookmarks.
    arguments.add("--bwsi");

    // Whether or not it's actually the first run.
    arguments.add("--no-first-run");

    // Disables the default browser check.
    arguments.add("--no-default-browser-check");

    // Bypass the error dialog when the profile lock couldn't be attained.
    arguments.add("--no-process-singleton-dialog");

    arguments.add(url);

    return arguments;
  }

//  private void connectToChromiumDebug(String browserName, ILaunch launch,
//      DartLaunchConfigWrapper launchConfig, IProgressMonitor monitor) throws CoreException {
//    LogTimer timer = new LogTimer("debug connection");
//
//    SocketAddress address = new InetSocketAddress(launchConfig.getConnectionHost(), PORT_NUMBER);
//
//    //ConsolePseudoProcess.Retransmitter consoleRetransmitter = new ConsolePseudoProcess.Retransmitter();
//
//    final Browser browser = BrowserFactory.getInstance().create(address,
//        new ConnectionLogger.Factory() {
//          @Override
//          public ConnectionLogger newConnectionLogger() {
//            return null;
//          }
//        });
//
//    monitor.worked(1);
//
//    // wait to see if browser is up
//    TabFetcher tabFetcher = null;
//    try {
//      int retry = 40;
//
//      while (retry > 0) {
//        try {
//          sleep(100);
//          tabFetcher = browser.createTabFetcher();
//          break;
//        } catch (Exception e) {
//          retry--;
//        }
//      }
//
//      if (tabFetcher == null) {
//        throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
//            "Could not connect to Browser"));
//      }
//
//      timer.endTimer();
//
//      monitor.worked(1);
//
//      // TODO(devoncarew): we need to determine how to make this go away 
//      sleep(3000);
//
//      monitor.worked(1);
//
//      List<? extends TabConnector> tabs = tabFetcher.getTabs();
//
//      Browser.TabConnector tabConnector;
//
//      if (tabs.size() == 0) {
//        tabConnector = null;
//      } else if (tabs.size() == 1) {
//        tabConnector = tabs.get(0);
//      } else {
//        tabConnector = selectTab(tabs);
//      }
//
//      monitor.worked(1);
//
//      if (tabConnector != null) {
//        timer = new LogTimer("tab attach");
//
//        ChromeDebugTarget debugTarget = new ChromeDebugTarget(browserName, launch);
//
//        BrowserTab browserTab = tabConnector.attach(debugTarget);
//
//        monitor.worked(1);
//
//        debugTarget.setBrowserTab(browserTab);
//
//        launch.addDebugTarget(debugTarget);
//        debugTarget.connected();
//
////        ConsolePseudoProcess consolePseudoProcess = new ConsolePseudoProcess(launch, title,
////            consoleRetransmitter, debugTarget);
////
////        debugTarget.setProcess(consolePseudoProcess);
////
////        consoleRetransmitter.startFlushing();
//
//        timer.endTimer();
//
//        monitor.worked(1);
//      }
//    } catch (IOException e) {
//      throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
//          e.getMessage(), e));
//    } catch (IllegalStateException e) {
//      throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
//          e.getMessage(), e));
//    }
//  }

  /**
   * This method creates a Chrome settings directory. Specifically, it creates a 'First Run' file
   * and a 'Default/Preferences' file so that we can avoid actions that occur the first time Chrome
   * is run. These include asking the user for a default search engine and asking them if they want
   * Chrome to be their default browser.
   * 
   * @param dataDir the location of the Chrome data directory
   */
  @SuppressWarnings("unused")
  private void createChromeDataDir(File dataDir) {
    try {
      // Create the data directory.
      dataDir.mkdir();

      // Create the (empty) first run file.
      File firstRunFile = new File(dataDir, "First Run");
      firstRunFile.createNewFile();

      // Create the Default directory.
      File defaultDir = new File(dataDir, "Default");
      defaultDir.mkdir();

      // Create the Preferences file.
      InputStream in = DartiumLaunchConfigurationDelegate.class.getResourceAsStream("Preferences");

      File prefFile = new File(defaultDir, "Preferences");
      FileOutputStream out = new FileOutputStream(prefFile);
      byte[] buffer = new byte[4096];
      int count = in.read(buffer);
      while (count != -1) {
        out.write(buffer, 0, count);
        count = in.read();
      }
      out.close();
      in.close();

    } catch (IOException ioe) {
      DartDebugCorePlugin.logError(ioe);
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
      //createChromeDataDir(dataDir);
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

//  private TabConnector selectTab(List<? extends TabConnector> tabs) throws IOException {
//    List<String> tabUrls = new ArrayList<String>();
//
//    for (TabConnector tab : tabs) {
//      tabUrls.add(tab.getUrl());
//    }
//
//    DebugUIHelper tabChooser = DebugUIHelperFactory.getDebugUIHelper();
//
//    int index = tabChooser.select(tabUrls);
//
//    if (index == -1) {
//      return null;
//    } else {
//      return tabs.get(index);
//    }
//  }

  private void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (Exception exception) {
    }
  }

}
