/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.style;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.wst.css.core.internal.parserz.CSSRegionContexts;
import org.eclipse.wst.css.ui.internal.CSSUIPlugin;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.ui.internal.provisional.style.AbstractLineStyleProvider;
import org.eclipse.wst.sse.ui.internal.provisional.style.LineStyleProvider;

import java.util.HashMap;
import java.util.Map;

/**
 */
public class LineStyleProviderForCSS extends AbstractLineStyleProvider implements LineStyleProvider {
  /** Contains region to style mapping */
  private Map fColorTypes;

  /**
   * LineStyleProviderForEmbeddedCSS constructor comment.
   */
  public LineStyleProviderForCSS() {
    super();
  }

  protected TextAttribute getAttributeFor(ITextRegion region) {
    if (region != null) {
      String type = region.getType();
      if (type != null) {
        return getAttributeFor(type);
      }
    }
    return (TextAttribute) getTextAttributes().get(IStyleConstantsCSS.NORMAL);
  }

  /**
   * Look up the TextAttribute for the given region context. Might return null for unusual text.
   * 
   * @param type
   * @return
   */
  protected TextAttribute getAttributeFor(String type) {
    return (TextAttribute) getTextAttributes().get(fColorTypes.get(type));
  }

  private void initAttributes() {
    if (fColorTypes == null) {
      fColorTypes = new HashMap();
    }
    fColorTypes.put(CSSRegionContexts.CSS_COMMENT, IStyleConstantsCSS.COMMENT);
    fColorTypes.put(CSSRegionContexts.CSS_CDO, IStyleConstantsCSS.COMMENT);
    fColorTypes.put(CSSRegionContexts.CSS_CDC, IStyleConstantsCSS.COMMENT);
    fColorTypes.put(CSSRegionContexts.CSS_S, IStyleConstantsCSS.NORMAL);

    fColorTypes.put(CSSRegionContexts.CSS_DELIMITER, IStyleConstantsCSS.SEMI_COLON);
    fColorTypes.put(CSSRegionContexts.CSS_LBRACE, IStyleConstantsCSS.CURLY_BRACE);
    fColorTypes.put(CSSRegionContexts.CSS_RBRACE, IStyleConstantsCSS.CURLY_BRACE);

    fColorTypes.put(CSSRegionContexts.CSS_IMPORT, IStyleConstantsCSS.ATMARK_RULE);
    fColorTypes.put(CSSRegionContexts.CSS_PAGE, IStyleConstantsCSS.ATMARK_RULE);
    fColorTypes.put(CSSRegionContexts.CSS_MEDIA, IStyleConstantsCSS.ATMARK_RULE);
    fColorTypes.put(CSSRegionContexts.CSS_FONT_FACE, IStyleConstantsCSS.ATMARK_RULE);
    fColorTypes.put(CSSRegionContexts.CSS_CHARSET, IStyleConstantsCSS.ATMARK_RULE);
    fColorTypes.put(CSSRegionContexts.CSS_ATKEYWORD, IStyleConstantsCSS.ATMARK_RULE);

    fColorTypes.put(CSSRegionContexts.CSS_STRING, IStyleConstantsCSS.STRING);
    fColorTypes.put(CSSRegionContexts.CSS_URI, IStyleConstantsCSS.URI);
    fColorTypes.put(CSSRegionContexts.CSS_MEDIUM, IStyleConstantsCSS.MEDIA);
    fColorTypes.put(CSSRegionContexts.CSS_MEDIA_SEPARATOR, IStyleConstantsCSS.MEDIA);

    fColorTypes.put(CSSRegionContexts.CSS_CHARSET_NAME, IStyleConstantsCSS.STRING);

    fColorTypes.put(CSSRegionContexts.CSS_PAGE_SELECTOR, IStyleConstantsCSS.MEDIA);

    fColorTypes.put(CSSRegionContexts.CSS_SELECTOR_ELEMENT_NAME, IStyleConstantsCSS.SELECTOR);
    fColorTypes.put(CSSRegionContexts.CSS_SELECTOR_UNIVERSAL, IStyleConstantsCSS.UNIVERSAL);

    fColorTypes.put(CSSRegionContexts.CSS_SELECTOR_PSEUDO, IStyleConstantsCSS.PSEUDO);
    fColorTypes.put(CSSRegionContexts.CSS_SELECTOR_CLASS, IStyleConstantsCSS.SELECTOR_CLASS);
    fColorTypes.put(CSSRegionContexts.CSS_SELECTOR_ID, IStyleConstantsCSS.ID);

    fColorTypes.put(CSSRegionContexts.CSS_SELECTOR_COMBINATOR, IStyleConstantsCSS.COMBINATOR);
    fColorTypes.put(CSSRegionContexts.CSS_SELECTOR_SEPARATOR, IStyleConstantsCSS.SELECTOR);

    /* Attribute selector */
    fColorTypes.put(CSSRegionContexts.CSS_SELECTOR_ATTRIBUTE_START,
        IStyleConstantsCSS.ATTRIBUTE_DELIM);
    fColorTypes.put(CSSRegionContexts.CSS_SELECTOR_ATTRIBUTE_END,
        IStyleConstantsCSS.ATTRIBUTE_DELIM);
    fColorTypes.put(CSSRegionContexts.CSS_SELECTOR_ATTRIBUTE_NAME,
        IStyleConstantsCSS.ATTRIBUTE_NAME);
    fColorTypes.put(CSSRegionContexts.CSS_SELECTOR_ATTRIBUTE_VALUE,
        IStyleConstantsCSS.ATTRIBUTE_VALUE);
    fColorTypes.put(CSSRegionContexts.CSS_SELECTOR_ATTRIBUTE_OPERATOR,
        IStyleConstantsCSS.ATTRIBUTE_OPERATOR);

    fColorTypes.put(CSSRegionContexts.CSS_DECLARATION_PROPERTY, IStyleConstantsCSS.PROPERTY_NAME);
    fColorTypes.put(CSSRegionContexts.CSS_DECLARATION_VALUE_IDENT,
        IStyleConstantsCSS.PROPERTY_VALUE);
    fColorTypes.put(CSSRegionContexts.CSS_DECLARATION_VALUE_DIMENSION,
        IStyleConstantsCSS.PROPERTY_VALUE);
    fColorTypes.put(CSSRegionContexts.CSS_DECLARATION_VALUE_PERCENTAGE,
        IStyleConstantsCSS.PROPERTY_VALUE);
    fColorTypes.put(CSSRegionContexts.CSS_DECLARATION_VALUE_NUMBER,
        IStyleConstantsCSS.PROPERTY_VALUE);
    fColorTypes.put(CSSRegionContexts.CSS_DECLARATION_VALUE_FUNCTION,
        IStyleConstantsCSS.PROPERTY_VALUE);
    fColorTypes.put(CSSRegionContexts.CSS_DECLARATION_VALUE_PARENTHESIS_CLOSE,
        IStyleConstantsCSS.PROPERTY_VALUE);
    fColorTypes.put(CSSRegionContexts.CSS_DECLARATION_VALUE_STRING,
        IStyleConstantsCSS.PROPERTY_VALUE);
    fColorTypes.put(CSSRegionContexts.CSS_DECLARATION_VALUE_URI, IStyleConstantsCSS.PROPERTY_VALUE);
    fColorTypes.put(CSSRegionContexts.CSS_DECLARATION_VALUE_HASH, IStyleConstantsCSS.PROPERTY_VALUE);
    fColorTypes.put(CSSRegionContexts.CSS_DECLARATION_VALUE_UNICODE_RANGE,
        IStyleConstantsCSS.PROPERTY_VALUE);
    fColorTypes.put(CSSRegionContexts.CSS_DECLARATION_VALUE_IMPORTANT,
        IStyleConstantsCSS.PROPERTY_VALUE);
    fColorTypes.put(CSSRegionContexts.CSS_DECLARATION_VALUE_OPERATOR,
        IStyleConstantsCSS.PROPERTY_VALUE);
    fColorTypes.put(CSSRegionContexts.CSS_DECLARATION_VALUE_S, IStyleConstantsCSS.PROPERTY_VALUE);
    fColorTypes.put(CSSRegionContexts.CSS_DECLARATION_SEPARATOR, IStyleConstantsCSS.COLON);
    fColorTypes.put(CSSRegionContexts.CSS_DECLARATION_DELIMITER, IStyleConstantsCSS.SEMI_COLON);

    fColorTypes.put(CSSRegionContexts.CSS_UNKNOWN, IStyleConstantsCSS.NORMAL);
  }

