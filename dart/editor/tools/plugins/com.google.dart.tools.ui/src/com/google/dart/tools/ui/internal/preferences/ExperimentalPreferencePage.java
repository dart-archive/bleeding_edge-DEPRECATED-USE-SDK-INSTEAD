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
package com.google.dart.tools.ui.internal.preferences;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Page for setting Hint Analysis preferences.
 */
public class ExperimentalPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  public static final String EXPERIMENTAL_PREF_PAGE_ID = "com.google.dart.tools.ui.preferences.ExperimentalPreferencePage"; //$NON-NLS-1$

  private static Button createCheckBox(Composite composite, String label, String tooltip) {
    Button checkBox = new Button(composite, SWT.CHECK);
    checkBox.setText(label);
    checkBox.setToolTipText(tooltip);
    return checkBox;
  }

  private Button enableAsyncSupportButton;
  private Button enableEnumsSupportButton;
  private Button enableAnalysisServerButton;
  private Label serverHttpPortLabel;
  private Text serverHttpPortText;

  public ExperimentalPreferencePage() {
    setPreferenceStore(null);
    noDefaultAndApplyButton();
  }

  @Override
  public void init(IWorkbench workbench) {
    // do nothing
  }

  @Override
  public boolean performOk() {
    IEclipsePreferences prefs = DartCore.getPlugin().getPrefs();
    if (prefs != null) {

      boolean serverChanged = setPref(
          DartCoreDebug.ENABLE_ANALYSIS_SERVER_PREF,
          enableAnalysisServerButton);
      boolean portChanged = setPref(
          DartCoreDebug.ANALYSIS_SERVER_HTTP_PORT_PREF,
          serverHttpPortText);
      boolean asyncChanged = setPref(DartCoreDebug.ENABLE_ASYNC_PREF, enableAsyncSupportButton);
      boolean enumsChanged = setPref(DartCoreDebug.ENABLE_ENUMS_PREF, enableEnumsSupportButton);

      boolean hasChanges = serverChanged || portChanged || asyncChanged || enumsChanged;
      try {
        DartCore.getPlugin().savePrefs();
      } catch (CoreException e) {
        DartToolsPlugin.log(e);
      }
      if (hasChanges) {
        MessageDialog.openInformation(
            getShell(),
            "Restart Required",
            "These changes will only take effect once the IDE has been restarted.");
      }
    }
    return true;
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);

    GridDataFactory.fillDefaults().grab(true, false).indent(0, 10).align(SWT.FILL, SWT.BEGINNING).applyTo(
        composite);
    GridLayoutFactory.fillDefaults().spacing(0, 8).margins(0, 10).applyTo(composite);

    // Enable Async support checkbox
    enableAsyncSupportButton = createCheckBox(
        composite,
        PreferencesMessages.ExperimentalPreferencePage_enable_async_support,
        PreferencesMessages.ExperimentalPreferencePage_enable_async_support_tooltip);
    GridDataFactory.fillDefaults().applyTo(enableAsyncSupportButton);

    // Enable Enums support checkbox
    enableEnumsSupportButton = createCheckBox(
        composite,
        PreferencesMessages.ExperimentalPreferencePage_enable_enums_support,
        PreferencesMessages.ExperimentalPreferencePage_enable_enums_support_tooltip);
    GridDataFactory.fillDefaults().applyTo(enableEnumsSupportButton);

    // Enable Analysis Server checkbox
    enableAnalysisServerButton = createCheckBox(
        composite,
        PreferencesMessages.ExperimentalPreferencePage_enable_analysis_server,
        PreferencesMessages.ExperimentalPreferencePage_enable_analysis_server_tooltip);
    GridDataFactory.fillDefaults().applyTo(enableAnalysisServerButton);
    enableAnalysisServerButton.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
      }

      @Override
      public void widgetSelected(SelectionEvent e) {
        updateServerOptionEnablement();
      }
    });

    // Analysis Server options
    {
      Composite group = new Composite(composite, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(group);
      GridLayoutFactory.fillDefaults().numColumns(2).extendedMargins(40, 0, 0, 0).applyTo(group);

      serverHttpPortLabel = new Label(group, SWT.NONE);
      serverHttpPortLabel.setText(PreferencesMessages.ExperimentalPreferencePage_server_http_port_label);

      // Http port for analysis server or empty string if none
      serverHttpPortText = new Text(group, SWT.BORDER | SWT.SINGLE | SWT.RIGHT);
      serverHttpPortText.setTextLimit(5);
      serverHttpPortText.setToolTipText(PreferencesMessages.ExperimentalPreferencePage_server_http_port_tooltip);
      GridDataFactory.fillDefaults().hint(50, SWT.DEFAULT).applyTo(serverHttpPortText);

      // Only allow integer values
      serverHttpPortText.addListener(SWT.Verify, new ValidIntListener());
    }

    // Separator
    {
      Label separatorLabel = new Label(composite, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(separatorLabel);
    }

    Label notes = new Label(composite, SWT.NONE);
    notes.setText(PreferencesMessages.ExperimentalPreferencePage_notes);
    GridDataFactory.fillDefaults().grab(true, true).applyTo(notes);

    // init
    initFromPrefs();

    return composite;
  }

  private String getPref(String prefKey) {
    return DartCore.getPlugin().getPrefs().get(prefKey, "").trim();
  }

  private boolean getPrefBool(String prefKey) {
    return DartCore.getPlugin().getPrefs().getBoolean(prefKey, false);
  }

  private void initFromPrefs() {
    enableAnalysisServerButton.setSelection(getPrefBool(DartCoreDebug.ENABLE_ANALYSIS_SERVER_PREF));
    enableAsyncSupportButton.setSelection(getPrefBool(DartCoreDebug.ENABLE_ASYNC_PREF));
    enableEnumsSupportButton.setSelection(getPrefBool(DartCoreDebug.ENABLE_ENUMS_PREF));
    String textValue = getPref(DartCoreDebug.ANALYSIS_SERVER_HTTP_PORT_PREF);
    try {
      if (Integer.parseInt(textValue) < 0) {
        textValue = "";
      }
    } catch (NumberFormatException nfe) {
      textValue = "";
    }
    serverHttpPortText.setText(textValue);
    updateServerOptionEnablement();
  }

  private boolean setPref(String prefKey, Button button) {
    boolean oldValue = getPrefBool(prefKey);
    boolean newValue = button.getSelection();
    DartCore.getPlugin().getPrefs().putBoolean(prefKey, newValue);
    return oldValue != newValue;
  }

  private boolean setPref(String prefKey, Text textBox) {
    String oldValue = getPref(prefKey);
    String newValue = textBox.getText().trim();
    DartCore.getPlugin().getPrefs().put(prefKey, newValue);
    return !oldValue.equals(newValue);
  }

  private void updateServerOptionEnablement() {
    boolean enabled = enableAnalysisServerButton.getSelection();
    serverHttpPortLabel.setForeground(getShell().getDisplay().getSystemColor(
        enabled ? SWT.COLOR_BLACK : SWT.COLOR_GRAY));
    serverHttpPortText.setEnabled(enabled);
  }
}
