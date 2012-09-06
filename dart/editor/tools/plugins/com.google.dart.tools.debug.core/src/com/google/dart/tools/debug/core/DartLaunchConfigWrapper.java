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
package com.google.dart.tools.debug.core;

import com.google.dart.tools.core.utilities.general.StringUtilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

import java.util.ArrayList;
import java.util.List;

/**
 * A wrapper class around ILaunchConfiguration and ILaunchConfigurationWorkingCopy objects. It adds
 * compiler type checking to what is essentially a property map.
 */
public class DartLaunchConfigWrapper {

  public static final int DEFAULT_CHROME_PORT = 9222;
  public static final String DEFAULT_HOST = "localhost";

  private static final String APPLICATION_ARGUMENTS = "applicationArguments";
  private static final String APPLICATION_NAME = "applicationName";
  private static final String VM_CHECKED_MODE = "vmCheckedMode";
  private static final String VM_ENABLE_DEBUGGING = "vmEnableDebugging";
  private static final String VM_HEAP_MB = "vmHeapMB";
  private static final String SHOW_LAUNCH_OUTPUT = "showLaunchOutput";

  private static final String IS_FILE = "launchHtmlFile";
  private static final String URL = "url";
  private static final String USE_DEFAULT_BROWSER = "systemDefaultBrowser";
  private static final String BROWSER_NAME = "browserName";

  private static final String CONNECTION_HOST = "connectionHost";
  private static final String CONNECTION_PORT = "connectionPort";

  private static final String PROJECT_NAME = "projectName";

  private static final String LAST_LAUNCH_TIME = "launchTime";

  private ILaunchConfiguration launchConfig;

  /**
   * Create a new DartLaunchConfigWrapper given either a ILaunchConfiguration (for read-only launch
   * configs) or ILaunchConfigurationWorkingCopy (for writeable launch configs).
   */
  public DartLaunchConfigWrapper(ILaunchConfiguration launchConfig) {
    this.launchConfig = launchConfig;
  }