  protected void handlePropertyChange(PropertyChangeEvent event) {
    String styleKey = null;

    if (event != null) {
      String prefKey = event.getProperty();
      // check if preference changed is a style preference
      if (IStyleConstantsCSS.ATMARK_RULE.equals(prefKey)) {
        styleKey = IStyleConstantsCSS.ATMARK_RULE;
      } else if (IStyleConstantsCSS.COLON.equals(prefKey)) {
        styleKey = IStyleConstantsCSS.COLON;
      } else if (IStyleConstantsCSS.COMMENT.equals(prefKey)) {
        styleKey = IStyleConstantsCSS.COMMENT;
      } else if (IStyleConstantsCSS.CURLY_BRACE.equals(prefKey)) {
        styleKey = IStyleConstantsCSS.CURLY_BRACE;
      } else if (IStyleConstantsCSS.ERROR.equals(prefKey)) {
        styleKey = IStyleConstantsCSS.ERROR;
      } else if (IStyleConstantsCSS.MEDIA.equals(prefKey)) {
        styleKey = IStyleConstantsCSS.MEDIA;
      } else if (IStyleConstantsCSS.NORMAL.equals(prefKey)) {
        styleKey = IStyleConstantsCSS.NORMAL;
      } else if (IStyleConstantsCSS.ATTRIBUTE_DELIM.equals(prefKey)) {
        styleKey = IStyleConstantsCSS.ATTRIBUTE_DELIM;
      } else if (IStyleConstantsCSS.ATTRIBUTE_NAME.equals(prefKey)) {
        styleKey = IStyleConstantsCSS.ATTRIBUTE_NAME;
      } else if (IStyleConstantsCSS.ATTRIBUTE_OPERATOR.equals(prefKey)) {
        styleKey = IStyleConstantsCSS.ATTRIBUTE_OPERATOR;
      } else if (IStyleConstantsCSS.ATTRIBUTE_VALUE.equals(prefKey)) {
        styleKey = IStyleConstantsCSS.ATTRIBUTE_VALUE;
      } else if (IStyleConstantsCSS.COMBINATOR.equals(prefKey)) {
        styleKey = IStyleConstantsCSS.COMBINATOR;
      } else if (IStyleConstantsCSS.PROPERTY_NAME.equals(prefKey)) {
        styleKey = IStyleConstantsCSS.PROPERTY_NAME;
      } else if (IStyleConstantsCSS.PROPERTY_VALUE.equals(prefKey)) {
        styleKey = IStyleConstantsCSS.PROPERTY_VALUE;
      } else if (IStyleConstantsCSS.SELECTOR.equals(prefKey)) {
        styleKey = IStyleConstantsCSS.SELECTOR;
      } else if (IStyleConstantsCSS.UNIVERSAL.equals(prefKey)) {
        styleKey = IStyleConstantsCSS.UNIVERSAL;
      } else if (IStyleConstantsCSS.ID.equals(prefKey)) {
        styleKey = IStyleConstantsCSS.ID;
      } else if (IStyleConstantsCSS.PSEUDO.equals(prefKey)) {
        styleKey = IStyleConstantsCSS.PSEUDO;
      } else if (IStyleConstantsCSS.SELECTOR_CLASS.equals(prefKey)) {
        styleKey = IStyleConstantsCSS.SELECTOR_CLASS;
      } else if (IStyleConstantsCSS.SEMI_COLON.equals(prefKey)) {
        styleKey = IStyleConstantsCSS.SEMI_COLON;
      } else if (IStyleConstantsCSS.STRING.equals(prefKey)) {
        styleKey = IStyleConstantsCSS.STRING;
      } else if (IStyleConstantsCSS.URI.equals(prefKey)) {
        styleKey = IStyleConstantsCSS.URI;
      }
    } else {
      // this is around for old deprecated preferencesChanged() method
      // TODO remove when preferencesChanged() is removed
      loadColors();
      super.handlePropertyChange(event);
    }

    if (styleKey != null) {
      // overwrite style preference with new value
      addTextAttribute(styleKey);
      super.handlePropertyChange(event);
    }
  }

