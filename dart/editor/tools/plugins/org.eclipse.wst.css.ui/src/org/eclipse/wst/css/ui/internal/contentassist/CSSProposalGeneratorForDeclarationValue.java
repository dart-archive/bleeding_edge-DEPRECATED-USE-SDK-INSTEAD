/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.contentassist;

import org.eclipse.wst.css.core.internal.CSSCorePlugin;
import org.eclipse.wst.css.core.internal.metamodel.CSSMMDescriptor;
import org.eclipse.wst.css.core.internal.metamodel.CSSMMFunction;
import org.eclipse.wst.css.core.internal.metamodel.CSSMMNode;
import org.eclipse.wst.css.core.internal.metamodel.CSSMMNumber;
import org.eclipse.wst.css.core.internal.metamodel.CSSMMProperty;
import org.eclipse.wst.css.core.internal.metamodel.CSSMMUnit;
import org.eclipse.wst.css.core.internal.metamodel.util.CSSFunctionID;
import org.eclipse.wst.css.core.internal.metamodel.util.CSSMetaModelUtil;
import org.eclipse.wst.css.core.internal.parserz.CSSRegionContexts;
import org.eclipse.wst.css.core.internal.preferences.CSSCorePreferenceNames;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSNode;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSPrimitiveValue;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSStyleDeclItem;
import org.eclipse.wst.css.core.internal.util.CSSUtil;
import org.eclipse.wst.css.core.internal.util.RegionIterator;
import org.eclipse.wst.css.ui.internal.image.CSSImageType;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.w3c.dom.css.CSSFontFaceRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

class CSSProposalGeneratorForDeclarationValue extends CSSProposalGenerator {

  private static final String IMPORTANT = "!important"; //$NON-NLS-1$
  private boolean fUseUpperCase;
  private boolean fAppendSemiColon;

  /**
   * CSSProposalGeneratorForDeclarationValue constructor comment.
   */
  CSSProposalGeneratorForDeclarationValue(CSSContentAssistContext context) {
    super(context);
    fUseUpperCase = CSSCorePlugin.getDefault().getPluginPreferences().getInt(
        CSSCorePreferenceNames.CASE_PROPERTY_VALUE) == CSSCorePreferenceNames.UPPER;
  }

  /**
	 *  
	 */
  private void addFunction(List candidates, CSSMMFunction prop) {
    String text = prop.toString();
    if (!isMatch(text)) {
      return;
    }

    int cursorPos = 0;
    StringBuffer buf = new StringBuffer();
    if (prop.getName().equals(CSSFunctionID.F_URI)) {
      StringAndOffset sao = generateURI();
      buf.append(sao.fString);
      cursorPos = sao.fOffset;
    } else {
      buf.append(prop.toString());
      cursorPos = buf.length();
      StringAndOffset sao = generateParenthesis();
      buf.append(sao.fString);
      cursorPos += sao.fOffset;
    }

    text = buf.toString();
    text = (fUseUpperCase) ? text.toUpperCase() : text.toLowerCase();

    CSSCACandidate item = new CSSCACandidate();
    item.setReplacementString(text);
    item.setCursorPosition(cursorPos);
    item.setDisplayString(text);
    item.setMMNode(prop);
    item.setImageType(CSSImageType.VALUE_FUNCTION);
    appendSemiColon(item);
    candidates.add(item);
  }

  /**
	 *  
	 */
  private void addNumber(List candidates, CSSMMNumber prop) {
    String fullText = fContext.getTextToReplace();
    // skip number
    int unitIndex = -1;
    for (int i = 0; i < fullText.length(); i++) {
      if (Character.isDigit(fullText.charAt(i))) {
        unitIndex = i + 1;
      } else {
        break;
      }
    }

    String unitSubText = ""; //$NON-NLS-1$
    String numSubText = ""; //$NON-NLS-1$
    if (0 <= unitIndex) {
      numSubText = fullText.substring(0, unitIndex);
      if (unitIndex < fullText.length()) {
        unitSubText = fullText.substring(unitIndex);
      }
    } else {
      unitSubText = fullText;
    }

    Iterator i = prop.getDescendants();
    while (i.hasNext()) {
      CSSMMUnit unit = (CSSMMUnit) i.next();
      String unitString = unit.getUnitString();
      if ((0 < unitSubText.length() && unitString.indexOf(unitSubText) != 0)
          || (0 < numSubText.length() && unitString.equals("#"))) { //$NON-NLS-1$
        continue;
      }

      String text = numSubText + unitString;
      text = (fUseUpperCase) ? text.toUpperCase() : text.toLowerCase();
      CSSCACandidate item = new CSSCACandidate();
      item.setReplacementString(text);
      if (0 < numSubText.length() || text.equals("#")) { //$NON-NLS-1$
        item.setCursorPosition(text.length());
      } else {
        item.setCursorPosition(0);
      }
      item.setDisplayString(text);
      item.setImageType(CSSImageType.VALUE_NUMBER);
      appendSemiColon(item);
      candidates.add(item);
    }
  }

