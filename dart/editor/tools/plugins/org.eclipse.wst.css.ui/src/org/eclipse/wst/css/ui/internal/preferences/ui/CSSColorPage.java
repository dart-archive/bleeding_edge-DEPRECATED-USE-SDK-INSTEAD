/*****************************************************************************
 * Copyright (c) 2004,2007 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 ****************************************************************************/
package org.eclipse.wst.css.ui.internal.preferences.ui;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.css.core.internal.parserz.CSSRegionContexts;
import org.eclipse.wst.css.core.internal.provisional.contenttype.ContentTypeIdForCSS;
import org.eclipse.wst.css.ui.internal.CSSUIMessages;
import org.eclipse.wst.css.ui.internal.CSSUIPlugin;
import org.eclipse.wst.css.ui.internal.editor.IHelpContextIds;
import org.eclipse.wst.css.ui.internal.style.IStyleConstantsCSS;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.ui.internal.preferences.OverlayPreferenceStore;
import org.eclipse.wst.sse.ui.internal.preferences.OverlayPreferenceStore.OverlayKey;
import org.eclipse.wst.sse.ui.internal.preferences.ui.AbstractColorPage;
import org.eclipse.wst.sse.ui.internal.preferences.ui.StyledTextColorPicker;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * @deprecated
 */
public class CSSColorPage extends AbstractColorPage {

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

  protected Control createContents(Composite parent) {
    Composite pageComponent = createComposite(parent, 1);
    ((GridData) pageComponent.getLayoutData()).horizontalAlignment = GridData.HORIZONTAL_ALIGN_FILL;

    super.createContents(pageComponent);

    // assigning one help for whole group
    PlatformUI.getWorkbench().getHelpSystem().setHelp(pageComponent,
        IHelpContextIds.CSS_PREFWEBX_STYLES_HELPID);

    return pageComponent;
  }

  protected Composite createColoringComposite(Composite parent) {
    Composite coloringComposite = super.createColoringComposite(parent);

    // assigning one help for whole group
    return coloringComposite;
  }

  /**
   * getSampleText method comment.
   */
  public String getSampleText() {
    return CSSUIMessages.PrefsLabel_ColorSample; //$NON-NLS-1$
  }

