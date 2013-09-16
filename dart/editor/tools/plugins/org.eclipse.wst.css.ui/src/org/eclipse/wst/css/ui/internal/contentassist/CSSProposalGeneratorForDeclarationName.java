/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.contentassist;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.wst.css.core.internal.CSSCorePlugin;
import org.eclipse.wst.css.core.internal.metamodel.CSSMMNode;
import org.eclipse.wst.css.core.internal.metamodel.util.CSSMetaModelUtil;
import org.eclipse.wst.css.core.internal.parserz.CSSRegionContexts;
import org.eclipse.wst.css.core.internal.preferences.CSSCorePreferenceNames;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSNode;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSStyleDeclItem;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSStyleDeclaration;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSStyleRule;
import org.eclipse.wst.css.ui.internal.image.CSSImageType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

class CSSProposalGeneratorForDeclarationName extends CSSProposalGenerator {

  /**
   * CSSProposalGeneratorForDeclaration constructor comment.
   */
  CSSProposalGeneratorForDeclarationName(CSSContentAssistContext context) {
    super(context);
  }

  /**
   * getCandidates method comment.
   */
  protected Iterator getCandidates() {
    List candidates = new ArrayList();
    Preferences preferences = CSSCorePlugin.getDefault().getPluginPreferences();
    String preDelim = "";//$NON-NLS-1$
    for (int i = 0; i < preferences.getInt(CSSCorePreferenceNames.FORMAT_PROP_PRE_DELIM); i++) {
      preDelim += ' ';//$NON-NLS-1$
    }
    String postDelim = "";//$NON-NLS-1$
    for (int i = 0; i < preferences.getInt(CSSCorePreferenceNames.FORMAT_PROP_POST_DELIM); i++) {
      postDelim += ' ';//$NON-NLS-1$
    }

    ICSSNode targetNode = fContext.getTargetNode();
    boolean bFontRule = false;
    for (ICSSNode node = targetNode; node != null; node = node.getParentNode()) {
      if (node instanceof org.w3c.dom.css.CSSFontFaceRule) {
        bFontRule = true;
        break;
      }
    }

    List names = new ArrayList();
    CSSMetaModelUtil util = new CSSMetaModelUtil(fContext.getMetaModel());
    Iterator iNames = util.collectNodesByType((bFontRule) ? CSSMMNode.TYPE_DESCRIPTOR
        : CSSMMNode.TYPE_PROPERTY);
    while (iNames.hasNext()) {
      CSSMMNode node = (CSSMMNode) iNames.next();
      names.add(node);
    }
    sortNames(names);
    // Collections.sort(names);

    boolean bAddColon = true;
    if ((targetNode instanceof ICSSStyleRule || targetNode instanceof ICSSStyleDeclItem || targetNode instanceof ICSSStyleDeclaration)
        && fContext.targetHas(CSSRegionContexts.CSS_DECLARATION_SEPARATOR)) {
      bAddColon = false;
    }

    Iterator i = names.iterator();
    while (i.hasNext()) {
      CSSMMNode node = (CSSMMNode) i.next();
      String text = node.getName();
      text = (preferences.getInt(CSSCorePreferenceNames.CASE_PROPERTY_NAME) == CSSCorePreferenceNames.UPPER)
          ? text.toUpperCase() : text.toLowerCase();
      if (!isMatch(text)) {
        continue;
      }

      int cursorPos = 0;
      StringBuffer buf = new StringBuffer();
      buf.append(text);
      buf.append(preDelim);
      cursorPos = buf.length();
      if (bAddColon) {
        buf.append(':');//$NON-NLS-1$
        buf.append(postDelim);
        cursorPos += 1 + postDelim.length();
      }
      // if (! (targetNode instanceof ICSSStyleDeclItem)) {
      // buf.append(';');//$NON-NLS-1$
      // }

      CSSCACandidate item = new CSSCACandidate();
      item.setReplacementString(buf.toString());
      item.setCursorPosition(cursorPos);
      item.setDisplayString(text);
      item.setMMNode(node);
      item.setImageType(getCategoryImageType(node));
      candidates.add(item);
    }

    return candidates.iterator();
  }

  void sortNames(List names) {
    Collections.sort(names, new Comparator() {
      public int compare(Object o1, Object o2) {
        CSSMMNode node1 = (CSSMMNode) o1;
        CSSMMNode node2 = (CSSMMNode) o2;
        if (node1 == null) {
          return 1;
        } else if (node2 == null) {
          return -1;
        }
        int diff = 0;
        String category1 = node1.getAttribute("category"); //$NON-NLS-1$
        String category2 = node2.getAttribute("category"); //$NON-NLS-1$
        if (category1 == null) {
          if (category2 == null) {
            diff = 0;
          } else {
            return 1;
          }
        } else if (category2 == null) {
          return -1;
        } else {
          diff = category1.compareTo(category2);
        }
        if (diff == 0) {
          String name = node1.getName();
          if (name == null) {
            return 1;
          } else {
            return name.compareTo(node2.getName());
          }
        } else {
          return diff;
        }
      }
    });
  }

  /*
   * retrieve default category icon name TODO: if node has "icon"(?), use it.
   */
  CSSImageType getCategoryImageType(CSSMMNode node) {
    CSSImageType imageType = null;
    if (node != null) {
      String category = node.getAttribute("category"); //$NON-NLS-1$
      if (category != null) {
        imageType = CSSImageType.getImageType(category);
      }
    }
    return (imageType == null) ? CSSImageType.CATEGORY_DEFAULT : imageType;
  }
}