  public void release() {
    if (fColorTypes != null) {
      fColorTypes.clear();
      fColorTypes = null;
    }
    super.release();
  }

  public void loadColors() {
    initAttributes();

    addTextAttribute(IStyleConstantsCSS.ATMARK_RULE);
    addTextAttribute(IStyleConstantsCSS.COLON);
    addTextAttribute(IStyleConstantsCSS.COMMENT);
    addTextAttribute(IStyleConstantsCSS.CURLY_BRACE);
    addTextAttribute(IStyleConstantsCSS.ERROR);
    addTextAttribute(IStyleConstantsCSS.MEDIA);
    addTextAttribute(IStyleConstantsCSS.NORMAL);
    addTextAttribute(IStyleConstantsCSS.PROPERTY_NAME);
    addTextAttribute(IStyleConstantsCSS.PROPERTY_VALUE);
    addTextAttribute(IStyleConstantsCSS.SELECTOR);
    addTextAttribute(IStyleConstantsCSS.UNIVERSAL);
    addTextAttribute(IStyleConstantsCSS.ATTRIBUTE_DELIM);
    addTextAttribute(IStyleConstantsCSS.ATTRIBUTE_NAME);
    addTextAttribute(IStyleConstantsCSS.ATTRIBUTE_OPERATOR);
    addTextAttribute(IStyleConstantsCSS.ATTRIBUTE_VALUE);
    addTextAttribute(IStyleConstantsCSS.COMBINATOR);
    addTextAttribute(IStyleConstantsCSS.ID);
    addTextAttribute(IStyleConstantsCSS.SELECTOR_CLASS);
    addTextAttribute(IStyleConstantsCSS.PSEUDO);
    addTextAttribute(IStyleConstantsCSS.SEMI_COLON);
    addTextAttribute(IStyleConstantsCSS.STRING);
    addTextAttribute(IStyleConstantsCSS.URI);
  }

  protected IPreferenceStore getColorPreferences() {
    return CSSUIPlugin.getDefault().getPreferenceStore();
  }
}
