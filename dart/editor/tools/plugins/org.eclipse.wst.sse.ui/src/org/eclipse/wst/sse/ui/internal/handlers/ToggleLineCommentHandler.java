/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.handlers;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.StructuredTextViewer;
import org.eclipse.wst.sse.ui.internal.comment.BlockCommentingStrategy;
import org.eclipse.wst.sse.ui.internal.comment.CommentingStrategy;
import org.eclipse.wst.sse.ui.internal.comment.CommentingStrategyRegistry;
import org.eclipse.wst.sse.ui.internal.comment.LineCommentingStrategy;

import java.lang.reflect.InvocationTargetException;

/**
 * <p>
 * A comment handler to toggle line comments, this means that if a comment already exists on a line
 * then toggling it will remove the comment, if the line in question is not already commented then
 * it will not be commented. If multiple lines are selected each will be commented separately. The
 * handler first attempts to find a {@link LineCommentingStrategy} for a line, if it can not find
 * one then it will try and find a {@link BlockCommentingStrategy} to wrap just that line in.
 * </p>
 * <p>
 * If a great number of lines are being toggled then a progress dialog will be displayed because
 * this can be a timely process
 * </p>
 */
public final class ToggleLineCommentHandler extends AbstractCommentHandler {
  /** if toggling more then this many lines then use a busy indicator */
  private static final int TOGGLE_LINES_MAX_NO_BUSY_INDICATOR = 10;

