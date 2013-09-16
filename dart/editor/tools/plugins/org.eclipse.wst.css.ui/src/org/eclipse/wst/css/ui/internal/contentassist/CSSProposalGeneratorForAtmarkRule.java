/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.contentassist;

import org.eclipse.wst.css.core.internal.CSSCorePlugin;
import org.eclipse.wst.css.core.internal.metamodel.CSSMMNode;
import org.eclipse.wst.css.core.internal.metamodel.util.CSSMetaModelUtil;
import org.eclipse.wst.css.core.internal.parserz.CSSRegionContexts;
import org.eclipse.wst.css.core.internal.preferences.CSSCorePreferenceNames;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSCharsetRule;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSDocument;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSImportRule;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSModel;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSNode;
import org.eclipse.wst.css.core.internal.util.SelectionCollector;
import org.eclipse.wst.css.ui.internal.image.CSSImageType;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class CSSProposalGeneratorForAtmarkRule extends CSSProposalGenerator {

  private boolean fUseUpperCase = false;
  private static final String CHARSET = "@charset";//$NON-NLS-1$
  private static final String FONT_FACE = "@font-face";//$NON-NLS-1$
  private static final String IMPORT = "@import";//$NON-NLS-1$
  private static final String MEDIA = "@media";//$NON-NLS-1$
  private static final String PAGE = "@page";//$NON-NLS-1$

  /**
   * CSSAtmarkRuleProposalGenerator constructor comment.
   */
  CSSProposalGeneratorForAtmarkRule(CSSContentAssistContext context) {
    super(context);
    fUseUpperCase = CSSCorePlugin.getDefault().getPluginPreferences().getInt(
        CSSCorePreferenceNames.CASE_IDENTIFIER) == CSSCorePreferenceNames.UPPER;
  }

  /**
	 *  
	 */
  private CSSCACandidate getCandidateCharsetRule() {
    // check content model
    CSSMetaModelUtil util = new CSSMetaModelUtil(fContext.getMetaModel());
    if (!util.collectNodesByType(CSSMMNode.TYPE_CHARSET_RULE).hasNext()) {
      return null;
    }

    // check if embedded or not
    if (fContext.getModel().getStyleSheetType() == ICSSModel.EMBEDDED) {
      return null;
    }

    // check if caret precede all other rules.
    int offset = fContext.getCursorPos();
    if (0 < offset) {
      SelectionCollector trav = new SelectionCollector();
      trav.setRegion(0, offset - 1);
      trav.apply(fContext.getModel().getDocument());
      Iterator i = trav.getSelectedNodes().iterator();
      while (i.hasNext()) {
        Object obj = i.next();
        if (obj instanceof ICSSNode && !(obj instanceof ICSSDocument)) {
          return null;
        }
      }
    }

    int cursorPos = 0;
    String ident = (fUseUpperCase) ? CHARSET.toUpperCase() : CHARSET.toLowerCase();
    StringBuffer buf = new StringBuffer();
    buf.append(ident);
    buf.append(" ");//$NON-NLS-1$
    cursorPos = buf.length();
    StringAndOffset sao;
    sao = generateQuotes();
    buf.append(sao.fString);
    cursorPos += sao.fOffset;
    sao = generateSemicolon();
    buf.append(sao.fString);

    String text = buf.toString();

    if (isMatch(text)) {
      CSSCACandidate item = new CSSCACandidate();
      item.setReplacementString(text);
      item.setCursorPosition(cursorPos);
      item.setDisplayString(ident);
      item.setImageType(CSSImageType.RULE_CHARSET);
      return item;
    } else {
      return null;
    }
  }

  /**
	 *  
	 */
  private CSSCACandidate getCandidateFontFaceRule() {
    CSSMetaModelUtil util = new CSSMetaModelUtil(fContext.getMetaModel());
    if (!util.collectNodesByType(CSSMMNode.TYPE_FONT_FACE_RULE).hasNext()) {
      return null;
    }

    int cursorPos = 0;
    String ident = (fUseUpperCase) ? FONT_FACE.toUpperCase() : FONT_FACE.toLowerCase();
    StringBuffer buf = new StringBuffer();
    buf.append(ident);
    buf.append(" ");//$NON-NLS-1$
    cursorPos = buf.length();
    StringAndOffset sao;
    sao = generateBraces();
    buf.append(sao.fString);
    cursorPos += sao.fOffset;

    String text = buf.toString();

    if (isMatch(text)) {
      CSSCACandidate item = new CSSCACandidate();
      item.setReplacementString(buf.toString());
      item.setCursorPosition(cursorPos);
      item.setDisplayString(ident);
      item.setImageType(CSSImageType.RULE_FONTFACE);
      return item;
    } else {
      return null;
    }
  }

  /**
	 *  
	 */
  private CSSCACandidate getCandidateImportRule() {
    // check content model
    CSSMetaModelUtil util = new CSSMetaModelUtil(fContext.getMetaModel());
    if (!util.collectNodesByType(CSSMMNode.TYPE_IMPORT_RULE).hasNext()) {
      return null;
    }

    // charset and import can precede import rule.
    int offset = fContext.getCursorPos();
    if (0 < offset) {
      SelectionCollector trav = new SelectionCollector();
      trav.setRegion(0, offset - 1);
      trav.apply(fContext.getModel().getDocument());
      Iterator i = trav.getSelectedNodes().iterator();
      while (i.hasNext()) {
        Object obj = i.next();
        if (obj instanceof ICSSNode
            && !(obj instanceof ICSSDocument || obj instanceof ICSSCharsetRule || obj instanceof ICSSImportRule)) {
          return null;
        }
      }
    }

    int cursorPos = 0;
    String ident = (fUseUpperCase) ? IMPORT.toUpperCase() : IMPORT.toLowerCase();
    StringBuffer buf = new StringBuffer();
    buf.append(ident);
    buf.append(" ");//$NON-NLS-1$
    cursorPos = buf.length();
    StringAndOffset sao;
    sao = generateURI();
    buf.append(sao.fString);
    cursorPos += sao.fOffset;
    sao = generateSemicolon();
    buf.append(sao.fString);

    String text = buf.toString();

    if (isMatch(text)) {
      CSSCACandidate item = new CSSCACandidate();
      item.setReplacementString(buf.toString());
      item.setCursorPosition(cursorPos);
      item.setDisplayString(ident);
      item.setImageType(CSSImageType.RULE_IMPORT);
      return item;
    } else {
      return null;
    }
  }

  /**
	 *  
	 */
  private CSSCACandidate getCandidateMediaRule() {
    CSSMetaModelUtil util = new CSSMetaModelUtil(fContext.getMetaModel());
    if (!util.collectNodesByType(CSSMMNode.TYPE_MEDIA_RULE).hasNext()) {
      return null;
    }

    int cursorPos = 0;
    String ident = (fUseUpperCase) ? MEDIA.toUpperCase() : MEDIA.toLowerCase();
    StringBuffer buf = new StringBuffer();
    buf.append(ident);
    buf.append("  ");//$NON-NLS-1$
    cursorPos = buf.length() - 1;
    StringAndOffset sao;
    sao = generateBraces();
    buf.append(sao.fString);

    String text = buf.toString();

    if (isMatch(text)) {
      CSSCACandidate item = new CSSCACandidate();
      item.setReplacementString(buf.toString());
      item.setCursorPosition(cursorPos);
      item.setDisplayString(ident);
      item.setImageType(CSSImageType.RULE_MEDIA);
      return item;
    } else {
      return null;
    }
  }

  /**
	 *  
	 */
  private CSSCACandidate getCandidatePageRule() {
    CSSMetaModelUtil util = new CSSMetaModelUtil(fContext.getMetaModel());
    if (!util.collectNodesByType(CSSMMNode.TYPE_PAGE_RULE).hasNext()) {
      return null;
    }

    int cursorPos = 0;
    String ident = (fUseUpperCase) ? PAGE.toUpperCase() : PAGE.toLowerCase();
    StringBuffer buf = new StringBuffer();
    buf.append(ident);
    buf.append(" ");//$NON-NLS-1$
    cursorPos = buf.length();
    StringAndOffset sao;
    sao = generateBraces();
    buf.append(sao.fString);
    cursorPos += sao.fOffset;

    String text = buf.toString();

    if (isMatch(text)) {
      CSSCACandidate item = new CSSCACandidate();
      item.setReplacementString(buf.toString());
      item.setCursorPosition(cursorPos);
      item.setDisplayString(ident);
      item.setImageType(CSSImageType.RULE_PAGE);
      return item;
    } else {
      return null;
    }
  }

  /**
   * getCandidates method comment.
   */
  protected Iterator getCandidates() {
    List candidates = new ArrayList();

    ITextRegion region = fContext.getTargetRegionPrevious();
    //	ITextRegion region = fContext.getSignificantTargetRegion();
    if (region != null) {
      String type = region.getType();
      if (type != CSSRegionContexts.CSS_RBRACE && type != CSSRegionContexts.CSS_DELIMITER) {
        return candidates.iterator();
      }
    }

    CSSCACandidate item;
    if ((item = getCandidateImportRule()) != null) {
      candidates.add(item);
    }
    if ((item = getCandidateCharsetRule()) != null) {
      candidates.add(item);
    }
    if ((item = getCandidateMediaRule()) != null) {
      candidates.add(item);
    }
    if ((item = getCandidatePageRule()) != null) {
      candidates.add(item);
    }
    if ((item = getCandidateFontFaceRule()) != null) {
      candidates.add(item);
    }
    return candidates.iterator();
  }
}
