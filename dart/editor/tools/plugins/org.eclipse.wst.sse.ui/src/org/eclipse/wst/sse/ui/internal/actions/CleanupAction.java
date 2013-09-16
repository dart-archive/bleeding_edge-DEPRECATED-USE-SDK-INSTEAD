/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.actions;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;
import org.eclipse.wst.sse.core.internal.cleanup.IStructuredCleanupProcessor;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;

import java.util.ResourceBundle;

public abstract class CleanupAction extends TextEditorAction {
  protected Dialog fCleanupDialog;

  public CleanupAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
    super(bundle, prefix, editor);
  }

  protected abstract Dialog getCleanupDialog(Shell shell);

  protected abstract IStructuredCleanupProcessor getCleanupProcessor();

  public void run() {
    if (getTextEditor() instanceof StructuredTextEditor) {
      final StructuredTextEditor editor = (StructuredTextEditor) getTextEditor();
      Dialog cleanupDialog = getCleanupDialog(editor.getSite().getShell());
      if (cleanupDialog != null) {
        if (cleanupDialog.open() == Window.OK) {
          // setup runnable
          Runnable runnable = new Runnable() {
            public void run() {
              IStructuredCleanupProcessor cleanupProcessor = getCleanupProcessor();
              if (cleanupProcessor != null)
                cleanupProcessor.cleanupModel(editor.getModel());
            }
          };

          // TODO: make independent of 'model'.
          IStructuredModel model = editor.getModel();
          if (model != null) {
            try {
              // begin recording
              ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
              model.beginRecording(this, SSEUIMessages.Cleanup_Document_UI_,
                  SSEUIMessages.Cleanup_Document_UI_, selection.getOffset(), selection.getLength()); //$NON-NLS-1$ //$NON-NLS-2$

              // tell the model that we are about to make a big
              // model change
              model.aboutToChangeModel();

              // run
              BusyIndicator.showWhile(editor.getTextViewer().getControl().getDisplay(), runnable);
            } finally {
              // tell the model that we are done with the big
              // model
              // change
              model.changedModel();

              // end recording
              ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
              model.endRecording(this, selection.getOffset(), selection.getLength());
            }
          }
        }

      }
    }
  }
}
