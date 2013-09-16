/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/

package org.eclipse.wst.css.ui.internal.contentassist;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.wst.css.core.internal.metamodel.CSSMetaModel;
import org.eclipse.wst.css.core.internal.metamodel.util.CSSMetaModelFinder;
import org.eclipse.wst.css.core.internal.parserz.CSSRegionContexts;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSDocument;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSModel;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSNode;
import org.eclipse.wst.css.core.internal.util.CSSUtil;
import org.eclipse.wst.css.core.internal.util.RegionIterator;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;

class CSSContentAssistContext {

  private int fReplaceBegin = -1;
  private String fTextToReplace = null;
  private String fTextToCompare = null;
  private int fTargetPos = -1;
  private ICSSNode fTargetNode = null;
  private int fCursorPos = -1;
  private int fLength = -1;
  private IStructuredDocument fStructuredDocument = null;
  private int fDocumentOffset = 0;
  private char fQuote = 0;
  private ICSSModel fModel = null;
  private boolean fSelected = false;

  /**
	 *  
	 */
  private CSSContentAssistContext() {
    super();
  }

  /**
	 *  
	 */
  CSSContentAssistContext(int documentPosition, ICSSNode node, int documentOffset, char quote) {
    this();
    fCursorPos = documentPosition;
    fDocumentOffset = documentOffset;
    fQuote = quote;
    initialize(node.getOwnerDocument());
  }

  CSSContentAssistContext(int documentPosition, ICSSNode node, int documentOffset, int length,
      char quote) {
    this();
    fCursorPos = documentPosition;
    fDocumentOffset = documentOffset;
    fQuote = quote;
    fLength = length;
    initialize(node.getOwnerDocument());
  }

  CSSContentAssistContext(int documentPosition, ICSSNode node, int documentOffset, char quote,
      boolean selected) {
    this();
    fCursorPos = documentPosition;
    fDocumentOffset = documentOffset;
    fQuote = quote;
    fSelected = selected;
    initialize(node.getOwnerDocument());
  }

  /**
   * @return int
   */
  int getCursorPos() {
    return fCursorPos;
  }

  /**
   * @return int
   */
  int getDocumentOffset() {
    return fDocumentOffset;
  }

  IStructuredDocument getStructuredDocument() {
    return fStructuredDocument;
  }

  ICSSModel getModel() {
    return fModel;
  }

  private ICSSNode getNodeAt(int offset) {
    return (ICSSNode) ((fModel != null) ? fModel.getIndexedRegion(offset) : null);
  }

  /**
   * @return char
   */
  char getQuoteOfStyleAttribute() {
    return fQuote;
  }

  ITextRegion getRegionByOffset(int offset) {
    ITextRegion region = null;
    if (fStructuredDocument != null) {
      IStructuredDocumentRegion flatNode = fStructuredDocument.getRegionAtCharacterOffset(offset);
      if (flatNode != null) {
        //if its a quoted region and at the beginning of a node then get the node before it
        if (offset == flatNode.getStartOffset() && fQuote != 0) {
          flatNode = fStructuredDocument.getRegionAtCharacterOffset(offset - 1);
        }

        //if its the end offset of the node then to get the region need to step back one
        if (offset == flatNode.getEndOffset()) {
          region = flatNode.getRegionAtCharacterOffset(offset - 1);
        } else {
          region = flatNode.getRegionAtCharacterOffset(offset);
        }
      }
    }
    return region;
  }

  /**
	 *  
	 */
  //	String getRegionText() {
  //		ITextRegion targetRegion = getTargetRegion();
  //		if (targetRegion != null) {
  //			return targetRegion.getText();
  //		} else {
  //			return ""; //$NON-NLS-1$
  //		}
  //	}
  /**
	 *  
	 */
  int getReplaceBegin() {
    return fReplaceBegin;
  }

  ICSSNode getTargetNode() {
    return fTargetNode;
  }

  private int getTargetPos() {
    return fTargetPos;
  }

  ITextRegion getTargetRegion() {
    return getRegionByOffset(getTargetPos());
  }

  private IStructuredDocumentRegion getTargetDocumentRegion() {
    return getDocumentRegionByOffset(getTargetPos());
  }

  private IStructuredDocumentRegion getDocumentRegionByOffset(int offset) {
    return (fStructuredDocument != null) ? fStructuredDocument.getRegionAtCharacterOffset(offset)
        : null;
  }