  /**
   * @see org.eclipse.wst.sse.ui.internal.handlers.AbstractCommentHandler#processAction(org.eclipse.ui.texteditor.ITextEditor,
   *      org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument,
   *      org.eclipse.jface.text.ITextSelection)
   */
  protected void processAction(ITextEditor textEditor, final IStructuredDocument document,
      ITextSelection textSelection) {

    IStructuredModel model = null;
    DocumentRewriteSession session = null;
    boolean changed = false;

    try {
      // get text selection lines info
      int selectionStartLine = textSelection.getStartLine();
      int selectionEndLine = textSelection.getEndLine();

      int selectionEndLineOffset = document.getLineOffset(selectionEndLine);
      int selectionEndOffset = textSelection.getOffset() + textSelection.getLength();

      // adjust selection end line
      if ((selectionEndLine > selectionStartLine) && (selectionEndLineOffset == selectionEndOffset)) {
        selectionEndLine--;
      }

      // save the selection position since it will be changing
      Position selectionPosition = null;
      selectionPosition = new Position(textSelection.getOffset(), textSelection.getLength());
      document.addPosition(selectionPosition);

      model = StructuredModelManager.getModelManager().getModelForEdit(document);
      if (model != null) {
        //makes it so one undo will undo all the edits to the document
        model.beginRecording(this, SSEUIMessages.ToggleComment_label,
            SSEUIMessages.ToggleComment_description);

        //keeps listeners from doing anything until updates are all done
        model.aboutToChangeModel();
        if (document instanceof IDocumentExtension4) {
          session = ((IDocumentExtension4) document).startRewriteSession(DocumentRewriteSessionType.UNRESTRICTED);
        }
        changed = true;

        //get the display for the editor if we can
        Display display = null;
        if (textEditor instanceof StructuredTextEditor) {
          StructuredTextViewer viewer = ((StructuredTextEditor) textEditor).getTextViewer();
          if (viewer != null) {
            display = viewer.getControl().getDisplay();
          }
        }

        //create the toggling operation
        IRunnableWithProgress toggleCommentsRunnable = new ToggleLinesRunnable(
            model.getContentTypeIdentifier(), document, selectionStartLine, selectionEndLine,
            display);

        //if toggling lots of lines then use progress monitor else just run the operation
        if ((selectionEndLine - selectionStartLine) > TOGGLE_LINES_MAX_NO_BUSY_INDICATOR
            && display != null) {
          ProgressMonitorDialog dialog = new ProgressMonitorDialog(display.getActiveShell());
          dialog.run(false, true, toggleCommentsRunnable);
        } else {
          toggleCommentsRunnable.run(new NullProgressMonitor());
        }
      }
    } catch (InvocationTargetException e) {
      Logger.logException("Problem running toggle comment progess dialog.", e); //$NON-NLS-1$
    } catch (InterruptedException e) {
      Logger.logException("Problem running toggle comment progess dialog.", e); //$NON-NLS-1$
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

  /**
   * <p>
   * The actual line toggling takes place in a runnable so it can be run as part of a progress
   * dialog if there are many lines to toggle and thus the operation will take a noticeable amount
   * of time the user should be aware of, this also allows for the operation to be canceled by the
   * user
   * </p>
   */
  private static class ToggleLinesRunnable implements IRunnableWithProgress {
    /** the content type for the document being commented */
    private String fContentType;

    /** the document that the lines will be toggled on */
    private IStructuredDocument fDocument;

    /** the first line in the document to toggle */
    private int fSelectionStartLine;

    /** the last line in the document to toggle */
    private int fSelectionEndLine;

    /** the display, so that it can be updated during a long operation */
    private Display fDisplay;

    /**
     * @param model {@link IStructuredModel} that the lines will be toggled on
     * @param document {@link IDocument} that the lines will be toggled on
     * @param selectionStartLine first line in the document to toggle
     * @param selectionEndLine last line in the document to toggle
     * @param display {@link Display}, so that it can be updated during a long operation
     */
    protected ToggleLinesRunnable(String contentTypeIdentifier, IStructuredDocument document,
        int selectionStartLine, int selectionEndLine, Display display) {

      this.fContentType = contentTypeIdentifier;
      this.fDocument = document;
      this.fSelectionStartLine = selectionStartLine;
      this.fSelectionEndLine = selectionEndLine;
      this.fDisplay = display;
    }

    /**
     * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void run(IProgressMonitor monitor) {
      //start work
      monitor.beginTask(SSEUIMessages.ToggleComment_progress, this.fSelectionEndLine
          - this.fSelectionStartLine);
      try {
        final CommentingStrategy[] strategies = new CommentingStrategy[fSelectionEndLine
            - fSelectionStartLine + 1];
        final int[] regions = new int[fSelectionEndLine - fSelectionStartLine + 1];
        boolean shouldComment = false;
        int strategyCount = 0;
        //toggle each line so long as task not canceled
        for (int line = this.fSelectionStartLine; line <= this.fSelectionEndLine
            && !monitor.isCanceled(); ++line) {

          //allows the user to be able to click the cancel button
          readAndDispatch(this.fDisplay);

          //get the line region
          IRegion lineRegion = this.fDocument.getLineInformation(line);

          //don't toggle empty lines
          String content = this.fDocument.get(lineRegion.getOffset(), lineRegion.getLength());
          if (content.trim().length() > 0) {
            //try to get a line comment type
            ITypedRegion[] lineTypedRegions = this.fDocument.computePartitioning(
                lineRegion.getOffset(), lineRegion.getLength());
            CommentingStrategy commentType = CommentingStrategyRegistry.getDefault().getLineCommentingStrategy(
                this.fContentType, lineTypedRegions);

            //could not find line comment type so find block comment type to use on line
            if (commentType == null) {
              commentType = CommentingStrategyRegistry.getDefault().getBlockCommentingStrategy(
                  this.fContentType, lineTypedRegions);
            }

            //toggle the comment on the line
            if (commentType != null) {
              strategies[strategyCount] = commentType;
              regions[strategyCount++] = line;
              if (!shouldComment
                  && !commentType.alreadyCommenting(this.fDocument, lineTypedRegions)) {
                shouldComment = true;
              }
            }
          }
          monitor.worked(1);
        }
        for (int i = 0; i < strategyCount; i++) {
          final IRegion lineRegion = fDocument.getLineInformation(regions[i]);
          if (shouldComment) {
            strategies[i].apply(this.fDocument, lineRegion.getOffset(), lineRegion.getLength());
          } else {
            strategies[i].remove(this.fDocument, lineRegion.getOffset(), lineRegion.getLength(),
                true);
          }
          monitor.worked(1);
        }
      } catch (BadLocationException e) {
        Logger.logException("Bad location while toggling comments.", e); //$NON-NLS-1$
      }
      //done work
      monitor.done();
    }

    /**
     * <p>
     * When calling {@link Display#readAndDispatch()} the game is off as to whose code you maybe
     * calling into because of event handling/listeners/etc. The only important thing is that the UI
     * has been given a chance to react to user clicks. Thus the logging of most {@link Exception}s
     * and {@link Error}s as caused by {@link Display#readAndDispatch()} because they are not caused
     * by this code and do not effect it.
     * </p>
     * 
     * @param display the {@link Display} to call <code>readAndDispatch</code> on with
     *          exception/error handling.
     */
    private void readAndDispatch(Display display) {
      try {
        display.readAndDispatch();
      } catch (Exception e) {
        Logger.log(Logger.WARNING,
            "Exception caused by readAndDispatch, not caused by or fatal to caller", e);
      } catch (LinkageError e) {
        Logger.log(Logger.WARNING,
            "LinkageError caused by readAndDispatch, not caused by or fatal to caller", e);
      } catch (VirtualMachineError e) {
        // re-throw these
        throw e;
      } catch (ThreadDeath e) {
        // re-throw these
        throw e;
      } catch (Error e) {
        // catch every error, except for a few that we don't want to handle
        Logger.log(Logger.WARNING,
            "Error caused by readAndDispatch, not caused by or fatal to caller", e);
      }
    }
  }
}
