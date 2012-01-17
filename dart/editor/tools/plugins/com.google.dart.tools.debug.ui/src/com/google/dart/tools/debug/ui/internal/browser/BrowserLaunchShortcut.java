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

import com.google.dart.compiler.backend.js.JavascriptBackend;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.HTMLFile;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.ui.internal.DartUtil;
import com.google.dart.tools.debug.ui.internal.util.AbstractLaunchShortcut;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;

import java.io.File;

/**
 * This class interprets the current selection or the currently active editor and opens html page in
 * the browser as configured. This involves running an existing launch configuration or creating a
 * new one if an appropriate launch configuration does not already exist.
 */
public class BrowserLaunchShortcut extends AbstractLaunchShortcut {

  public static File getJsAppArtifactFile(IPath sourceLocation) {
    return sourceLocation.addFileExtension(JavascriptBackend.EXTENSION_APP_JS).toFile();
  }

  /**
   * Create a new BrowserLaunchShortcut.
   */
  public BrowserLaunchShortcut() {
    super("Application");
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
    if (resource == null) {
      return;
    }

    // before proceeding with launch, check if js has been generated
    DartElement element = DartCore.create(resource);

    if (element == null) {
      DartDebugCorePlugin.logError("File is not associated with a Dart library");
    } else if (!(element instanceof HTMLFile)) {
      DartDebugCorePlugin.logError("File is not an html file");
    } else {

      HTMLFile htmlFile = (HTMLFile) element;

      try {
        if (htmlFile.getReferencedLibraries().length > 0) {
          DartLibrary library = htmlFile.getReferencedLibraries()[0];
          File jsOutFile = getJsAppArtifactFile(library.getCorrespondingResource().getLocation());

          if (!jsOutFile.exists()) {
            DartDebugCorePlugin.logError("The Javascript output was not generated for the library");
//            MessageDialog.openError(
//                window.getShell(),
//                NLS.bind("Unable to Launch File {0}", resource.getName()),
//                NLS.bind("The Javascript output was not generated for the {1} library",
//                    library.getDisplayName()));
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
    launchWrapper.setApplicationName(resource.getLocation().toOSString());

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

    DartLaunchConfigWrapper launchWrapper = new DartLaunchConfigWrapper(config);

    String resourcePath = resource.getLocation().toOSString();

    // TODO(keertip): Check for more launch config params to match
    return resourcePath.equals(launchWrapper.getApplicationName());

  }

}
