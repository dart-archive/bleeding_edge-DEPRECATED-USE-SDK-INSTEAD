/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.css.ui.internal.CSSUIPlugin;
import org.eclipse.wst.css.ui.internal.style.IStyleConstantsCSS;
import org.eclipse.wst.sse.ui.internal.preferences.ui.ColorHelper;

/**
 * Sets default values for CSS UI preferences
 */
public class CSSUIPreferenceInitializer extends AbstractPreferenceInitializer {

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences
   * ()
   */
  public void initializeDefaultPreferences() {
    IPreferenceStore store = CSSUIPlugin.getDefault().getPreferenceStore();
    ColorRegistry registry = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();

    // CSS Style Preferences
    String NOBACKGROUNDBOLD = " | null | false"; //$NON-NLS-1$
    String JUSTITALIC = " | null | false | true"; //$NON-NLS-1$
    String JUSTBOLD = " | null | true";
    String styleValue = "null" + NOBACKGROUNDBOLD; //$NON-NLS-1$
    store.setDefault(IStyleConstantsCSS.NORMAL, styleValue);

    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsCSS.ATMARK_RULE, 63, 127, 127)
        + NOBACKGROUNDBOLD;
    store.setDefault(IStyleConstantsCSS.ATMARK_RULE, styleValue);
    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsCSS.SELECTOR, 63, 127, 127)
        + JUSTBOLD;
    store.setDefault(IStyleConstantsCSS.SELECTOR, styleValue);
    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsCSS.UNIVERSAL, 63, 127, 127)
        + NOBACKGROUNDBOLD;
    store.setDefault(IStyleConstantsCSS.UNIVERSAL, styleValue);
    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsCSS.COMBINATOR, 63, 127, 127)
        + NOBACKGROUNDBOLD;
    store.setDefault(IStyleConstantsCSS.COMBINATOR, styleValue);

    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsCSS.SELECTOR_CLASS, 63, 127,
        127) + JUSTITALIC;
    store.setDefault(IStyleConstantsCSS.SELECTOR_CLASS, styleValue);
    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsCSS.ID, 63, 127, 127)
        + JUSTITALIC;
    store.setDefault(IStyleConstantsCSS.ID, styleValue);
    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsCSS.PSEUDO, 63, 127, 127)
        + JUSTITALIC;
    store.setDefault(IStyleConstantsCSS.PSEUDO, styleValue);

    /* Attribute selector */
    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsCSS.ATTRIBUTE_DELIM, 63, 127,
        127) + NOBACKGROUNDBOLD;
    store.setDefault(IStyleConstantsCSS.ATTRIBUTE_DELIM, styleValue);
    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsCSS.ATTRIBUTE_NAME, 63, 127,
        127) + NOBACKGROUNDBOLD;
    store.setDefault(IStyleConstantsCSS.ATTRIBUTE_NAME, styleValue);
    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsCSS.ATTRIBUTE_OPERATOR, 63,
        127, 127) + NOBACKGROUNDBOLD;
    store.setDefault(IStyleConstantsCSS.ATTRIBUTE_OPERATOR, styleValue);
    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsCSS.ATTRIBUTE_VALUE, 63, 127,
        127) + NOBACKGROUNDBOLD;
    store.setDefault(IStyleConstantsCSS.ATTRIBUTE_VALUE, styleValue);

    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsCSS.MEDIA, 42, 0, 225)
        + NOBACKGROUNDBOLD;
    store.setDefault(IStyleConstantsCSS.MEDIA, styleValue);

    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsCSS.COMMENT, 63, 95, 191)
        + NOBACKGROUNDBOLD;
    store.setDefault(IStyleConstantsCSS.COMMENT, styleValue);

    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsCSS.PROPERTY_NAME, 127, 0, 127)
        + NOBACKGROUNDBOLD;
    store.setDefault(IStyleConstantsCSS.PROPERTY_NAME, styleValue);

    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsCSS.PROPERTY_VALUE, 42, 0, 225)
        + JUSTITALIC;
    store.setDefault(IStyleConstantsCSS.PROPERTY_VALUE, styleValue);
    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsCSS.URI, 42, 0, 225)
        + JUSTITALIC;
    store.setDefault(IStyleConstantsCSS.URI, styleValue);
    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsCSS.STRING, 42, 0, 225)
        + JUSTITALIC;
    store.setDefault(IStyleConstantsCSS.STRING, styleValue);

    styleValue = "null" + NOBACKGROUNDBOLD; //$NON-NLS-1$
    store.setDefault(IStyleConstantsCSS.COLON, styleValue);
    store.setDefault(IStyleConstantsCSS.SEMI_COLON, styleValue);
    store.setDefault(IStyleConstantsCSS.CURLY_BRACE, styleValue);

    styleValue = ColorHelper.findRGBString(registry, IStyleConstantsCSS.ERROR, 191, 63, 63)
        + NOBACKGROUNDBOLD;
    store.setDefault(IStyleConstantsCSS.ERROR, styleValue);

    // set default new css file template to use in new file wizard
    /*
     * Need to find template name that goes with default template id (name may change for different
     * language)
     */
    store.setDefault(CSSUIPreferenceNames.NEW_FILE_TEMPLATE_ID,
        "org.eclipse.wst.css.ui.internal.templates.newcss"); //$NON-NLS-1$

    // Defaults for Content Assist preference page
    store.setDefault(CSSUIPreferenceNames.CONTENT_ASSIST_DO_NOT_DISPLAY_ON_DEFAULT_PAGE, "");
    store.setDefault(CSSUIPreferenceNames.CONTENT_ASSIST_DO_NOT_DISPLAY_ON_OWN_PAGE, "");
    store.setDefault(CSSUIPreferenceNames.CONTENT_ASSIST_DEFAULT_PAGE_SORT_ORDER,
        "org.eclipse.wst.css.ui.proposalCategory.css\0"
            + "org.eclipse.wst.css.ui.proposalCategory.cssTemplates");
    store.setDefault(CSSUIPreferenceNames.CONTENT_ASSIST_OWN_PAGE_SORT_ORDER,
        "org.eclipse.wst.css.ui.proposalCategory.cssTemplates\0"
            + "org.eclipse.wst.css.ui.proposalCategory.css");

    store.setDefault(CSSUIPreferenceNames.INSERT_SINGLE_SUGGESTION, true);
  }

}
