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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.utilities.general.StringUtilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * A wrapper class around ILaunchConfiguration and ILaunchConfigurationWorkingCopy objects. It adds
 * compiler type checking to what is essentially a property map.
 */
public class DartLaunchConfigWrapper {
  private static final String APPLICATION_ARGUMENTS = "applicationArguments";
  private static final String APPLICATION_ENVIRONMENT = "applicationEnvironment";
  private static final String APPLICATION_NAME = "applicationName";
  private static final String SOURCE_DIRECTORY = "sourceDirectory";
  private static final String URL_QUERY_PARAMS = "urlQueryParams";
  private static final String DART2JS_FLAGS = "dart2jsFlags";
  private static final String INSTALL_CONTENT_SHELL = "installContentShell";
  private static final String LAUNCH_CONTENT_SHELL = "runContentShell";
  private static final String USE_PUB_SERVE = "usePubServe";

  private static final String VM_CHECKED_MODE = "vmCheckedMode";
  private static final String PAUSE_ISOLATE_ON_EXIT = "pauseIsolateOnExit";
  private static final String PAUSE_ISOLATE_ON_START = "pauseIsolateOnStart";
  private static final String OBSERVATORY_PORT = "observatoryPort";
  private static final String SHOW_LAUNCH_OUTPUT = "showLaunchOutput";

  // --enable-experimental-webkit-features and --enable-devtools-experiments
  private static final String DARTIUM_USE_WEB_COMPONENTS = "enableExperimentalWebkitFeatures";

  private static final String IS_FILE = "launchHtmlFile";
  private static final String URL = "url";

  private static final String CONNECTION_HOST = "connectionHost";
  private static final String CONNECTION_PORT = "connectionPort";

  private static final String PROJECT_NAME = "projectName";
  private static final String WORKING_DIRECTORY = "workingDirectory";

  private static final String LAST_LAUNCH_TIME = "launchTime";
  private static final String VM_ARGUMENTS = "vmArguments";

  private ILaunchConfiguration launchConfig;

  /**
   * Create a new DartLaunchConfigWrapper given either a ILaunchConfiguration (for read-only launch
   * configs) or ILaunchConfigurationWorkingCopy (for writeable launch configs).
   */
  public DartLaunchConfigWrapper(ILaunchConfiguration launchConfig) {
    this.launchConfig = launchConfig;
  }

  /**
   * Return either the original url, or url + '?' + params.
   * 
   * @param url
   * @return
   */
  public String appendQueryParams(String url) {
    if (getUrlQueryParams().length() > 0) {
      return url + "?" + getUrlQueryParams();
    } else {
      return url;
    }
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
      return ResourcesPlugin.getWorkspace().getRoot().findMember(path);
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

  public boolean getCheckedMode() {
    return getCheckedMode(true);
  }

  public boolean getCheckedMode(boolean defaultValue) {
    try {
      return launchConfig.getAttribute(VM_CHECKED_MODE, defaultValue);
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
   * @return the string for additional flags to Dart2js
   */
  public String getDart2jsFlags() {
    try {
      return launchConfig.getAttribute(DART2JS_FLAGS, "");
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);

      return "";
    }
  }

  /**
   * @return the string for additional flags to Dart2js
   */
  public String[] getDart2jsFlagsAsArray() {
    String command = getDart2jsFlags();

    if (command == null || command.length() == 0) {
      return new String[0];
    }

    return StringUtilities.parseArgumentString(command);
  }

  /**
   * @return any configured environment variables
   */
  public Map<String, String> getEnvironment() {
    String env = getEnvironmentString();

    Map<String, String> map = new HashMap<String, String>();

    if (env.isEmpty()) {
      return map;
    }

    Properties props = new Properties();

    try {
      props.load(new StringReader(env));
    } catch (IOException e) {

    }

    for (Object key : props.keySet()) {
      String strKey = (String) key;
      map.put(strKey, props.getProperty(strKey));
    }

    return map;
  }

  public String getEnvironmentString() {
    try {
      return launchConfig.getAttribute(APPLICATION_ENVIRONMENT, "");
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);

      return "";
    }
  }

  public boolean getInstallContentShell() {
    try {
      return launchConfig.getAttribute(INSTALL_CONTENT_SHELL, true);
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);

      return false;
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

  public boolean getLaunchContentShell() {
    try {
      return launchConfig.getAttribute(LAUNCH_CONTENT_SHELL, true);
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);

      return false;
    }
  }

  public int getObservatoryPort() {
    try {
      return launchConfig.getAttribute(OBSERVATORY_PORT, -1);
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);

      return -1;
    }
  }

