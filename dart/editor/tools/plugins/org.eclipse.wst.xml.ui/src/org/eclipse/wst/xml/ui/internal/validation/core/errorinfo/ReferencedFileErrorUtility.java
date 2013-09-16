/*******************************************************************************
 * Copyright (c) 2001, 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.validation.core.errorinfo;

import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.wst.xml.core.internal.validation.core.logging.LoggerFactory;

public class ReferencedFileErrorUtility {
  public static void openEditorAndGotoError(String uristring, final int line, final int column) {
    if (uristring != null) {
      try {
        URL uri = new URL(uristring);
        if (uri != null) {
          if ("file".equals(uri.getProtocol())) //$NON-NLS-1$
          {
            String pathString = uri.getPath();
            IPath path = new Path(pathString);
            String device = path.getDevice();
            if ((device != null) && device.startsWith("/")) //$NON-NLS-1$
            {
              path = path.setDevice(device.substring(1));
            }
            final IFile iFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(path);
            if ((iFile != null) && iFile.exists()) {
              // Open the editor for this file.
              final IWorkbench workbench = PlatformUI.getWorkbench();
              final IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();

              Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                  try {
                    IContentType contentType = iFile.getContentDescription().getContentType();
                    IEditorRegistry editorRegistry = workbench.getEditorRegistry();
                    String fileName = iFile.getName();
                    IEditorDescriptor descriptor = editorRegistry.getDefaultEditor(fileName,
                        contentType);
                    String editorId;
                    if (descriptor != null) {
                      editorId = descriptor.getId();
                    } else {
                      descriptor = editorRegistry.getDefaultEditor(fileName + ".txt"); //$NON-NLS-1$
                      editorId = descriptor.getId();
                    }

                    if (editorId != null) {
                      FileEditorInput editorInput = new FileEditorInput(iFile);
                      IWorkbenchPage activePage = workbenchWindow.getActivePage();
                      activePage.openEditor(editorInput, editorId);
                    }
                  } catch (Exception ex) {
                    LoggerFactory.getLoggerInstance().logError(
                        "Exception encountered when attempting to open file: " + iFile + "\n\n", ex); //$NON-NLS-1$ //$NON-NLS-2$
                  }
                }
              });

              Runnable runnable = new Runnable() {
                public void run() {
                  IEditorPart editorPart = workbenchWindow.getActivePage().getActiveEditor();
                  gotoError(editorPart, line, column);
                }
              };
              Display.getCurrent().asyncExec(runnable);
            }
          }
        }
      } catch (Exception e) {
        // Do nothing.
      }
    }
  }

  static void gotoError(IEditorPart editorPart, int line, int column) {
    if (editorPart != null) {
      TextEditor textEditor = (TextEditor) editorPart.getAdapter(TextEditor.class);
      if (textEditor != null) {
        try {
          IDocumentProvider dp = textEditor.getDocumentProvider();
          IDocument document = (dp != null) ? dp.getDocument(textEditor.getEditorInput()) : null;
          textEditor.selectAndReveal(document.getLineOffset(line - 1) + column - 1, 0);
        } catch (BadLocationException x) {
          // marker refers to invalid text position -> do nothing
        }
      }
    }
  }
}
