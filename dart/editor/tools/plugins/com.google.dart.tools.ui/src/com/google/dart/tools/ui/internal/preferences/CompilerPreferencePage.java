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
package com.google.dart.tools.ui.internal.preferences;

import com.google.dart.tools.core.model.DartSdkManager;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Preference page for compiler preferences
 */
@Deprecated
public class CompilerPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  public static final String PAGE_ID = "com.google.dart.tools.ui.compilerPreferencePage"; //$NON-NLS-1$

  public CompilerPreferencePage() {
    setDescription("Dart SDK Status");
    noDefaultAndApplyButton();
  }

  @Override
  public void init(IWorkbench workbench) {

  }

  @Override
  public boolean performOk() {
    return true;
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);

    GridDataFactory.fillDefaults().grab(true, false).indent(0, 10).align(SWT.FILL, SWT.BEGINNING).applyTo(
        composite);
    GridLayoutFactory.fillDefaults().spacing(0, 8).margins(0, 10).applyTo(composite);

    // dart sdk
    Group sdkGroup = new Group(composite, SWT.NONE);
    sdkGroup.setText("Dart SDK");
    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(
        sdkGroup);
    GridLayoutFactory.fillDefaults().margins(8, 8).applyTo(sdkGroup);
    Label sdkLabel = new Label(sdkGroup, SWT.NONE);
    if (DartSdkManager.getManager().hasSdk()) {
      String version = DartSdkManager.getManager().getSdk().getSdkVersion();
      if (version.equals("0")) {
        sdkLabel.setText("Dart SDK is installed");
      } else {
        sdkLabel.setText("Dart SDK version " + version);
      }
    } else {
      sdkLabel.setText("Dart SDK is not installed");
    }

    return composite;
  }

}
