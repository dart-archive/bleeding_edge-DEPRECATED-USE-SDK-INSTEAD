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
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.core.DartLaunchConfigurationDelegate;
import com.google.dart.tools.debug.core.DebugUIHelper;
import com.google.dart.tools.debug.core.pubserve.PubCallback;
import com.google.dart.tools.debug.core.pubserve.PubResult;
import com.google.dart.tools.debug.core.pubserve.PubServeManager;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.util.LaunchUtils;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Launches the Dart application (compiled to js) in the browser.
 */
public class BrowserLaunchConfigurationDelegate extends DartLaunchConfigurationDelegate {

  private String url;

  private static DartLaunchConfigWrapper wrapper;

  private static PubCallback<String> pubConnectionCallback = new PubCallback<String>() {

    @Override
    public void handleResult(PubResult<String> result) {
      if (result.isError()) {
        DebugUIHelper.getHelper().showError(
            "Launch Error",
            "Pub serve communication error: " + result.getErrorMessage());
        return;
      }

      try {
        String launchUrl = result.getResult();
        launchUrl = wrapper.appendQueryParams(launchUrl);
        if (DartDebugCorePlugin.getPlugin().getIsDefaultBrowser()) {
          LaunchUtils.openBrowser(launchUrl);
        } else {
          LaunchUtils.launchInExternalBrowser(launchUrl);
        }
      } catch (CoreException e) {
        DartDebugUIPlugin.logError(e);
      }
    }
  };

  @Override
  public void doLaunch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor, InstrumentationBuilder instrumentation) throws CoreException {

    wrapper = new DartLaunchConfigWrapper(configuration);
    wrapper.markAsLaunched();

    if (wrapper.getShouldLaunchFile()) {
      IResource resource = wrapper.getApplicationResource();

      if (resource == null) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            Messages.BrowserLaunchConfigurationDelegate_HtmlFileNotFound));
      }

      // launch pub serve 
      PubServeManager manager = PubServeManager.getManager();

      try {

        manager.serve(wrapper, pubConnectionCallback);

      } catch (Exception e) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            "Could not start pub serve or connect to pub\n",
            e));
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
      if (DartDebugCorePlugin.getPlugin().getIsDefaultBrowser()) {
        LaunchUtils.openBrowser(url);
      } else {
        LaunchUtils.launchInExternalBrowser(url);
      }
    }

    DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);

  }
}
