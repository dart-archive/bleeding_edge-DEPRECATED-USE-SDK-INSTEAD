/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.image;

import org.eclipse.wst.css.core.internal.metamodel.CSSMMProperty;
import org.eclipse.wst.css.core.internal.metamodel.CSSMetaModel;
import org.eclipse.wst.css.core.internal.metamodel.util.CSSMetaModelFinder;
import org.eclipse.wst.css.core.internal.metamodel.util.CSSMetaModelUtil;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSDocument;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSModel;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSNode;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSPrimitiveValue;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSSelector;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSSelectorItem;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSSelectorList;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSSimpleSelector;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSStyleDeclItem;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSStyleRule;
import org.w3c.dom.css.CSSCharsetRule;
import org.w3c.dom.css.CSSFontFaceRule;
import org.w3c.dom.css.CSSImportRule;
import org.w3c.dom.css.CSSMediaRule;
import org.w3c.dom.css.CSSPageRule;
import org.w3c.dom.css.CSSPrimitiveValue;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.css.CSSStyleRule;
import org.w3c.dom.css.CSSValue;

import java.util.HashMap;
import java.util.Map;

/**
 */
public class CSSImageType {

  private final String fName;

  private CSSImageType(String name) {
    this.fName = name;
  }

  public String toString() {
    return fName;
  }

  public static final CSSImageType STYLESHEET = new CSSImageType("STYLESHEET"); //$NON-NLS-1$

  public static final CSSImageType RULE_CHARSET = new CSSImageType("RULE_CHARSET"); //$NON-NLS-1$
  public static final CSSImageType RULE_FONTFACE = new CSSImageType("RULE_FONTFACE"); //$NON-NLS-1$
  public static final CSSImageType RULE_IMPORT = new CSSImageType("RULE_IMPORT"); //$NON-NLS-1$
  public static final CSSImageType RULE_MEDIA = new CSSImageType("RULE_MEDIA"); //$NON-NLS-1$
  public static final CSSImageType RULE_PAGE = new CSSImageType("RULE_PAGE"); //$NON-NLS-1$
  public static final CSSImageType RULE_STYLE = new CSSImageType("RULE_STYLE"); //$NON-NLS-1$
  public static final CSSImageType RULE_UNKNOWN = new CSSImageType("RULE_UNKNOWN"); //$NON-NLS-1$

  public static final CSSImageType SELECTOR_CLASS = new CSSImageType("SELECTOR_CLASS"); //$NON-NLS-1$
  public static final CSSImageType SELECTOR_ID = new CSSImageType("SELECTOR_ID"); //$NON-NLS-1$
  public static final CSSImageType SELECTOR_DEFAULT = new CSSImageType("SELECTOR_DEFAULT"); //$NON-NLS-1$
  public static final CSSImageType SELECTOR_PSEUDO = new CSSImageType("SELECTOR_PSEUDO"); //$NON-NLS-1$
  public static final CSSImageType SELECTOR_TAG = new CSSImageType("SELECTOR_TAG"); //$NON-NLS-1$
  public static final CSSImageType SELECTOR_LINK = new CSSImageType("SELECTOR_LINK"); //$NON-NLS-1$

  public static final CSSImageType VALUE_FUNCTION = new CSSImageType("VALUE_FUNCTION"); //$NON-NLS-1$
  public static final CSSImageType VALUE_NUMBER = new CSSImageType("VALUE_NUMBER"); //$NON-NLS-1$
  public static final CSSImageType VALUE_STRING = new CSSImageType("VALUE_STRING"); //$NON-NLS-1$

