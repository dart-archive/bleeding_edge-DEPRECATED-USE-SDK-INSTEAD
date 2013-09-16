/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/

package org.eclipse.wst.xml.ui.internal.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.cleanup.IStructuredCleanupProcessor;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.xml.core.internal.cleanup.CleanupProcessorXML;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;

/**
 * Cleanup action delegate for CSS editor
 */
public class CleanupActionXMLDelegate implements IEditorActionDelegate, IActionDelegate2,
    IViewActionDelegate {
  private IEditorPart fEditor;
  private IStructuredCleanupProcessor fCleanupProcessor;

  public void setActiveEditor(IAction action, IEditorPart targetEditor) {
    fEditor = targetEditor;
  }

  public void dispose() {
    // nulling out just in case
    fEditor = null;
    fCleanupProcessor = null;
  }

  public void init(IAction action) {
    if (action != null) {
      action.setText(XMLUIMessages.CleanupDocument_label);
      action.setToolTipText(XMLUIMessages.CleanupDocument_tooltip);
      action.setDescription(XMLUIMessages.CleanupDocument_description);
    }
  }

  public void runWithEvent(IAction action, Event event) {
    run(action);
  }

  public void init(IViewPart view) {
    // do nothing
  }

  public void run(IAction action) {
    if (fEditor instanceof ITextEditor) {
      final ITextEditor editor = (ITextEditor) fEditor;
      Dialog cleanupDialog = new CleanupDialogXML(editor.getSite().getShell());
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
                if (model != null) {
                  cleanupProcessor.cleanupModel(model);
                }
              } finally {
                if (model != null) {
                  model.releaseFromEdit();
                }
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
                SSEUIMessages.Cleanup_Document_UI_, selection.getOffset(), selection.getLength());

            // tell the model that we are about to make a big
            // model change
            model.aboutToChangeModel();

            // run
            BusyIndicator.showWhile(
                fEditor.getEditorSite().getWorkbenchWindow().getShell().getDisplay(), runnable);
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
  }

  public void selectionChanged(IAction action, ISelection selection) {
    // do nothing
  }

  IStructuredCleanupProcessor getCleanupProcessor() {
    if (fCleanupProcessor == null) {
      fCleanupProcessor = new CleanupProcessorXML();
    }

    return fCleanupProcessor;
  }
}
