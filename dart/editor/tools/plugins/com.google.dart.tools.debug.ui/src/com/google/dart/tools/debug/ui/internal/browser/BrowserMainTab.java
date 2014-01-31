/*
 * Copyright (c) 2013, the Dart project authors.
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

import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.util.LaunchTargetComposite;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * Main launch tab for Browser launch configurations
 */
public class BrowserMainTab extends AbstractLaunchConfigurationTab {

  private LaunchTargetComposite launchTargetGroup;

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

    return launchTargetGroup.getErrorMessage();
  }

  /**
   * Answer the image to show in the configuration tab or <code>null</code> if none
   */
  @Override
  public Image getImage() {
    return DartDebugUIPlugin.getImage("obj16/globe_dark.png"); //$NON-NLS-1$
  }

  @Override
  public String getMessage() {
    return Messages.BrowserMainTab_Description;
  }

  /**
   * Answer the name to show in the configuration tab
   */
  @Override
  public String getName() {
    return Messages.BrowserMainTab_Name;
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

  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(configuration);
    wrapper.setShouldLaunchFile(true);
    wrapper.setApplicationName(""); //$NON-NLS-1$
    wrapper.setRunDart2js(true);
  }

  private void notifyPanelChanged() {
    setDirty(true);
    updateLaunchConfigurationDialog();
  }

  private String performSdkCheck() {
    if (!DartSdkManager.getManager().hasSdk()) {
      return "Dart2js is not installed ("
          + DartSdkManager.getManager().getSdk().getDart2JsExecutable() + ")";
    } else {
      return null;
    }
  }

}
