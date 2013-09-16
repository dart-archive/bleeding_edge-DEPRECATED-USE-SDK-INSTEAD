/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal.edit.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.html.core.internal.cleanup.HTMLCleanupProcessorImpl;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.cleanup.IStructuredCleanupProcessor;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;

public class CleanupDocumentHandler extends AbstractHandler implements IHandler {
  private IStructuredCleanupProcessor fCleanupProcessor;

  public void dispose() {
    // nulling out just in case
    fCleanupProcessor = null;
  }

  public Object execute(ExecutionEvent event) throws ExecutionException {
    IEditorPart editorPart = HandlerUtil.getActiveEditor(event);
    ITextEditor textEditor = null;
    if (editorPart instanceof ITextEditor)
      textEditor = (ITextEditor) editorPart;
    else {
      Object o = editorPart.getAdapter(ITextEditor.class);
      if (o != null)
        textEditor = (ITextEditor) o;
    }
    if (textEditor != null) {
      final ITextEditor editor = textEditor;
      CleanupDialogHTML cleanupDialog = new CleanupDialogHTML(editor.getSite().getShell());
      cleanupDialog.setisXHTMLType(isXHTML(editor));
      if (cleanupDialog.open() == Window.OK) {
        // setup runnable
        Runnable runnable = new Runnable() {
          public void run() {
            IStructuredCleanupProcessor cleanupProcessor = getCleanupProcessor();
            if (cleanupProcessor != null) {
              IStructuredModel model = null;
              try {
                model = StructuredModelManager.getModelManager().getExistingModelForEdit(
                    editor.getDocumentProvider().getDocument(editor.getEditorInput()));
                if (model != null)
                  cleanupProcessor.cleanupModel(model);
              } finally {
                if (model != null)
                  model.releaseFromEdit();
              }
            }
          }
        };

        // TODO: make independent of 'model'.
        IStructuredModel model = null;
        try {
          model = StructuredModelManager.getModelManager().getExistingModelForEdit(
              editor.getDocumentProvider().getDocument(editor.getEditorInput()));
          if (model != null) {
            // begin recording
            ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
            model.beginRecording(this, SSEUIMessages.Cleanup_Document_UI_,
                SSEUIMessages.Cleanup_Document_UI_, selection.getOffset(), selection.getLength()); //$NON-NLS-1$ //$NON-NLS-2$

            // tell the model that we are about to make a big
            // model change
            model.aboutToChangeModel();

            // run
            BusyIndicator.showWhile(
                editor.getEditorSite().getWorkbenchWindow().getShell().getDisplay(), runnable);
          }
        } finally {
          if (model != null) {
            // tell the model that we are done with the big
            // model
            // change
            model.changedModel();

            // end recording
            ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
            model.endRecording(this, selection.getOffset(), selection.getLength());
            model.releaseFromEdit();
          }
        }
      }
    }
    return null;
  }

  IStructuredCleanupProcessor getCleanupProcessor() {
    if (fCleanupProcessor == null)
      fCleanupProcessor = new HTMLCleanupProcessorImpl();

    return fCleanupProcessor;
  }

  private boolean isXHTML(ITextEditor editor) {
    boolean isxhtml = false;
    if (editor != null) {
      IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
      IStructuredModel model = null;
      try {
        model = StructuredModelManager.getModelManager().getExistingModelForRead(document);
        if (model instanceof IDOMModel) {
          IDOMDocument domDocument = ((IDOMModel) model).getDocument();
          if (domDocument != null)
            isxhtml = domDocument.isXMLType();
        }
      } finally {
        if (model != null) {
          model.releaseFromRead();
        }
      }
    }
    return isxhtml;
  }
}
