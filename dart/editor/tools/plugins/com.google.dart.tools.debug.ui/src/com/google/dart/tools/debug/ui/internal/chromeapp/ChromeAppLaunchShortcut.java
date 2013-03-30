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

package com.google.dart.tools.debug.ui.internal.chromeapp;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.ui.internal.DartDebugUITools;
import com.google.dart.tools.debug.ui.internal.DartUtil;
import com.google.dart.tools.debug.ui.internal.util.AbstractLaunchShortcut;
import com.google.dart.tools.debug.ui.internal.util.ILaunchShortcutExt;
import com.google.dart.tools.debug.ui.internal.util.LaunchUtils;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;

/**
 * A launch shortcut to run Chrome packaged applications. This will launch a Chrome app pointed to
 * by a manifest.json file.
 * 
 * @see http://developer.chrome.com/extensions/manifest.html
 */
public class ChromeAppLaunchShortcut extends AbstractLaunchShortcut implements ILaunchShortcutExt {
  private static final String MANIFEST_FILE_NAME = "manifest.json";

  public ChromeAppLaunchShortcut() {
    super("Chrome App");
  }

  @Override
  public boolean canLaunch(IResource resource) {
    if (!DartSdkManager.getManager().hasSdk()
        || !DartSdkManager.getManager().getSdk().isDartiumInstalled()) {
      return false;
    }

    // If it's contained by a folder which itself contains a manifest.json file, we can launch it.
    if (resource instanceof IContainer) {
      return containsManifestJsonFile((IContainer) resource);
    } else {
      return containsManifestJsonFile(resource.getParent());
    }
  }

  @Override
  protected ILaunchConfigurationType getConfigurationType() {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(DartDebugCorePlugin.CHROMEAPP_LAUNCH_CONFIG_ID);

    return type;
  }

  @Override
  protected IResource getLaunchableResource(Object originalResource) throws DartModelException {
    if (!(originalResource instanceof IResource)) {
      return null;
    }

    IResource resource = (IResource) originalResource;

    if (resource instanceof IContainer) {
      return findManifestJsonFile((IContainer) resource);
    } else {
      return findManifestJsonFile(resource.getParent());
    }
  }

  @Override
  protected void launch(IResource resource, String mode) {
    if (resource == null) {
      return;
    }

    // Launch an existing configuration if one exists
    ILaunchConfiguration config = findConfig(resource);

    if (config == null) {
      // Create and launch a new configuration
      ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
      ILaunchConfigurationType type = manager.getLaunchConfigurationType(DartDebugCorePlugin.CHROMEAPP_LAUNCH_CONFIG_ID);

      try {
        ILaunchConfigurationWorkingCopy launchConfig = type.newInstance(
            null,
            manager.generateLaunchConfigurationName(getLaunchName(resource)));

        DartLaunchConfigWrapper launchWrapper = new DartLaunchConfigWrapper(launchConfig);

        launchWrapper.setApplicationName(resource.getFullPath().toString());
        launchWrapper.setProjectName(resource.getProject().getName());
        launchConfig.setMappedResources(new IResource[] {resource});

        config = launchConfig.doSave();
      } catch (CoreException e) {
        DartUtil.logError(e);
        return;
      }
    }

    LaunchUtils.clearDartiumConsoles();

    DartLaunchConfigWrapper launchWrapper = new DartLaunchConfigWrapper(config);
    launchWrapper.markAsLaunched();
    DartDebugUITools.launch(config, mode);
  }

  @Override
  protected boolean testSimilar(IResource resource, ILaunchConfiguration config) {
    DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(config);

    IResource configResource = wrapper.getApplicationResource();

    if (configResource == null || !isManifestFile(configResource)) {
      return false;
    }

    try {
      IResource launchAbleResource = getLaunchableResource(resource);

      return configResource.equals(launchAbleResource);
    } catch (DartModelException ex) {
      return false;
    }
  }

  private boolean containsManifestJsonFile(IContainer container) {
    return findManifestJsonFile(container) != null;
  }

  private IFile findManifestJsonFile(IContainer container) {
    if (container.exists(new Path(MANIFEST_FILE_NAME))) {
      return container.getFile(new Path(MANIFEST_FILE_NAME));
    }

    if (container.getParent() != null) {
      return findManifestJsonFile(container.getParent());
    } else {
      return null;
    }
  }

  /**
   * Given a manifest.json resource (http://developer.chrome.com/extensions/manifest.html), extract
   * the "name" field.
   * 
   * @param jsonResource
   * @return
   */
  private String getLaunchName(IResource jsonResource) {
    String name = parseNameFromJson(jsonResource);

    return name == null ? jsonResource.getName() : name;
  }

  private boolean isManifestFile(IResource resource) {
    return resource instanceof IFile && resource.getName().equals(MANIFEST_FILE_NAME);
  }

  /**
   * Do a best effort to extract the "name" field out of a manifest.json file. On any failure,
   * return null.
   */
  private String parseNameFromJson(IResource resource) {
    if (!(resource instanceof IFile)) {
      return null;
    }

    try {
      IFile file = (IFile) resource;
      String text = CharStreams.toString(new InputStreamReader(file.getContents(), Charsets.UTF_8));
      JSONObject obj = new JSONObject(text);
      return obj.optString("name", null);
    } catch (IOException ioe) {

    } catch (CoreException e) {

    } catch (JSONException e) {

    }

    return null;
  }

}
