/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.tools.debug.core.mobile;

import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.tools.core.mobile.AndroidDebugBridge;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.core.DartLaunchConfigurationDelegate;
import com.google.dart.tools.debug.core.DebugUIHelper;
import com.google.dart.tools.debug.core.pubserve.PubCallback;
import com.google.dart.tools.debug.core.pubserve.PubResult;
import com.google.dart.tools.debug.core.pubserve.PubServeManager;
import com.google.dart.tools.debug.core.util.ResourceServer;
import com.google.dart.tools.debug.core.util.ResourceServerManager;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * A LaunchConfigurationDelegate to launch in the browser on a connected device.
 */
public class MobileLaunchConfigurationDelegate extends DartLaunchConfigurationDelegate {

  private PubCallback<String> pubConnectionCallback = new PubCallback<String>() {
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
        launchOnMobile(launchUrl);
      } catch (CoreException e) {
        DartDebugCorePlugin.logError(e);
        DebugUIHelper.getHelper().showError("Dartium Launch Error", e.getMessage());
      }
    }

  };

  private DartLaunchConfigWrapper wrapper;

  @Override
  public void doLaunch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor, InstrumentationBuilder instrumentation) throws CoreException {

    wrapper = new DartLaunchConfigWrapper(configuration);
    wrapper.markAsLaunched();

    String launchUrl = "";

    if (wrapper.getShouldLaunchFile()) {

      IResource resource = wrapper.getApplicationResource();
      if (resource == null) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            "Html file could not be found"));
      }

      instrumentation.metric("Resource-Class", resource.getClass().toString());
      instrumentation.data("Resource-Name", resource.getName());

      try {
        boolean usePubServe = true; // TODO(keertip): get from launch config
        if (usePubServe) {
          PubServeManager manager = PubServeManager.getManager();

          try {
            manager.serve(wrapper, pubConnectionCallback);
          } catch (Exception e) {
            throw new CoreException(new Status(
                IStatus.ERROR,
                DartDebugCorePlugin.PLUGIN_ID,
                "Could not start pub serve or connect to pub\n" + manager.getStdErrorString(),
                e));
          }

        } else {
          launchUrl = getUrlFromResourceServer(resource);
          launchOnMobile(launchUrl);
        }

      } catch (Exception e) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            "Unable to launch on device: " + e.getLocalizedMessage(),
            e));
      }
    } else {
      launchUrl = wrapper.getUrl();
      try {
        String scheme = new URI(launchUrl).getScheme();

        if (scheme == null) { // add scheme else browser will not launch
          launchUrl = "http://" + launchUrl;
          launchOnMobile(launchUrl);
        }
      } catch (URISyntaxException e) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            "Error in specified url"));
      }
    }

    DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);
  }

  protected void launchOnMobile(String launchUrl) throws CoreException {

    AndroidDebugBridge devBridge = AndroidDebugBridge.getAndroidDebugBridge();

    devBridge.startAdbServer();
    String deviceId = devBridge.getConnectedDevice();
    if (deviceId == null) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          "No devices detected.\n\nConnect device, enable USB debugging and try again."));
    }

    if (wrapper.getInstallContentShell()) {
      if (!devBridge.installContentShellApk(deviceId)) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            "Failed to install content shell on mobile"));
      }
    }
    devBridge.launchContentShell(deviceId, launchUrl);
  }

  private String getUrlFromResourceServer(IResource resource) throws IOException, CoreException,
      URISyntaxException {

    ResourceServer server = ResourceServerManager.getServer();
    String resPath = resource.getFullPath().toPortableString();
    String localAddress = server.getLocalAddress();
    if (localAddress == null) {
      // TODO (danrubel) Improve UX to help user work through this issue
      throw new CoreException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          "Unable to get local IP address"));
    }

    URI uri = new URI("http", null, localAddress, server.getPort(), resPath, null, null);

    String launchUrl = uri.toString();
    launchUrl = wrapper.appendQueryParams(launchUrl);
    return launchUrl;
  }

}
