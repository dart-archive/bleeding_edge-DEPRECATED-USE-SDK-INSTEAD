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

import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.core.DartLaunchConfigurationDelegate;
import com.google.dart.tools.debug.core.util.ResourceServer;
import com.google.dart.tools.debug.core.util.ResourceServerManager;
import com.google.dart.tools.debug.ui.internal.util.LaunchUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Launches the Dart application (compiled to js) in the browser.
 */
public class BrowserLaunchConfigurationDelegate extends DartLaunchConfigurationDelegate {

  /**
   * Match both the input and id, so that different types of editor can be opened on the same input.
   */
  @Override
  public void doLaunch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor, InstrumentationBuilder instrumentation) throws CoreException {

    mode = ILaunchManager.RUN_MODE;

    DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(configuration);
    wrapper.markAsLaunched();

    String url;

    if (wrapper.getShouldLaunchFile()) {
      IResource resource = wrapper.getApplicationResource();

      if (resource == null) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            Messages.BrowserLaunchConfigurationDelegate_HtmlFileNotFound));
      }

      try {
        // This returns just a plain file: url.
        ResourceServer server = ResourceServerManager.getServer();

        url = server.getUrlForResource(resource);

        url = wrapper.appendQueryParams(url);
      } catch (IOException ioe) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            "Could not launch browser - unable to start embedded server",
            ioe));
      }
    } else {
      url = wrapper.getUrl();

      try {
        String scheme = new URI(url).getScheme();

        if (scheme == null) { // add scheme else browser will not launch
          url = "http://" + url;
        }
      } catch (URISyntaxException e) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            Messages.BrowserLaunchConfigurationDelegate_UrlError));
      }
    }

    if (DartDebugCorePlugin.getPlugin().getIsDefaultBrowser()) {
      LaunchUtils.openBrowser(url);
    } else {
      LaunchUtils.launchInExternalBrowser(url);
    }

    DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);

  }

  @SuppressWarnings("unused")
  private IResource locateMappedFile(IResource resourceFile) {
    String mappingPath = DartCore.getResourceRemapping((IFile) resourceFile);

    if (mappingPath != null) {
      IResource mappedResource = ResourcesPlugin.getWorkspace().getRoot().findMember(
          Path.fromPortableString(mappingPath));

      if (mappedResource != null && mappedResource.exists()) {
        return mappedResource;
      }
    }
    return null;
  }

}