  /**
	 *  
	 */
  private void checkSemiColon() {
    fAppendSemiColon = false;

    ITextRegion targetRegion = fContext.getTargetRegion();
    if (targetRegion != null
        && targetRegion.getType() != CSSRegionContexts.CSS_DECLARATION_DELIMITER) {
      // find trailing ":" or ";"
      // if ":" before ";" is found, add ";"
      RegionIterator iterator = fContext.getRegionIterator();
      IStructuredDocumentRegion container = iterator.getStructuredDocumentRegion();
      while (iterator.hasNext()) {
        ITextRegion region = iterator.next();
        if (iterator.getStructuredDocumentRegion() != container) {
          break;
        }
        if (region.getType() == CSSRegionContexts.CSS_DECLARATION_SEPARATOR) {
          fAppendSemiColon = true;
          break;
        }
      }
      if (!fAppendSemiColon) {
        // second chance:
        // leading IStructuredDocumentRegion is not ";"
        IStructuredDocumentRegion nextStructuredDocumentRegion = CSSUtil.findNextSignificantNode(container);
        if (CSSUtil.getStructuredDocumentRegionType(nextStructuredDocumentRegion) != CSSRegionContexts.CSS_DECLARATION_DELIMITER) {
          fAppendSemiColon = true;
        }
      }
    }
  }

  /**
	 *  
	 */
  private void appendSemiColon(CSSCACandidate item) {
    if (fAppendSemiColon) {
      String replacementString = item.getReplacementString();
      item.setReplacementString(replacementString + ";"); //$NON-NLS-1$
      int cursorPosition = item.getCursorPosition();
      if (replacementString.length() <= cursorPosition) {
        // cursorpos is tail of string
        cursorPosition++;
        item.setCursorPosition(cursorPosition);
      }
    }
  }

  /**
	 *  
	 */
  private void addSemiColon(List candidates) {
    ICSSNode targetNode = fContext.getTargetNode();
    if (targetNode instanceof ICSSStyleDeclItem) {
      ICSSNode firstChild = targetNode.getFirstChild();
      if (firstChild == null) {
        return;
      }
      if (firstChild instanceof IndexedRegion) {
        int startOffset = ((IndexedRegion) firstChild).getStartOffset();
        if (fContext.getCursorPos() <= startOffset) {
          return;
        }
      }
    }

    boolean bAddCloser = false;

    ITextRegion targetRegion = fContext.getTargetRegion();
    if (targetRegion != null
        && targetRegion.getType() != CSSRegionContexts.CSS_DECLARATION_DELIMITER) {
      // find trailing ":" or ";"
      // if ":" before ";" is found, add ";"
      RegionIterator iterator = fContext.getRegionIterator();
      IStructuredDocumentRegion container = iterator.getStructuredDocumentRegion();
      while (iterator.hasNext()) {
        ITextRegion region = iterator.next();
        if (iterator.getStructuredDocumentRegion() != container) {
          break;
        }
        if (region.getType() == CSSRegionContexts.CSS_DECLARATION_SEPARATOR) {
          bAddCloser = true;
          break;
        }
      }
      if (!bAddCloser) {
        // second chance:
        // leading IStructuredDocumentRegion is not ";"
        IStructuredDocumentRegion nextStructuredDocumentRegion = CSSUtil.findNextSignificantNode(container);
        if (CSSUtil.getStructuredDocumentRegionType(nextStructuredDocumentRegion) != CSSRegionContexts.CSS_DECLARATION_DELIMITER) {
          bAddCloser = true;
        }
      }
    }

    if (bAddCloser) {
      CSSCACandidate item = new CSSCACandidate();
      String text = fContext.getTextToReplace() + ";";//$NON-NLS-1$
      item.setReplacementString(text);
      item.setCursorPosition(text.length());
      item.setDisplayString(";");//$NON-NLS-1$
      item.setImageType(null);
      candidates.add(item);
    }
  }

