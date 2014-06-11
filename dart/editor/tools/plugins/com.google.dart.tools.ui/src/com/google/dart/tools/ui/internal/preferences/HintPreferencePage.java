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
package com.google.dart.tools.ui.internal.preferences;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.jobs.CleanLibrariesJob;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Page for setting Hint Analysis preferences.
 */
public class HintPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  public static final String HINT_PREF_PAGE_ID = "com.google.dart.tools.ui.preferences.HintPreferencePage"; //$NON-NLS-1$

  private static Button createCheckBox(Composite composite, String label, String tooltip) {
    Button checkBox = new Button(composite, SWT.CHECK);
    checkBox.setText(label);
    checkBox.setToolTipText(tooltip);
    return checkBox;
  }

  private Group hintGroup;
  private Button enableHintsButton;

  private Button enableAngularAnalysisButton;
  private Button enableDart2jsHintsButton;

  public HintPreferencePage() {
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
      boolean hintsEnabled = DartCore.getPlugin().isHintsEnabled();
      boolean hintsDart2JSEnabled = DartCore.getPlugin().isHintsDart2JSEnabled();
      boolean angularAnalysisEnabled = DartCore.getPlugin().isAngularAnalysisEnabled();
      prefs.putBoolean(DartCore.ENABLE_HINTS_PREFERENCE, enableHintsButton.getSelection());
      prefs.putBoolean(
          DartCore.ENABLE_HINTS_DART2JS_PREFERENCE,
          enableDart2jsHintsButton.getSelection());
      prefs.putBoolean(
          DartCore.ENABLE_ANGULAR_ANALYSIS_PREFERENCE,
          enableAngularAnalysisButton.getSelection());
      if (hintsEnabled != enableHintsButton.getSelection()
          || angularAnalysisEnabled != enableAngularAnalysisButton.getSelection()
          || hintsDart2JSEnabled != enableDart2jsHintsButton.getSelection()) {
        // trigger processing for hints
        DartCore.getProjectManager().setHintOption(enableHintsButton.getSelection());
        DartCore.getProjectManager().setDart2JSHintOption(enableDart2jsHintsButton.getSelection());
        DartCore.getProjectManager().setAngularAnalysisOption(
            enableAngularAnalysisButton.getSelection());
        hasChanges = true;
      }
      try {
        DartCore.getPlugin().savePrefs();
      } catch (CoreException e) {
        DartToolsPlugin.log(e);
      }
      if (hasChanges) {
        Job job = new CleanLibrariesJob();
        job.schedule();
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

    // Enable Hints checkbox
    enableHintsButton = createCheckBox(
        composite,
        PreferencesMessages.HintPreferencePage_enable_hints,
        PreferencesMessages.HintPreferencePage_enable_hints_tooltip);
    enableHintsButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        // disable all widgets in the Hint Group
        boolean enableHintsButtonSelection = enableHintsButton.getSelection();
        enableDart2jsHintsButton.setEnabled(enableHintsButtonSelection);
        hintGroup.setEnabled(enableHintsButtonSelection);
      }
    });
    GridDataFactory.fillDefaults().applyTo(enableHintsButton);

    // Separator
    {
      Label separatorLabel = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(separatorLabel);
    }

    // Hints group
    hintGroup = new Group(composite, SWT.NONE);
    hintGroup.setText(PreferencesMessages.HintPreferencePage_hints_group);
    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(
        hintGroup);
    GridLayoutFactory.fillDefaults().margins(8, 8).applyTo(hintGroup);

    // Enable dart2js hints checkbox
    enableDart2jsHintsButton = createCheckBox(
        hintGroup,
        PreferencesMessages.HintPreferencePage_enable_dart2js_hints,
        PreferencesMessages.HintPreferencePage_enable_dart2js_hints_tooltip);
    GridDataFactory.fillDefaults().applyTo(enableDart2jsHintsButton);

    // Separator
    {
      Label separatorLabel = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(separatorLabel);
    }

    // Enable Angular analysis checkbox
    enableAngularAnalysisButton = createCheckBox(
        composite,
        PreferencesMessages.HintPreferencePage_enable_angular_analysis,
        PreferencesMessages.HintPreferencePage_enable_angular_analysis_tooltip);
    GridDataFactory.fillDefaults().applyTo(enableAngularAnalysisButton);

    // init
    initFromPrefs();

    return composite;
  }

  private void initFromPrefs() {
    IEclipsePreferences prefs = DartCore.getPlugin().getPrefs();
    if (prefs != null) {
      boolean enableHintsButtonSelection = prefs.getBoolean(DartCore.ENABLE_HINTS_PREFERENCE, true);
      enableHintsButton.setSelection(enableHintsButtonSelection);
      enableDart2jsHintsButton.setSelection(prefs.getBoolean(
          DartCore.ENABLE_HINTS_DART2JS_PREFERENCE,
          true));
      enableAngularAnalysisButton.setSelection(prefs.getBoolean(
          DartCore.ENABLE_ANGULAR_ANALYSIS_PREFERENCE,
          true));
      enableDart2jsHintsButton.setEnabled(enableHintsButtonSelection);
      hintGroup.setEnabled(enableHintsButtonSelection);
    }
  }

}