  public static final CSSImageType CATEGORY_AURAL = new CSSImageType("CATEGORY_AURAL"); //$NON-NLS-1$
  public static final CSSImageType CATEGORY_BOX = new CSSImageType("CATEGORY_BOX"); //$NON-NLS-1$
  public static final CSSImageType CATEGORY_COLORANDBACKGROUND = new CSSImageType(
      "CATEGORY_COLORANDBACKGROUND"); //$NON-NLS-1$
  public static final CSSImageType CATEGORY_CONTENT = new CSSImageType("CATEGORY_CONTENT"); //$NON-NLS-1$
  public static final CSSImageType CATEGORY_FONT = new CSSImageType("CATEGORY_FONT"); //$NON-NLS-1$
  public static final CSSImageType CATEGORY_PAGE = new CSSImageType("CATEGORY_PAGE"); //$NON-NLS-1$
  public static final CSSImageType CATEGORY_TABLES = new CSSImageType("CATEGORY_TABLES"); //$NON-NLS-1$
  public static final CSSImageType CATEGORY_TEXT = new CSSImageType("CATEGORY_TEXT"); //$NON-NLS-1$
  public static final CSSImageType CATEGORY_UI = new CSSImageType("CATEGORY_UI"); //$NON-NLS-1$
  public static final CSSImageType CATEGORY_VISUAL = new CSSImageType("CATEGORY_VISUAL"); //$NON-NLS-1$
  public static final CSSImageType CATEGORY_DEFAULT = new CSSImageType("CATEGORY_DEFAULT"); //$NON-NLS-1$

  /**
   * by node
   */
  public static CSSImageType getImageType(ICSSNode node) {
    CSSImageType imageType = null;
    if (node instanceof CSSCharsetRule) {
      imageType = RULE_CHARSET;
    } else if (node instanceof CSSFontFaceRule) {
      imageType = RULE_FONTFACE;
    } else if (node instanceof CSSImportRule) {
      imageType = RULE_IMPORT;
    } else if (node instanceof CSSMediaRule) {
      imageType = RULE_MEDIA;
    } else if (node instanceof CSSPageRule) {
      imageType = RULE_PAGE;
    } else if (node instanceof CSSStyleRule) {
      imageType = getImageType(((ICSSStyleRule) node).getSelectors());
    } else if (node instanceof CSSStyleDeclaration) {
      ICSSNode parent = node.getParentNode();
      if (parent != null) {
        return getImageType(parent);
      }
    } else if (node instanceof ICSSStyleDeclItem) {
      String name = ((ICSSStyleDeclItem) node).getPropertyName();
      ICSSDocument doc = node.getOwnerDocument();
      ICSSModel model = (doc != null) ? doc.getModel() : null;
      CSSMetaModelFinder finder = CSSMetaModelFinder.getInstance();
      CSSMetaModel metaModel = finder.findMetaModelFor(model);
      // is font-face rule ?
      ICSSNode parent = node.getParentNode();
      if (parent != null) {
        parent = parent.getParentNode();
        if (parent instanceof CSSFontFaceRule) {
          imageType = CSSImageType.CATEGORY_FONT;
        }
      }
      if (imageType == null) {
        CSSMMProperty prop = new CSSMetaModelUtil(metaModel).getProperty(name);
        if (prop != null) {
          String category = prop.getAttribute("category"); //$NON-NLS-1$
          imageType = getImageType(category);
        }
        if (imageType == null) {
          imageType = CSSImageType.CATEGORY_DEFAULT;
        }
      }
    } else if (node instanceof CSSValue) {
      switch (((CSSValue) node).getCssValueType()) {
        case CSSPrimitiveValue.CSS_NUMBER:
        case CSSPrimitiveValue.CSS_PERCENTAGE:
        case CSSPrimitiveValue.CSS_EMS:
        case CSSPrimitiveValue.CSS_EXS:
        case CSSPrimitiveValue.CSS_PX:
        case CSSPrimitiveValue.CSS_CM:
        case CSSPrimitiveValue.CSS_MM:
        case CSSPrimitiveValue.CSS_IN:
        case CSSPrimitiveValue.CSS_PT:
        case CSSPrimitiveValue.CSS_PC:
        case CSSPrimitiveValue.CSS_DEG:
        case CSSPrimitiveValue.CSS_RAD:
        case CSSPrimitiveValue.CSS_GRAD:
        case CSSPrimitiveValue.CSS_MS:
        case CSSPrimitiveValue.CSS_S:
        case CSSPrimitiveValue.CSS_HZ:
        case CSSPrimitiveValue.CSS_KHZ:
        case CSSPrimitiveValue.CSS_DIMENSION:
        case ICSSPrimitiveValue.CSS_INTEGER:
        case ICSSPrimitiveValue.CSS_HASH:
          imageType = VALUE_NUMBER;
          break;
        case CSSPrimitiveValue.CSS_ATTR:
        case CSSPrimitiveValue.CSS_COUNTER:
        case CSSPrimitiveValue.CSS_RECT:
        case CSSPrimitiveValue.CSS_RGBCOLOR:
        case CSSPrimitiveValue.CSS_URI:
        case ICSSPrimitiveValue.CSS_FORMAT:
        case ICSSPrimitiveValue.CSS_LOCAL:
          imageType = VALUE_FUNCTION;
          break;
        default:
          imageType = VALUE_STRING;
          break;
      }
    }
    return imageType;
  }

