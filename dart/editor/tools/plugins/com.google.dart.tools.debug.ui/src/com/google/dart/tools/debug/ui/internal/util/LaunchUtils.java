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
package com.google.dart.tools.debug.ui.internal.util;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.HTMLFile;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.ui.internal.DartUtil;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * A utility class for launching and launch configurations.
 */
public class LaunchUtils {

  private static List<ILaunchShortcut> shortcuts;

  /**
   * Allow the user to choose one from a set of launch configurations.
   * 
   * @param configList
   * @return
   */
  public static ILaunchConfiguration chooseConfiguration(List<ILaunchConfiguration> configList) {
    IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();

    ElementListSelectionDialog dialog = new ElementListSelectionDialog(
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), labelProvider);
    dialog.setElements(configList.toArray());
    dialog.setTitle("Select Dart Application");
    dialog.setMessage("&Select existing configuration:");
    dialog.setMultipleSelection(false);
    int result = dialog.open();
    labelProvider.dispose();

    if (result == Window.OK) {
      return (ILaunchConfiguration) dialog.getFirstResult();
    }

    return null;
  }

  /**
   * @return given an IResource, return the cooresponding DartLibrary
   */
  public static DartLibrary getDartLibrary(IResource resource) {
    DartElement element = DartCore.create(resource);

    if (element instanceof CompilationUnit) {
      CompilationUnit unit = (CompilationUnit) element;

      return unit.getLibrary();
    } else if (element instanceof DartLibrary) {
      return (DartLibrary) element;
    } else if (element instanceof HTMLFile) {
      HTMLFile htmlFile = (HTMLFile) element;

      try {
        DartLibrary libraries[] = htmlFile.getReferencedLibraries();

        if (libraries.length > 0) {
          return libraries[0];
        }
      } catch (DartModelException exception) {
        DartUtil.logError(exception);
      }

      return null;
    } else {
      return null;
    }
  }

  /**
   * @return a list of all the launch shortcuts in the system
   */
  public static List<ILaunchShortcut> getLaunchShortcuts() {
    if (shortcuts == null) {
      shortcuts = new ArrayList<ILaunchShortcut>();

      IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(
          IDebugUIConstants.PLUGIN_ID, IDebugUIConstants.EXTENSION_POINT_LAUNCH_SHORTCUTS);
      IConfigurationElement[] infos = extensionPoint.getConfigurationElements();

      try {
        for (IConfigurationElement element : infos) {
          ILaunchShortcut shortcut = (ILaunchShortcut) element.createExecutableExtension("class");

          shortcuts.add(shortcut);
        }
      } catch (CoreException ce) {
        DartUtil.logError(ce);
      }
    }

    return shortcuts;
  }

  /**
   * @param resource
   * @param config
   * @return whether the given launch config could be used to launch the given resource
   */
  public static boolean isLaunchableWith(IResource resource, ILaunchConfiguration config) {
    DartLaunchConfigWrapper launchWrapper = new DartLaunchConfigWrapper(config);

    DartLibrary testLibrary = LaunchUtils.getDartLibrary(resource);
    DartLibrary existingLibrary = LaunchUtils.getDartLibrary(launchWrapper.getApplicationResource());

    return testLibrary != null && testLibrary.equals(existingLibrary);
  }

  private LaunchUtils() {

  }

}
