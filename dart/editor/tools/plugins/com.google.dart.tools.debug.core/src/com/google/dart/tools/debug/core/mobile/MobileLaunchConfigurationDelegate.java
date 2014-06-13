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
import com.google.dart.tools.core.mobile.AndroidDevice;
import com.google.dart.tools.core.mobile.MobileUrlConnectionException;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.core.DartLaunchConfigurationDelegate;
import com.google.dart.tools.debug.core.DebugUIHelper;
import com.google.dart.tools.debug.core.pubserve.PubCallback;
import com.google.dart.tools.debug.core.pubserve.PubResult;
import com.google.dart.tools.debug.core.pubserve.PubServeManager;
import com.google.dart.tools.debug.core.pubserve.PubServeResourceResolver;
import com.google.dart.tools.debug.core.util.BrowserManager;
import com.google.dart.tools.debug.core.util.IRemoteConnectionDelegate;
import com.google.dart.tools.debug.core.util.IResourceResolver;
import com.google.dart.tools.debug.core.util.ResourceServer;
import com.google.dart.tools.debug.core.util.ResourceServerManager;
import com.google.dart.tools.debug.core.webkit.ChromiumTabInfo;
import com.google.dart.tools.debug.core.webkit.DefaultChromiumTabChooser;
import com.google.dart.tools.debug.core.webkit.IChromiumTabChooser;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * A LaunchConfigurationDelegate to launch in the browser on a connected device.
 */
public class MobileLaunchConfigurationDelegate extends DartLaunchConfigurationDelegate implements
    IRemoteConnectionDelegate {

  /**
   * A class to choose a tab from the given list of tabs.
   */
  public static class ChromeTabChooser implements IChromiumTabChooser {
    public ChromeTabChooser() {

    }

    @Override
    public ChromiumTabInfo chooseTab(final List<ChromiumTabInfo> tabs) {
      if (tabs.size() == 0) {
        return null;
      }

      int tabCount = 0;

      for (ChromiumTabInfo tab : tabs) {
        if (!tab.isChromeExtension()) {
          tabCount++;
        }
      }

      if (tabCount == 1) {
        return tabs.get(0);
      }

      for (ChromiumTabInfo tab : tabs) {
        if (!tab.isChromeExtension()) {
          return tab;
        }

      }

      return new DefaultChromiumTabChooser().chooseTab(tabs);
    }
  }

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
        launchOnMobile(launchUrl, true, new NullProgressMonitor());
      } catch (MobileUrlConnectionException e) {
        // DartDebugCorePlugin.logError(e);
        DebugUIHelper.getHelper().showError("Dartium Launch Error", e);
      } catch (CoreException e) {
        DartDebugCorePlugin.logError(e);
        DebugUIHelper.getHelper().showError("Dartium Launch Error", e);
      }
    }

  };

  private DartLaunchConfigWrapper wrapper;

  private static final int REMOTE_DEBUG_PORT = 9224;

  @Override
  public void doLaunch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor, InstrumentationBuilder instrumentation) throws CoreException {

    wrapper = new DartLaunchConfigWrapper(configuration);
    wrapper.markAsLaunched();

    String launchUrl = "";

    boolean usePubServe = wrapper.getUsePubServe();

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
          launchOnMobile(launchUrl, usePubServe, monitor);
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
          launchOnMobile(launchUrl, usePubServe, monitor);
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

  @Override
  public IDebugTarget performRemoteConnection(String host, int port, IProgressMonitor monitor,
      boolean usePubServe) throws CoreException {

    BrowserManager browserManager = new BrowserManager();

    IResourceResolver resolver = null;
    try {
      resolver = usePubServe ? new PubServeResourceResolver() : ResourceServerManager.getServer();
    } catch (IOException e) {
      return null;
    }

    return browserManager.performRemoteConnection(
        new ChromeTabChooser(),
        host,
        port,
        monitor,
        resolver);

  }

  protected void launchOnMobile(String launchUrl, boolean usePubServe, IProgressMonitor monitor)
      throws CoreException {

    AndroidDebugBridge devBridge = AndroidDebugBridge.getAndroidDebugBridge();

    devBridge.startAdbServer();
    AndroidDevice device = devBridge.getConnectedDevice();
    if (device == null) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          "No devices detected.\n\nConnect device, enable USB debugging and try again."));
    }
    if (device.getDeviceId() == null) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          "Unauthorized device detected.\n\nAuthorize device and try again."));
    }

    if (wrapper.getInstallContentShell()) {
      if (!devBridge.installContentShellApk(device)) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            "Failed to install content shell on mobile"));
      }
    }
    devBridge.launchContentShell(device.getDeviceId(), launchUrl);

    if (!devBridge.isHtmlPageAccessible(device, launchUrl).isOK()) {
      // pub serve is always localhost over USB while old dev server is always over wifi
      boolean localhostOverUsb = usePubServe;
      throw new MobileUrlConnectionException(launchUrl, localhostOverUsb);
    }

    // check if remote connection is alive
    if (!isRemoteConnected()) {
      devBridge.setupPortForwarding(Integer.toString(REMOTE_DEBUG_PORT));
      performRemoteConnection("localhost", REMOTE_DEBUG_PORT, monitor, usePubServe);
    }
  }

  private String getUrlFromResourceServer(IResource resource) throws IOException, CoreException,
      URISyntaxException {

    ResourceServer server = ResourceServerManager.getServer();
    String resPath = resource.getFullPath().toPortableString();
    String localAddress = server.getLocalAddress();
    if (localAddress == null) {
      throw new MobileUrlConnectionException(null, false);
    }

    URI uri = new URI("http", null, localAddress, server.getPort(), resPath, null, null);

    String launchUrl = uri.toString();
    launchUrl = wrapper.appendQueryParams(launchUrl);
    return launchUrl;
  }

  private boolean isRemoteConnected() {

    IDebugTarget[] targets = DebugPlugin.getDefault().getLaunchManager().getDebugTargets();
    for (IDebugTarget target : targets) {
      try {
        if (target.getName().equals("Remote") && !target.isTerminated()) {
          return true;
        }
      } catch (DebugException e) {

      }
    }
    return false;
  }

}
