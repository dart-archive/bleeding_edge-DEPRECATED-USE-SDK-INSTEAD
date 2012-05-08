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
package com.google.dart.tools.debug.ui.internal.preferences;

import com.google.dart.tools.core.model.DartSdk;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * The preference page for Dart debugging.
 */
public class DebugPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  public static final String PAGE_ID = "com.google.dart.tools.debug.debugPreferencePage"; //$NON-NLS-1$

  private Text vmField;

  /**
   * Create a new preference page.
   */
  public DebugPreferencePage() {
    setDescription("Dart Launch Preferences");
  }

  @Override
  public void init(IWorkbench workbench) {
    noDefaultAndApplyButton();
  }

  @Override
  public boolean performOk() {
    if (vmField != null) {
      DartDebugCorePlugin.getPlugin().setDartVmExecutablePath(vmField.getText());
    }

    return true;
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridDataFactory.fillDefaults().grab(true, false).indent(0, 10).align(SWT.FILL, SWT.BEGINNING).applyTo(
        composite);
    GridLayoutFactory.fillDefaults().spacing(0, 8).margins(0, 10).applyTo(composite);

    // Dart VM
    if (!DartSdk.isInstalled()) { // no sdk is installed
      createVmConfig(composite);
      if (DartDebugCorePlugin.getPlugin().getDartVmExecutablePath() != null) {
        vmField.setText(DartDebugCorePlugin.getPlugin().getDartVmExecutablePath());
      }
    } else {
      Label label = new Label(composite, SWT.NONE);
      label.setText("There are no launch settings available.");
    }

    return composite;
  }

  protected void handleVmBrowseButton() {
    FileDialog fd = new FileDialog(getShell(), SWT.OPEN);

    String filePath = fd.open();

    if (filePath != null) {
      vmField.setText(filePath);
    }
  }

  private void createVmConfig(Composite composite) {
    Group vmGroup = new Group(composite, SWT.NONE);
    vmGroup.setText(DebugPreferenceMessages.DebugPreferencePage_VMExecutableLocation);
    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(vmGroup);
    GridLayoutFactory.fillDefaults().numColumns(3).margins(8, 8).applyTo(vmGroup);

    Label vmLabel = new Label(vmGroup, SWT.NONE);
    vmLabel.setText(DebugPreferenceMessages.DebugPreferencePage_VMPath);
    vmField = new Text(vmGroup, SWT.SINGLE | SWT.BORDER);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).hint(100, SWT.DEFAULT).grab(
        true,
        false).applyTo(vmField);

    Button browseVmButton = new Button(vmGroup, SWT.PUSH);
    browseVmButton.setText(DebugPreferenceMessages.DebugPreferencePage_Browse);
    PixelConverter converter = new PixelConverter(browseVmButton);
    int widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
    GridDataFactory.swtDefaults().hint(widthHint, -1).applyTo(browseVmButton);
    browseVmButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleVmBrowseButton();
      }
    });
  }

}
