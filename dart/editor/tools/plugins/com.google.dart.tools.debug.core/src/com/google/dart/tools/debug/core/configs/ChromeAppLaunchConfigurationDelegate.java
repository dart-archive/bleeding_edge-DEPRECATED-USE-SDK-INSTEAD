/*
 * Copyright (c) 2013, the Dart project authors.
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
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.core.utilities.net.NetUtils;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.core.DartLaunchConfigurationDelegate;
import com.google.dart.tools.debug.core.DebugUIHelper;
import com.google.dart.tools.debug.core.dartium.DartiumDebugTarget;
import com.google.dart.tools.debug.core.util.BrowserManager;
import com.google.dart.tools.debug.core.util.IResourceResolver;
import com.google.dart.tools.debug.core.webkit.ChromiumConnector;
import com.google.dart.tools.debug.core.webkit.ChromiumTabInfo;
import com.google.dart.tools.debug.core.webkit.WebkitConnection;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//[ {
//  "title": "New Tab",
//  "type": "page",
//  "url": "chrome://newtab/",
//}, {
//  "title": "chrome-extension://becjelbpddbpmopbobpojhgneicbhlgj/_generated_background_page.html",
//  "type": "other",
//  "url": "chrome-extension://becjelbpddbpmopbobpojhgneicbhlgj/_generated_background_page.html",
//}, {
//  "title": "Packy",
//  "type": "other",
//  "url": "chrome-extension://becjelbpddbpmopbobpojhgneicbhlgj/packy.html",
//} ]

/**
 * A ILaunchConfigurationDelegate implementation that can launch Chrome applications. We
 * conceptually launch the manifest.json file which specifies a Chrome app. We currently send
 * Dartium the path to the manifest file's parent directory via the --load-extension flag.
 */
public class ChromeAppLaunchConfigurationDelegate extends DartLaunchConfigurationDelegate {

  private static class ChromeAppResourceResolver implements IResourceResolver {
    private IContainer container;

    String prefix;

    public ChromeAppResourceResolver(IContainer container, ChromiumTabInfo tab) {
      this.container = container;

      prefix = tab.getUrl();
      int index = prefix.indexOf("//");

      if (index != -1) {
        index = prefix.indexOf('/', index + 2);

        if (index != -1) {
          prefix = prefix.substring(0, index + 1);
        }
      }
    }

    @Override
    public String getUrlForFile(File file) {
      IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(file.toURI());

      if (files.length > 0) {
        return getUrlForResource(files[0]);
      } else {
        return null;
      }
    }

    @Override
    public String getUrlForResource(IResource resource) {
      String relPath = calcRelPath(container, resource);

      if (relPath != null) {
        return prefix + relPath;
      } else {
        return null;
      }
    }

    @Override
    public String getUrlRegexForResource(IResource resource) {
      //final String PACKAGES = "/packages/";

      String relPath = calcRelPath(container, resource);

      if (relPath != null) {
        return relPath;
      }

      return resource.getFullPath().toString();

//      if (resourcePath.contains(PACKAGES)) {
//        int index = resourcePath.indexOf(PACKAGES);
//
//        return resourcePath.substring(index + PACKAGES.length());
//      }
//
//      return null;
    }

    @Override
    public IResource resolveUrl(String url) {
      if (url.startsWith(prefix)) {
        return container.findMember(url.substring(prefix.length()));
      } else {
        return null;
      }
    }

    private String calcRelPath(IContainer container, IResource resource) {
      if (container == null) {
        return null;
      }

      String containerPath = container.getFullPath().toString();
      String resourcePath = resource.getFullPath().toString();

      if (resourcePath.startsWith(containerPath)) {
        String relPath = resourcePath.substring(containerPath.length());

        if (relPath.startsWith("/")) {
          return relPath.substring(1);
        } else {
          return relPath;
        }
      } else {
        return null;
      }
    }
  }

  private static final int DEFAULT_DEBUGGER_PORT = 9422;

  private static Process chromeAppBrowserProcess;

  /**
   * Create a new ChromeAppLaunchConfigurationDelegate.
   */
  public ChromeAppLaunchConfigurationDelegate() {

  }

