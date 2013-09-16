/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/

package org.eclipse.wst.xml.ui.internal.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.ui.internal.Logger;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;

/**
 * Toggle comment action delegate for XML editor
 */
public class ToggleCommentActionXMLDelegate extends AbstractCommentActionXMLDelegate {
  public void init(IAction action) {
    if (action != null) {
      action.setText(XMLUIMessages.ToggleComment_label);
      action.setToolTipText(XMLUIMessages.ToggleComment_tooltip);
      action.setDescription(XMLUIMessages.ToggleComment_description);
    }
  }

  void processAction(IDocument document, ITextSelection textSelection) {
    // get text selection lines info
    int selectionStartLine = textSelection.getStartLine();
    int selectionEndLine = textSelection.getEndLine();
    try {
      int selectionEndLineOffset = document.getLineOffset(selectionEndLine);
      int selectionEndOffset = textSelection.getOffset() + textSelection.getLength();

      // adjust selection end line
      if ((selectionEndLine > selectionStartLine) && (selectionEndLineOffset == selectionEndOffset)) {
        selectionEndLine--;
      }

    } catch (BadLocationException e) {
      Logger.log(Logger.WARNING_DEBUG, e.getMessage(), e);
    }

    // save the selection position since it will be changing
    Position selectionPosition = null;
    boolean updateStartOffset = false;
    try {
      selectionPosition = new Position(textSelection.getOffset(), textSelection.getLength());
      document.addPosition(selectionPosition);

      // extra check if commenting from beginning of line
      int selectionStartLineOffset = document.getLineOffset(selectionStartLine);
      if ((textSelection.getLength() > 0)
          && (selectionStartLineOffset == textSelection.getOffset())
          && !isCommentLine(document, selectionStartLine)) {
        updateStartOffset = true;
      }
    } catch (BadLocationException e) {
      Logger.log(Logger.WARNING_DEBUG, e.getMessage(), e);
    }

    processAction(document, selectionStartLine, selectionEndLine);

    updateCurrentSelection(selectionPosition, document, updateStartOffset);
  }

  private void processAction(IDocument document, int selectionStartLine, int selectionEndLine) {
    IStructuredModel model = StructuredModelManager.getModelManager().getExistingModelForEdit(
        document);
    if (model != null) {
      try {
        model.beginRecording(this, XMLUIMessages.ToggleComment_tooltip);
        model.aboutToChangeModel();

        for (int i = selectionStartLine; i <= selectionEndLine; i++) {
          try {
            if (document.getLineLength(i) > 0) {
              if (isCommentLine(document, i)) {
                int lineOffset = document.getLineOffset(i);
                IRegion region = document.getLineInformation(i);
                String string = document.get(region.getOffset(), region.getLength());
                int openCommentOffset = lineOffset + string.indexOf(OPEN_COMMENT);
                int closeCommentOffset = lineOffset + string.indexOf(CLOSE_COMMENT)
                    - OPEN_COMMENT.length();
                uncomment(document, openCommentOffset, closeCommentOffset);
              } else {
                int openCommentOffset = document.getLineOffset(i);
                int lineDelimiterLength = document.getLineDelimiter(i) == null ? 0
                    : document.getLineDelimiter(i).length();
                int closeCommentOffset = openCommentOffset + document.getLineLength(i)
                    - lineDelimiterLength + OPEN_COMMENT.length();
                comment(document, openCommentOffset, closeCommentOffset);
              }
            }
          } catch (BadLocationException e) {
            Logger.log(Logger.WARNING_DEBUG, e.getMessage(), e);
          }
        }
      } finally {
        model.changedModel();
        model.endRecording(this);
        model.releaseFromEdit();
      }
    }
  }

  private boolean isCommentLine(IDocument document, int line) {
    boolean isComment = false;

    try {
      IRegion region = document.getLineInformation(line);
      String string = document.get(region.getOffset(), region.getLength()).trim();
      isComment = (string.length() >= OPEN_COMMENT.length() + CLOSE_COMMENT.length())
          && string.startsWith(OPEN_COMMENT) && string.endsWith(CLOSE_COMMENT);
    } catch (BadLocationException e) {
      Logger.log(Logger.WARNING_DEBUG, e.getMessage(), e);
    }
    return isComment;
  }

  private void comment(IDocument document, int openCommentOffset, int closeCommentOffset) {
    try {
      document.replace(openCommentOffset, 0, OPEN_COMMENT);
      document.replace(closeCommentOffset, 0, CLOSE_COMMENT);
      removeOpenCloseComments(document, openCommentOffset + OPEN_COMMENT.length(),
          closeCommentOffset - openCommentOffset - CLOSE_COMMENT.length());
    } catch (BadLocationException e) {
      Logger.log(Logger.WARNING_DEBUG, e.getMessage(), e);
    }
  }

  private void uncomment(IDocument document, int openCommentOffset, int closeCommentOffset) {
    try {
      document.replace(openCommentOffset, OPEN_COMMENT.length(), ""); //$NON-NLS-1$
      document.replace(closeCommentOffset, CLOSE_COMMENT.length(), ""); //$NON-NLS-1$
    } catch (BadLocationException e) {
      Logger.log(Logger.WARNING_DEBUG, e.getMessage(), e);
    }
  }

  private void updateCurrentSelection(Position selectionPosition, IDocument document,
      boolean updateStartOffset) {
    if (fEditor instanceof ITextEditor) {
      // update the selection if text selection changed
      if (selectionPosition != null) {
        ITextSelection selection = null;
        if (updateStartOffset) {
          selection = new TextSelection(document, selectionPosition.getOffset()
              - OPEN_COMMENT.length(), selectionPosition.getLength() + OPEN_COMMENT.length());
        } else {
          selection = new TextSelection(document, selectionPosition.getOffset(),
              selectionPosition.getLength());
        }
        ISelectionProvider provider = ((ITextEditor) fEditor).getSelectionProvider();
        if (provider != null) {
          provider.setSelection(selection);
        }
        document.removePosition(selectionPosition);
      }
    }
  }
}
