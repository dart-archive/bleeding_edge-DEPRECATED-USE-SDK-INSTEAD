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
package com.google.dart.tools.debug.ui.internal.dartium;

import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.ui.internal.DartUtil;
import com.google.dart.tools.debug.ui.internal.util.AbstractLaunchShortcut;
import com.google.dart.tools.debug.ui.internal.util.ILaunchShortcutExt;
import com.google.dart.tools.debug.ui.internal.util.LaunchUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;

/**
 * A launch shortcut to allow users to launch Dart applications in Chromium / Dartium.
 */
public class DartiumLaunchShortcut extends AbstractLaunchShortcut implements ILaunchShortcutExt {

  public DartiumLaunchShortcut() {
    super("Chromium");
  }

  @Override
  public boolean canLaunch(IResource resource) {
    if (!DartSdkManager.getManager().hasSdk()) {
      return false;
    }

    if (!DartSdkManager.getManager().getSdk().isDartiumInstalled()) {
      return false;
    }

    if (resource instanceof IFile) {
      if ("html".equalsIgnoreCase(resource.getFileExtension())) {
        return true;
      }
    }

    DartLibrary[] libraries = LaunchUtils.getDartLibraries(resource);

    if (libraries.length > 0) {
      for (DartLibrary library : libraries) {
        if (library instanceof DartLibraryImpl) {
          DartLibraryImpl impl = (DartLibraryImpl) library;
          if (impl.isBrowserApplication()) {
            return true;
          }
        }
      }
    }

    return false;
  }

  @Override
  protected ILaunchConfigurationType getConfigurationType() {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(DartDebugCorePlugin.DARTIUM_LAUNCH_CONFIG_ID);

    return type;
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
      ILaunchConfigurationType type = manager.getLaunchConfigurationType(DartDebugCorePlugin.DARTIUM_LAUNCH_CONFIG_ID);
      ILaunchConfigurationWorkingCopy launchConfig = null;
      try {
        launchConfig = type.newInstance(
            null,
            manager.generateLaunchConfigurationName(resource.getName()));
      } catch (CoreException ce) {
        DartUtil.logError(ce);
        return;
      }

      DartLaunchConfigWrapper launchWrapper = new DartLaunchConfigWrapper(launchConfig);

      launchWrapper.setApplicationName(resource.getFullPath().toString());
      launchWrapper.setProjectName(resource.getProject().getName());
      launchConfig.setMappedResources(new IResource[] {resource});

      try {
        config = launchConfig.doSave();
      } catch (CoreException e) {
        DartUtil.logError(e);
        return;
      }
    }

    try {
      ILaunchConfiguration configCopy = config.copy(LaunchUtils.DARTIUM_LAUNCH_NAME);
      DartLaunchConfigWrapper launchWrapper = new DartLaunchConfigWrapper(config);
      launchWrapper.markAsLaunched();
      LaunchUtils.clearDartiumConsoles();

      if (configCopy != null) {
        DebugUITools.launch(configCopy, mode);
      } else {
        DebugUITools.launch(config, mode);
      }
    } catch (CoreException e) {
      DartUtil.logError(e);
    }

  }

  @Override
  protected boolean testSimilar(IResource resource, ILaunchConfiguration config) {
    return LaunchUtils.isLaunchableWith(resource, config);
  }

}
