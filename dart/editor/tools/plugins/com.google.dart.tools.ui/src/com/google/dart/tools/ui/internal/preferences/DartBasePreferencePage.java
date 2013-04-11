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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.PreferenceConstants;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.editors.text.EditorsPlugin;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

import java.io.IOException;

/**
 * Page for setting general Dart plug-in preferences (the root of all Dart preferences).
 */
@SuppressWarnings("restriction")
public class DartBasePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  public static final String DART_BASE_PREF_PAGE_ID = "com.google.dart.tools.ui.preferences.DartBasePreferencePage"; //$NON-NLS-1$

  private Button lineNumbersCheck;
  private Button printMarginCheck;
  private Text printMarginText;
  private Button removeTrailingWhitespaceCheck;
  private Button enableFolding;
  private Button enableAutoCompletion;
  private Button runPubAutoCheck;
  private Button legacyAnalyzer;

  public DartBasePreferencePage() {
    setPreferenceStore(DartToolsPlugin.getDefault().getPreferenceStore());

    noDefaultAndApplyButton();

    if (DartCore.isPluginsBuild()) {
      setDescription("Dart Editor version " + DartToolsPlugin.getVersionString()); //$NON-NLS-1$
    }
  }

  @Override
  public void init(IWorkbench workbench) {
    // do nothing
  }

  @Override
  public boolean performOk() {
    IPreferenceStore editorPreferences = EditorsPlugin.getDefault().getPreferenceStore();

    editorPreferences.setValue(
        AbstractDecoratedTextEditorPreferenceConstants.EDITOR_LINE_NUMBER_RULER,
        lineNumbersCheck.getSelection());

    editorPreferences.setValue(
        AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN,
        printMarginCheck.getSelection());

    if (printMarginCheck.getSelection()) {
      editorPreferences.setValue(
          AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN,
          printMarginText.getText());
    }

    handleSave(editorPreferences);

    IPreferenceStore toolsPreferenceStore = PreferenceConstants.getPreferenceStore();

    toolsPreferenceStore.setValue(
        PreferenceConstants.CODEASSIST_AUTOACTIVATION,
        enableAutoCompletion.getSelection());
    toolsPreferenceStore.setValue(
        PreferenceConstants.EDITOR_FOLDING_ENABLED,
        enableFolding.getSelection());
    toolsPreferenceStore.setValue(
        PreferenceConstants.EDITOR_REMOVE_TRAILING_WS,
        removeTrailingWhitespaceCheck.getSelection());
    handleSave(toolsPreferenceStore);

    IEclipsePreferences prefs = DartCore.getPlugin().getPrefs();
    if (prefs != null) {
      prefs.putBoolean(DartCore.PUB_AUTO_RUN_PREFERENCE, runPubAutoCheck.getSelection());
    }

    return true;
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);

    GridDataFactory.fillDefaults().grab(true, false).indent(0, 10).align(SWT.FILL, SWT.BEGINNING).applyTo(
        composite);
    GridLayoutFactory.fillDefaults().spacing(0, 8).margins(0, 10).applyTo(composite);

    // General preferences
    Group generalGroup = new Group(composite, SWT.NONE);
    generalGroup.setText(PreferencesMessages.DartBasePreferencePage_general);
    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(
        generalGroup);
    GridLayoutFactory.fillDefaults().numColumns(2).margins(8, 8).applyTo(generalGroup);

    lineNumbersCheck = createCheckBox(
        generalGroup,
        PreferencesMessages.DartBasePreferencePage_show_line_numbers,
        PreferencesMessages.DartBasePreferencePage_show_line_numbers_tooltip);
    GridDataFactory.fillDefaults().span(2, 1).applyTo(lineNumbersCheck);

    printMarginCheck = createCheckBox(
        generalGroup,
        PreferencesMessages.DartBasePreferencePage_show_print_margin,
        PreferencesMessages.DartBasePreferencePage_show_print_margin_tooltip);
    printMarginCheck.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        printMarginText.setEnabled(printMarginCheck.getSelection());
      }
    });

    printMarginText = new Text(generalGroup, SWT.BORDER | SWT.SINGLE | SWT.RIGHT);
    printMarginText.setTextLimit(5);
    GridDataFactory.fillDefaults().hint(50, SWT.DEFAULT).applyTo(printMarginText);

    enableAutoCompletion = createCheckBox(
        generalGroup,
        PreferencesMessages.DartBasePreferencePage_enable_auto_completion,
        PreferencesMessages.DartBasePreferencePage_enable_auto_completion_tooltip);
    GridDataFactory.fillDefaults().span(2, 1).applyTo(enableAutoCompletion);

    enableFolding = createCheckBox(
        generalGroup,
        PreferencesMessages.DartBasePreferencePage_enable_code_folding,
        PreferencesMessages.DartBasePreferencePage_enable_code_folding_tooltip);
    GridDataFactory.fillDefaults().span(2, 1).applyTo(enableFolding);

    Group saveGroup = new Group(composite, SWT.NONE);
    saveGroup.setText(PreferencesMessages.DartBasePreferencePage_save);
    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(
        saveGroup);
    GridLayoutFactory.fillDefaults().margins(8, 8).applyTo(saveGroup);

    removeTrailingWhitespaceCheck = createCheckBox(
        saveGroup,
        PreferencesMessages.DartBasePreferencePage_trailing_ws_label,
        PreferencesMessages.DartBasePreferencePage_trailing_ws_details);
    GridDataFactory.fillDefaults().applyTo(removeTrailingWhitespaceCheck);

    Group pubGroup = new Group(composite, SWT.NONE);
    pubGroup.setText(PreferencesMessages.DartBasePreferencePage_pub);
    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(
        pubGroup);
    GridLayoutFactory.fillDefaults().margins(8, 8).applyTo(pubGroup);

    runPubAutoCheck = createCheckBox(
        pubGroup,
        PreferencesMessages.DartBasePreferencePage_pub_auto_label,
        PreferencesMessages.DartBasePreferencePage_pub_auto_details);
    GridDataFactory.fillDefaults().applyTo(runPubAutoCheck);

    // Temporary experimental settings

    Group experimentalGroup = new Group(composite, SWT.NONE);
    experimentalGroup.setText("Experimental"); //$NON-NLS-1$
    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(
        experimentalGroup);
    GridLayoutFactory.fillDefaults().margins(8, 8).applyTo(experimentalGroup);

    legacyAnalyzer = createCheckBox(experimentalGroup, "Use legacy analyzer (not recommended)", //$NON-NLS-1$
        "Use legacy analyzer"); //$NON-NLS-1$
    GridDataFactory.fillDefaults().applyTo(legacyAnalyzer);

    legacyAnalyzer.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        boolean confirmRestart = MessageDialog.openConfirm(getShell(), "Confirm restart", //$NON-NLS-1$
            "The editor will restart NOW for this setting to take effect. " + //$NON-NLS-1$
                "If you need to save your work, please click Cancel, save your work, " + //$NON-NLS-1$
                "and return to this preference page to change the preference."); //$NON-NLS-1$
        if (confirmRestart) {
          DartCore.setUserDefinedProperty(
              DartCoreDebug.ENABLE_NEW_ANALYSIS_USER_FLAG,
              Boolean.toString(!legacyAnalyzer.getSelection()));
          PlatformUI.getWorkbench().restart();
        } else {
          // Cancel
          legacyAnalyzer.setSelection(!legacyAnalyzer.getSelection());
        }
      }
    });

    initFromPrefs();

    return composite;
  }

  private Button createCheckBox(Composite composite, String label, String tooltip) {
    final Button checkBox = new Button(composite, SWT.CHECK);

    checkBox.setText(label);
    checkBox.setToolTipText(tooltip);

    return checkBox;
  }

  private void handleSave(IPreferenceStore store) {
    if (store != null && store.needsSaving() && store instanceof IPersistentPreferenceStore) {
      try {
        ((IPersistentPreferenceStore) store).save();
      } catch (IOException e) {
        DartToolsPlugin.log(e);
      }
    }
  }

  private void initFromPrefs() {
    IPreferenceStore editorPreferences = EditorsPlugin.getDefault().getPreferenceStore();
    IPreferenceStore toolsPreferences = PreferenceConstants.getPreferenceStore();

    lineNumbersCheck.setSelection(editorPreferences.getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_LINE_NUMBER_RULER));
    printMarginCheck.setSelection(editorPreferences.getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN));
    printMarginText.setText(editorPreferences.getString(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN));
    printMarginText.setEnabled(printMarginCheck.getSelection());

    removeTrailingWhitespaceCheck.setSelection(toolsPreferences.getBoolean(PreferenceConstants.EDITOR_REMOVE_TRAILING_WS));
    enableAutoCompletion.setSelection(toolsPreferences.getBoolean(PreferenceConstants.CODEASSIST_AUTOACTIVATION));
    enableFolding.setSelection(toolsPreferences.getBoolean(PreferenceConstants.EDITOR_FOLDING_ENABLED));

    IEclipsePreferences prefs = DartCore.getPlugin().getPrefs();
    if (prefs != null) {
      runPubAutoCheck.setSelection(prefs.getBoolean(DartCore.PUB_AUTO_RUN_PREFERENCE, true));
    }

    legacyAnalyzer.setSelection(!DartCoreDebug.ENABLE_NEW_ANALYSIS);

  }

}