  public boolean getPauseIsolateOnExit() {
    try {
      return launchConfig.getAttribute(PAUSE_ISOLATE_ON_EXIT, false);
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);

      return false;
    }
  }

  public boolean getPauseIsolateOnStart() {
    try {
      return launchConfig.getAttribute(PAUSE_ISOLATE_ON_START, false);
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);

      return false;
    }
  }

  /**
   * @return the DartProject that contains the application to run
   */
  public IProject getProject() {
    if (getShouldLaunchFile()) {
      IResource resource = getApplicationResource();

      if (resource != null) {
        return resource.getProject();
      }
    } else {
      IContainer container = getSourceDirectory();

      if (container != null) {
        return container.getProject();
      }
    }

    String projectName = getProjectName();

    if (projectName.length() > 0) {
      return ResourcesPlugin.getWorkspace().getRoot().getProject(getProjectName());
    }

    return null;
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

  public IContainer getSourceDirectory() {
    String path = getSourceDirectoryName();

    if (path == null || path.length() == 0) {
      return null;
    } else {
      IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);

      return (resource instanceof IContainer ? (IContainer) resource : null);
    }
  }

  public String getSourceDirectoryName() {
    try {
      return launchConfig.getAttribute(SOURCE_DIRECTORY, "");
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);

      return "";
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

  /**
   * @return the url query parameters, if any
   */
  public String getUrlQueryParams() {
    try {
      return launchConfig.getAttribute(URL_QUERY_PARAMS, "");
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);

      return "";
    }
  }

  public boolean getUsePubServe() {
    try {
      return launchConfig.getAttribute(USE_PUB_SERVE, true);
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);

      return true;
    }
  }

  public boolean getUseWebComponents() {
    try {
      return launchConfig.getAttribute(DARTIUM_USE_WEB_COMPONENTS, true);
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);

      return true;
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

    List<String> args = new ArrayList<String>();
    args.addAll(Arrays.asList(StringUtilities.parseArgumentString(getVmArguments())));

    if (getCheckedMode()) {
      args.add("--enable-checked-mode");
    }

    return args.toArray(new String[args.size()]);
  }

  /**
   * @return the cwd for command-line launches
   */
  public String getWorkingDirectory() {
    try {
      return launchConfig.getAttribute(WORKING_DIRECTORY, "");
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);

      return "";
    }
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

  public void save() {
    try {
      getWorkingCopy().doSave();
    } catch (CoreException e) {
      DartCore.logError(e);
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

  /**
   * @see #getDart2jsFlags()
   */
  public void setDart2jsFlags(String value) {
    getWorkingCopy().setAttribute(DART2JS_FLAGS, value);
  }

  public void setEnvironmentString(String value) {
    getWorkingCopy().setAttribute(APPLICATION_ENVIRONMENT, value);
  }

  public void setInstallContentShell(boolean value) {
    getWorkingCopy().setAttribute(INSTALL_CONTENT_SHELL, value);
  }

  public void setLaunchContentShell(boolean value) {
    getWorkingCopy().setAttribute(LAUNCH_CONTENT_SHELL, value);
  }

  public void setObservatoryPort(int value) {
    getWorkingCopy().setAttribute(OBSERVATORY_PORT, value);
  }

  public void setPauseIsolateOnExit(boolean value) {
    getWorkingCopy().setAttribute(PAUSE_ISOLATE_ON_EXIT, value);
  }

  public void setPauseIsolateOnStart(boolean value) {
    getWorkingCopy().setAttribute(PAUSE_ISOLATE_ON_START, value);
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
   * @see #getSourceDirectoryName()
   */
  public void setSourceDirectoryName(String value) {
    getWorkingCopy().setAttribute(SOURCE_DIRECTORY, value);
  }

  /**
   * @see #getUrl()
   */
  public void setUrl(String value) {
    getWorkingCopy().setAttribute(URL, value);
  }

  /**
   * @see #getUrlQueryParams()()
   */
  public void setUrlQueryParams(String value) {
    getWorkingCopy().setAttribute(URL_QUERY_PARAMS, value);
  }

  public void setUsePubServe(boolean value) {
    getWorkingCopy().setAttribute(USE_PUB_SERVE, value);
  }

  public void setUseWebComponents(boolean value) {
    getWorkingCopy().setAttribute(DARTIUM_USE_WEB_COMPONENTS, value);
  }

  /**
   * @see #getVmArguments()
   */
  public void setVmArguments(String value) {
    getWorkingCopy().setAttribute(VM_ARGUMENTS, value);
  }

  public void setWorkingDirectory(String value) {
    getWorkingCopy().setAttribute(WORKING_DIRECTORY, value);
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
