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
package com.google.dart.tools.debug.core;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartProject;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

/**
 * A wrapper class around ILaunchConfiguration and ILaunchConfigurationWorkingCopy objects. It adds
 * compiler type checking to what is essentially a property map.
 */
public class DartLaunchConfigWrapper {
  public static final String CONNECTION_TYPE_CHROME = "Chrome";

  public static final String CONNECTION_TYPE_V8 = "V8";
  public static final String[] CONNECTION_TYPES = new String[] {"Chrome", "V8"};

  public static final int DEFAULT_CHROME_PORT = 9222;
  public static final String DEFAULT_HOST = "localhost";

  private static final String VM_ARGUMENTS = "vmArguments";
  private static final String APPLICATION_ARGUMENTS = "applicationArguments";
  private static final String APPLICATION_NAME = "applicationName";

  private static final String BROWSER_CONFIG = "browserConfig";
  private static final String CONNECTION_HOST = "connectionHost";
  private static final String CONNECTION_PORT = "connectionPort";

  private static final String CONNECTION_TYPE = "connectionType";

  private static final String PROJECT_NAME = "projectName";

  private static final String LIBRARY_LOCATION = "libraryLocation";

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

  /**
   * @return the arguments string for the Dart application
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
   * @return the arguments for the Dart application
   */
  public String[] getArgumentsAsArray() {
    String command = getArguments();

    if (command == null || command.length() == 0) {
      return new String[0];
    }

    return command.split(" ");
  }

  /**
   * @return the browser config to use for the launch configuration
   */
  public String getBrowserConfig() {
    try {
      return launchConfig.getAttribute(BROWSER_CONFIG, "");
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);

      return "";
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

  /**
   * @return the remote debug protocol connection type (Chrome, V8, ...)
   */
  public String getConnectionType() {
    try {
      return launchConfig.getAttribute(CONNECTION_TYPE, CONNECTION_TYPES[0]);
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);

      return CONNECTION_TYPES[0];
    }
  }

  public String getLibraryLocation() {

    try {
      return launchConfig.getAttribute(LIBRARY_LOCATION, "");
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);
      return "";
    }

  }

  /**
   * @return the DartProject that contains the application to run
   */
  public DartProject getProject() {
    return DartCore.create(ResourcesPlugin.getWorkspace().getRoot()).getDartProject(
        getProjectName());
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

  /**
   * @return the arguments string for the Dart VM
   */
  public String getVmArguments() {
    try {
      return launchConfig.getAttribute(VM_ARGUMENTS, "");
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);

      return "";
    }
  }

  /**
   * @return the arguments for the Dart VM
   */
  public String[] getVmArgumentsAsArray() {
    // TODO(keertip): add --new_gen_heap_size=64 to list of vm args
    String command = getVmArguments();

    if (command == null || command.length() == 0) {
      return new String[0];
    }

    return command.split(" ");
  }

  /**
   * @see #getApplicationName()
   */
  public void setApplicationName(String value) {
    getWorkingCopy().setAttribute(APPLICATION_NAME, value);
  }

  /**
   * @see #getArguments()
   */
  public void setArguments(String value) {
    getWorkingCopy().setAttribute(APPLICATION_ARGUMENTS, value);
  }

  /**
   * @see #getBrowserConfig()
   */
  public void setBrowserConfig(String value) {
    getWorkingCopy().setAttribute(BROWSER_CONFIG, value);
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

  /**
   * @see #getConnectionType()
   */
  public void setConnectionType(String value) {
    getWorkingCopy().setAttribute(CONNECTION_TYPE, value);
  }

  public void setLibraryLocation(String location) {
    getWorkingCopy().setAttribute(LIBRARY_LOCATION, location);
  }

  /**
   * @see #getProjectName()
   */
  public void setProjectName(String value) {
    getWorkingCopy().setAttribute(PROJECT_NAME, value);
  }

  /**
   * @see #getVmArguments()
   */
  public void setVmArguments(String value) {
    getWorkingCopy().setAttribute(VM_ARGUMENTS, value);
  }

  protected ILaunchConfigurationWorkingCopy getWorkingCopy() {
    return (ILaunchConfigurationWorkingCopy) launchConfig;
  }

}
