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
package com.google.dart.tools.debug.ui.internal.server;

import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.ui.internal.DartUtil;
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
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The launch shortcut for the Dart Server Application launch configuration.
 */
public class DartServerLaunchShortcut implements ILaunchShortcut, ILaunchShortcutExt {

  /**
   * Create a new DartServerLaunchShortcut.
   */
  public DartServerLaunchShortcut() {

  }

  @Override
  public boolean canLaunch(IResource resource) {
    if (!(resource instanceof IFile)) {
      return false;
    }

    DartLibrary[] libraries = LaunchUtils.getDartLibraries(resource);

    if (libraries.length > 0) {
      for (DartLibrary library : libraries) {
        if (library instanceof DartLibraryImpl) {
          DartLibraryImpl impl = (DartLibraryImpl) library;
          if (impl.isServerApplication()) {
            return true;
          }
        }
      }
    }

    return false;
  }

  @Override
  public ILaunchConfiguration[] getAssociatedLaunchConfigurations(IResource resource) {
    List<ILaunchConfiguration> results = new ArrayList<ILaunchConfiguration>();

    try {
      ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(
          getConfigurationType());

      for (int i = 0; i < configs.length; i++) {
        ILaunchConfiguration config = configs[i];

        if (testSimilar(resource, config)) {
          results.add(config);
        }
      }
    } catch (CoreException e) {
      DartUtil.logError(e);
    }

    return results.toArray(new ILaunchConfiguration[results.size()]);
  }

  @Override
  public void launch(IEditorPart editor, String mode) {
    IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);

    if (resource != null) {
      launch(resource, mode);
    }
  }

  @Override
  public void launch(ISelection selection, String mode) {
    if (selection instanceof IStructuredSelection) {
      Object sel = ((IStructuredSelection) selection).getFirstElement();

      if (sel instanceof IResource) {
        launch((IResource) sel, mode);
      }
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

  protected final ILaunchConfiguration findConfig(IResource resource) {
    List<ILaunchConfiguration> candidateConfigs = Collections.emptyList();

    try {
      ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(
          getConfigurationType());

      candidateConfigs = new ArrayList<ILaunchConfiguration>(configs.length);

      for (int i = 0; i < configs.length; i++) {
        ILaunchConfiguration config = configs[i];

        if (testSimilar(resource, config)) {
          candidateConfigs.add(config);
        }
      }
    } catch (CoreException e) {
      DartUtil.logError(e);
    }

    int candidateCount = candidateConfigs.size();

    if (candidateCount == 1) {
      return candidateConfigs.get(0);
    } else if (candidateCount > 1) {
      return LaunchUtils.chooseConfiguration(candidateConfigs);
    }

    return null;
  }

  protected ILaunchConfigurationType getConfigurationType() {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(DartDebugCorePlugin.SERVER_LAUNCH_CONFIG_ID);

    return type;
  }

  /**
   * Launch the Dart application associated with the specified resource.
   * 
   * @param resource the resource
   * @param mode the launch mode ("run", "debug", ...)
   */
  protected void launch(IResource resource, String mode) {

    if (resource == null) {
      return;
    }

    // Launch an existing configuration if one exists
    ILaunchConfiguration config = findConfig(resource);
    if (config != null) {
      DebugUITools.launch(config, mode);
      return;
    }

    // Create and launch a new configuration
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(DartDebugCorePlugin.SERVER_LAUNCH_CONFIG_ID);
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

    try {
      config = launchConfig.doSave();
    } catch (CoreException e) {
      DartUtil.logError(e);
      return;
    }

    DebugUITools.launch(config, mode);
  }

  protected boolean testSimilar(IResource resource, ILaunchConfiguration config) {
    return LaunchUtils.isLaunchableWith(resource, config);
  }

}
