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
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.HTMLFile;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.program.Program;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Launches the Dart application (compiled to js) in the browser.
 */
public class BrowserLaunchConfigurationDelegate extends LaunchConfigurationDelegate {

  /**
   * Match both the input and id, so that different types of editor can be opened on the same input.
   */
  @Override
  public void launch(ILaunchConfiguration config, String mode, ILaunch launch,
      IProgressMonitor monitor) throws CoreException {
    mode = ILaunchManager.RUN_MODE;
    DartLaunchConfigWrapper launchConfig = new DartLaunchConfigWrapper(config);
    String url;

    if (launchConfig.getShouldLaunchFile()) {
      IResource resource = launchConfig.getApplicationResource();
      if (resource == null) {
        throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
            Messages.BrowserLaunchConfigurationDelegate_HtmlFileNotFound));
      }

      checkJavascriptIsAvailable(resource);

      url = resource.getLocationURI().toString();
    } else {
      url = launchConfig.getUrl();
      try {
        String scheme = new URI(url).getScheme();
        if (scheme == null) { // add scheme else browser will not launch
          url = "http://" + url;
        }
      } catch (URISyntaxException e) {
        throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
            Messages.BrowserLaunchConfigurationDelegate_UrlError));
      }
    }

    if (launchConfig.getUseDefaultBrowser()) {
      Program.launch(url);
    } else {
      launchInExternalBrowser(launchConfig, url);
    }
  }

  /**
   * Before proceeding with launch, check if Javascript has been generated.
   * 
   * @param resource
   * @throws CoreException
   */
  private void checkJavascriptIsAvailable(IResource resource) throws CoreException {
    DartElement element = DartCore.create(resource);

    if (element == null) {
      throw new CoreException(new Status(IStatus.ERROR, DartDebugUIPlugin.PLUGIN_ID,
          Messages.BrowserLaunchShortcut_NotInLibraryErrorMessage));
    } else if (!(element instanceof HTMLFile)) {
      throw new CoreException(new Status(IStatus.ERROR, DartDebugUIPlugin.PLUGIN_ID,
          Messages.BrowserLaunchShortcut_NotHtmlFileErrorMessage));
    } else {
      HTMLFile htmlFile = (HTMLFile) element;

      try {
        if (htmlFile.getReferencedLibraries().length > 0) {
          DartLibrary library = htmlFile.getReferencedLibraries()[0];
          File jsOutFile = DartBuilder.getJsAppArtifactFile(library.getCorrespondingResource().getLocation());

          if (!jsOutFile.exists()) {
            String errMsg = NLS.bind(
                "Unable to launch {0}. The Javascript output was not generated for the {1} library.",
                resource.getName(), library.getDisplayName());

            DartDebugCorePlugin.logError(errMsg);

            throw new CoreException(new Status(IStatus.ERROR, DartDebugUIPlugin.PLUGIN_ID, errMsg));
          }
        }
      } catch (DartModelException e) {
        DartDebugCorePlugin.logError(e);

        throw new CoreException(new Status(IStatus.ERROR, DartDebugUIPlugin.PLUGIN_ID,
            e.toString(), e));
      }
    }
  }

  private Program findProgram(String name) {
    Program[] programs = Program.getPrograms();
    for (Program program : programs) {
      if (program.getName().equals(name)) {
        return program;
      }
    }

    return null;
  }

  private void launchInExternalBrowser(DartLaunchConfigWrapper launchConfig, String url)
      throws CoreException {
    String browserName = launchConfig.getBrowserName();
    if (!browserName.isEmpty()) {
      Program program = findProgram(browserName);
      if (program != null) {
        program.execute(url);
      } else {
        throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
            Messages.BrowserLaunchConfigurationDelegate_BrowserNotFound));
      }
    }
  }

}
