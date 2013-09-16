/*******************************************************************************
 * Copyright (c) 2001, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.debug;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProviderExtension4;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.util.Debug;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;
import org.eclipse.wst.sse.ui.internal.extension.BreakpointProviderBuilder;
import org.eclipse.wst.sse.ui.internal.provisional.extensions.ISourceEditingTextTools;
import org.eclipse.wst.sse.ui.internal.provisional.extensions.breakpoint.IBreakpointProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * ToggleBreakpointAction
 */
public class ToggleBreakpointAction extends BreakpointRulerAction {
  IAction fFallbackAction;

  /**
   * @param editor
   * @param rulerInfo
   */
  public ToggleBreakpointAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
    super(editor, rulerInfo);
    setText(SSEUIMessages.ToggleBreakpointAction_0); //$NON-NLS-1$
  }

  public ToggleBreakpointAction(ITextEditor editor, IVerticalRulerInfo rulerInfo,
      IAction fallbackAction) {
    this(editor, rulerInfo);
    fFallbackAction = fallbackAction;
  }

  protected boolean createBreakpoints(int lineNumber) {
    /*
     * Note: we'll always allow processing to continue, even for a "read only" IStorageEditorInput,
     * for the ActiveScript debugger. But this means sometimes the ActiveScript provider might get
     * an input from CVS or something that is not related to debugging.
     */

    ITextEditor editor = getTextEditor();
    IEditorInput input = editor.getEditorInput();
    IDocument document = editor.getDocumentProvider().getDocument(input);
    if (document == null)
      return false;

    String contentType = getContentType(document);
    IBreakpointProvider[] providers = BreakpointProviderBuilder.getInstance().getBreakpointProviders(
        editor, contentType, getFileExtension(input));

    int pos = -1;
    ISourceEditingTextTools tools = (ISourceEditingTextTools) editor.getAdapter(ISourceEditingTextTools.class);
    if (tools != null) {
      pos = tools.getCaretOffset();
    }

    final int n = providers.length;
    List errors = new ArrayList(0);
    for (int i = 0; i < n; i++) {
      try {
        if (Debug.debugBreakpoints)
          System.out.println(providers[i].getClass().getName()
              + " adding breakpoint to line " + lineNumber); //$NON-NLS-1$
        IStatus status = providers[i].addBreakpoint(document, input, lineNumber, pos);
        if (status != null && !status.isOK()) {
          errors.add(status);
        }
      } catch (CoreException e) {
        errors.add(e.getStatus());
      } catch (Exception t) {
        Logger.logException("exception while adding breakpoint", t); //$NON-NLS-1$
      }
    }

    IStatus status = null;
    if (errors.size() > 0) {
      Shell shell = editor.getSite().getShell();
      if (errors.size() > 1) {
        status = new MultiStatus(SSEUIPlugin.ID, IStatus.OK,
            (IStatus[]) errors.toArray(new IStatus[0]),
            SSEUIMessages.ManageBreakpoints_error_adding_message1, null); //$NON-NLS-1$
      } else {
        status = (IStatus) errors.get(0);
      }
      if ((status.getSeverity() > IStatus.INFO) || (Platform.inDebugMode() && !status.isOK())) {
        Platform.getLog(SSEUIPlugin.getDefault().getBundle()).log(status);
      }
      /*
       * Show for conditions more severe than INFO or when no breakpoints were created
       */
      if (status.getSeverity() > IStatus.INFO && getBreakpoints(getMarkers()).length < 1) {
        ErrorDialog.openError(shell, SSEUIMessages.ManageBreakpoints_error_adding_title1,
            status.getMessage(), status); //$NON-NLS-1$ //$NON-NLS-2$
        return false;
      }
    }
    /*
     * Although no errors were reported, no breakpoints exist on this line after having run the
     * existing providers. Run the fallback action.
     */
    if ((status == null || status.getSeverity() < IStatus.WARNING) && fFallbackAction != null
        && !hasMarkers()) {
      if (fFallbackAction instanceof ISelectionListener) {
        ((ISelectionListener) fFallbackAction).selectionChanged(null, null);
      }
      fFallbackAction.run();
    }
    return true;
  }

  protected String getContentType(IDocument document) {
    IModelManager mgr = StructuredModelManager.getModelManager();
    String contentType = null;

    IDocumentProvider provider = fTextEditor.getDocumentProvider();
    if (provider instanceof IDocumentProviderExtension4) {
      try {
        IContentType type = ((IDocumentProviderExtension4) provider).getContentType(fTextEditor.getEditorInput());
        if (type != null)
          contentType = type.getId();
      } catch (CoreException e) {
        /*
         * A failure accessing the underlying store really isn't interesting, although it can be a
         * problem for IStorageEditorInputs.
         */
      }
    }

    if (contentType == null) {
      IStructuredModel model = null;
      try {
        model = mgr.getExistingModelForRead(document);
        if (model != null) {
          contentType = model.getContentTypeIdentifier();
        }
      } finally {
        if (model != null) {
          model.releaseFromRead();
        }
      }
    }
    return contentType;
  }

  protected void removeBreakpoints(int lineNumber) {
    IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
    IBreakpoint[] breakpoints = getBreakpoints(getMarkers());
    for (int i = 0; i < breakpoints.length; i++) {
      try {
        breakpointManager.removeBreakpoint(breakpoints[i], true);
      } catch (CoreException e) {
        Logger.logException(e);
      }
    }
  }

  public void run() {
    int lineNumber = fRulerInfo.getLineOfLastMouseButtonActivity() + 1;
    boolean doAdd = !hasMarkers();
    if (doAdd)
      createBreakpoints(lineNumber);
    else
      removeBreakpoints(lineNumber);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.IUpdate#update()
   */
  public void update() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.Action#getImageDescriptor() overriding for lazy loading
   */
  public ImageDescriptor getImageDescriptor() {

    ImageDescriptor image = super.getImageDescriptor();
    if (image == null) {
      image = DebugUITools.getImageDescriptor(IDebugUIConstants.IMG_OBJS_BREAKPOINT);
      setImageDescriptor(image);
    }
    return image;
  }
}
