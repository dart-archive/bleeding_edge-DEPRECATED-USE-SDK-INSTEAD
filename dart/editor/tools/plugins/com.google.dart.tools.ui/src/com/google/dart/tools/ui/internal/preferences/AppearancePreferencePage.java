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
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

/**
 * @deprecated this page has been removed from the plugin.xml declaration
 */
@Deprecated
public class AppearancePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  public static final String PREF_COLORED_LABELS = "colored_labels_in_views"; //$NON-NLS-1$

//  private static final String PREF_METHOD_RETURNTYPE = PreferenceConstants.APPEARANCE_METHOD_RETURNTYPE;
//  private static final String PREF_CATEGORY = PreferenceConstants.APPEARANCE_CATEGORY;

//  private SelectionButtonDialogField fShowMethodReturnType;
//  private SelectionButtonDialogField fShowCategory;
//  private SelectionButtonDialogField fShowColoredLabels;

  public AppearancePreferencePage() {
    setPreferenceStore(DartToolsPlugin.getDefault().getPreferenceStore());
    setDescription(PreferencesMessages.AppearancePreferencePage_description);

//    IDialogFieldListener listener = new IDialogFieldListener() {
//      @Override
//      public void dialogFieldChanged(DialogField field) {
//        doDialogFieldChanged(field);
//      }
//    };

//    fShowMethodReturnType = new SelectionButtonDialogField(SWT.CHECK);
//    fShowMethodReturnType.setDialogFieldListener(listener);
//    fShowMethodReturnType.setLabelText(PreferencesMessages.AppearancePreferencePage_inferredmethodreturntype_label);
//
//    fShowCategory = new SelectionButtonDialogField(SWT.CHECK);
//    fShowCategory.setDialogFieldListener(listener);
//    fShowCategory.setLabelText(PreferencesMessages.AppearancePreferencePage_showCategory_label);
//
//    fShowColoredLabels = new SelectionButtonDialogField(SWT.CHECK);
//    fShowColoredLabels.setDialogFieldListener(listener);
//    fShowColoredLabels.setLabelText(PreferencesMessages.AppearancePreferencePage_coloredlabels_label);
  }

  /*
   * @see PreferencePage#createControl(Composite)
   */
  @Override
  public void createControl(Composite parent) {
    super.createControl(parent);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        getControl(),
        DartHelpContextIds.APPEARANCE_PREFERENCE_PAGE);
  }

  /*
   * @see IWorkbenchPreferencePage#init(IWorkbench)
   */
  @Override
  public void init(IWorkbench workbench) {
  }

  /*
   * @see IPreferencePage#performOk()
   */
  @Override
  public boolean performOk() {
//    IPreferenceStore prefs = getPreferenceStore();

//    prefs.setValue(PREF_METHOD_RETURNTYPE, fShowMethodReturnType.isSelected());
//    prefs.setValue(PREF_CATEGORY, fShowCategory.isSelected());
//    prefs.setValue(PREF_COLORED_LABELS, fShowColoredLabels.isSelected());

    DartToolsPlugin.getDefault().savePluginPreferences();

    return super.performOk();
  }

  /*
   * @see PreferencePage#createContents(Composite)
   */
  @Override
  protected Control createContents(Composite parent) {
    initializeDialogUnits(parent);
    int nColumns = 1;

    Composite result = new Composite(parent, SWT.NONE);
    result.setFont(parent.getFont());

    GridLayout layout = new GridLayout();
    layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
    layout.marginWidth = 0;
    layout.numColumns = nColumns;
    result.setLayout(layout);

//    fShowMethodReturnType.doFillIntoGrid(result, nColumns);
//    fShowCategory.doFillIntoGrid(result, nColumns);
//    fShowColoredLabels.doFillIntoGrid(result, nColumns);

    initFields();

    Dialog.applyDialogFont(result);
    return result;
  }

  /*
   * @see PreferencePage#performDefaults()
   */
  @Override
  protected void performDefaults() {
//    IPreferenceStore prefs = getPreferenceStore();

//    fShowMethodReturnType.setSelection(prefs.getDefaultBoolean(PREF_METHOD_RETURNTYPE));
//    fShowCategory.setSelection(prefs.getDefaultBoolean(PREF_CATEGORY));
//    fShowColoredLabels.setSelection(false);

    super.performDefaults();
  }

//  private void doDialogFieldChanged(DialogField field) {
//    updateStatus(getValidationStatus());
//  }

//  private IStatus getValidationStatus() {
//    return new StatusInfo();
//  }

  private void initFields() {
//    IPreferenceStore prefs = getPreferenceStore();

//    fShowMethodReturnType.setSelection(prefs.getBoolean(PREF_METHOD_RETURNTYPE));
//    fShowCategory.setSelection(prefs.getBoolean(PREF_CATEGORY));
//    fShowColoredLabels.setSelection(prefs.getBoolean(PREF_COLORED_LABELS));
  }

//  private void updateStatus(IStatus status) {
//    setValid(!status.matches(IStatus.ERROR));
//    StatusUtil.applyToStatusLine(this, status);
//  }
}