  ITextRegion getTargetRegionPrevious() {
    ITextRegion previousRegion = null;
    ITextRegion targetRegion = getTargetRegion();
    RegionIterator iterator = null;
    if (targetRegion == null) {
      if (0 < fCursorPos) {
        iterator = new RegionIterator(fStructuredDocument, fCursorPos - 1);
      }
    } else {
      iterator = getRegionIterator();
      if (iterator.hasPrev()) {
        iterator.prev();
      } else {
        iterator = null;
      }
    }
    if (iterator != null) {
      while (iterator.hasPrev()) {
        ITextRegion region = iterator.prev();
        String type = region.getType();
        if (type != CSSRegionContexts.CSS_S && type != CSSRegionContexts.CSS_COMMENT
            && type != CSSRegionContexts.CSS_CDO && type != CSSRegionContexts.CSS_CDC) {
          previousRegion = region;
          break;
        }
      }
    }

    return previousRegion;
  }

  /**
   * @return java.lang.String
   */
  String getTextToCompare() {
    return fTextToCompare;
  }

  /**
	 *  
	 */
  String getTextToReplace() {
    return fTextToReplace;
  }

  /**
	 *  
	 */
  private void initialize(ICSSDocument doc) {
    if (doc == null) {
      return;
    }
    ICSSModel model = doc.getModel();
    fModel = model;
    fStructuredDocument = model.getStructuredDocument();

    initializeTargetPos();
    initializeTargetText();
    initializeTargetNode();
  }

  /**
	 *  
	 */
  private void initializeTargetNode() {
    if (fCursorPos == 0) {
      fTargetNode = fModel.getDocument();
      return;
    }

    // find edge of tree node
    ICSSNode cursorNode = getNodeAt(fCursorPos);
    if (cursorNode == null) { // end of document
      cursorNode = fModel.getDocument();
    }
    ICSSNode node = null;
    IStructuredDocumentRegion flatNode = fStructuredDocument.getRegionAtCharacterOffset(fCursorPos - 1);
    while (flatNode != null && (node = getNodeAt(flatNode.getStartOffset())) == cursorNode
        && ((IndexedRegion) node).getStartOffset() != flatNode.getStartOffset()) {
      flatNode = flatNode.getPrevious();
    }
    if (flatNode == null) { // top of document
      fTargetNode = (node == null) ? fModel.getDocument() : node;
      return;
    }
    //   v<--|
    //   AAAAAA
    // BBBBBBBBBB cursorNode:A , node:B -> target is A
    if (cursorNode != null) {
      for (ICSSNode parent = cursorNode.getParentNode(); parent != null; parent = parent.getParentNode()) {
        if (parent == cursorNode) {
          fTargetNode = cursorNode;
          return;
        }
      }
    }
    //     v<--|
    //   AAA
    // BBBBBBBBBB cursorNode:B , node:A -> depend on A's node type
    short nodeType = node.getNodeType();
    if (nodeType == ICSSNode.STYLEDECLITEM_NODE || nodeType == ICSSNode.CHARSETRULE_NODE
        || nodeType == ICSSNode.IMPORTRULE_NODE) {
      String type = CSSUtil.getStructuredDocumentRegionType(flatNode);
      if (type == CSSRegionContexts.CSS_DELIMITER
          || type == CSSRegionContexts.CSS_DECLARATION_DELIMITER) {
        fTargetNode = node.getParentNode();
      } else {
        fTargetNode = node;
      }
      //			fTargetNode = (bOverSemiColon) ? node.getParentNode() : node;
    } else if (CSSUtil.getStructuredDocumentRegionType(flatNode) == CSSRegionContexts.CSS_RBRACE) {
      fTargetNode = node.getParentNode();
    } else {
      fTargetNode = node;
    }

    return;
  }

  /**
	 *  
	 */
  private void initializeTargetPos() {
    if (fCursorPos == 0 || isSpecialDelimiterRegion(fCursorPos - 1)) {
      fTargetPos = fCursorPos;
    } else {
      fTargetPos = fCursorPos - 1;
    }

    //deal with the leading quote
    if (fQuote != 0) {
      fTargetPos--;
    }
  }

