/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.preferences.ui;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.wst.sse.core.internal.SSECorePlugin;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.preferences.TabFolderLayout;

public class FilePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  private IPreferenceTab[] fTabs = null;

  protected Composite createComposite(Composite parent, int numColumns) {
    Composite composite = new Composite(parent, SWT.NULL);

    //GridLayout
    GridLayout layout = new GridLayout();
    layout.numColumns = numColumns;
    composite.setLayout(layout);

    //GridData
    GridData data = new GridData(GridData.FILL);
    data.horizontalIndent = 0;
    data.verticalAlignment = GridData.FILL_VERTICAL;
    data.horizontalAlignment = GridData.FILL_HORIZONTAL;
    composite.setLayoutData(data);

    return composite;
  }

  protected Control createContents(Composite parent) {
    Composite composite = createComposite(parent, 1);

    String description = SSEUIMessages.FilePreferencePage_0; //$NON-NLS-1$
    createLabel(composite, description);
    createLabel(composite, ""); //$NON-NLS-1$

    TabFolder folder = new TabFolder(composite, SWT.NONE);
    folder.setLayout(new TabFolderLayout());
    folder.setLayoutData(new GridData(GridData.FILL_BOTH));

//		TabItem taskItem = new TabItem(folder, SWT.NONE);
//		IPreferenceTab tasksTab = new TaskTagPreferenceTab();
//		taskItem.setText(tasksTab.getTitle());
//		Control taskTags = tasksTab.createContents(folder);
//		taskItem.setControl(taskTags);

    TabItem translucenceItem = new TabItem(folder, SWT.NONE);
    IPreferenceTab translucenceTab = new TranslucencyPreferenceTab(this);
    translucenceItem.setText(translucenceTab.getTitle());
    Control translucenceControl = translucenceTab.createContents(folder);
    translucenceItem.setControl(translucenceControl);

    fTabs = new IPreferenceTab[] {/* tasksTab, */translucenceTab};

    return composite;
  }

  protected Label createLabel(Composite parent, String text) {
    Label label = new Label(parent, SWT.LEFT);
    label.setText(text);

    //GridData
    GridData data = new GridData(GridData.FILL);
    data.horizontalAlignment = GridData.HORIZONTAL_ALIGN_FILL;
    label.setLayoutData(data);

    return label;
  }

  public void init(IWorkbench desktop) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.preference.PreferencePage#performApply()
   */
  protected void performApply() {
    super.performApply();
    for (int i = 0; i < fTabs.length; i++) {
      fTabs[i].performApply();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
   */
  protected void performDefaults() {
    super.performDefaults();
    for (int i = 0; i < fTabs.length; i++) {
      fTabs[i].performDefaults();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.preference.IPreferencePage#performOk()
   */
  public boolean performOk() {
    boolean ok = super.performOk();
    for (int i = 0; i < fTabs.length; i++) {
      fTabs[i].performOk();
    }
    SSECorePlugin.getDefault().savePluginPreferences();
    return ok;
  }
}
