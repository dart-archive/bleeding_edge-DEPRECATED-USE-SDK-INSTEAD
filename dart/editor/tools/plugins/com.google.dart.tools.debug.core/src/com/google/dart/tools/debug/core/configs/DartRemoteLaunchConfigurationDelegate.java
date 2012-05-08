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
package com.google.dart.tools.debug.core.configs;

import com.google.dart.tools.debug.core.DartDebugCorePlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;

/**
 * The Dart Remote Debug launch configuration.
 */
public class DartRemoteLaunchConfigurationDelegate extends LaunchConfigurationDelegate {

  /**
   * Create a new DartRemoteLaunchConfigurationDelegate.
   */
  public DartRemoteLaunchConfigurationDelegate() {

  }

  @Override
  public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor) throws CoreException {
    if (!ILaunchManager.DEBUG_MODE.equals(mode)) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          "Execution mode '" + mode + "' is not supported."));
    }

//    DartLaunchConfigWrapper launchConfig = new DartLaunchConfigWrapper(configuration);

//    String connectionType = launchConfig.getConnectionType();

//    if (DartLaunchConfigWrapper.CONNECTION_TYPE_CHROME.equals(connectionType)) {
//      connectToChrome(launch, launchConfig);
//    } else if (DartLaunchConfigWrapper.CONNECTION_TYPE_V8.equals(connectionType)) {
//      connectToV8(launch, launchConfig);
//    } else {
//    throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
//        "Connection type '" + connectionType + "' is not supported."));
//    }
  }

//  private void connectToChrome(ILaunch launch, DartLaunchConfigWrapper launchConfig)
//      throws CoreException {
//    final String title = "Dart Chrome debug";
//
//    SocketAddress address = new InetSocketAddress(launchConfig.getConnectionHost(),
//        launchConfig.getConnectionPort());
//
//    ConsolePseudoProcess.Retransmitter consoleRetransmitter = new ConsolePseudoProcess.Retransmitter();
//
//    final Browser browser = BrowserFactory.getInstance().create(address,
//        new ConnectionLogger.Factory() {
//          @Override
//          public ConnectionLogger newConnectionLogger() {
//            return null;
//          }
//        });
//
//    try {
//      Browser.TabConnector tabConnector = selectTab(browser.createTabFetcher());
//
//      if (tabConnector != null) {
//        ChromeDebugTarget debugTarget = new ChromeDebugTarget("remote", launch);
//
//        BrowserTab browserTab = tabConnector.attach(debugTarget);
//
//        debugTarget.setBrowserTab(browserTab);
//
//        launch.addDebugTarget(debugTarget);
//        debugTarget.connected();
//
//        ConsolePseudoProcess consolePseudoProcess = new ConsolePseudoProcess(launch, title,
//            consoleRetransmitter, debugTarget);
//
//        debugTarget.setProcess(consolePseudoProcess);
//
//        consoleRetransmitter.startFlushing();
//      }
//    } catch (IOException e) {
//      throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
//          e.getMessage(), e));
//    } catch (UnsupportedVersionException e) {
//      throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
//          e.getMessage(), e));
//    }
//  }
//
//  private void connectToV8(final ILaunch launch, DartLaunchConfigWrapper launchConfig)
//      throws CoreException {
//    final String title = "Dart V8 debug";
//
//    SocketAddress address = new InetSocketAddress(launchConfig.getConnectionHost(),
//        launchConfig.getConnectionPort());
//
//    ConsolePseudoProcess.Retransmitter consoleRetransmitter = new ConsolePseudoProcess.Retransmitter();
//
//    final StandaloneVm standaloneVm = BrowserFactory.getInstance().createStandalone(address, null);
//
//    ChromeDebugTarget debugTarget = new ChromeDebugTarget("remote", launch, standaloneVm);
//
//    try {
//      standaloneVm.attach(debugTarget);
//
//      launch.addDebugTarget(debugTarget);
//      debugTarget.connected();
//
//      ConsolePseudoProcess consolePseudoProcess = new ConsolePseudoProcess(launch, title,
//          consoleRetransmitter, debugTarget);
//
//      debugTarget.setProcess(consolePseudoProcess);
//
//      consoleRetransmitter.startFlushing();
//    } catch (IOException e) {
//      throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
//          e.getMessage(), e));
//    } catch (UnsupportedVersionException e) {
//      throw new CoreException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
//          e.getMessage(), e));
//    }
//  }
//
//  private TabConnector selectTab(TabFetcher createTabFetcher) throws IOException {
//    List<? extends TabConnector> tabs = createTabFetcher.getTabs();
//
//    List<String> tabUrls = new ArrayList<String>();
//
//    for (TabConnector tab : tabs) {
//      tabUrls.add(tab.getUrl());
//    }
//
//    DebugUIHelper tabChooser = DebugUIHelperFactory.getDebugUIHelper();
//
//    int index = tabChooser.select(tabUrls);
//
//    if (index == -1) {
//      return null;
//    } else {
//      return tabs.get(index);
//    }
//  }

}