  /**
	 *  
	 */
  private void addString(List candidates, String text) {
    if (!isMatch(text)) {
      return;
    }

    text = (fUseUpperCase) ? text.toUpperCase() : text.toLowerCase();

    CSSCACandidate item = new CSSCACandidate();
    item.setReplacementString(text);
    item.setCursorPosition(text.length());
    item.setDisplayString(text);
    item.setImageType(CSSImageType.VALUE_STRING);
    appendSemiColon(item);
    candidates.add(item);
  }

  private void addImportant(List candidates) {
    ICSSNode targetNode = fContext.getTargetNode();
    while (targetNode instanceof ICSSPrimitiveValue) {
      targetNode = targetNode.getParentNode();
    }
    if (!(targetNode instanceof ICSSStyleDeclItem)) {
      return;
    }
    // 1. has no priority region
    // 2. cursor position is after of last child
    // 3. normal isMatch method
    String priority = ((ICSSStyleDeclItem) targetNode).getPriority();
    if (priority == null || priority.length() == 0) {
      ICSSNode lastChild = targetNode.getLastChild();
      if (lastChild instanceof IndexedRegion) {
        int startOffset = ((IndexedRegion) lastChild).getStartOffset();
        //	int endOffset = ((IndexedRegion)lastChild).getEndOffset();
        if (startOffset < fContext.getCursorPos() && isMatch(IMPORTANT)) {
          CSSCACandidate item = new CSSCACandidate();
          item.setReplacementString(IMPORTANT);
          item.setCursorPosition(IMPORTANT.length());
          item.setDisplayString(IMPORTANT);
          item.setImageType(CSSImageType.VALUE_STRING);
          appendSemiColon(item);
          candidates.add(item);
        }
      }
    }
  }

  /**
   * getCandidates method comment.
   */
  protected Iterator getCandidates() {
    List candidates = new ArrayList();

    checkSemiColon(); // check should add semi-colon or not

    String name = getPropertyName();
    if (name != null) {
      CSSMetaModelUtil util = new CSSMetaModelUtil(fContext.getMetaModel());
      Iterator i = Collections.EMPTY_LIST.iterator();
      if (isFontFaceRule()) {
        CSSMMDescriptor desc = util.getDescriptor(name);
        if (desc != null) {
          i = desc.getValues();
        }
      } else {
        CSSMMProperty prop = util.getProperty(name);
        if (prop != null) {
          i = prop.getValues();
        }
      }
      while (i.hasNext()) {
        CSSMMNode val = (CSSMMNode) i.next();
        String valueType = val.getType();
        if (valueType == CSSMMNode.TYPE_KEYWORD) {
          addString(candidates, val.toString());
        } else if (valueType == CSSMMNode.TYPE_NUMBER) {
          addNumber(candidates, (CSSMMNumber) val);
        } else if (valueType == CSSMMNode.TYPE_FUNCTION) {
          addFunction(candidates, (CSSMMFunction) val);
        }
      }
    }

    addImportant(candidates);
    addSemiColon(candidates);

    return candidates.iterator();
  }

  /**
   * @return java.lang.String
   */
  private String getPropertyName() {
    ICSSNode targetNode = fContext.getTargetNode();
    while (targetNode instanceof ICSSPrimitiveValue) {
      targetNode = targetNode.getParentNode();
    }
    if (targetNode instanceof ICSSStyleDeclItem) {
      return ((ICSSStyleDeclItem) targetNode).getPropertyName();
    } else {
      return null;
    }
  }

  /**
	 *  
	 */
  private boolean isFontFaceRule() {
    ICSSNode targetNode = fContext.getTargetNode();
    while (targetNode instanceof ICSSPrimitiveValue) {
      targetNode = targetNode.getParentNode();
    }
    if (targetNode instanceof ICSSStyleDeclItem) {
      targetNode = targetNode.getParentNode(); // get Declaration
      if (targetNode != null) {
        // inline style has no rule node
        targetNode = targetNode.getParentNode(); // get rule
      }
    }
    return (targetNode instanceof CSSFontFaceRule);
  }
}