  /**
	 *  
	 */
  private void initializeTargetText() {
    ITextRegion targetRegion = getTargetRegion();
    IStructuredDocumentRegion documentRegion = getTargetDocumentRegion();
    if (targetRegion == null) {
      fReplaceBegin = fCursorPos;
      fTextToReplace = ""; //$NON-NLS-1$
      fTextToCompare = ""; //$NON-NLS-1$
    } else {
      String regionText = documentRegion.getText(targetRegion);
      int regionStart = documentRegion.getStartOffset(targetRegion);
      if (!fSelected
          && (regionStart == fCursorPos || regionText.trim().length() == 0 || regionStart
              + regionText.length() - 1 < this.fTargetPos)) {
        // to insertion
        fReplaceBegin = fCursorPos;
        fTextToReplace = ""; //$NON-NLS-1$
        fTextToCompare = ""; //$NON-NLS-1$
        try {
          if (fLength > 0)
            fTextToReplace = fStructuredDocument.get(fReplaceBegin, fLength);
        } catch (BadLocationException e) {
        }
      } else {
        // to replace
        fReplaceBegin = regionStart;
        fTextToReplace = regionText;

        //math to deal with leading quote
        int matchLength = fCursorPos - fReplaceBegin;
        if (fQuote != 0) {
          matchLength--;
          fReplaceBegin++;
        }

        if (fCursorPos >= fReplaceBegin && regionText.trim().length() >= (matchLength))
          fTextToCompare = regionText.substring(0, matchLength);
        else
          fTextToCompare = ""; //$NON-NLS-1$
      }
    }
  }

  /**
	 *  
	 */
  private boolean isSpecialDelimiterRegion(int pos) {
    ITextRegion region = getRegionByOffset(pos);
    String type = null;
    if (region != null) {
      type = region.getType();
    }
    return (type != null)
        && ((type == CSSRegionContexts.CSS_LBRACE || type == CSSRegionContexts.CSS_RBRACE
            || type == CSSRegionContexts.CSS_DELIMITER
            || type == CSSRegionContexts.CSS_DECLARATION_SEPARATOR
            || type == CSSRegionContexts.CSS_DECLARATION_DELIMITER
            || type == CSSRegionContexts.CSS_DECLARATION_VALUE_OPERATOR || type == CSSRegionContexts.CSS_S));
  }

  /**
	 *  
	 */
  boolean isTargetPosAfterOf(String regionType) {
    int start = ((IndexedRegion) fTargetNode).getStartOffset();
    if (start < 0 || ((IndexedRegion) fTargetNode).getEndOffset() <= 0) {
      return false;
    }

    RegionIterator iRegion = new RegionIterator(fStructuredDocument, start);
    while (iRegion.hasNext()) {
      ITextRegion region = iRegion.next();
      if (fTargetPos < iRegion.getStructuredDocumentRegion().getTextEndOffset(region)) {
        break;
      }
      if (region.getType() == regionType) {
        return true;
      }
    }

    return false;
  }

  /**
	 *  
	 */
  boolean isTargetPosBeforeOf(String regionType) {
    return !isTargetPosAfterOf(regionType);
  }

  /**
   * @return boolean
   * @param regionType java.lang.String
   */
  boolean targetFollows(String regionType) {
    RegionIterator iRegion;
    ITextRegion region = null;
    if (fStructuredDocument.getLength() <= fTargetPos) {
      iRegion = new RegionIterator(fStructuredDocument, fStructuredDocument.getLength() - 1);
    } else {
      iRegion = new RegionIterator(fStructuredDocument, fTargetPos);
      try {
        if (!Character.isWhitespace(fStructuredDocument.getChar(fTargetPos)) && iRegion.hasPrev()) {
          region = iRegion.prev();
        }
      } catch (BadLocationException e) {
        iRegion = new RegionIterator(fStructuredDocument, fStructuredDocument.getLength() - 1);
      }
    }
    while (iRegion.hasPrev()) {
      region = iRegion.prev();
      String type = region.getType();
      if (type == CSSRegionContexts.CSS_S || type == CSSRegionContexts.CSS_COMMENT) {
        continue;
      } else {
        break;
      }
    }
    if (region != null && region.getType() == regionType) {
      return true;
    } else {
      return false;
    }
  }

  /**
	 *  
	 */
  boolean targetHas(String regionType) {
    int end = ((IndexedRegion) fTargetNode).getEndOffset();
    if (getTargetPos() < 0 || end <= 0) {
      return false;
    }
    RegionIterator iRegion = new RegionIterator(fStructuredDocument, getTargetPos());
    while (iRegion.hasNext()) {
      ITextRegion region = iRegion.next();
      if (end <= iRegion.getStructuredDocumentRegion().getStartOffset(region)) {
        break;
      }
      if (region.getType() == regionType) {
        return true;
      }
    }
    return false;
  }

  RegionIterator getRegionIterator() {
    return new RegionIterator(getStructuredDocument(), getTargetPos());
  }

  /**
	 *  
	 */
  CSSMetaModel getMetaModel() {
    CSSMetaModelFinder finder = CSSMetaModelFinder.getInstance();
    ICSSModel model = getModel();
    if (model.getOwnerDOMNode() != null) {
      return finder.findMetaModelFor(model.getOwnerDOMNode());
    }
    return finder.findMetaModelFor(getModel());
  }
}
