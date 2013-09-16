/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.handlers;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.Logger;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.comment.CommentingStrategy;
import org.eclipse.wst.sse.ui.internal.comment.CommentingStrategyRegistry;

/**
 * <p>
 * A comment handler to add block comments
 * </p>
 */
public final class AddBlockCommentHandler extends AbstractCommentHandler {

  /**
   * @see org.eclipse.wst.sse.ui.internal.handlers.AbstractCommentHandler#processAction(org.eclipse.ui.texteditor.ITextEditor,
   *      org.eclipse.jface.text.IDocument, org.eclipse.jface.text.ITextSelection)
   */
  protected void processAction(ITextEditor textEditor, IStructuredDocument document,
      ITextSelection textSelection) {
    IStructuredModel model = null;
    boolean changed = false;
    DocumentRewriteSession session = null;

    try {
      model = StructuredModelManager.getModelManager().getModelForEdit(document);
      if (model != null) {
        //makes it so one undo will undo all the edits to the document
        model.beginRecording(this, SSEUIMessages.AddBlockComment_label,
            SSEUIMessages.AddBlockComment_description);

        //keeps listeners from doing anything until updates are all done
        model.aboutToChangeModel();
        if (document instanceof IDocumentExtension4) {
          session = ((IDocumentExtension4) document).startRewriteSession(DocumentRewriteSessionType.UNRESTRICTED);
        }
        changed = true;

        ITypedRegion[] typedRegions = document.computePartitioning(textSelection.getOffset(),
            textSelection.getLength());
        CommentingStrategy commentType = CommentingStrategyRegistry.getDefault().getBlockCommentingStrategy(
            model.getContentTypeIdentifier(), typedRegions);

        if (commentType != null) {
          commentType.apply(document, textSelection.getOffset(), textSelection.getLength());
        }
      }
    } catch (BadLocationException e) {
      Logger.logException("The given selection " + textSelection + " must be invalid", e); //$NON-NLS-1$ //$NON-NLS-2$
    } finally {
      //clean everything up
      if (session != null && document instanceof IDocumentExtension4) {
        ((IDocumentExtension4) document).stopRewriteSession(session);
      }

      if (model != null) {
        model.endRecording(this);
        if (changed) {
          model.changedModel();
        }
        model.releaseFromEdit();
      }
    }
  }
}
