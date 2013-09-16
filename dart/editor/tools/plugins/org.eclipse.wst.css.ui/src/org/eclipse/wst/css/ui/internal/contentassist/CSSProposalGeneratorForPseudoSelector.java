/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.contentassist;

import org.eclipse.wst.css.core.internal.CSSCorePlugin;
import org.eclipse.wst.css.core.internal.metamodel.CSSMMNode;
import org.eclipse.wst.css.core.internal.metamodel.CSSMMSelector;
import org.eclipse.wst.css.core.internal.metamodel.util.CSSMMTypeCollector;
import org.eclipse.wst.css.core.internal.parser.CSSRegionUtil;
import org.eclipse.wst.css.core.internal.parserz.CSSRegionContexts;
import org.eclipse.wst.css.core.internal.preferences.CSSCorePreferenceNames;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSNode;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSPageRule;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSStyleRule;
import org.eclipse.wst.css.ui.internal.image.CSSImageType;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 *  
 */
class CSSProposalGeneratorForPseudoSelector extends CSSProposalGenerator {

  /**
   * CSSProposalGeneratorForPseudoSelector constructor comment.
   */
  CSSProposalGeneratorForPseudoSelector(CSSContentAssistContext context) {
    super(context);
  }

  /**
   * getCandidates method comment.
   */
  protected Iterator getCandidates() {
    List candidates = new ArrayList();

    boolean hasLeadingColon = checkLeadingColon();
    String textToReplace = fContext.getTextToReplace();
    if (!hasLeadingColon && 0 < textToReplace.length()
        && !textToReplace.equals(fContext.getTextToCompare())) {
      // cursor placed midpoint of the region
      return candidates.iterator();
    }
    ITextRegion region = fContext.getTargetRegion();
    if (region != null) {
      String type = region.getType();
      if (type != CSSRegionContexts.CSS_S && !CSSRegionUtil.isSelectorBegginingType(type)) {
        return candidates.iterator();
      }
    }

    boolean useUpperCase = CSSCorePlugin.getDefault().getPluginPreferences().getInt(
        CSSCorePreferenceNames.CASE_IDENTIFIER) == CSSCorePreferenceNames.UPPER;

    List tags = getSelectorTags();
    Collections.sort(tags, new Comparator() {
      /*
       * (non-Javadoc)
       * 
       * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
       */
      public int compare(Object o1, Object o2) {
        return clean(((CSSMMSelector) o1).getName()).compareTo(
            clean(((CSSMMSelector) o2).getName()));
      }

      private String clean(String str) {
        int length = str.length();
        for (int i = 0; i < length; i++) {
          if (str.charAt(i) != ':') {
            return str.substring(i);
          }
        }
        return str;
      }
    });

    Iterator i = tags.iterator();
    while (i.hasNext()) {
      CSSMMSelector selector = (CSSMMSelector) i.next();
      String text = selector.getSelectorString();
      if (hasLeadingColon && !isMatch(text)) {
        continue;
      }
      text = (useUpperCase) ? text.toUpperCase() : text.toLowerCase();

      int cursorPos = 0;
      StringBuffer buf = new StringBuffer();

      if (!hasLeadingColon)
        buf.append(textToReplace);

      buf.append(text);
      cursorPos += buf.length();

      if (0 < buf.length()) {
        // Pseudoclass/element takes arguments
        if (buf.charAt(buf.length() - 1) == ')') {
          --cursorPos;
        }
        boolean inRule = (fContext.getTargetNode() instanceof ICSSStyleRule || fContext.getTargetNode() instanceof ICSSPageRule);
        if (!inRule || (textToReplace.length() == 0 && !hasLeadingColon)) {
          buf.append(" ");//$NON-NLS-1$
          cursorPos += 1;
        }
        if (!inRule) {
          StringAndOffset sao = generateBraces();
          buf.append(sao.fString);
          cursorPos += sao.fOffset;
        }
        CSSCACandidate item = new CSSCACandidate();
        item.setReplacementString(buf.toString());
        item.setCursorPosition(cursorPos);
        item.setDisplayString(text);
        item.setImageType(CSSImageType.SELECTOR_PSEUDO);
        item.setMMNode(selector);
        candidates.add(item);
      }
    }

    return candidates.iterator();
  }

  /**
	 *  
	 */
  List getSelectorTags() {
    List tagList = new ArrayList();
    ICSSNode targetNode = fContext.getTargetNode();
    String rootType = (targetNode instanceof ICSSPageRule) ? CSSMMNode.TYPE_PAGE_RULE
        : CSSMMNode.TYPE_STYLE_RULE;

    CSSMMTypeCollector collector = new CSSMMTypeCollector();
    collector.collectNestedType(false);
    collector.apply(fContext.getMetaModel(), rootType);
    Iterator i;
    i = collector.getNodes();
    if (!i.hasNext()) {
      return tagList;
    }
    CSSMMNode node = (CSSMMNode) i.next();
    i = node.getChildNodes();
    while (i.hasNext()) {
      CSSMMNode child = (CSSMMNode) i.next();
      if (child.getType() == CSSMMNode.TYPE_SELECTOR) {
        String selType = ((CSSMMSelector) child).getSelectorType();
        if (selType == CSSMMSelector.TYPE_PSEUDO_CLASS
            || selType == CSSMMSelector.TYPE_PSEUDO_ELEMENT) {
          tagList.add(child);
        }
      }
    }
    return tagList;
  }
}
