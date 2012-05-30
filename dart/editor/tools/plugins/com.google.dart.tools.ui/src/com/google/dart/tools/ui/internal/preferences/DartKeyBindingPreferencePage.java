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

import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.Geometry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.activities.IActivityManager;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.keys.IBindingService;

import java.io.File;

// TODO verify .savepath
public class DartKeyBindingPreferencePage extends PreferencePage implements
    IWorkbenchPreferencePage {

  private static final String EXT_XML = ".xml"; //$NON-NLS-1$
  private static final String ALL_XML_FILES = "*" + EXT_XML; //$NON-NLS-1$
  private static final String ALL_FILES = "*.*"; //$NON-NLS-1$
  private static final String ENCODING = "UTF-8"; //$NON-NLS-1$
  private static final String LAST_KEY_PATH = "com.google.dart.tools.ui.prefs.key.path"; //$NON-NLS-1$
  private static final String SAVE_PATH = ".savepath"; //$NON-NLS-1$ // currently using same previous entry for both import & export
  private static final String DESCR_EXPORT = PreferencesMessages.DartKeyBindingPref_Export;
  private static final String DESCR_IMPORT = PreferencesMessages.DartKeyBindingPref_Import;
  private static final String DESCR_RESET = PreferencesMessages.DartKeyBindingPref_Reset;
  private static final String DESCR_EMACS = PreferencesMessages.DartKeyBindingPref_Emacs;

  private Button exportButton;
  private Button importButton;
  private Button resetButton;
  private IActivityManager activityManager;
  private IBindingService bindingService;
  private ICommandService commandService;
  private Button emacsButton;

  public DartKeyBindingPreferencePage() {
    setPreferenceStore(DartToolsPlugin.getDefault().getPreferenceStore());
    noDefaultAndApplyButton();
  }

  @Override
  public void init(IWorkbench workbench) {
    activityManager = workbench.getActivitySupport().getActivityManager();
    bindingService = (IBindingService) workbench.getService(IBindingService.class);
    commandService = (ICommandService) workbench.getService(ICommandService.class);
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);

    GridDataFactory.fillDefaults().grab(true, false).indent(0, 10).align(SWT.FILL, SWT.BEGINNING).applyTo(
        composite);
    GridLayoutFactory.fillDefaults().spacing(0, 8).margins(0, 10).applyTo(composite);

    Group generalGroup = new Group(composite, SWT.NONE);
    generalGroup.setText(PreferencesMessages.DartKeyBindingPref_Modify);
    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(
        generalGroup);
    GridLayoutFactory.fillDefaults().numColumns(1).margins(8, 8).applyTo(generalGroup);

    new Label(generalGroup, SWT.NONE).setText(DESCR_EXPORT);
    exportButton = new Button(generalGroup, SWT.PUSH | SWT.FLAT | SWT.CENTER);
    exportButton.setText(PreferencesMessages.DartKeyBindingPref_ToFile);
    exportButton.setLayoutData(new GridData(SWT.CENTER, SWT.END, false, false));
    exportButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        exportKeys();
      }
    });

    new Label(generalGroup, SWT.NONE).setText(DESCR_IMPORT);
    importButton = new Button(generalGroup, SWT.PUSH | SWT.FLAT | SWT.CENTER);
    importButton.setText(PreferencesMessages.DartKeyBindingPref_FromFile);
    importButton.setLayoutData(new GridData(SWT.CENTER, SWT.END, false, false));
    importButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        importKeys();
      }
    });

    new Label(generalGroup, SWT.NONE).setText(DESCR_RESET);
    resetButton = new Button(generalGroup, SWT.PUSH | SWT.FLAT | SWT.CENTER);
    resetButton.setText(PreferencesMessages.DartKeyBindingPref_ToDefaults);
    resetButton.setLayoutData(new GridData(SWT.CENTER, SWT.END, false, false));
    resetButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        resetBindings();
      }
    });

    if (DartCoreDebug.ENABLE_ALT_KEY_BINDINGS) {
      new Label(generalGroup, SWT.NONE).setText(DESCR_EMACS);
      emacsButton = new Button(generalGroup, SWT.PUSH | SWT.FLAT | SWT.CENTER);
      emacsButton.setText(PreferencesMessages.DartKeyBindingPref_ToEmacs);
      emacsButton.setLayoutData(new GridData(SWT.CENTER, SWT.END, false, false));
      emacsButton.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          setEmacsBindings();
        }
      });
    }

    Point preferredSize = resetButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, false);
    Point hint = Geometry.max(LayoutConstants.getMinButtonSize(), preferredSize);
    GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).hint(hint).applyTo(exportButton);
    GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).hint(hint).applyTo(importButton);
    GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).hint(hint).applyTo(resetButton);
    if (DartCoreDebug.ENABLE_ALT_KEY_BINDINGS) {
      GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).hint(hint).applyTo(emacsButton);
    }

    return composite;
  }

  private void exportKeys() {
    String path = requestFilePath(SWT.SAVE,
        PreferencesMessages.DartKeyBindingPref_ChooseExportFile, SAVE_PATH);
    if (path == null) {
      return;
    }
    File file = new File(path);
    if (file.exists()
        && !MessageDialog.openQuestion(
            getShell(),
            PreferencesMessages.DartKeyBindingPref_ExportTitle,
            Messages.format(PreferencesMessages.DartKeyBindingPref_ConfirmReplace,
                file.getAbsolutePath()))) {
      return;
    }
    try {
      DartKeyBindingPersistence persist;
      persist = new DartKeyBindingPersistence(activityManager, bindingService, commandService);
      persist.writeFile(file, getEncoding());
    } catch (CoreException ex) {
      String title = PreferencesMessages.DartKeyBindingPref_ExportTitle;
      String message = PreferencesMessages.DartKeyBindingPref_CouldNotExport;
      ExceptionHandler.handle(ex, getShell(), title, message);
    }
  }

  private String getEncoding() {
    String encoding = ENCODING;
    IContentType type = Platform.getContentTypeManager().getContentType(
        "com.google.dart.tools.core.runtime.xml"); //$NON-NLS-1$
    if (type != null) {
      encoding = type.getDefaultCharset();
    }
    return encoding;
  }

  private void importKeys() {
    String path = requestFilePath(SWT.OPEN,
        PreferencesMessages.DartKeyBindingPref_ChooseImportFile, SAVE_PATH);
    if (path == null) {
      return;
    }
    File file = new File(path);
    if (!(file.exists() && file.canRead())) {
      MessageDialog.openError(
          getShell(),
          PreferencesMessages.DartKeyBindingPref_ImportTitle,
          Messages.format(PreferencesMessages.DartKeyBindingPref_FileReadError,
              file.getAbsolutePath()));
      return;
    }
    try {
      DartKeyBindingPersistence persist;
      persist = new DartKeyBindingPersistence(activityManager, bindingService, commandService);
      persist.readFile(file);
    } catch (Exception ex) {
      MessageDialog.openError(
          getShell(),
          PreferencesMessages.DartKeyBindingPref_ImportTitle,
          Messages.format(PreferencesMessages.DartKeyBindingPref_CouldNotLoad,
              new Object[] {
                  file.getAbsolutePath(),
                  ex.getMessage() == null ? ex.getClass().getCanonicalName() : ex.getMessage()}));
    }
  }

  private String requestFilePath(int mode, String prompt, String keySuffix) {
    FileDialog file = new FileDialog(getShell(), mode);
    file.setText(PreferencesMessages.DartKeyBindingPref_ChooseFile);
    file.setFilterNames(new String[] {ALL_XML_FILES, ALL_FILES});
    file.setFilterExtensions(new String[] {ALL_XML_FILES, ALL_FILES});
    String lastPath = DartToolsPlugin.getDefault().getDialogSettings().get(
        LAST_KEY_PATH + keySuffix);
    if (lastPath != null) {
      file.setFilterPath(lastPath);
    }
    String path = file.open();
    if (path != null) {
      if (!path.endsWith(EXT_XML)) {
        path = path + EXT_XML;
      }
      DartToolsPlugin.getDefault().getDialogSettings().put(LAST_KEY_PATH + SAVE_PATH,
          file.getFilterPath());
      return path;
    }
    return null;
  }

  private void resetBindings() {
    if (!MessageDialog.openQuestion(getShell(),
        PreferencesMessages.DartKeyBindingPref_ResetBindings,
        PreferencesMessages.DartKeyBindingPref_ConfirmReset)) {
      return;
    }
    try {
      DartKeyBindingPersistence persist;
      persist = new DartKeyBindingPersistence(activityManager, bindingService, commandService);
      persist.resetBindings();
    } catch (CoreException ex) {
      DartToolsPlugin.log(ex);
    }
  }

  private void setEmacsBindings() {
    // TODO see DartCoreDebug.ENABLE_ALT_KEY_BINDINGS
    MessageDialog.openInformation(getShell(), "Not Implemented", //$NON-NLS-1$
        "Emacs bindings are not defined yet."); //$NON-NLS-1$
  }
}
