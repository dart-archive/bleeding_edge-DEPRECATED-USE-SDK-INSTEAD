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
import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;
import com.google.dart.tools.core.jobs.CleanLibrariesJob;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.PreferenceConstants;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.internal.editors.text.EditorsPlugin;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.osgi.service.prefs.BackingStoreException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Page for setting general Dart plug-in preferences (the root of all Dart preferences).
 */
@SuppressWarnings("restriction")
public class DartBasePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  public static final String JAVA_BASE_PREF_PAGE_ID = "com.google.dart.tools.ui.preferences.DartBasePreferencePage"; //$NON-NLS-1$

  private Button lineNumbersCheck;
  private Button printMarginCheck;
  private Text printMarginText;
  private Button removeTrailingWhitespaceCheck;
  private Text packageRootDir;
  private Text auxDirText;
  private Button enableFolding;

  public DartBasePreferencePage() {
    setPreferenceStore(DartToolsPlugin.getDefault().getPreferenceStore());

    noDefaultAndApplyButton();

    if (DartCore.isPluginsBuild()) {
      setDescription("Dart Editor version " + DartToolsPlugin.getVersionString());
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
        PreferenceConstants.EDITOR_FOLDING_ENABLED,
        enableFolding.getSelection());
    toolsPreferenceStore.setValue(
        PreferenceConstants.EDITOR_REMOVE_TRAILING_WS,
        removeTrailingWhitespaceCheck.getSelection());
    handleSave(toolsPreferenceStore);

    List<File> packageRoots = new ArrayList<File>();
    String newRoot = packageRootDir.getText().trim();
    String extDir = auxDirText.getText().trim();
    if (!newRoot.isEmpty()) {
      packageRoots.add(new File(newRoot));
    }
    if (!extDir.isEmpty()) {
      packageRoots.add(new File(extDir));
    }
    IEclipsePreferences prefs = DartCore.getPlugin().getPrefs();
    if (prefs != null) {
      String root = prefs.get(DartCore.PACKAGE_ROOT_DIR_PREFERENCE, ""); //$NON-NLS-1$
      if (!root.equals(newRoot)) {
        prefs.put(DartCore.PACKAGE_ROOT_DIR_PREFERENCE, newRoot); //$NON-NLS-1$
        try {
          prefs.flush();
        } catch (BackingStoreException e) {
          DartToolsPlugin.log(e);
        }
      }
      String extDirPref = prefs.get(DartCore.AUXILIARY_DIR_PREFERENCE, ""); //$NON-NLS-1$

      if (!extDirPref.equals(extDir)) {
        prefs.put(DartCore.AUXILIARY_DIR_PREFERENCE, extDir); //$NON-NLS-1$
        try {
          prefs.flush();
        } catch (BackingStoreException e) {
          DartToolsPlugin.log(e);
        }
      }
      if (!root.equals(newRoot) || !extDir.equals(extDirPref)) {
        PackageLibraryManagerProvider.getAnyLibraryManager().setPackageRoots(packageRoots);
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

    createDirectorySettings(composite);

    initFromPrefs();

    return composite;
  }

  private Button createCheckBox(Composite composite, String label, String tooltip) {
    final Button checkBox = new Button(composite, SWT.CHECK);

    checkBox.setText(label);
    checkBox.setToolTipText(tooltip);

    return checkBox;
  }

  private void createDirectorySettings(Composite composite) {
    // Package directory preferences
    Group packageGroup = new Group(composite, SWT.NONE);
    packageGroup.setText(PreferencesMessages.DartBasePreferencePage_groupTitle);
    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(
        packageGroup);
    GridLayoutFactory.fillDefaults().numColumns(2).margins(8, 8).applyTo(packageGroup);

    Label packageRootLabel = new Label(packageGroup, SWT.NONE);
    packageRootLabel.setText(PreferencesMessages.DartBasePreferencePage_Package_Title);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).span(2, 1).grab(true, false).applyTo(
        packageRootLabel);

    packageRootDir = new Text(packageGroup, SWT.SINGLE | SWT.BORDER);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).hint(350, SWT.DEFAULT).grab(
        true,
        false).applyTo(packageRootDir);

    Button selectPackageDirButton = new Button(packageGroup, SWT.PUSH);
    selectPackageDirButton.setText(PreferencesMessages.DartBasePreferencePage_Browse);
    PixelConverter converter = new PixelConverter(selectPackageDirButton);
    int widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
    GridDataFactory.swtDefaults().hint(widthHint, -1).applyTo(selectPackageDirButton);
    selectPackageDirButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        String dirPath = handleBrowseButton();
        if (dirPath != null) {
          packageRootDir.setText(dirPath);
        }
      }
    });

    Label auxDirLabel = new Label(packageGroup, SWT.NONE);
    auxDirLabel.setText(PreferencesMessages.DartBasePreferencePage_auxiliaryDirectory);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).span(2, 1).grab(true, false).applyTo(
        auxDirLabel);

    auxDirText = new Text(packageGroup, SWT.SINGLE | SWT.BORDER);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).hint(350, SWT.DEFAULT).grab(
        true,
        false).applyTo(auxDirText);
    Button selectAuxDirButton = new Button(packageGroup, SWT.PUSH);
    selectAuxDirButton.setText(PreferencesMessages.DartBasePreferencePage_Browse);
    GridDataFactory.swtDefaults().hint(widthHint, -1).applyTo(selectAuxDirButton);
    selectAuxDirButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleBrowseButton();
        String dirPath = handleBrowseButton();
        if (dirPath != null) {
          auxDirText.setText(dirPath);
        }
      }
    });
  }

  private String handleBrowseButton() {

    DirectoryDialog directoryDialog = new DirectoryDialog(getShell());

    return directoryDialog.open();

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
    enableFolding.setSelection(toolsPreferences.getBoolean(PreferenceConstants.EDITOR_FOLDING_ENABLED));

    IEclipsePreferences prefs = DartCore.getPlugin().getPrefs();
    if (prefs != null) {
      String root = prefs.get(DartCore.PACKAGE_ROOT_DIR_PREFERENCE, ""); //$NON-NLS-1$
      packageRootDir.setText(root);
      root = prefs.get(DartCore.AUXILIARY_DIR_PREFERENCE, ""); //$NON-NLS-1$
      auxDirText.setText(root);
    }

  }

}
