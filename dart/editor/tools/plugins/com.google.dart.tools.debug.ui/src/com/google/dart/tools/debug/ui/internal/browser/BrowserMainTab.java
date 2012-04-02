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

import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.dartium.DartiumMainTab;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Main launch tab for Browser launch configurations
 */
public class BrowserMainTab extends DartiumMainTab {
  private Button selectBrowserButton;
  private Button defaultBrowserButton;
  private Text browserText;

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
    GridDataFactory.swtDefaults().span(3, 1).applyTo(filler);

    createUrlField(group);

    Group browserGroup = new Group(composite, SWT.NONE);
    browserGroup.setText(Messages.BrowserMainTab_Browser);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(browserGroup);
    GridLayoutFactory.swtDefaults().numColumns(3).applyTo(browserGroup);

    defaultBrowserButton = new Button(browserGroup, SWT.CHECK);
    defaultBrowserButton.setText(Messages.BrowserMainTab_DefaultBrowserMessage);
    GridDataFactory.swtDefaults().span(3, 1).applyTo(defaultBrowserButton);
    defaultBrowserButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (defaultBrowserButton.getSelection()) {
          selectBrowserButton.setEnabled(false);
          browserText.setEnabled(false);
        } else {
          browserText.setEnabled(true);
          selectBrowserButton.setEnabled(true);
        }
        notifyPanelChanged();
      }
    });

    Label browserLabel = new Label(browserGroup, SWT.NONE);
    browserLabel.setText(Messages.BrowserMainTab_Browser);
    GridDataFactory.swtDefaults().hint(getLabelColumnWidth(), -1).span(3, 1).applyTo(browserLabel);

    browserText = new Text(browserGroup, SWT.BORDER | SWT.SINGLE);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(browserText);
    browserText.addModifyListener(textModifyListener);

    selectBrowserButton = new Button(browserGroup, SWT.PUSH);
    selectBrowserButton.setText(Messages.BrowserMainTab_Select);
    PixelConverter converter = new PixelConverter(selectBrowserButton);
    int widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
    GridDataFactory.swtDefaults().hint(widthHint, -1).applyTo(selectBrowserButton);
    selectBrowserButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleBrowserConfigBrowseButton();
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
    String message = super.getErrorMessage();

    if (message != null) {
      return message;
    }

    if (!defaultBrowserButton.getSelection() && browserText.getText().length() == 0) {
      return Messages.BrowserMainTab_BrowserNotSpecifiedErrorMessage;
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
    DartLaunchConfigWrapper dartLauncher = new DartLaunchConfigWrapper(config);
    if (dartLauncher.getUseDefaultBrowser()) {
      defaultBrowserButton.setSelection(true);
      selectBrowserButton.setEnabled(false);
      browserText.setEnabled(false);
    } else {
      defaultBrowserButton.setSelection(false);
      selectBrowserButton.setEnabled(true);
      browserText.setEnabled(true);
    }
    browserText.setText(dartLauncher.getBrowserName());

  }

  /**
   * Store the value specified in the UI into the launch configuration
   */
  @Override
  public void performApply(ILaunchConfigurationWorkingCopy config) {
    super.performApply(config);

    DartLaunchConfigWrapper dartLauncher = new DartLaunchConfigWrapper(config);
    dartLauncher.setUseDefaultBrowser(defaultBrowserButton.getSelection());
    dartLauncher.setBrowserName(browserText.getText().trim());
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    super.setDefaults(configuration);
    DartLaunchConfigWrapper dartLauncher = new DartLaunchConfigWrapper(configuration);
    dartLauncher.setUseDefaultBrowser(true);

  }

  @Override
  protected String performSdkCheck() {
    // This tab does not care if the Dart SDK is installed or not.

    return null;
  }

  private void handleBrowserConfigBrowseButton() {

    FileDialog fd = new FileDialog(getShell(), SWT.OPEN);

    String filePath = fd.open();

    if (filePath != null) {
      browserText.setText(filePath);
    }
  }

}
