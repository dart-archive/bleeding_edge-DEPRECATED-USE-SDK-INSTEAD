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

import java.net.URI;
import java.net.URISyntaxException;

/**
 * A LaunchConfigurationDelegate to launch in the browser on a connected device.
 */
public class MobileLaunchConfigurationDelegate extends DartLaunchConfigurationDelegate {

  @Override
  public void doLaunch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor, InstrumentationBuilder instrumentation) throws CoreException {

    DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(configuration);
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

      ResourceServer server;
      // TODO(keertip): change to localhost and pubserve once port forwarding is setup right
      try {
        server = ResourceServerManager.getServer();
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

        launchUrl = uri.toString();
        // launchUrl = server.getUrlForFile((File) resource);
        wrapper.appendQueryParams(launchUrl);

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
        }
      } catch (URISyntaxException e) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            "Error in specified url"));
      }
    }

    AndroidDebugBridge devBridge = new AndroidDebugBridge();

    String deviceId = devBridge.getConnectedDevice();
    if (deviceId == null) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          "No devices detected.\n\nConnect device, enable USB debugging and try again."));
    }

    if (wrapper.getLaunchContentShell()) {
      if (wrapper.getInstallContentShell()) {
        if (!devBridge.installContentShellApk(deviceId)) {
          throw new CoreException(new Status(
              IStatus.ERROR,
              DartDebugCorePlugin.PLUGIN_ID,
              "Failed to install content shell on mobile"));
        }
      }
      devBridge.launchContentShell(deviceId, launchUrl);
    } else {
      devBridge.launchChromeBrowser(launchUrl);
    }

    DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);
  }
}
