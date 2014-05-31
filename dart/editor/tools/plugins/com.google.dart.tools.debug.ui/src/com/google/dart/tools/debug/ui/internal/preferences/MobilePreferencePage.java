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
package com.google.dart.tools.debug.ui.internal.preferences;

import com.google.dart.tools.debug.core.util.RemoteConnectionPreferenceManager;
import com.google.dart.tools.ui.internal.util.GridDataFactory;
import com.google.dart.tools.ui.internal.util.GridLayoutFactory;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * The preference page for Mobile.
 */
public class MobilePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
  public static final String PAGE_ID = "com.google.dart.tools.debug.mobilePreferencePage"; //$NON-NLS-1$

//  private Text androidSdkText;

  private Button remoteConnectButton;

//  private static String ANDROID_SDK_URL = "http://developer.android.com/sdk/index.html";

  /**
   * Create a new preference page.
   */
  public MobilePreferencePage() {

  }

  @Override
  public void init(IWorkbench workbench) {
    noDefaultAndApplyButton();
  }

  @Override
  public boolean performOk() {
    RemoteConnectionPreferenceManager.getManager().setAllowRemoteConnectionPreference(
        remoteConnectButton.getSelection());

//    AndroidSdkManager.getManager().setSdkLocationPreference(androidSdkText.getText().trim());
    return true;
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridDataFactory.create(composite).indent(0, 10).grabHorizontal().fill();
    GridLayoutFactory.create(composite).spacing(0, 8).margins(0, 10);

    createRemoteConnectionConfig(composite);

//    createAndroidSdkConfig(composite);

    return composite;
  }

//  private void createAndroidSdkConfig(Composite composite) {
//    Group androidGroup = new Group(composite, SWT.NONE);
//    androidGroup.setText("Android SDK");
//    GridDataFactory.create(androidGroup).grabHorizontal().fill();
//    GridLayoutFactory.create(androidGroup).columns(3).marginBottom(5);
//
//    Label sdkLabel = new Label(androidGroup, SWT.NONE);
//    sdkLabel.setText("SDK Location:");
//
//    androidSdkText = new Text(androidGroup, SWT.BORDER | SWT.SINGLE);
//    GridDataFactory.create(androidSdkText).grabHorizontal().fillHorizontal();
//
//    Button selectSdkButton = new Button(androidGroup, SWT.PUSH);
//    selectSdkButton.setText(DebugPreferenceMessages.DebugPreferencePage_Select);
//    GridDataFactory.create(selectSdkButton).hintWidthUnits(IDialogConstants.BUTTON_WIDTH);
//    selectSdkButton.addSelectionListener(new SelectionAdapter() {
//      @Override
//      public void widgetSelected(SelectionEvent e) {
//        handleSdkConfigBrowseButton();
//      }
//    });
//
//    androidSdkText.setText(AndroidSdkManager.getManager().getSdkLocationPreference());
//
//    Link infoLink = new Link(androidGroup, SWT.NONE);
//    infoLink.setText("<a href=\"" + ANDROID_SDK_URL + "\">Download the Android SDK</a>");
//    GridDataFactory.create(infoLink).spanHorizontal(3);
//    infoLink.addSelectionListener(new SelectionAdapter() {
//      @Override
//      public void widgetSelected(SelectionEvent e) {
//        ExternalBrowserUtil.openInExternalBrowser(ANDROID_SDK_URL);
//      }
//    });
//
//  }

  private void createRemoteConnectionConfig(Composite composite) {
    Group remoteGroup = new Group(composite, SWT.NONE);
    remoteGroup.setText("Remote Connection");
    GridDataFactory.create(remoteGroup).grabHorizontal().fillHorizontal();
    GridLayoutFactory.create(remoteGroup).marginBottom(5);

    remoteConnectButton = new Button(remoteGroup, SWT.CHECK);
    remoteConnectButton.setText("Allow connections from non-localhost address");
    remoteConnectButton.setSelection(RemoteConnectionPreferenceManager.getManager().getAllowRemoteConnectionPrefs());
  }

//  private void handleSdkConfigBrowseButton() {
//    DirectoryDialog dirDialog = new DirectoryDialog(getShell(), SWT.OPEN);
//
//    String dirPath = dirDialog.open();
//
//    if (dirPath != null) {
//      androidSdkText.setText(dirPath);
//    }
//  }
}
