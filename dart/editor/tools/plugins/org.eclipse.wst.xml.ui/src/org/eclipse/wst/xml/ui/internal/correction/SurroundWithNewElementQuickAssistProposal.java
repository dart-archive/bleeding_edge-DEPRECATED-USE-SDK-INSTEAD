/*******************************************************************************
 * Copyright (c) 2001, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.correction;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;
import org.eclipse.wst.sse.core.internal.format.IStructuredFormatProcessor;
import org.eclipse.wst.sse.ui.internal.contentassist.ContentAssistUtils;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.core.internal.provisional.format.FormatProcessorXML;
import org.eclipse.wst.xml.ui.internal.Logger;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImageHelper;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImages;
import org.w3c.dom.Node;

public class SurroundWithNewElementQuickAssistProposal extends RenameInFileQuickAssistProposal {
  private static final String ELEMENT_NAME = "element"; //$NON-NLS-1$

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#apply(org.eclipse.jface.
   * text.ITextViewer, char, int, int)
   */
  public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
    try {
      int startTagOffset = offset;
      int endTagOffset = offset + viewer.getSelectedRange().y;

      // surround the node if no selection
      if (startTagOffset == endTagOffset) {
        IDOMNode cursorNode = (IDOMNode) ContentAssistUtils.getNodeAt(viewer, offset);
        // use parent node if text node is empty
        if ((cursorNode.getNodeType() == Node.TEXT_NODE)
            && (cursorNode.getNodeValue().trim().length() == 0)) {
          cursorNode = (IDOMNode) cursorNode.getParentNode();
        }

        startTagOffset = cursorNode.getStartOffset();
        endTagOffset = cursorNode.getEndOffset();
      }

      // insert new element
      MultiTextEdit multiTextEdit = new MultiTextEdit();
      // element tag name cannot be DBCS, do not translate "<element>"
      // and "</element>"
      final String startElement = "<" + ELEMENT_NAME + ">"; //$NON-NLS-1$ //$NON-NLS-2$
      multiTextEdit.addChild(new InsertEdit(startTagOffset, startElement));
      multiTextEdit.addChild(new InsertEdit(endTagOffset, "</" + ELEMENT_NAME + ">")); //$NON-NLS-1$ //$NON-NLS-2$
      multiTextEdit.apply(viewer.getDocument());
      Position start = new Position(startTagOffset);
      Position end = new Position(endTagOffset + startElement.length());

      try {
        viewer.getDocument().addPosition(start);
        viewer.getDocument().addPosition(end);

        // get new element node
        IDOMNode newElementNode = (IDOMNode) ContentAssistUtils.getNodeAt(viewer, startTagOffset);
        IStructuredFormatProcessor formatProcessor = new FormatProcessorXML();
        formatProcessor.formatNode(newElementNode);

        // rename new element
        apply(viewer, trigger, stateMask, start, end, ELEMENT_NAME.length());
      } finally {
        viewer.getDocument().removePosition(start);
        viewer.getDocument().removePosition(end);
      }
    } catch (MalformedTreeException e) {
      // log for now, unless find reason not to
      Logger.log(Logger.INFO, e.getMessage());
    } catch (BadLocationException e) {
      // log for now, unless find reason not to
      Logger.log(Logger.INFO, e.getMessage());
    }
  }

  private void apply(ITextViewer viewer, char trigger, int stateMask, Position start, Position end,
      int length) {
    IDocument document = viewer.getDocument();
    LinkedPositionGroup group = new LinkedPositionGroup();
    try {
      group.addPosition(new LinkedPosition(document, start.offset + 1, length, 0)); // offset by 1 for <
      group.addPosition(new LinkedPosition(document, end.offset + 2, length, 1)); // offset by 2 for </

      if (viewer instanceof ITextViewerExtension)
        ((ITextViewerExtension) viewer).setRedraw(true);

      LinkedModeModel linkedModeModel = new LinkedModeModel();
      linkedModeModel.addGroup(group);
      linkedModeModel.forceInstall();

      LinkedModeUI ui = new EditorLinkedModeUI(linkedModeModel, viewer);
      ui.setExitPosition(viewer, start.offset, 0, LinkedPositionGroup.NO_STOP);
      ui.enter();

      fSelectedRegion = ui.getSelectedRegion();
    } catch (BadLocationException e) {
      // log for now, unless find reason not to
      Logger.log(Logger.INFO, e.getMessage());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
   */
  public String getAdditionalProposalInfo() {
    return XMLUIMessages.SurroundWithNewElementQuickAssistProposal_0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getDisplayString()
   */
  public String getDisplayString() {
    return XMLUIMessages.SurroundWithNewElementQuickAssistProposal_1;
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
}