  /**
   * @param contextStyleMap java.util.Dictionary
   */
  protected void initContextStyleMap(Dictionary contextStyleMap) {
    contextStyleMap.put(CSSRegionContexts.CSS_COMMENT, IStyleConstantsCSS.COMMENT);
    contextStyleMap.put(CSSRegionContexts.CSS_CDO, IStyleConstantsCSS.COMMENT);
    contextStyleMap.put(CSSRegionContexts.CSS_CDC, IStyleConstantsCSS.COMMENT);
    contextStyleMap.put(CSSRegionContexts.CSS_S, IStyleConstantsCSS.NORMAL);

    contextStyleMap.put(CSSRegionContexts.CSS_DELIMITER, IStyleConstantsCSS.SEMI_COLON);
    contextStyleMap.put(CSSRegionContexts.CSS_LBRACE, IStyleConstantsCSS.CURLY_BRACE);
    contextStyleMap.put(CSSRegionContexts.CSS_RBRACE, IStyleConstantsCSS.CURLY_BRACE);

    contextStyleMap.put(CSSRegionContexts.CSS_IMPORT, IStyleConstantsCSS.ATMARK_RULE);
    contextStyleMap.put(CSSRegionContexts.CSS_PAGE, IStyleConstantsCSS.ATMARK_RULE);
    contextStyleMap.put(CSSRegionContexts.CSS_MEDIA, IStyleConstantsCSS.ATMARK_RULE);
    contextStyleMap.put(CSSRegionContexts.CSS_FONT_FACE, IStyleConstantsCSS.ATMARK_RULE);
    contextStyleMap.put(CSSRegionContexts.CSS_CHARSET, IStyleConstantsCSS.ATMARK_RULE);
    contextStyleMap.put(CSSRegionContexts.CSS_ATKEYWORD, IStyleConstantsCSS.ATMARK_RULE);

    contextStyleMap.put(CSSRegionContexts.CSS_STRING, IStyleConstantsCSS.STRING);
    contextStyleMap.put(CSSRegionContexts.CSS_URI, IStyleConstantsCSS.URI);
    contextStyleMap.put(CSSRegionContexts.CSS_MEDIUM, IStyleConstantsCSS.MEDIA);
    contextStyleMap.put(CSSRegionContexts.CSS_MEDIA_SEPARATOR, IStyleConstantsCSS.MEDIA);

    contextStyleMap.put(CSSRegionContexts.CSS_CHARSET_NAME, IStyleConstantsCSS.STRING);

    contextStyleMap.put(CSSRegionContexts.CSS_PAGE_SELECTOR, IStyleConstantsCSS.MEDIA);

    contextStyleMap.put(CSSRegionContexts.CSS_SELECTOR_ELEMENT_NAME, IStyleConstantsCSS.SELECTOR);
    contextStyleMap.put(CSSRegionContexts.CSS_SELECTOR_UNIVERSAL, IStyleConstantsCSS.SELECTOR);
    contextStyleMap.put(CSSRegionContexts.CSS_SELECTOR_PSEUDO, IStyleConstantsCSS.SELECTOR);
    contextStyleMap.put(CSSRegionContexts.CSS_SELECTOR_CLASS, IStyleConstantsCSS.SELECTOR);
    contextStyleMap.put(CSSRegionContexts.CSS_SELECTOR_ID, IStyleConstantsCSS.SELECTOR);

    contextStyleMap.put(CSSRegionContexts.CSS_SELECTOR_COMBINATOR, IStyleConstantsCSS.SELECTOR);
    contextStyleMap.put(CSSRegionContexts.CSS_SELECTOR_SEPARATOR, IStyleConstantsCSS.SELECTOR);
    contextStyleMap.put(CSSRegionContexts.CSS_SELECTOR_ATTRIBUTE_START, IStyleConstantsCSS.SELECTOR);
    contextStyleMap.put(CSSRegionContexts.CSS_SELECTOR_ATTRIBUTE_END, IStyleConstantsCSS.SELECTOR);
    contextStyleMap.put(CSSRegionContexts.CSS_SELECTOR_ATTRIBUTE_NAME, IStyleConstantsCSS.SELECTOR);
    contextStyleMap.put(CSSRegionContexts.CSS_SELECTOR_ATTRIBUTE_VALUE, IStyleConstantsCSS.SELECTOR);
    contextStyleMap.put(CSSRegionContexts.CSS_SELECTOR_ATTRIBUTE_OPERATOR,
        IStyleConstantsCSS.SELECTOR);

    contextStyleMap.put(CSSRegionContexts.CSS_DECLARATION_PROPERTY,
        IStyleConstantsCSS.PROPERTY_NAME);
    contextStyleMap.put(CSSRegionContexts.CSS_DECLARATION_VALUE_IDENT,
        IStyleConstantsCSS.PROPERTY_VALUE);
    contextStyleMap.put(CSSRegionContexts.CSS_DECLARATION_VALUE_DIMENSION,
        IStyleConstantsCSS.PROPERTY_VALUE);
    contextStyleMap.put(CSSRegionContexts.CSS_DECLARATION_VALUE_PERCENTAGE,
        IStyleConstantsCSS.PROPERTY_VALUE);
    contextStyleMap.put(CSSRegionContexts.CSS_DECLARATION_VALUE_NUMBER,
        IStyleConstantsCSS.PROPERTY_VALUE);
    contextStyleMap.put(CSSRegionContexts.CSS_DECLARATION_VALUE_FUNCTION,
        IStyleConstantsCSS.PROPERTY_VALUE);
    contextStyleMap.put(CSSRegionContexts.CSS_DECLARATION_VALUE_PARENTHESIS_CLOSE,
        IStyleConstantsCSS.PROPERTY_VALUE);
    contextStyleMap.put(CSSRegionContexts.CSS_DECLARATION_VALUE_STRING,
        IStyleConstantsCSS.PROPERTY_VALUE);
    contextStyleMap.put(CSSRegionContexts.CSS_DECLARATION_VALUE_URI,
        IStyleConstantsCSS.PROPERTY_VALUE);
    contextStyleMap.put(CSSRegionContexts.CSS_DECLARATION_VALUE_HASH,
        IStyleConstantsCSS.PROPERTY_VALUE);
    contextStyleMap.put(CSSRegionContexts.CSS_DECLARATION_VALUE_UNICODE_RANGE,
        IStyleConstantsCSS.PROPERTY_VALUE);
    contextStyleMap.put(CSSRegionContexts.CSS_DECLARATION_VALUE_IMPORTANT,
        IStyleConstantsCSS.PROPERTY_VALUE);
    contextStyleMap.put(CSSRegionContexts.CSS_DECLARATION_VALUE_OPERATOR,
        IStyleConstantsCSS.PROPERTY_VALUE);
    contextStyleMap.put(CSSRegionContexts.CSS_DECLARATION_VALUE_S,
        IStyleConstantsCSS.PROPERTY_VALUE);
    contextStyleMap.put(CSSRegionContexts.CSS_DECLARATION_SEPARATOR, IStyleConstantsCSS.COLON);
    contextStyleMap.put(CSSRegionContexts.CSS_DECLARATION_DELIMITER, IStyleConstantsCSS.SEMI_COLON);

    contextStyleMap.put(CSSRegionContexts.CSS_UNKNOWN, IStyleConstantsCSS.ERROR);
  }

