/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.correction;

import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.ui.internal.contentassist.ContentAssistUtils;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.eclipse.wst.xml.ui.internal.Logger;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImageHelper;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImages;

public class InsertRequiredAttrsQuickAssistProposal implements ICompletionProposal,
    ICompletionProposalExtension2 {
  private final List fRequiredAttrs;

  /**
   * @param requiredAttrs
   */
  public InsertRequiredAttrsQuickAssistProposal(List requiredAttrs) {
    fRequiredAttrs = requiredAttrs;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.text.contentassist.ICompletionProposal#apply(org.eclipse.jface.text.IDocument
   * )
   */
  public void apply(IDocument document) {
    // not implemented?
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#apply(org.eclipse.jface.
   * text.ITextViewer, char, int, int)
   */
  public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
    IDOMNode node = (IDOMNode) ContentAssistUtils.getNodeAt(viewer, offset);
    IStructuredDocumentRegion startStructuredDocumentRegion = node.getStartStructuredDocumentRegion();
    int index = startStructuredDocumentRegion.getEndOffset();
    ITextRegion lastRegion = startStructuredDocumentRegion.getLastRegion();
    if (lastRegion.getType() == DOMRegionContext.XML_TAG_CLOSE) {
      index--;
      lastRegion = startStructuredDocumentRegion.getRegionAtCharacterOffset(index - 1);
    } else if (lastRegion.getType() == DOMRegionContext.XML_EMPTY_TAG_CLOSE) {
      index = index - 2;
      lastRegion = startStructuredDocumentRegion.getRegionAtCharacterOffset(index - 1);
    }
    MultiTextEdit multiTextEdit = new MultiTextEdit();
    try {
      for (int i = 0; i < fRequiredAttrs.size(); i++) {
        CMAttributeDeclaration attrDecl = (CMAttributeDeclaration) fRequiredAttrs.get(i);
        String requiredAttributeName = attrDecl.getAttrName();
        String defaultValue = attrDecl.getDefaultValue();
        if (defaultValue == null) {
          defaultValue = ""; //$NON-NLS-1$
        }
        String nameAndDefaultValue = " "; //$NON-NLS-1$
        if ((i == 0) && (lastRegion.getLength() > lastRegion.getTextLength())) {
          nameAndDefaultValue = ""; //$NON-NLS-1$
        }
        nameAndDefaultValue += requiredAttributeName + "=\"" + defaultValue + "\""; //$NON-NLS-1$//$NON-NLS-2$
        multiTextEdit.addChild(new InsertEdit(index, nameAndDefaultValue));
        // BUG3381: MultiTextEdit applies all child TextEdit's basing
        // on offsets
        // in the document before the first TextEdit, not after each
        // child TextEdit. Therefore, do not need to advance the
        // index.
        // index += nameAndDefaultValue.length();
      }
      multiTextEdit.apply(viewer.getDocument());
    } catch (BadLocationException e) {
      // log, for now, unless we find there's reasons why we get some
      // here.
      Logger.log(Logger.INFO, e.getMessage());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
   */
  public String getAdditionalProposalInfo() {
    return XMLUIMessages.InsertRequiredAttrsQuickAssistProposal_0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getContextInformation()
   */
  public IContextInformation getContextInformation() {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getDisplayString()
   */
  public String getDisplayString() {
    return XMLUIMessages.InsertRequiredAttrsQuickAssistProposal_1;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getImage()
   */
  public Image getImage() {
    // return JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
    return XMLEditorPluginImageHelper.getInstance().getImage(
        XMLEditorPluginImages.IMG_OBJ_ADD_CORRECTION);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.text.contentassist.ICompletionProposal#getSelection(org.eclipse.jface.text
   * .IDocument)
   */
  public Point getSelection(IDocument document) {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#selected(org.eclipse.jface
   * .text.ITextViewer, boolean)
   */
  public void selected(ITextViewer viewer, boolean smartToggle) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#unselected(org.eclipse.jface
   * .text.ITextViewer)
   */
  public void unselected(ITextViewer viewer) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#validate(org.eclipse.jface
   * .text.IDocument, int, org.eclipse.jface.text.DocumentEvent)
   */
  public boolean validate(IDocument document, int offset, DocumentEvent event) {
    return false;
  }
}
