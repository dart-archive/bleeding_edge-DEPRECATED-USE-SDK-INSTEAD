/*
 * Copyright (c) 2011, the Dart project authors.
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

import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
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
import org.eclipse.ui.internal.editors.text.EditorsPlugin;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

/**
 * Page for setting general Dart plug-in preferences (the root of all Dart preferences).
 */
@SuppressWarnings("restriction")
public class DartBasePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  public static final String JAVA_BASE_PREF_PAGE_ID = "com.google.dart.tools.ui.preferences.DartBasePreferencePage"; //$NON-NLS-1$

  private Button lineNumbersCheck;

  private Button printMarginCheck;
  private Text printMarginText;

  public DartBasePreferencePage() {
    setPreferenceStore(DartToolsPlugin.getDefault().getPreferenceStore());

    setDescription(PreferencesMessages.DartBasePreferencePage_editor_preferences);

    noDefaultAndApplyButton();
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

    editorPreferences.setValue(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN,
        printMarginCheck.getSelection());

    if (printMarginCheck.getSelection()) {
      editorPreferences.setValue(
          AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN,
          printMarginText.getText());
    }

    return true;
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);

    GridDataFactory.fillDefaults().grab(true, false).indent(0, 10).align(SWT.FILL, SWT.BEGINNING).applyTo(
        composite);
    GridLayoutFactory.fillDefaults().spacing(0, 8).margins(0, 10).applyTo(composite);

    Group generalGroup = new Group(composite, SWT.NONE);
    generalGroup.setText(PreferencesMessages.DartBasePreferencePage_general);
    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(
        generalGroup);
    GridLayoutFactory.fillDefaults().margins(8, 8).applyTo(generalGroup);

    // line numbers
    lineNumbersCheck = createCheckBox(generalGroup,
        PreferencesMessages.DartBasePreferencePage_show_line_numbers,
        PreferencesMessages.DartBasePreferencePage_show_line_numbers_tooltip);

    // print margin
    printMarginCheck = createCheckBox(generalGroup,
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
    GridDataFactory.fillDefaults().indent(20, 0).applyTo(printMarginText);

    // TODO(devoncarew): implement the text font preference
//    // text font
//    Group textGroup = new Group(composite, SWT.NONE);
//    textGroup.setText(PreferencesMessages.DartBasePreferencePage_text_font);
//    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(
//        textGroup);
//    GridLayoutFactory.fillDefaults().numColumns(2).margins(8, 8).applyTo(textGroup);
//
//    Label label = new Label(textGroup, SWT.NONE);
//    label.setText("blah blah 12pt");
//    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(label);
//
//    Button selectFontButton = new Button(textGroup, SWT.PUSH);
//    selectFontButton.setText(PreferencesMessages.DartBasePreferencePage_select);

    initFromPrefs();

    return composite;
  }

  private Button createCheckBox(Composite composite, String label, String tooltip) {
    final Button checkBox = new Button(composite, SWT.CHECK);

    checkBox.setText(label);
    checkBox.setToolTipText(tooltip);

    return checkBox;
  }

  private void initFromPrefs() {
    IPreferenceStore editorPreferences = EditorsPlugin.getDefault().getPreferenceStore();

    lineNumbersCheck.setSelection(editorPreferences.getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_LINE_NUMBER_RULER));

    printMarginCheck.setSelection(editorPreferences.getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN));
    printMarginText.setText(editorPreferences.getString(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN));

    printMarginText.setEnabled(printMarginCheck.getSelection());
  }

}