  /**
   * @param descriptions java.util.Dictionary
   */
  protected void initDescriptions(Dictionary descriptions) {
    // create descriptions for hilighting types
    descriptions.put(IStyleConstantsCSS.NORMAL, CSSUIMessages.PrefsLabel_ColorNormal);//$NON-NLS-1$
    descriptions.put(IStyleConstantsCSS.ATMARK_RULE, CSSUIMessages.PrefsLabel_ColorAtmarkRule);//$NON-NLS-1$
    descriptions.put(IStyleConstantsCSS.SELECTOR, CSSUIMessages.PrefsLabel_ColorSelector);//$NON-NLS-1$
    descriptions.put(IStyleConstantsCSS.MEDIA, CSSUIMessages.PrefsLabel_ColorMedia);//$NON-NLS-1$
    descriptions.put(IStyleConstantsCSS.COMMENT, CSSUIMessages.PrefsLabel_ColorComment);//$NON-NLS-1$
    descriptions.put(IStyleConstantsCSS.PROPERTY_NAME, CSSUIMessages.PrefsLabel_ColorPropertyName);//$NON-NLS-1$
    descriptions.put(IStyleConstantsCSS.PROPERTY_VALUE, CSSUIMessages.PrefsLabel_ColorPropertyValue);//$NON-NLS-1$
    descriptions.put(IStyleConstantsCSS.URI, CSSUIMessages.PrefsLabel_ColorUri);//$NON-NLS-1$
    descriptions.put(IStyleConstantsCSS.STRING, CSSUIMessages.PrefsLabel_ColorString);//$NON-NLS-1$
    descriptions.put(IStyleConstantsCSS.COLON, CSSUIMessages.PrefsLabel_ColorColon);//$NON-NLS-1$
    descriptions.put(IStyleConstantsCSS.SEMI_COLON, CSSUIMessages.PrefsLabel_ColorSemiColon);//$NON-NLS-1$
    descriptions.put(IStyleConstantsCSS.CURLY_BRACE, CSSUIMessages.PrefsLabel_ColorCurlyBrace);//$NON-NLS-1$
    descriptions.put(IStyleConstantsCSS.ERROR, CSSUIMessages.PrefsLabel_ColorError);//$NON-NLS-1$
  }

  /**
   * @param list java.util.ArrayList
   */
  protected void initStyleList(ArrayList list) {
    list.add(IStyleConstantsCSS.NORMAL);
    list.add(IStyleConstantsCSS.ATMARK_RULE);
    list.add(IStyleConstantsCSS.SELECTOR);
    list.add(IStyleConstantsCSS.MEDIA);
    list.add(IStyleConstantsCSS.COMMENT);
    list.add(IStyleConstantsCSS.PROPERTY_NAME);
    list.add(IStyleConstantsCSS.PROPERTY_VALUE);
    list.add(IStyleConstantsCSS.URI);
    list.add(IStyleConstantsCSS.STRING);
    list.add(IStyleConstantsCSS.COLON);
    list.add(IStyleConstantsCSS.SEMI_COLON);
    list.add(IStyleConstantsCSS.CURLY_BRACE);
    list.add(IStyleConstantsCSS.ERROR);
  }

  /**
   * setupPicker method comment.
   */
  protected void setupPicker(StyledTextColorPicker picker) {
    IModelManager mmanager = StructuredModelManager.getModelManager();
    picker.setParser(mmanager.createStructuredDocumentFor(ContentTypeIdForCSS.ContentTypeID_CSS).getParser());

    Dictionary descriptions = new Hashtable();
    initDescriptions(descriptions);

    Dictionary contextStyleMap = new Hashtable();
    initContextStyleMap(contextStyleMap);

    ArrayList styleList = new ArrayList();
    initStyleList(styleList);

    picker.setContextStyleMap(contextStyleMap);
    picker.setDescriptions(descriptions);
    picker.setStyleList(styleList);
  }

  protected IPreferenceStore doGetPreferenceStore() {
    return CSSUIPlugin.getDefault().getPreferenceStore();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.sse.ui.preferences.ui.AbstractColorPage#savePreferences()
   */
  protected void savePreferences() {
    CSSUIPlugin.getDefault().savePluginPreferences();
  }
}
