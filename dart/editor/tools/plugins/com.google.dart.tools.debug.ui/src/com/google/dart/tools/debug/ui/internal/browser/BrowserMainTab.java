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

import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.dartium.DartiumMainTab;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

/**
 * Main launch tab for Browser launch configurations
 */
public class BrowserMainTab extends DartiumMainTab {

  @Override
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayoutFactory.swtDefaults().spacing(1, 3).applyTo(composite);

    // Project group
    Group group = new Group(composite, SWT.NONE);
    group.setText(Messages.BrowserMainTab_LaunchTarget);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
    GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);

    createHtmlField(group);

    Label filler = new Label(group, SWT.NONE);
    GridDataFactory.swtDefaults().span(3, 1).hint(-1, 4).applyTo(filler);

    createUrlField(group);

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
    String message = super.getErrorMessage();

    if (message != null) {
      return message;
    }

    return null;
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
    super.initializeFrom(config);
  }

  /**
   * Store the value specified in the UI into the launch configuration
   */
  @Override
  public void performApply(ILaunchConfigurationWorkingCopy config) {
    super.performApply(config);

  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    super.setDefaults(configuration);
  }

  @Override
  protected String performSdkCheck() {
    // This tab does not care if the Dart SDK is installed or not.

    return null;
  }

}
