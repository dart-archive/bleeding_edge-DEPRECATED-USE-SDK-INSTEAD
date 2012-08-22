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
package com.google.dart.tools.ui.update;

import com.google.dart.tools.ui.dialogs.UpdateStatusControl;
import com.google.dart.tools.ui.internal.preferences.PreferencesMessages;
import com.google.dart.tools.update.core.UpdateCore;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * The preference page for update.
 */
public class UpdatePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  public static final String PAGE_ID = "com.google.dart.tools.ui.update.updatePreferencePage"; //$NON-NLS-1$

  private Button autoDownloadCheck;
  private Group statusGroup;

  @Override
  public void init(IWorkbench workbench) {
    noDefaultAndApplyButton();
  }

  @Override
  public boolean performOk() {

    UpdateCore.enableAutoDownload(autoDownloadCheck.getSelection());

    return true;
  }

  @Override
  protected Control createContents(Composite parent) {

    Composite composite = new Composite(parent, SWT.NONE);
    GridDataFactory.fillDefaults().grab(true, false).indent(0, 10).align(SWT.FILL, SWT.BEGINNING).applyTo(
        composite);
    GridLayoutFactory.fillDefaults().spacing(0, 8).margins(0, 10).applyTo(composite);

    createUpdateGroup(composite);

    initFromPrefs();

    return composite;
  }

  private Button createCheckBox(Composite composite, String label, String tooltip) {
    Button checkBox = new Button(composite, SWT.CHECK);
    checkBox.setText(label);
    checkBox.setToolTipText(tooltip);
    return checkBox;
  }

  private void createUpdateGroup(Composite composite) {

    Group settingsGroup = new Group(composite, SWT.NONE);
    settingsGroup.setText("Settings"); //$NON-NLS-1$
    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(
        settingsGroup);
    GridLayoutFactory.fillDefaults().numColumns(1).margins(8, 8).applyTo(settingsGroup);

    autoDownloadCheck = createCheckBox(
        settingsGroup,
        PreferencesMessages.DartBasePreferencePage_auto_download_label,
        PreferencesMessages.DartBasePreferencePage_auto_download_tooltip);
    GridDataFactory.fillDefaults().applyTo(autoDownloadCheck);

    statusGroup = new Group(composite, SWT.NONE);
    statusGroup.setText("Update"); //$NON-NLS-1$
    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(
        statusGroup);
    GridLayoutFactory.fillDefaults().numColumns(1).margins(8, 8).spacing(0, 0).applyTo(statusGroup);

    CLabel currentVersionLabel = new CLabel(statusGroup, SWT.NONE);
    currentVersionLabel.setText(NLS.bind("Dart Editor build {0}", UpdateCore.getCurrentRevision()));
    GridDataFactory.fillDefaults().applyTo(currentVersionLabel);

    new UpdateStatusControl(statusGroup, null, new Point(0, 0), false);

  }

  private void initFromPrefs() {
    autoDownloadCheck.setSelection(UpdateCore.isAutoDownloadEnabled());
  }

}
