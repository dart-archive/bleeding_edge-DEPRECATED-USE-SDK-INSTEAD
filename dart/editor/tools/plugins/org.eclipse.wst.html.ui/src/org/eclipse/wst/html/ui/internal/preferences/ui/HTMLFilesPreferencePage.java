/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal.preferences.ui;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.html.core.internal.HTMLCorePlugin;
import org.eclipse.wst.html.core.internal.provisional.contenttype.ContentTypeIdForHTML;
import org.eclipse.wst.html.ui.internal.HTMLUIMessages;
import org.eclipse.wst.html.ui.internal.HTMLUIPlugin;
import org.eclipse.wst.html.ui.internal.editor.IHelpContextIds;
import org.eclipse.wst.sse.core.internal.encoding.CommonEncodingPreferenceNames;
import org.eclipse.wst.xml.ui.internal.preferences.WorkbenchDefaultEncodingSettings;
import org.eclipse.wst.xml.ui.internal.preferences.XMLFilesPreferencePage;

public class HTMLFilesPreferencePage extends XMLFilesPreferencePage {
  private WorkbenchDefaultEncodingSettings fInputEncodingSettings = null;

  protected Preferences getModelPreferences() {
    return HTMLCorePlugin.getDefault().getPluginPreferences();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.preference.PreferencePage#doGetPreferenceStore()
   */
  protected IPreferenceStore doGetPreferenceStore() {
    return HTMLUIPlugin.getDefault().getPreferenceStore();
  }

  protected void doSavePreferenceStore() {
    HTMLCorePlugin.getDefault().savePluginPreferences(); // model
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
   */
  protected Control createContents(Composite parent) {
    Composite scrolledComposite = createScrolledComposite(parent);
    createContentsForCreatingGroup(scrolledComposite);
    createContentsForLoadingGroup(scrolledComposite);

    PlatformUI.getWorkbench().getHelpSystem().setHelp(scrolledComposite,
        IHelpContextIds.HTML_PREFWEBX_FILES_HELPID);

    setSize(scrolledComposite);
    loadPreferences();

    return scrolledComposite;
  }

  protected void createContentsForLoadingGroup(Composite parent) {
    Group group = createGroup(parent, 1);
    group.setText(HTMLUIMessages.HTMLFilesPreferencePage_0);

    fInputEncodingSettings = new WorkbenchDefaultEncodingSettings(group);
  }

  protected IContentType getContentType() {
    return Platform.getContentTypeManager().getContentType(ContentTypeIdForHTML.ContentTypeID_HTML);
  }

  protected void initializeValues() {
    super.initializeValues();
    initializeValuesForLoadingGroup();
  }

  protected void initializeValuesForLoadingGroup() {
    String encoding = getModelPreferences().getString(CommonEncodingPreferenceNames.INPUT_CODESET);

    fInputEncodingSettings.setIANATag(encoding);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
   */
  protected void performDefaults() {
    super.performDefaults();
    performDefaultsForLoadingGroup();
  }

  protected void performDefaultsForLoadingGroup() {
    String encoding = getModelPreferences().getDefaultString(
        CommonEncodingPreferenceNames.INPUT_CODESET);

    fInputEncodingSettings.setIANATag(encoding);
  }

  protected void storeValues() {
    super.storeValues();
    storeValuesForLoadingGroup();
  }

  protected void storeValuesForLoadingGroup() {
    getModelPreferences().setValue(CommonEncodingPreferenceNames.INPUT_CODESET,
        fInputEncodingSettings.getIANATag());
  }
}