  public static CSSImageType getImageType(ICSSSelectorList selectorList) {
    if (selectorList == null || selectorList.getLength() == 0) {
      return SELECTOR_DEFAULT;
    }
    CSSImageType imageType = null;
    int nSelectors = selectorList.getLength();
    for (int i = 0; i < nSelectors; i++) {
      CSSImageType candidate = getImageType(selectorList.getSelector(i));
      if (imageType == null) {
        imageType = candidate;
      } else if (imageType != candidate) {
        imageType = null;
        break;
      }
    }

    return (imageType == null) ? SELECTOR_DEFAULT : imageType;
  }

  public static CSSImageType getImageType(ICSSSelector selector) {
    CSSImageType imageType = SELECTOR_DEFAULT;
    if (selector == null || selector.getLength() == 0) {
      return imageType;
    }
    ICSSSelectorItem item = selector.getItem(selector.getLength() - 1);
    if (item.getItemType() == ICSSSelectorItem.SIMPLE) {
      ICSSSimpleSelector ss = (ICSSSimpleSelector) item;
      if (0 < ss.getNumOfIDs()) {
        imageType = SELECTOR_ID;
      } else if (0 < ss.getNumOfClasses()) {
        imageType = SELECTOR_CLASS;
      } else if (0 < ss.getNumOfPseudoNames()) {
        imageType = SELECTOR_PSEUDO;
      } else {
        imageType = SELECTOR_TAG;
      }
    }
    return imageType;
  }

  public static CSSImageType getImageType(String category) {
    if (fCategoryMap == null) {
      fCategoryMap = new HashMap();
      fCategoryMap.put("aural", CATEGORY_AURAL); //$NON-NLS-1$
      fCategoryMap.put("box", CATEGORY_BOX); //$NON-NLS-1$
      fCategoryMap.put("colorandbackground", CATEGORY_COLORANDBACKGROUND); //$NON-NLS-1$
      fCategoryMap.put("content", CATEGORY_CONTENT); //$NON-NLS-1$
      fCategoryMap.put("font", CATEGORY_FONT); //$NON-NLS-1$
      fCategoryMap.put("page", CATEGORY_PAGE); //$NON-NLS-1$
      fCategoryMap.put("tables", CATEGORY_TABLES); //$NON-NLS-1$
      fCategoryMap.put("text", CATEGORY_TEXT); //$NON-NLS-1$
      fCategoryMap.put("ui", CATEGORY_UI); //$NON-NLS-1$
      fCategoryMap.put("visual", CATEGORY_VISUAL); //$NON-NLS-1$
    }
    CSSImageType imageType = (CSSImageType) fCategoryMap.get(category);
    return (imageType == null) ? CATEGORY_DEFAULT : imageType;
  }

  private static Map fCategoryMap = null;
}