  @Override
  public void doLaunch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor, InstrumentationBuilder instrumentation) throws CoreException {
    if (!ILaunchManager.RUN_MODE.equals(mode) && !ILaunchManager.DEBUG_MODE.equals(mode)) {
      throw new CoreException(DartDebugCorePlugin.createErrorStatus("Execution mode '" + mode
          + "' is not supported."));
    }

    boolean enableDebugging = ILaunchManager.DEBUG_MODE.equals(mode);

    File dartium = DartSdkManager.getManager().getSdk().getDartiumExecutable();

    if (dartium == null) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          "Could not find Dartium"));
    }

    DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(configuration);

    wrapper.markAsLaunched();

    IResource jsonResource = wrapper.getApplicationResource();

    if (jsonResource == null) {
      throw newDebugException("No file specified to launch");
    }

    File cwd = getWorkingDirectory(jsonResource);
    String extensionPath = jsonResource.getParent().getLocation().toFile().getAbsolutePath();

    List<String> commandsList = new ArrayList<String>();

    commandsList.add(dartium.getAbsolutePath());
    commandsList.add("--user-data-dir="
        + BrowserManager.getCreateUserDataDirectoryPath("chrome-apps"));
    commandsList.add("--no-first-run");
    commandsList.add("--no-default-browser-check");

    // This is currently only supported on the mac. 
    if (DartCore.isMac()) {
      commandsList.add("--no-startup-window");
    }

    commandsList.add("--load-and-launch-app=" + extensionPath);

    for (String arg : wrapper.getArgumentsAsArray()) {
      commandsList.add(arg);
    }

    int devToolsPortNumber = DEFAULT_DEBUGGER_PORT;

    if (enableDebugging) {
      devToolsPortNumber = NetUtils.findUnusedPort(DEFAULT_DEBUGGER_PORT);

      commandsList.add("--remote-debugging-port=" + devToolsPortNumber);
    }

    monitor.beginTask("Dartium", IProgressMonitor.UNKNOWN);

    terminatePreviousLaunch();

    String[] commands = commandsList.toArray(new String[commandsList.size()]);
    ProcessBuilder processBuilder = new ProcessBuilder(commands);
    processBuilder.directory(cwd);

    Map<String, String> wrapperEnv = wrapper.getEnvironment();

    if (!wrapperEnv.isEmpty()) {
      Map<String, String> env = processBuilder.environment();

      for (String key : wrapperEnv.keySet()) {
        env.put(key, wrapperEnv.get(key));
      }
    }

    // Add the environment variable DART_FLAGS="--enable-checked-mode" to enable asserts and type
    // checks. Default to false for Chrome apps.
    if (wrapper.getCheckedMode(false)) {
      Map<String, String> env = processBuilder.environment();
      env.put("DART_FLAGS", "--enable-checked-mode");
    }

    Process runtimeProcess = null;

    try {
      runtimeProcess = processBuilder.start();
    } catch (IOException ioe) {
      throw newDebugException(ioe);
    }

    saveLaunchedProcess(runtimeProcess);

    if (enableDebugging) {
      try {
        // Poll until we find a good tab to connect to.
        ChromiumTabInfo tab = getChromiumTab(runtimeProcess, devToolsPortNumber);

        if (tab != null && tab.getWebSocketDebuggerUrl() != null) {
          WebkitConnection connection = new WebkitConnection(
              tab.getHost(),
              tab.getPort(),
              tab.getWebSocketDebuggerFile());

          final DartiumDebugTarget debugTarget = new DartiumDebugTarget(
              dartium.getName(),
              connection,
              launch,
              runtimeProcess,
              new ChromeAppResourceResolver(jsonResource.getParent(), tab),
              true,
              false);

          monitor.worked(1);

          launch.setAttribute(DebugPlugin.ATTR_CONSOLE_ENCODING, "UTF-8");
          launch.addDebugTarget(debugTarget);
          launch.addProcess(debugTarget.getProcess());

          try {
            debugTarget.openConnection();
          } catch (IOException ioe) {
            DartDebugCorePlugin.logError(ioe);
          }
        }

        // Give the app a little time to open the main window.
        sleep(500);

        DebugUIHelper.getHelper().activateApplication(dartium, "Chromium");
      } catch (CoreException ce) {
        DartDebugCorePlugin.logError(ce);
      }
    } else {
      Map<String, String> processAttributes = new HashMap<String, String>();

      processAttributes.put(IProcess.ATTR_PROCESS_TYPE, "Dartium");

      IProcess eclipseProcess = DebugPlugin.newProcess(
          launch,
          runtimeProcess,
          configuration.getName(),
          processAttributes);

      if (eclipseProcess == null) {
        throw newDebugException("Error starting Dartium");
      }

      // We need to wait until the process is started before we can try and activate the window.
      sleep(1000);

      DebugUIHelper.getHelper().activateApplication(dartium, "Chromium");
    }

    monitor.done();
  }

  private ChromiumTabInfo findTargetTab(List<ChromiumTabInfo> tabs) {
    for (ChromiumTabInfo tab : tabs) {
      if (tab.getTitle().startsWith("chrome-extension://")) {
        continue;
      }

      // chrome-extension://kohcodfehgoaolndkcophkcmhjenpfmc/_generated_background_page.html
      if (tab.getTitle().endsWith("_generated_background_page.html")) {
        continue;
      }

      // chrome-extension://nkeimhogjdpnpccoofpliimaahmaaome/background.html
      if (tab.getUrl().endsWith("_generated_background_page.html")
          || tab.getUrl().endsWith("/background.html")) {
        continue;
      }

      if (tab.getUrl().startsWith("chrome-extension://") && tab.getTitle().length() > 0) {
        return tab;
      }
    }

    return null;
  }

  private ChromiumTabInfo getChromiumTab(Process process, int port) throws CoreException {
    // Give Chromium 10 seconds to start up.
    final int maxStartupDelay = 10 * 1000;

    long endTime = System.currentTimeMillis() + maxStartupDelay;

    while (true) {
      if (isProcessTerminated(process)) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            "Could not launch browser - process terminated while trying to connect. "
                + "Try closing any running Dartium instances."));
      }

      try {
        List<ChromiumTabInfo> tabs = ChromiumConnector.getAvailableTabs(port);

        ChromiumTabInfo targetTab = findTargetTab(tabs);

        if (targetTab != null) {
          for (ChromiumTabInfo tab : tabs) {
            DartDebugCorePlugin.log("Found: " + tab.toString());
          }

          DartDebugCorePlugin.log("Choosing: " + targetTab);

          return targetTab;
        }
      } catch (IOException exception) {
        if (System.currentTimeMillis() > endTime) {
          throw new CoreException(new Status(
              IStatus.ERROR,
              DartDebugCorePlugin.PLUGIN_ID,
              "Could not connect to Dartium",
              exception));
        }
      }

      if (System.currentTimeMillis() > endTime) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            "Timed out trying to connect to Dartium"));
      }

      sleep(250);
    }
  }

  /**
   * Return the parent of the Chrome app directory. The Chrome app directory contains the given
   * manifest.json file.
   * 
   * @param jsonResource
   * @return
   */
  private File getWorkingDirectory(IResource jsonResource) {
    IContainer containingDir = jsonResource.getParent();
    File containingFile = containingDir.getLocation().toFile();

    // Return the parent of this directory.
    return containingFile.getParentFile();
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

  private DebugException newDebugException(String message) {
    return new DebugException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID, message));
  }

  private DebugException newDebugException(Throwable t) {
    return new DebugException(new Status(
        IStatus.ERROR,
        DartDebugCorePlugin.PLUGIN_ID,
        t.toString(),
        t));
  }

  /**
   * Store the successfully launched process into a static variable;
   * 
   * @param process
   */
  private void saveLaunchedProcess(Process process) {
    chromeAppBrowserProcess = process;
  }

  private void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (Exception exception) {

    }
  }

  private void terminatePreviousLaunch() {
    if (chromeAppBrowserProcess != null) {
      try {
        chromeAppBrowserProcess.exitValue();
        chromeAppBrowserProcess = null;
      } catch (IllegalThreadStateException ex) {
        // exitValue() will throw if the process has not yet stopped. In that case, we ask it to.
        chromeAppBrowserProcess.destroy();
        chromeAppBrowserProcess = null;

        // Delay a bit.
        sleep(100);
      }
    }
  }

}
