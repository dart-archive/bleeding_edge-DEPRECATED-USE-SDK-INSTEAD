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

import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartDebugCorePlugin.BreakOnExceptions;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
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

  private Combo exceptionsCombo;
  private Button invokeToStringButton;

  private Button defaultBrowserButton;

  private Text browserNameText;

  private Button selectBrowserButton;

  private Text browserArgumentText;

  /**
   * Create a new preference page.
   */
  public DebugPreferencePage() {

  }

  @Override
  public void init(IWorkbench workbench) {
    noDefaultAndApplyButton();
  }

  @Override
  public boolean performOk() {
    DartDebugCorePlugin.getPlugin().setBreakOnExceptions(
        BreakOnExceptions.valueOf(exceptionsCombo.getText()));
    DartDebugCorePlugin.getPlugin().setInvokeToString(invokeToStringButton.getSelection());

    DartDebugCorePlugin.getPlugin().setBrowserPreferences(
        defaultBrowserButton.getSelection(),
        browserNameText.getText().trim(),
        browserArgumentText.getText().trim());

    return true;
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridDataFactory.fillDefaults().grab(true, false).indent(0, 10).align(SWT.FILL, SWT.BEGINNING).applyTo(
        composite);
    GridLayoutFactory.fillDefaults().spacing(0, 8).margins(0, 10).applyTo(composite);

    Group group = new Group(composite, SWT.NONE);
    group.setText("Debugging");
    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(group);
    GridLayoutFactory.fillDefaults().numColumns(2).margins(8, 8).applyTo(group);

    Label label = new Label(group, SWT.NONE);
    label.setText("Break on exceptions:");
    label.pack();
    int labelWidth = label.getSize().x;

    exceptionsCombo = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
    exceptionsCombo.setItems(new String[] {
        BreakOnExceptions.none.toString(), BreakOnExceptions.uncaught.toString(),
        BreakOnExceptions.all.toString()});

    exceptionsCombo.select(exceptionsCombo.indexOf(DartDebugCorePlugin.getPlugin().getBreakOnExceptions().toString()));

    invokeToStringButton = new Button(group, SWT.CHECK);
    invokeToStringButton.setText("Invoke toString() methods when debugging");
    GridDataFactory.swtDefaults().span(2, 1).applyTo(invokeToStringButton);

    createBrowserConfig(composite, labelWidth);

    return composite;
  }

  private void createBrowserConfig(Composite composite, int labelWidth) {
    Group browserGroup = new Group(composite, SWT.NONE);
    browserGroup.setText("Launching");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(browserGroup);
    GridLayoutFactory.swtDefaults().numColumns(3).applyTo(browserGroup);
    ((GridLayout) browserGroup.getLayout()).marginBottom = 5;

    defaultBrowserButton = new Button(browserGroup, SWT.CHECK);
    defaultBrowserButton.setText(DebugPreferenceMessages.DebugPreferencePage_DefaultBrowserMessage);
    GridDataFactory.swtDefaults().span(3, 1).applyTo(defaultBrowserButton);
    defaultBrowserButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (defaultBrowserButton.getSelection()) {
          setEnablement(false);
        } else {
          setEnablement(true);
        }
      }
    });

    Label browserLabel = new Label(browserGroup, SWT.NONE);
    browserLabel.setText(DebugPreferenceMessages.DebugPreferencePage_BrowserLabel);
    GridDataFactory.swtDefaults().hint(labelWidth, -1).applyTo(browserLabel);

    browserNameText = new Text(browserGroup, SWT.BORDER | SWT.SINGLE);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(
        browserNameText);

    selectBrowserButton = new Button(browserGroup, SWT.PUSH);
    selectBrowserButton.setText(DebugPreferenceMessages.DebugPreferencePage_Select);
    PixelConverter converter = new PixelConverter(selectBrowserButton);
    int widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
    GridDataFactory.swtDefaults().hint(widthHint, -1).applyTo(selectBrowserButton);
    selectBrowserButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleBrowserConfigBrowseButton();
      }
    });

    Label argsLabel = new Label(browserGroup, SWT.NONE);
    argsLabel.setText("Browser arguments:");
    GridDataFactory.swtDefaults().hint(labelWidth, -1).applyTo(argsLabel);

    browserArgumentText = new Text(browserGroup, SWT.BORDER | SWT.SINGLE);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(
        browserArgumentText);

    initFromPrefs();
  }

  private void handleBrowserConfigBrowseButton() {
    FileDialog fd = new FileDialog(getShell(), SWT.OPEN);

    String filePath = fd.open();

    if (filePath != null) {
      browserNameText.setText(filePath);
    }
  }

  private void initFromPrefs() {
    boolean useDefaultBrowser = DartDebugCorePlugin.getPlugin().getIsDefaultBrowser();
    defaultBrowserButton.setSelection(useDefaultBrowser);
    browserNameText.setText(DartDebugCorePlugin.getPlugin().getBrowserName());
    browserArgumentText.setText(DartDebugCorePlugin.getPlugin().getBrowserArgs());
    setEnablement(!useDefaultBrowser);
    invokeToStringButton.setSelection(DartDebugCorePlugin.getPlugin().getInvokeToString());
  }

  private void setEnablement(boolean value) {
    selectBrowserButton.setEnabled(value);
    browserNameText.setEnabled(value);
    browserArgumentText.setEnabled(value);
  }
}
