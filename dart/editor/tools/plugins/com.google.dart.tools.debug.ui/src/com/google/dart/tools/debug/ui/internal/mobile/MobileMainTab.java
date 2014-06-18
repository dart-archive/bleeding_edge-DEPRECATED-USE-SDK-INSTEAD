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
package com.google.dart.tools.debug.ui.internal.mobile;

import com.google.dart.tools.core.mobile.AndroidDebugBridge;
import com.google.dart.tools.core.mobile.AndroidDevice;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.util.LaunchTargetComposite;
import com.google.dart.tools.ui.internal.util.ExternalBrowserUtil;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main tab for Mobile launch configurations
 */
public class MobileMainTab extends AbstractLaunchConfigurationTab {

  private static final String DEVICE_NOT_AUTHORIZED = "Connected mobile is not authorized";
  private static final String DEVICE_NOT_FOUND = "No mobile found or USB development not enabled on mobile";

  public static final String MOBILE_DOC_URL = "https://www.dartlang.org/tools/editor/mobile.html";
  // PORT_FORWARD_DOC_URL should be #set-up-port-forwarding but is #connect-the-devices
  // until dartbug.com/19457 is fixed.
  public static final String PORT_FORWARD_DOC_URL = MOBILE_DOC_URL + "#connect-the-devices";

  private static final String INFO_TEXT = "Pub-Serve runs on your local machine, and serves your application over the USB cable."
      + "To use it you have to <a href=\""
      + PORT_FORWARD_DOC_URL
      + "\">setup port forwarding</a> so that your mobile can see the server.";

  private static final String SERVER_INFO_TEXT = "Use the server embedded in the Dart Editor, "
      + "connecting over Wifi. This requires that your phone can establish a direct connection by Wifi "
      + "to the computer where Dart Editor is running.";

  // When these change, be sure to change the messaging in MobileUrlConnectionException
  private String[] servers = {"Embedded server over WiFi network", "Pub Serve over USB"};

  private LaunchTargetComposite launchTargetGroup;

  private AndroidDevice connectedDevice = null;
  private final AtomicBoolean monitorDeviceConnection = new AtomicBoolean(false);
  private Link infoLink;
  private Combo serversCombo;

  @Override
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayoutFactory.swtDefaults().spacing(1, 3).applyTo(composite);

    launchTargetGroup = new LaunchTargetComposite(composite, SWT.NONE);
    launchTargetGroup.addListener(SWT.Modify, new Listener() {

      @Override
      public void handleEvent(Event event) {
        notifyPanelChanged();
      }
    });
    launchTargetGroup.addDisposeListener(new DisposeListener() {
      @Override
      public void widgetDisposed(DisposeEvent e) {
        stopMonitorDeviceConnectionInBackground();
      }
    });

    // pub serve setting
    Group group = new Group(composite, SWT.NONE);
    group.setText("Server");
    GridDataFactory.fillDefaults().grab(true, true).applyTo(group);
    GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
    ((GridLayout) group.getLayout()).marginBottom = 5;
    ((GridLayout) group.getLayout()).verticalSpacing = 10;

