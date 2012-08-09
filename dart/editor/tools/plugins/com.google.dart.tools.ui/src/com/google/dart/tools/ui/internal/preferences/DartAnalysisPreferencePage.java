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
import com.google.dart.tools.ui.internal.util.CleanLibrariesJob;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Page for setting Dart analysis preferences.
 */
public class DartAnalysisPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  public static final String JAVA_BASE_PREF_PAGE_ID = "com.google.dart.tools.ui.preferences.DartAnalysisPreferencePage"; //$NON-NLS-1$

  private static boolean applyCheck(String key, boolean currentValue, boolean def) {
    IEclipsePreferences prefs = DartCore.getPlugin().getPrefs();
    if (prefs != null) {
      boolean root = prefs.getBoolean(key, def);
      if (currentValue != root) {
        prefs.putBoolean(key, currentValue);
        return true;
      }
    }
    return false;
  }

  private static Button createCheckBox(Composite composite, String label, String tooltip) {
    Button checkBox = new Button(composite, SWT.CHECK);
    checkBox.setText(label);
    checkBox.setToolTipText(tooltip);
    return checkBox;
  }

  private Button warningForInferredTypes_check;

  public DartAnalysisPreferencePage() {
    setPreferenceStore(null);
    noDefaultAndApplyButton();
  }

  @Override
  public void init(IWorkbench workbench) {
  }

  @Override
  public boolean performOk() {
    boolean hasChanges = false;
    hasChanges |= applyCheck(
        DartCore.SUPPRESS_NO_MEMBER_FOR_INFERRED_TYPES,
        !warningForInferredTypes_check.getSelection(),
        true);
    if (hasChanges) {
      Job job = new CleanLibrariesJob();
      job.schedule();
    }
    return true;
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayoutFactory.fillDefaults().spacing(0, 8).margins(0, 10).applyTo(composite);
    // create UI
    warningForInferredTypes_check = createCheckBox(
        composite,
        PreferencesMessages.DartAnalysisPreferencePage_suppressNoMemberWarningForInferredTypes,
        null);
    // done
    initFromPrefs();
    return composite;
  }

  private void initFromPrefs() {
    IEclipsePreferences prefs = DartCore.getPlugin().getPrefs();
    warningForInferredTypes_check.setSelection(!prefs.getBoolean(
        DartCore.SUPPRESS_NO_MEMBER_FOR_INFERRED_TYPES,
        true));
  }

}
