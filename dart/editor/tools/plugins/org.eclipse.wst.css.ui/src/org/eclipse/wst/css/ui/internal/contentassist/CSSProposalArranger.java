/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.contentassist;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.wst.css.core.internal.parserz.CSSRegionContexts;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSMediaRule;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSNode;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSPageRule;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSPrimitiveValue;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSStyleDeclItem;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSStyleDeclaration;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSStyleRule;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSStyleSheet;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.w3c.dom.css.CSSFontFaceRule;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class CSSProposalArranger {

  private List fProposals = new ArrayList();
  private CSSContentAssistContext fContext = null;

  /**
   * CSSProposalArranger constructor comment.
   */
  private CSSProposalArranger() {
    super();
  }

  /**
   * CSSProposalArranger constructor comment.
   */
  CSSProposalArranger(int documentPosition, ICSSNode node, int documentOffset, char quote) {
    this();
    fContext = new CSSContentAssistContext(documentPosition, node, documentOffset, quote);
  }

  CSSProposalArranger(int documentPosition, ICSSNode node, int documentOffset, int length,
      char quote) {
    this();
    fContext = new CSSContentAssistContext(documentPosition, node, documentOffset, length, quote);
  }

  CSSProposalArranger(int documentPosition, ICSSNode node, int documentOffset, char quote,
      boolean selected) {
    this();
    fContext = new CSSContentAssistContext(documentPosition, node, documentOffset, quote, selected);
  }

  /**
	 *  
	 */
  void buildProposals() {
    fProposals.clear();

    /*
     * String text; ICompletionProposal item; text = "---- Test Information
     * ----"; item = new CompletionProposal("", fContext.getReplaceBegin(), 0, 0, null, text, null,
     * null); fProposals.add(item);
     * 
     * text = "Target: \"" + fContext.getRegionText() + "\"";
     * 
     * item = new CompletionProposal("", fContext.getReplaceBegin(), 0, 0, null, text, null, null);
     * fProposals.add(item);
     * 
     * text = fContext.getTargetNode().getClass().toString(); int lastPeriodPos =
     * text.lastIndexOf('.'); text = "Node: " + text.substring(lastPeriodPos + 1); item = new
     * CompletionProposal("", fContext.getReplaceBegin(), 0, 0, null, text, null, null);
     * fProposals.add(item);
     */

    ICSSNode targetNode = fContext.getTargetNode();
    //int targetPos = fContext.getTargetPos();
    if (targetNode instanceof ICSSStyleSheet) {
      buildProposalsForAnyRule();
    } else if ((targetNode instanceof ICSSMediaRule && fContext.isTargetPosAfterOf(CSSRegionContexts.CSS_LBRACE))
        || (targetNode instanceof ICSSStyleRule && fContext.isTargetPosBeforeOf(CSSRegionContexts.CSS_LBRACE))) {
      buildProposalsForAnyRule();
      //		buildProposalsForStyleRule();
    } else if ((targetNode instanceof ICSSPageRule && fContext.isTargetPosBeforeOf(CSSRegionContexts.CSS_LBRACE))) {
      buildProposalsForPageRulePseudoClass();
    } else if ((targetNode instanceof ICSSStyleRule || targetNode instanceof CSSFontFaceRule
        || targetNode instanceof ICSSPageRule || targetNode instanceof ICSSStyleDeclaration)
        && (targetNode.getOwnerDocument() instanceof ICSSStyleDeclaration
            || fContext.targetFollows(CSSRegionContexts.CSS_DECLARATION_DELIMITER) || fContext.targetFollows(CSSRegionContexts.CSS_LBRACE))) {
      buildProposalsForDeclarationName();
    } else if (targetNode instanceof ICSSStyleDeclItem) {
      if (fContext.isTargetPosAfterOf(CSSRegionContexts.CSS_DECLARATION_SEPARATOR)) {
        buildProposalsForDeclarationValue();
      } else {
        buildProposalsForDeclarationName();
      }
    } else if (targetNode instanceof ICSSPrimitiveValue) {
      buildProposalsForDeclarationValue();
    }
    /*
     * else if (targetNode instanceof ICSSPrimitiveValue || ((targetNode instanceof ICSSStyleRule ||
     * targetNode instanceof CSSFontFaceRule || targetNode instanceof ICSSStyleDeclaration ||
     * targetNode instanceof ICSSStyleDeclItem) &&
     * fContext.isTargetPosAfterOf(CSSRegionContexts.COLON))) { buildProposalsForDeclarationValue();
     * }
     */

    // for Test
  }

  /**
	 *  
	 */
  void buildProposalsForAnyRule() {
    CSSProposalGenerator generator;
    if (isFirstRegion()) {
      generator = new CSSProposalGeneratorForAtmarkRule(fContext);
      fProposals.addAll(generator.getProposals());
    }
    generator = new CSSProposalGeneratorForHTMLTag(fContext);
    fProposals.addAll(generator.getProposals());
    generator = new CSSProposalGeneratorForPseudoSelector(fContext);
    fProposals.addAll(generator.getProposals());
  }

  private boolean isFirstRegion() {
    if (fContext.getCursorPos() == 0)
      return true;

    ITextRegion region = fContext.getTargetRegion();
    ITextRegion currRegion = fContext.getRegionByOffset(fContext.getCursorPos());
    String type = null;
    String crType = null;
    if (region != null) {
      type = region.getType();
    }
    if (currRegion != null) {
      crType = currRegion.getType();
    }
    if (type == null || crType == null || (crType == type))
      return true;
    else
      return false;
  }

  /**
	 *  
	 */
  void buildProposalsForDeclarationName() {
    CSSProposalGenerator generator;
    generator = new CSSProposalGeneratorForDeclarationName(fContext);
    fProposals.addAll(generator.getProposals());
  }

  /**
	 *  
	 */
  void buildProposalsForDeclarationValue() {
    CSSProposalGenerator generator;
    generator = new CSSProposalGeneratorForDeclarationValue(fContext);
    fProposals.addAll(generator.getProposals());
  }

  /**
	 *  
	 */
  void buildProposalsForPageRulePseudoClass() {
    CSSProposalGenerator generator;
    generator = new CSSProposalGeneratorForPseudoSelector(fContext);
    fProposals.addAll(generator.getProposals());
  }

  /**
	 *  
	 */
  void buildProposalsForStyleRule() {
    CSSProposalGenerator generator;
    generator = new CSSProposalGeneratorForHTMLTag(fContext);
    fProposals.addAll(generator.getProposals());
    generator = new CSSProposalGeneratorForPseudoSelector(fContext);
    fProposals.addAll(generator.getProposals());
  }

  /**
	 *  
	 */
  ICompletionProposal[] getProposals() {
    buildProposals();
    ICompletionProposal[] proposalArray = new ICompletionProposal[fProposals.size()];
    Iterator iItem = fProposals.iterator();
    for (int i = 0; iItem.hasNext(); i++) {
      proposalArray[i] = (ICompletionProposal) iItem.next();
    }
    return proposalArray;
  }
}
