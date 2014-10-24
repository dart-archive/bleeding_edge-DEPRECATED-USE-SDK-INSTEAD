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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
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

//  private Button enableAnalysisServerButton;
  private Button enableAsyncSupportButton;

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
    boolean hasChanges = false;
    IEclipsePreferences prefs = DartCore.getPlugin().getPrefs();
    if (prefs != null) {

//      boolean analysisServerEnabled = DartCoreDebug.ENABLE_ANALYSIS_SERVER;
//      prefs.putBoolean(
//          DartCoreDebug.ENABLE_ANALYSIS_SERVER_PREF,
//          enableAnalysisServerButton.getSelection());

      boolean asyncEnabled = DartCoreDebug.ENABLE_ASYNC;
      prefs.putBoolean(DartCoreDebug.ENABLE_ASYNC_PREF, enableAsyncSupportButton.getSelection());

      if (/* analysisServerEnabled != enableAnalysisServerButton.getSelection()
          || */asyncEnabled != enableAsyncSupportButton.getSelection()) {
        hasChanges = true;
      }
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

    // Enable Analysis Server checkbox
//    enableAnalysisServerButton = createCheckBox(
//        composite,
//        PreferencesMessages.ExperimentalPreferencePage_enable_analysis_server,
//        PreferencesMessages.ExperimentalPreferencePage_enable_analysis_server_tooltip);
//    GridDataFactory.fillDefaults().applyTo(enableAnalysisServerButton);

    // Enable Async support checkbox
    enableAsyncSupportButton = createCheckBox(
        composite,
        PreferencesMessages.ExperimentalPreferencePage_enable_async_support,
        PreferencesMessages.ExperimentalPreferencePage_enable_async_support_tooltip);
    GridDataFactory.fillDefaults().applyTo(enableAsyncSupportButton);

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

  private void initFromPrefs() {
//    enableAnalysisServerButton.setSelection(DartCoreDebug.ENABLE_ANALYSIS_SERVER);
    enableAsyncSupportButton.setSelection(DartCoreDebug.ENABLE_ASYNC);
  }
}
