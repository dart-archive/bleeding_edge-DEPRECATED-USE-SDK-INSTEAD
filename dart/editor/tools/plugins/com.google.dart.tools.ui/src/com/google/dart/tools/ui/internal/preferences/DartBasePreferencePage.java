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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.framework.Version;

/**
 * Page for setting general Dart plug-in preferences (the root of all Dart preferences).
 */
public class DartBasePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  public static final String JAVA_BASE_PREF_PAGE_ID = "com.google.dart.tools.ui.preferences.DartBasePreferencePage"; //$NON-NLS-1$

  private static String getVersionText() {
    Version version = DartToolsPlugin.getDefault().getBundle().getVersion();

    return version.getMajor() + "." + version.getMinor() + "." + version.getMicro(); //$NON-NLS-1$ //$NON-NLS-2$
  }

  public DartBasePreferencePage() {
    super();

    setPreferenceStore(DartToolsPlugin.getDefault().getPreferenceStore());

    setDescription("Dart Editor v" + getVersionText()); //$NON-NLS-1$

    noDefaultAndApplyButton();
  }

  @Override
  public void init(IWorkbench workbench) {
    // do nothing
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite result = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
    layout.marginWidth = 0;
    layout.verticalSpacing = convertVerticalDLUsToPixels(10);
    layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
    result.setLayout(layout);

    Composite composite = new Composite(result, SWT.NONE);
    composite.setLayout(GridLayoutFactory.fillDefaults().margins(0, 0).spacing(0, 0).create());

    Label header = new Label(composite, SWT.NONE);
    header.setText(PreferencesMessages.DartBasePreferencePage_header_text);
    new Label(composite, SWT.NONE); //spacer
    Label description = new Label(composite, SWT.NONE);
    description.setText(PreferencesMessages.DartBasePreferencePage_description_text);

    return result;
  }

}
