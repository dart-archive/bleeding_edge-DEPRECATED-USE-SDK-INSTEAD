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
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.PubFolder;
import com.google.dart.tools.core.pub.RunPubJob;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.instrumentation.UIInstrumentation;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.formatter.DartFormatter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
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
import org.eclipse.swt.events.SelectionListener;
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
import org.osgi.service.prefs.BackingStoreException;

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
  private Button enableAnalysisServerButton;
  private Button enableFolding;
  private Button enableAutoCompletion;
  private Button runPubAutoCheck;
  private boolean runPubChanged = false;

  private Button formatCheck;

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
    if (!DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      toolsPreferenceStore.setValue(
          PreferenceConstants.EDITOR_FOLDING_ENABLED,
          enableFolding.getSelection());
    }
    toolsPreferenceStore.setValue(
        PreferenceConstants.EDITOR_REMOVE_TRAILING_WS,
        removeTrailingWhitespaceCheck.getSelection());

    toolsPreferenceStore.setValue(
        PreferenceConstants.EDITOR_FORMAT_ON_SAVES,
        formatCheck.getSelection());

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
      //
      // If the user has changed the preference to true,
      // then run pub on all pubspecs in the workspace
      //
      if (runPubChanged) {
        UIInstrumentationBuilder instrumentation = UIInstrumentation.builder(this.getClass());
        try {
          boolean autoRunPubEnabled = runPubAutoCheck.getSelection();
          instrumentation.metric("autoRunPubEnabled", autoRunPubEnabled);
          if (autoRunPubEnabled) {
            PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
              @Override
              public void run() {
                runPubNow();
              }
            });
          }
        } catch (RuntimeException e) {
          instrumentation.record(e);
          throw e;
        } finally {
          instrumentation.log();
        }
      }
    }

    boolean serverChanged = setPrefBool(
        DartCoreDebug.ENABLE_ANALYSIS_SERVER_PREF,
        true,
        enableAnalysisServerButton);

    if (serverChanged) {
      MessageDialog.openInformation(
          getShell(),
          "Restart Required",
          "These changes will only take effect once the IDE has been restarted.");
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

    if (!DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      enableFolding = createCheckBox(
          generalGroup,
          PreferencesMessages.DartBasePreferencePage_enable_code_folding,
          PreferencesMessages.DartBasePreferencePage_enable_code_folding_tooltip);
      GridDataFactory.fillDefaults().span(2, 1).applyTo(enableFolding);
    }

    lineNumbersCheck = createCheckBox(
        generalGroup,
        PreferencesMessages.DartBasePreferencePage_show_line_numbers,
        PreferencesMessages.DartBasePreferencePage_show_line_numbers_tooltip);
    GridDataFactory.fillDefaults().span(2, 1).applyTo(lineNumbersCheck);

    // Analysis group
    Group serverGroup = new Group(composite, SWT.NONE);
    serverGroup.setText("Analysis");
    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(
        serverGroup);
    GridLayoutFactory.fillDefaults().margins(8, 8).applyTo(serverGroup);

    enableAnalysisServerButton = createCheckBox(
        serverGroup,
        PreferencesMessages.ExperimentalPreferencePage_enable_analysis_server,
        PreferencesMessages.ExperimentalPreferencePage_enable_analysis_server_tooltip);
    GridDataFactory.fillDefaults().applyTo(enableAnalysisServerButton);

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

    formatCheck = createCheckBox(
        saveGroup,
        PreferencesMessages.DartBasePreferencePage_format_label,
        PreferencesMessages.DartBasePreferencePage_format_details);
    GridDataFactory.fillDefaults().applyTo(formatCheck);

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
    runPubAutoCheck.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
      }

      @Override
      public void widgetSelected(SelectionEvent e) {
        runPubChanged = true;
      }
    });

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

    enableAutoCompletion.setSelection(toolsPreferences.getBoolean(PreferenceConstants.CODEASSIST_AUTOACTIVATION));
    if (!DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      enableFolding.setSelection(toolsPreferences.getBoolean(PreferenceConstants.EDITOR_FOLDING_ENABLED));
    }
    removeTrailingWhitespaceCheck.setSelection(toolsPreferences.getBoolean(PreferenceConstants.EDITOR_REMOVE_TRAILING_WS));
    formatCheck.setSelection(toolsPreferences.getBoolean(PreferenceConstants.EDITOR_FORMAT_ON_SAVES));

    IEclipsePreferences prefs = DartCore.getPlugin().getPrefs();
    if (prefs != null) {
      runPubAutoCheck.setSelection(prefs.getBoolean(DartCore.PUB_AUTO_RUN_PREFERENCE, true));
      enableAnalysisServerButton.setSelection(prefs.getBoolean(
          DartCoreDebug.ENABLE_ANALYSIS_SERVER_PREF,
          true));
    }

  }

  /**
   * Run the "pub get" command against every pubspec in the workspace.
   */
  private void runPubNow() {
    final String pubCmd = RunPubJob.INSTALL_COMMAND;
    Job firstJob = null;
    Job previousJob = null;
    for (Project proj : DartCore.getProjectManager().getProjects()) {
      for (final PubFolder pubFolder : proj.getPubFolders()) {
        final RunPubJob job = new RunPubJob(pubFolder.getResource(), pubCmd, true);
        if (firstJob == null) {
          firstJob = job;
        } else {
          previousJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
              //
              // When one job completes, schedule the next unless the user cancels
              //
              if (event.getResult().getSeverity() != IStatus.CANCEL) {
                job.schedule();
              }
            }
          });
        }
        previousJob = job;
      }
    }
    firstJob.schedule();
  }

  private boolean setPrefBool(String prefKey, boolean def, Button button) {
    IEclipsePreferences prefs = DartCore.getPlugin().getPrefs();
    boolean oldValue = prefs.getBoolean(prefKey, def);
    boolean newValue = button.getSelection();
    prefs.putBoolean(prefKey, newValue);
    try {
      prefs.flush();
    } catch (BackingStoreException e) {

    }
    return oldValue != newValue;
  }
}
