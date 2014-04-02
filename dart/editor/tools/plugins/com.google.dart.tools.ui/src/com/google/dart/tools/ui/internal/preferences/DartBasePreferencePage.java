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
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.internal.formatter.DartFormatter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.internal.editors.text.EditorsPlugin;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

import java.io.IOException;

/**
 * Page for setting general Dart plug-in preferences (the root of all Dart preferences).
 */
@SuppressWarnings("restriction")
public class DartBasePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  /**
   * Listener that only allows digits to be entered into a text field
   */
  private final class ValidIntListener implements Listener {
    @Override
    public void handleEvent(Event e) {
      String txt = e.text;
      // Allow for delete
      if (txt.isEmpty()) {
        return;
      }
      try {
        // Only allow digits
        int num = Integer.parseInt(txt);
        if (num >= 0) {
          return;
        }
      } catch (NumberFormatException nfe) {
        // Error
      }

      e.doit = false;
      return;
    }
  }

  public static final String DART_BASE_PREF_PAGE_ID = "com.google.dart.tools.ui.preferences.DartBasePreferencePage"; //$NON-NLS-1$

  private Button lineNumbersCheck;
  private Button printMarginCheck;
  private Text printMarginText;
  private Button removeTrailingWhitespaceCheck;
  private Button insertSpacesForTabs;
  private Text tabDisplaySize;
  private Button enableFolding;
  private Button enableAutoCompletion;
  private Button runPubAutoCheck;

  private Button performCodeTransforms;

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

    DartFormatter.setMaxLineLengthEnabled(printMarginCheck.getSelection());
    if (printMarginCheck.getSelection()) {
      DartFormatter.setMaxLineLength(printMarginText.getText());
    }

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
    DartFormatter.setInsertSpacesForTabs(insertSpacesForTabs.getSelection());

    String tabWidth = tabDisplaySize.getText();
    if (tabWidth == null || tabWidth.isEmpty()) {
      tabWidth = Integer.toString(PreferenceConstants.EDITOR_DEFAULT_TAB_WIDTH);
    }
    DartFormatter.setSpacesPerIndent(tabWidth);

    DartFormatter.setPerformTransforms(performCodeTransforms.getSelection());

    handleSave(editorPreferences);
    handleSave(toolsPreferenceStore);

    IEclipsePreferences prefs = DartCore.getPlugin().getPrefs();
    if (prefs != null) {
      prefs.putBoolean(DartCore.PUB_AUTO_RUN_PREFERENCE, runPubAutoCheck.getSelection());
      try {
        DartCore.getPlugin().savePrefs();
      } catch (CoreException e) {
        DartToolsPlugin.log(e);
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

    // General group
    Group generalGroup = new Group(composite, SWT.NONE);
    generalGroup.setText(PreferencesMessages.DartBasePreferencePage_general);
    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(
        generalGroup);
    GridLayoutFactory.fillDefaults().numColumns(2).margins(8, 8).applyTo(generalGroup);

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

    lineNumbersCheck = createCheckBox(
        generalGroup,
        PreferencesMessages.DartBasePreferencePage_show_line_numbers,
        PreferencesMessages.DartBasePreferencePage_show_line_numbers_tooltip);
    GridDataFactory.fillDefaults().span(2, 1).applyTo(lineNumbersCheck);

    // Format group
    Group formatGroup = new Group(composite, SWT.NONE);
    formatGroup.setText(PreferencesMessages.DartBasePreferencePage_format);
    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(
        formatGroup);
    GridLayoutFactory.fillDefaults().numColumns(2).margins(8, 8).applyTo(formatGroup);

    printMarginCheck = createCheckBox(
        formatGroup,
        PreferencesMessages.DartBasePreferencePage_max_line_length,
        PreferencesMessages.DartBasePreferencePage_max_line_length_tooltip);
    printMarginCheck.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        printMarginText.setEnabled(printMarginCheck.getSelection());
      }
    });

    printMarginText = new Text(formatGroup, SWT.BORDER | SWT.SINGLE | SWT.RIGHT);
    printMarginText.setTextLimit(5);
    GridDataFactory.fillDefaults().hint(50, SWT.DEFAULT).applyTo(printMarginText);

    // Only allow integer values
    printMarginText.addListener(SWT.Verify, new ValidIntListener());

    Label tabDisplayLabel = new Label(formatGroup, SWT.NONE);
    tabDisplayLabel.setText(PreferencesMessages.DartBasePreferencePage_tab_width);

    tabDisplaySize = new Text(formatGroup, SWT.BORDER | SWT.SINGLE | SWT.RIGHT);
    tabDisplaySize.setTextLimit(2);
    GridDataFactory.fillDefaults().hint(50, SWT.DEFAULT).applyTo(tabDisplaySize);

    // Only allow integer values
    tabDisplaySize.addListener(SWT.Verify, new ValidIntListener());

    insertSpacesForTabs = createCheckBox(
        formatGroup,
        PreferencesMessages.DartBasePreferencePage_indent_using_spaces,
        PreferencesMessages.DartBasePreferencePage_indent_using_spaces_tooltip);
    GridDataFactory.fillDefaults().span(2, 1).applyTo(insertSpacesForTabs);

    performCodeTransforms = createCheckBox(
        formatGroup,
        PreferencesMessages.DartBasePreferencePage_perform_code_transforms,
        PreferencesMessages.DartBasePreferencePage_perform_code_transforms_tooltip);
    GridDataFactory.fillDefaults().span(2, 1).applyTo(performCodeTransforms);

    // Save actions group
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

    // Pub group
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

    // init
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
    printMarginCheck.setSelection(DartFormatter.getMaxLineLengthEnabled());
    printMarginText.setText(DartFormatter.getMaxLineLength());
    printMarginText.setEnabled(printMarginCheck.getSelection());

    removeTrailingWhitespaceCheck.setSelection(toolsPreferences.getBoolean(PreferenceConstants.EDITOR_REMOVE_TRAILING_WS));
    enableAutoCompletion.setSelection(toolsPreferences.getBoolean(PreferenceConstants.CODEASSIST_AUTOACTIVATION));
    enableFolding.setSelection(toolsPreferences.getBoolean(PreferenceConstants.EDITOR_FOLDING_ENABLED));
    insertSpacesForTabs.setSelection(DartFormatter.getInsertSpacesForTabs());
    tabDisplaySize.setText(DartFormatter.getSpacesPerIndent());
    performCodeTransforms.setSelection(DartFormatter.getPerformTransforms());

    IEclipsePreferences prefs = DartCore.getPlugin().getPrefs();
    if (prefs != null) {
      runPubAutoCheck.setSelection(prefs.getBoolean(DartCore.PUB_AUTO_RUN_PREFERENCE, true));
    }
  }
}
