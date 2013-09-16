/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Benjamin Muskalla, b.muskalla@gmx.net - [158660] character entities should have
 * their own syntax highlighting preference
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.ui.internal.preferences.ui.ColorHelper;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;
import org.eclipse.wst.xml.ui.internal.style.IStyleConstantsXML;

/**
 * Sets default values for XML UI preferences
 */
public class XMLUIPreferenceInitializer extends AbstractPreferenceInitializer {

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences
   * ()
   */
  public void initializeDefaultPreferences() {
    IPreferenceStore store = XMLUIPlugin.getDefault().getPreferenceStore();
    ColorRegistry registry = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();

    store.setDefault(XMLUIPreferenceNames.INSERT_SINGLE_SUGGESTION, true);
    store.setDefault(XMLUIPreferenceNames.AUTO_PROPOSE, true);
    store.setDefault(XMLUIPreferenceNames.AUTO_PROPOSE_CODE, "<=:"); //$NON-NLS-1$
    store.setDefault(XMLUIPreferenceNames.AUTO_PROPOSE_DELAY, 500);
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=140946
    store.setDefault(XMLUIPreferenceNames.SUGGESTION_STRATEGY,
        XMLUIPreferenceNames.SUGGESTION_STRATEGY_VALUE_STRICT);
    store.setDefault(XMLUIPreferenceNames.USE_INFERRED_GRAMMAR, true);

    // XML Style Preferences
    String NOBACKGROUNDBOLD = " | null | false"; //$NON-NLS-1$
    String JUSTITALIC = " | null | false | true"; //$NON-NLS-1$
    String styleValue = ColorHelper.findRGBString(registry, IStyleConstantsXML.TAG_ATTRIBUTE_NAME,
        127, 0, 127) + NOBACKGROUNDBOLD;
    store.setDefault(IStyleConstantsXML.TAG_ATTRIBUTE_NAME, styleValue);

    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsXML.TAG_ATTRIBUTE_VALUE, 42, 0,
        255) + JUSTITALIC;
    store.setDefault(IStyleConstantsXML.TAG_ATTRIBUTE_VALUE, styleValue);

    styleValue = "null" + NOBACKGROUNDBOLD; //$NON-NLS-1$
    store.setDefault(IStyleConstantsXML.TAG_ATTRIBUTE_EQUALS, styleValue); // specified
    // value is black; leaving as widget default

    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsXML.COMMENT_BORDER, 63, 95, 191)
        + NOBACKGROUNDBOLD;
    store.setDefault(IStyleConstantsXML.COMMENT_BORDER, styleValue);
    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsXML.COMMENT_TEXT, 63, 95, 191)
        + NOBACKGROUNDBOLD;
    store.setDefault(IStyleConstantsXML.COMMENT_TEXT, styleValue);

    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsXML.DECL_BORDER, 0, 128, 128)
        + NOBACKGROUNDBOLD;
    store.setDefault(IStyleConstantsXML.DECL_BORDER, styleValue);

    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsXML.DOCTYPE_NAME, 0, 0, 128)
        + NOBACKGROUNDBOLD;
    store.setDefault(IStyleConstantsXML.DOCTYPE_NAME, styleValue);
    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsXML.DOCTYPE_EXTERNAL_ID_PUBREF,
        0, 0, 128) + NOBACKGROUNDBOLD;
    store.setDefault(IStyleConstantsXML.DOCTYPE_EXTERNAL_ID_PUBREF, styleValue);

    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsXML.DOCTYPE_EXTERNAL_ID, 128,
        128, 128) + NOBACKGROUNDBOLD;
    store.setDefault(IStyleConstantsXML.DOCTYPE_EXTERNAL_ID, styleValue);

    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsXML.DOCTYPE_EXTERNAL_ID_SYSREF,
        63, 127, 95) + NOBACKGROUNDBOLD;
    store.setDefault(IStyleConstantsXML.DOCTYPE_EXTERNAL_ID_SYSREF, styleValue);

    styleValue = "null" + NOBACKGROUNDBOLD; //$NON-NLS-1$
    store.setDefault(IStyleConstantsXML.XML_CONTENT, styleValue); // specified
    // value is black; leaving as widget default

    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsXML.TAG_BORDER, 0, 128, 128)
        + NOBACKGROUNDBOLD;
    store.setDefault(IStyleConstantsXML.TAG_BORDER, styleValue);

    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsXML.TAG_NAME, 63, 127, 127)
        + NOBACKGROUNDBOLD;
    store.setDefault(IStyleConstantsXML.TAG_NAME, styleValue);

    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsXML.PI_BORDER, 0, 128, 128)
        + NOBACKGROUNDBOLD;
    store.setDefault(IStyleConstantsXML.PI_BORDER, styleValue);

    styleValue = "null" + NOBACKGROUNDBOLD; //$NON-NLS-1$
    store.setDefault(IStyleConstantsXML.PI_CONTENT, styleValue); // specified
    // value is black; leaving as widget default

    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsXML.CDATA_BORDER, 0, 128, 128)
        + NOBACKGROUNDBOLD;
    store.setDefault(IStyleConstantsXML.CDATA_BORDER, styleValue);

    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsXML.CDATA_TEXT, 0, 0, 0)
        + NOBACKGROUNDBOLD;
    store.setDefault(IStyleConstantsXML.CDATA_TEXT, styleValue);

    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsXML.ENTITY_REFERENCE, 42, 0,
        255) + NOBACKGROUNDBOLD;
    store.setDefault(IStyleConstantsXML.ENTITY_REFERENCE, styleValue);

    // set default new xml file template to use in new file wizard
    /*
     * Need to find template name that goes with default template id (name may change for different
     * language)
     */
    store.setDefault(XMLUIPreferenceNames.NEW_FILE_TEMPLATE_ID,
        "org.eclipse.wst.xml.ui.internal.templates.xmldeclaration"); //$NON-NLS-1$

    // Defaults for the Typing preference page
    store.setDefault(XMLUIPreferenceNames.TYPING_COMPLETE_COMMENTS, true);
    store.setDefault(XMLUIPreferenceNames.TYPING_COMPLETE_END_TAGS, true);
    store.setDefault(XMLUIPreferenceNames.TYPING_COMPLETE_ELEMENTS, true);
    store.setDefault(XMLUIPreferenceNames.TYPING_REMOVE_END_TAGS, true);
    store.setDefault(XMLUIPreferenceNames.TYPING_CLOSE_STRINGS, true);
    store.setDefault(XMLUIPreferenceNames.TYPING_CLOSE_BRACKETS, true);

    // Defaults for Content Assist preference page
    store.setDefault(XMLUIPreferenceNames.CONTENT_ASSIST_DO_NOT_DISPLAY_ON_DEFAULT_PAGE, "");
    store.setDefault(XMLUIPreferenceNames.CONTENT_ASSIST_DO_NOT_DISPLAY_ON_OWN_PAGE, "");
    store.setDefault(XMLUIPreferenceNames.CONTENT_ASSIST_DEFAULT_PAGE_SORT_ORDER,
        "org.eclipse.wst.xml.ui.proposalCategory.xmlTags\0"
            + "org.eclipse.wst.xml.ui.proposalCategory.xmlTemplates");
    store.setDefault(XMLUIPreferenceNames.CONTENT_ASSIST_OWN_PAGE_SORT_ORDER,
        "org.eclipse.wst.xml.ui.proposalCategory.xmlTemplates\0"
            + "org.eclipse.wst.xml.ui.proposalCategory.xmlTags");
  }

}