  /**
   * @return the Dart application file name (e.g. src/HelloWorld.dart)
   */
  public String getApplicationName() {
    try {
      return launchConfig.getAttribute(APPLICATION_NAME, "");
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);

      return "";
    }
  }

  public IResource getApplicationResource() {
    String path = getApplicationName();

    if (path == null || path.length() == 0) {
      return null;
    } else {
      return ResourcesPlugin.getWorkspace().getRoot().findMember(getApplicationName());
    }
  }

  /**
   * @return the arguments string for the Dart application or Browser
   */
  public String getArguments() {
    try {
      return launchConfig.getAttribute(APPLICATION_ARGUMENTS, "");
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);

      return "";
    }
  }

  /**
   * @return the arguments for the Dart application or Browser
   */
  public String[] getArgumentsAsArray() {
    String command = getArguments();

    if (command == null || command.length() == 0) {
      return new String[0];
    }

    return StringUtilities.parseArgumentString(command);
  }

  /**
   * @return the name of browser to use for the launch configuration
   */
  public String getBrowserName() {
    try {
      return launchConfig.getAttribute(BROWSER_NAME, "");
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);

      return "";
    }
  }

  public boolean getCheckedMode() {
    try {
      return launchConfig.getAttribute(VM_CHECKED_MODE, true);
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);

      return false;
    }
  }

  /**
   * @return the launch configuration that this DartLaucnConfigWrapper wraps
   */
  public ILaunchConfiguration getConfig() {
    return launchConfig;
  }

  /**
   * @return the host to connect to for remote debugging
   */
  public String getConnectionHost() {
    try {
      return launchConfig.getAttribute(CONNECTION_HOST, DEFAULT_HOST);
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);

      return DEFAULT_HOST;
    }
  }

  /**
   * @return the port to connect to for remote debugging
   */
  public int getConnectionPort() {
    try {
      return launchConfig.getAttribute(CONNECTION_PORT, DEFAULT_CHROME_PORT);
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);

      return DEFAULT_CHROME_PORT;
    }
  }

  public boolean getEnableDebugging() {
    try {
      return launchConfig.getAttribute(VM_ENABLE_DEBUGGING, true);
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);

      return false;
    }
  }

  public String getHeapMB() {
    try {
      return launchConfig.getAttribute(VM_HEAP_MB, "");
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);

      return "";
    }
  }

  /**
   * @return the last time this config was launched, or 0 or no such
   */
  public long getLastLaunchTime() {
    try {
      String value = launchConfig.getAttribute(LAST_LAUNCH_TIME, "0");

      return Long.parseLong(value);
    } catch (NumberFormatException ex) {
      return 0;
    } catch (CoreException ce) {
      DartDebugCorePlugin.logError(ce);

      return 0;
    }
  }

  /**
   * @return the DartProject that contains the application to run
   */
  public IProject getProject() {
    String projectName = getProjectName();

    if (projectName.length() == 0) {
      IResource resource = getApplicationResource();

      if (resource == null) {
        return null;
      } else {
        return resource.getProject();
      }
    } else {
      return ResourcesPlugin.getWorkspace().getRoot().getProject(getProjectName());
    }
  }

  /**
   * @return the name of the DartProject that contains the application to run
   */
  public String getProjectName() {
    try {
      return launchConfig.getAttribute(PROJECT_NAME, "");
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);

      return "";
    }
  }

  public boolean getShouldLaunchFile() {
    try {
      return launchConfig.getAttribute(IS_FILE, true);
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);

      return true;
    }
  }

  public boolean getShowLaunchOutput() {
    try {
      return launchConfig.getAttribute(SHOW_LAUNCH_OUTPUT, false);
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);

      return false;
    }
  }

  public String getUrl() {
    try {
      return launchConfig.getAttribute(URL, "");
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);

      return "";
    }
  }

  public boolean getUseDefaultBrowser() {
    try {
      return launchConfig.getAttribute(USE_DEFAULT_BROWSER, true);
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);

      return true;
    }
  }

  /**
   * @return the arguments for the Dart VM
   */
  public String[] getVmArgumentsAsArray() {
    List<String> args = new ArrayList<String>();

    if (getCheckedMode()) {
      args.add("--enable-checked-mode");
    }

    try {
      int heap = Integer.parseInt(getHeapMB());

      if (heap > 0) {
        args.add("--new_gen_heap_size=" + heap);
      }
    } catch (NumberFormatException ex) {

    }

    return args.toArray(new String[args.size()]);
  }

  /**
   * Indicate that this launch configuration was just launched.
   */
  public void markAsLaunched() {
    try {
      ILaunchConfigurationWorkingCopy workingCopy = launchConfig.getWorkingCopy();

      long launchTime = System.currentTimeMillis();

      workingCopy.setAttribute(LAST_LAUNCH_TIME, Long.toString(launchTime));

      workingCopy.doSave();
    } catch (CoreException ce) {
      DartDebugCorePlugin.logError(ce);
    }
  }

  /**
   * @see #getApplicationName()
   */
  public void setApplicationName(String value) {
    getWorkingCopy().setAttribute(APPLICATION_NAME, value);

    updateMappedResources(value);
  }

  /**
   * @see #getArguments()
   */
  public void setArguments(String value) {
    getWorkingCopy().setAttribute(APPLICATION_ARGUMENTS, value);
  }

  /**
   * @see #getBrowserName()
   */
  public void setBrowserName(String value) {
    getWorkingCopy().setAttribute(BROWSER_NAME, value);
  }

  public void setCheckedMode(boolean value) {
    getWorkingCopy().setAttribute(VM_CHECKED_MODE, value);
  }

  /**
   * @see #getConnectionHost()
   */
  public void setConnectionHost(String value) {
    getWorkingCopy().setAttribute(CONNECTION_HOST, value);
  }

  /**
   * @see #getConnectionPort()
   */
  public void setConnectionPort(int value) {
    getWorkingCopy().setAttribute(CONNECTION_PORT, value);
  }

  public void setEnableDebugging(boolean value) {
    getWorkingCopy().setAttribute(VM_ENABLE_DEBUGGING, value);
  }

  public void setHeapMB(String value) {
    getWorkingCopy().setAttribute(VM_HEAP_MB, value);
  }

  /**
   * @see #getProjectName()
   */
  public void setProjectName(String value) {
    getWorkingCopy().setAttribute(PROJECT_NAME, value);

    if (getApplicationResource() == null) {
      updateMappedResources(value);
    }
  }

  public void setShouldLaunchFile(boolean value) {
    getWorkingCopy().setAttribute(IS_FILE, value);
  }

  public void setShowLaunchOutput(boolean value) {
    getWorkingCopy().setAttribute(SHOW_LAUNCH_OUTPUT, value);
  }

  /**
   * @see #getUrl()
   */
  public void setUrl(String value) {
    getWorkingCopy().setAttribute(URL, value);
  }

  public void setUseDefaultBrowser(boolean value) {
    getWorkingCopy().setAttribute(USE_DEFAULT_BROWSER, value);
  }

  protected ILaunchConfigurationWorkingCopy getWorkingCopy() {
    return (ILaunchConfigurationWorkingCopy) launchConfig;
  }

  private void updateMappedResources(String resourcePath) {
    IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(resourcePath);

    if (resource != null && !(resource instanceof IWorkspaceRoot)) {
      getWorkingCopy().setMappedResources(new IResource[] {resource});
    } else {
      getWorkingCopy().setMappedResources(null);
    }
  }

}
