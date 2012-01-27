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
package com.google.dart.tools.debug.ui.internal.browser;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.builder.DartBuilder;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.HTMLFile;
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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

import java.io.File;

/**
 * This class interprets the current selection or the currently active editor and opens html page in
 * the browser as configured. This involves running an existing launch configuration or creating a
 * new one if an appropriate launch configuration does not already exist.
 */
public class BrowserLaunchShortcut extends AbstractLaunchShortcut implements ILaunchShortcutExt {

  /**
   * Create a new BrowserLaunchShortcut.
   */
  public BrowserLaunchShortcut() {
    super("Application"); //$NON-NLS-1$
  }

  @Override
  public boolean canLaunch(IResource resource) {
    if (resource instanceof IFile) {
      if ("html".equalsIgnoreCase(resource.getFileExtension())) {
        return true;
      }
    }

    DartLibrary library = LaunchUtils.getDartLibrary(resource);

    if (library instanceof DartLibraryImpl) {
      DartLibraryImpl impl = (DartLibraryImpl) library;

      return impl.isBrowserApplication();
    } else {
      return false;
    }
  }

  @Override
  protected ILaunchConfigurationType getConfigurationType() {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(DartDebugCorePlugin.BROWSER_LAUNCH_CONFIG_ID);

    return type;
  }

  /**
   * Open in a browser the url or page specified
   * 
   * @param resource the resource
   * @param mode the launch mode ("run", "debug", ...)
   */
  @Override
  protected void launch(IResource resource, String mode) {
    mode = ILaunchManager.RUN_MODE;

    if (resource == null) {
      return;
    }

    // before proceeding with launch, check if js has been generated
    DartElement element = DartCore.create(resource);

    if (element == null) {
      DartDebugCorePlugin.logError(Messages.BrowserLaunchShortcut_NotInLibraryErrorMessage);
    } else if (!(element instanceof HTMLFile)) {
      DartDebugCorePlugin.logError(Messages.BrowserLaunchShortcut_NotHtmlFileErrorMessage);
    } else {

      HTMLFile htmlFile = (HTMLFile) element;

      try {
        if (htmlFile.getReferencedLibraries().length > 0) {
          DartLibrary library = htmlFile.getReferencedLibraries()[0];
          File jsOutFile = DartBuilder.getJsAppArtifactFile(library.getCorrespondingResource().getLocation());

          if (!jsOutFile.exists()) {
            String errMsg = NLS.bind(
                "The Javascript output was not generated for the {0} library.\nCannot find {1}.",
                library.getDisplayName(), jsOutFile.getPath());
            DartDebugCorePlugin.logError(errMsg);
            MessageDialog.openError(Display.getDefault().getActiveShell(),
                NLS.bind("Unable to Launch File {0}", resource.getName()), errMsg);
            return;
          }
        }
      } catch (DartModelException e) {
        DartDebugCorePlugin.logError(e);
      }
    }

    // Launch an existing configuration if one exists
    ILaunchConfiguration config = findConfig(resource);

    if (config != null) {
      DebugUITools.launch(config, mode);
      return;
    }

    // Create and launch a new configuration
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(DartDebugCorePlugin.BROWSER_LAUNCH_CONFIG_ID);
    ILaunchConfigurationWorkingCopy launchConfig = null;
    try {
      launchConfig = type.newInstance(null, resource.getName());
    } catch (CoreException ce) {
      DartUtil.logError(ce);
      return;
    }

    DartLaunchConfigWrapper launchWrapper = new DartLaunchConfigWrapper(launchConfig);

    launchWrapper.setProjectName(resource.getProject().getName());
    launchWrapper.setApplicationName(resource.getFullPath().toPortableString());
    launchWrapper.setUseDefaultBrowser(true);

    launchConfig.setMappedResources(new IResource[] {resource});

    try {
      config = launchConfig.doSave();
    } catch (CoreException e) {
      DartUtil.logError(e);
      return;
    }

    DebugUITools.launch(config, mode);
  }

  @Override
  protected boolean testSimilar(IResource resource, ILaunchConfiguration config) {
    return LaunchUtils.isLaunchableWith(resource, config);
  }

}
