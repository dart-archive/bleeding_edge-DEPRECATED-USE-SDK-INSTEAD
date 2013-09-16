/*******************************************************************************
 * Copyright (c) 2004,2007 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal.preferences.ui;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.html.core.internal.provisional.contenttype.ContentTypeIdForHTML;
import org.eclipse.wst.html.ui.internal.HTMLUIMessages;
import org.eclipse.wst.html.ui.internal.HTMLUIPlugin;
import org.eclipse.wst.html.ui.internal.editor.IHelpContextIds;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.ui.internal.preferences.OverlayPreferenceStore;
import org.eclipse.wst.sse.ui.internal.preferences.OverlayPreferenceStore.OverlayKey;
import org.eclipse.wst.sse.ui.internal.preferences.ui.StyledTextColorPicker;
import org.eclipse.wst.xml.ui.internal.preferences.XMLColorPage;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * @deprecated
 */
public class HTMLColorPage extends XMLColorPage {

  /**
   * Set up all the style preference keys in the overlay store
   */
  protected OverlayKey[] createOverlayStoreKeys() {
    ArrayList overlayKeys = new ArrayList();

    ArrayList styleList = new ArrayList();
    initStyleList(styleList);
    Iterator i = styleList.iterator();
    while (i.hasNext()) {
      overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING,
          (String) i.next()));
    }

    OverlayPreferenceStore.OverlayKey[] keys = new OverlayPreferenceStore.OverlayKey[overlayKeys.size()];
    overlayKeys.toArray(keys);
    return keys;
  }

  public String getSampleText() {
    return HTMLUIMessages.Sample_HTML_doc;

  }

  protected void initContextStyleMap(Dictionary contextStyleMap) {

    initCommonContextStyleMap(contextStyleMap);
    initDocTypeContextStyleMap(contextStyleMap);
    // FIXME: these were "brute forced" commented out when moving XMLJSPRegionContexts
    // effect is unknown, but thought just to effect preference page
    //contextStyleMap.put(XMLJSPRegionContexts.JSP_DIRECTIVE_NAME, IStyleConstantsXML.TAG_NAME);
    //contextStyleMap.put(XMLJSPRegionContexts.JSP_COMMENT_OPEN, IStyleConstantsXML.COMMENT_BORDER);
    //contextStyleMap.put(XMLJSPRegionContexts.JSP_COMMENT_TEXT, IStyleConstantsXML.COMMENT_TEXT);
    //contextStyleMap.put(XMLJSPRegionContexts.JSP_COMMENT_CLOSE, IStyleConstantsXML.COMMENT_BORDER);
  }

  protected void initDescriptions(Dictionary descriptions) {

    initCommonDescriptions(descriptions);
    initDocTypeDescriptions(descriptions);
  }

  protected void initStyleList(ArrayList list) {
    initCommonStyleList(list);
    initDocTypeStyleList(list);
    //	list.add(HTMLColorManager.SCRIPT_AREA_BORDER);

  }

  protected void setupPicker(StyledTextColorPicker picker) {
    IModelManager mmanager = StructuredModelManager.getModelManager();
    picker.setParser(mmanager.createStructuredDocumentFor(ContentTypeIdForHTML.ContentTypeID_HTML).getParser());

    // create descriptions for hilighting types
    Dictionary descriptions = new Hashtable();
    initDescriptions(descriptions);

    // map region types to hilighting types
    Dictionary contextStyleMap = new Hashtable();
    initContextStyleMap(contextStyleMap);

    ArrayList styleList = new ArrayList();
    initStyleList(styleList);

    picker.setContextStyleMap(contextStyleMap);
    picker.setDescriptions(descriptions);
    picker.setStyleList(styleList);

    //	updatePickerFont(picker);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
   */
  protected Control createContents(Composite parent) {
    Control c = super.createContents(parent);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(c,
        IHelpContextIds.HTML_PREFWEBX_STYLES_HELPID);
    return c;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.preference.PreferencePage#doGetPreferenceStore()
   */
  protected IPreferenceStore doGetPreferenceStore() {
    return HTMLUIPlugin.getDefault().getPreferenceStore();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.sse.ui.preferences.ui.AbstractColorPage#savePreferences()
   */
  protected void savePreferences() {
    HTMLUIPlugin.getDefault().savePluginPreferences();
  }
}
