/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.contentassist;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.css.core.internal.CSSCorePlugin;
import org.eclipse.wst.css.core.internal.parserz.CSSRegionContexts;
import org.eclipse.wst.css.core.internal.preferences.CSSCorePreferenceNames;
import org.eclipse.wst.css.ui.internal.image.CSSImageHelper;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

abstract class CSSProposalGenerator {

  protected class StringAndOffset {
    StringAndOffset(String string, int offset) {
      this.fString = string;
      this.fOffset = offset;
    }

    String fString;
    int fOffset;
  }

  protected CSSContentAssistContext fContext = null;

  /**
   * CSSProposalGenerator constructor comment.
   */
  private CSSProposalGenerator() {
    super();
  }

  CSSProposalGenerator(CSSContentAssistContext context) {
    this();
    fContext = context;
  }

  /**
	 * 
	 */
  protected boolean checkLeadingColon() {
    boolean hasLeadingColon = false;
    ITextRegion targetRegion = fContext.getTargetRegion();
    if (targetRegion == null && 0 < fContext.getCursorPos()) {
      targetRegion = fContext.getRegionByOffset(fContext.getCursorPos() - 1);
      if (targetRegion != null && targetRegion.getType() == CSSRegionContexts.CSS_SELECTOR_PSEUDO) {
        hasLeadingColon = true;
      }
    } else if (targetRegion != null) {
      // BUG 92848 - Changed how pseudo-selectors are defined in the
      // tokenizer
      // The target region should be the CSS_SELECTOR_PSEUDO now
      if (targetRegion.getType() == CSSRegionContexts.CSS_SELECTOR_PSEUDO) {
        hasLeadingColon = true;
      }
    }
    return hasLeadingColon;
  }

  /**
	 * 
	 */
  protected StringAndOffset generateBraces() {
    StringBuffer buf = new StringBuffer();
    String lineDelimiter = fContext.getStructuredDocument().getLineDelimiter();
    Preferences preferences = CSSCorePlugin.getDefault().getPluginPreferences();
    String indentStr = getIndentString();
    if (preferences.getBoolean(CSSCorePreferenceNames.WRAPPING_NEWLINE_ON_OPEN_BRACE)) {
      buf.append(lineDelimiter);
    }
    buf.append("{");//$NON-NLS-1$
    if (preferences.getBoolean(CSSCorePreferenceNames.WRAPPING_ONE_PER_LINE)) {
      buf.append(lineDelimiter);
      buf.append(indentStr);
    } else {
      buf.append(" ");//$NON-NLS-1$
    }
    int offset = buf.length();
    if (preferences.getBoolean(CSSCorePreferenceNames.WRAPPING_ONE_PER_LINE)) {
      buf.append(lineDelimiter);
    } else {
      buf.append(" ");//$NON-NLS-1$
    }
    buf.append("}");//$NON-NLS-1$
    return new StringAndOffset(buf.toString(), offset);
  }

  /**
	 * 
	 */
  protected StringAndOffset generateParenthesis() {
    StringBuffer buf = new StringBuffer();
    int offset;
    buf.append("(");//$NON-NLS-1$
    offset = 1;
    buf.append(")");//$NON-NLS-1$
    return new StringAndOffset(buf.toString(), offset);
  }

  /**
	 * 
	 */
  protected StringAndOffset generateQuotes() {
    StringBuffer buf = new StringBuffer();
    char quoteChar = getQuoteChar();
    buf.append(quoteChar);
    buf.append(quoteChar);
    return new StringAndOffset(buf.toString(), 1);
  }

  /**
	 * 
	 */
  protected StringAndOffset generateSemicolon() {
    StringBuffer buf = new StringBuffer();
    int offset;
    buf.append(";");//$NON-NLS-1$
    offset = 0;
    return new StringAndOffset(buf.toString(), offset);
  }

  /**
	 * 
	 */
  protected StringAndOffset generateURI() {
    StringBuffer buf = new StringBuffer();

    boolean isQuoteInURI = CSSCorePlugin.getDefault().getPluginPreferences().getBoolean(
        CSSCorePreferenceNames.FORMAT_QUOTE_IN_URI);
    char quoteChar = getQuoteChar();
    buf.append("url(");//$NON-NLS-1$
    if (isQuoteInURI) {
      buf.append(quoteChar);
    }
    int offset = buf.length();
    if (isQuoteInURI) {
      buf.append(quoteChar);
    }
    buf.append(")");//$NON-NLS-1$
    return new StringAndOffset(buf.toString(), offset);
  }

  abstract protected Iterator getCandidates();

  List getProposals() {
    List proposals = new ArrayList();

    CSSImageHelper imageHelper = CSSImageHelper.getInstance();
    Iterator i = getCandidates();
    while (i.hasNext()) {
      CSSCACandidate candidate = (CSSCACandidate) i.next();
      Image image = imageHelper.getImage(candidate.getImageType());
      ICompletionProposal item = new CompletionProposal(candidate.getReplacementString(),
          fContext.getReplaceBegin() + fContext.getDocumentOffset(),
          fContext.getTextToReplace().length(), candidate.getCursorPosition(), image,
          candidate.getDisplayString(), null, candidate.getDisplayString(), candidate.getMMNode());
      proposals.add(item);
    }

    return proposals;
  }

  /**
   * @return char
   */
  private char getQuoteChar() {

    String quoteStr = CSSCorePlugin.getDefault().getPluginPreferences().getString(
        CSSCorePreferenceNames.FORMAT_QUOTE);
    char quoteChar = (quoteStr != null && 0 < quoteStr.length()) ? quoteStr.charAt(0) : '"';
    char attrQuote = fContext.getQuoteOfStyleAttribute();
    if (attrQuote != 0) {
      if (attrQuote == '"' && quoteChar == '"') {
        quoteChar = '\'';
      } else if (attrQuote == '\'' && quoteChar == '\'') {
        quoteChar = '"';
      }
    }
    return quoteChar;
  }

  /**
	 * 
	 */
  protected boolean isMatch(String text) {
    String textToCompare = fContext.getTextToCompare();
    if (textToCompare.length() == 0) {
      return true;
    } else {
      return (text.toUpperCase().indexOf(textToCompare.toUpperCase()) == 0);
    }
    /*
     * String textToReplace = fContext.getTextToReplace(); if (textToReplace.length() == 0) { return
     * true; } else { return (text.toUpperCase().indexOf(textToReplace.toUpperCase()) == 0); }
     */
  }

  private String getIndentString() {
    StringBuffer indent = new StringBuffer();

    Preferences preferences = CSSCorePlugin.getDefault().getPluginPreferences();
    if (preferences != null) {
      char indentChar = ' ';
      String indentCharPref = preferences.getString(CSSCorePreferenceNames.INDENTATION_CHAR);
      if (CSSCorePreferenceNames.TAB.equals(indentCharPref)) {
        indentChar = '\t';
      }
      int indentationWidth = preferences.getInt(CSSCorePreferenceNames.INDENTATION_SIZE);

      for (int i = 0; i < indentationWidth; i++) {
        indent.append(indentChar);
      }
    }
    return indent.toString();
  }
}