    serversCombo = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
    serversCombo.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleComboChanged(serversCombo.getSelectionIndex() == 1);
      }
    });
    serversCombo.setItems(servers);

    Label separator = new Label(group, SWT.WRAP);
    GridDataFactory.swtDefaults().grab(true, false).span(1, 2).applyTo(separator);

    infoLink = new Link(group, SWT.WRAP);
    infoLink.setText(INFO_TEXT);

    infoLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
      }

      @Override
      public void widgetSelected(SelectionEvent e) {
        ExternalBrowserUtil.openInExternalBrowser(e.text.trim());
      }
    });
    GridDataFactory.swtDefaults().span(2, 1).grab(true, false).hint(415, SWT.DEFAULT).applyTo(
        infoLink);
    new Label(group, SWT.NONE);

    setControl(composite);
  }

  @Override
  public void dispose() {
    Control control = getControl();

    if (control != null) {
      control.dispose();
      setControl(null);
    }

  }

  @Override
  public String getErrorMessage() {
    if (performSdkCheck() != null) {
      return performSdkCheck();
    }
    if (connectedDevice == null) {
      return DEVICE_NOT_FOUND;
    }
    if (!connectedDevice.isAuthorized()) {
      return DEVICE_NOT_AUTHORIZED;
    }
    return launchTargetGroup.getErrorMessage();
  }

  /**
   * Answer the image to show in the configuration tab or <code>null</code> if none
   */
  @Override
  public Image getImage() {
    return DartDebugUIPlugin.getImage("android.png"); //$NON-NLS-1$
  }

  @Override
  public String getMessage() {
    return "Create a configuration to launch a Dart application on a device."
        + "This installs a browser with the Dart VM on the device and launches the app in it.";
  }

  /**
   * Answer the name to show in the configuration tab
   */
  @Override
  public String getName() {
    return "Main";
  }

  /**
   * Initialize the UI from the specified configuration
   */
  @Override
  public void initializeFrom(ILaunchConfiguration config) {

    DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(config);
    launchTargetGroup.setHtmlTextValue(wrapper.appendQueryParams(wrapper.getApplicationName()));
    launchTargetGroup.setUrlTextValue(wrapper.getUrl());

    launchTargetGroup.setSourceDirectoryTextValue(wrapper.getSourceDirectoryName());

    if (wrapper.getShouldLaunchFile()) {
      launchTargetGroup.setHtmlButtonSelection(true);
    } else {
      launchTargetGroup.setHtmlButtonSelection(false);
    }

    if (wrapper.getUsePubServe()) {
      serversCombo.select(1);
      handleComboChanged(true);
    } else {
      serversCombo.select(0);
      handleComboChanged(false);
    }

    startMonitorDeviceConnectionInBackground(launchTargetGroup.getDisplay());
  }

  /**
   * Store the value specified in the UI into the launch configuration
   */
  @Override
  public void performApply(ILaunchConfigurationWorkingCopy config) {

    DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(config);
    wrapper.setShouldLaunchFile(launchTargetGroup.getHtmlButtonSelection());

    String fileUrl = launchTargetGroup.getHtmlFileName();

    if (fileUrl.indexOf('?') == -1) {
      wrapper.setApplicationName(fileUrl);
      wrapper.setUrlQueryParams("");
    } else {
      int index = fileUrl.indexOf('?');

      wrapper.setApplicationName(fileUrl.substring(0, index));
      wrapper.setUrlQueryParams(fileUrl.substring(index + 1));
    }

    wrapper.setUrl(launchTargetGroup.getUrlString());
    wrapper.setSourceDirectoryName(launchTargetGroup.getSourceDirectory());
    wrapper.setUsePubServe(serversCombo.getSelectionIndex() == 1);
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(configuration);
    wrapper.setShouldLaunchFile(true);
    wrapper.setApplicationName(""); //$NON-NLS-1$
    wrapper.setLaunchContentShell(true);
    wrapper.setUsePubServe(true);
  }

  protected void handleComboChanged(boolean usePubServe) {
    if (usePubServe) {
      infoLink.setText(INFO_TEXT);
    } else {
      infoLink.setText(SERVER_INFO_TEXT);
    }
    notifyPanelChanged();
  }

  private void notifyPanelChanged() {
    setDirty(true);
    updateLaunchConfigurationDialog();
  }

  private String performSdkCheck() {
    if (!DartSdkManager.getManager().hasSdk()) {
      return "Dart SDK is not installed ("
          + DartSdkManager.getManager().getSdk().getDart2JsExecutable() + ")";
    } else {
      return null;
    }
  }

  /**
   * Start the background process that monitors device connection via ADB
   */
  private void startMonitorDeviceConnectionInBackground(final Display display) {
    if (!monitorDeviceConnection.get()) {
      monitorDeviceConnection.set(true);
      Thread thread = new Thread("Monitor mobile connection") {
        @Override
        public void run() {

          AndroidDebugBridge devBridge = AndroidDebugBridge.getAndroidDebugBridge();
          AndroidDevice oldDevice = devBridge.getConnectedDevice();
          update(oldDevice);

          while (monitorDeviceConnection.get()) {
            AndroidDevice newDevice = devBridge.getConnectedDevice();
            if (!AndroidDevice.isEqual(oldDevice, newDevice)) {
              oldDevice = newDevice;
              update(oldDevice);
            }
            try {
              Thread.sleep(1000);
            } catch (InterruptedException e) {
              //$FALL-THROUGH$
            }
          }
        }

        private void update(final AndroidDevice device) {
          display.asyncExec(new Runnable() {
            @Override
            public void run() {
              updateMobileStatus(device);
            }
          });
        }
      };
      thread.setDaemon(true);
      thread.start();
    }
  }

  /**
   * Stop the background process that monitors device connection via ADB
   */
  private void stopMonitorDeviceConnectionInBackground() {
    monitorDeviceConnection.set(false);
  }

  /**
   * Update the mobile status. Must be called on the UI thread.
   * 
   * @param isDeviceConnected {@code true} if a mobile device is currently connected
   */
  private void updateMobileStatus(AndroidDevice device) {
    connectedDevice = device;
    updateLaunchConfigurationDialog();

  }
}
