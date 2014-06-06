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
package com.google.dart.tools.debug.ui.internal.mobile;

import com.google.dart.tools.core.mobile.AndroidDebugBridge;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.ui.internal.DartUtil;
import com.google.dart.tools.debug.ui.internal.dialogs.ManageLaunchesDialog;
import com.google.dart.tools.debug.ui.internal.util.AbstractLaunchShortcut;
import com.google.dart.tools.debug.ui.internal.util.ILaunchShortcutExt;
import com.google.dart.tools.debug.ui.internal.util.LaunchUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.ui.PlatformUI;

/**
 * A launch shortcut to allow users to launch Dart applications on Mobile.
 */
public class MobileLaunchShortcut extends AbstractLaunchShortcut implements ILaunchShortcutExt {

  public MobileLaunchShortcut() {
    super("Mobile");
  }

  @Override
  public boolean canLaunch(IResource resource) {
    if (!DartSdkManager.getManager().hasSdk()) {
      return false;
    }

    if (resource instanceof IFile) {
      if ("html".equalsIgnoreCase(resource.getFileExtension())) {
        return true;
      }
    }

    return isBrowserApplication(resource);
  }

  @Override
  protected ILaunchConfigurationType getConfigurationType() {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(DartDebugCorePlugin.MOBILE_LAUNCH_CONFIG_ID);

    return type;
  }

  @Override
  protected void launch(IResource resource, String mode) {
    if (resource == null) {
      return;
    }

    // Launch an existing configuration if one exists
    ILaunchConfiguration config;

    try {
      config = findConfig(resource);
    } catch (OperationCanceledException ex) {
      return;
    }

    if (config == null) {
      // Create and launch a new configuration
      ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
      ILaunchConfigurationType type = manager.getLaunchConfigurationType(DartDebugCorePlugin.MOBILE_LAUNCH_CONFIG_ID);
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
      launchWrapper.setUsePubServe(false);
      launchConfig.setMappedResources(new IResource[] {resource});

      try {
        config = launchConfig.doSave();
      } catch (CoreException e) {
        DartUtil.logError(e);
        return;
      }
    }

    // If device is not connected or not authorized then open launch dialog
    if (!AndroidDebugBridge.getAndroidDebugBridge().isDeviceConnectedAndAuthorized()) {
      ManageLaunchesDialog.openAsync(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), config);
      return;
    }

    DartLaunchConfigWrapper launchWrapper = new DartLaunchConfigWrapper(config);
    launchWrapper.markAsLaunched();
    LaunchUtils.clearDartiumConsoles();

    LaunchUtils.launch(config, mode);
  }

  @Override
  protected boolean testSimilar(IResource resource, ILaunchConfiguration config) {
    return super.testSimilar(resource, config);
  }

}
