/*******************************************************************************
 * Copyright (c) 2001, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorPart;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.sse.ui.internal.TransferBuilder.TransferProxyForDelayLoading;

/**
 * ExtendedEditorDropTargetAdapter
 */
public class ExtendedEditorDropTargetAdapter extends DropTargetAdapter {
  private String[] editorIds;
  private Point originalRange = null;
  private IEditorPart targetEditor = null;
  private ITextViewer textViewer = null;

  private Transfer[] transfers = null;

  private boolean useProxy;

  /**
   * @deprecated use ExtendedEditorDropTargetAdapter(boolean useProxy) for the performance
   */
  public ExtendedEditorDropTargetAdapter() {
    this(false);
  }

  public ExtendedEditorDropTargetAdapter(boolean useProxy) {
    super();
    this.useProxy = useProxy;
  }

  protected boolean doDrop(Transfer transfer, DropTargetEvent event) {
    TransferBuilder tb = new TransferBuilder(useProxy);

    IDropAction[] as = null;
    if (editorIds != null && editorIds.length > 0)
      as = tb.getDropActions(editorIds, transfer);
    else
      as = tb.getDropActions(getTargetEditor().getClass().getName(), transfer);

    for (int i = 0; i < as.length; ++i) {
      IDropAction da = as[i];
      Transfer actualTransfer;
      if (transfer instanceof TransferProxyForDelayLoading) {
        actualTransfer = ((TransferProxyForDelayLoading) transfer).getTransferClass();
      } else {
        actualTransfer = transfer;
      }
      if (actualTransfer instanceof FileTransfer) {
        if (event.data == null) {
          Logger.log(Logger.ERROR, "No data in DropTargetEvent from " + event.widget); //$NON-NLS-1$
          return false;
        }
        String[] strs = (String[]) event.data;
        boolean[] bs = new boolean[strs.length];
        int c = 0;
        for (int j = 0; j < strs.length; ++j) {
          bs[j] = false;
          if (da.isSupportedData(strs[j])) {
            event.data = new String[] {strs[j]};
            if (!da.run(event, targetEditor)) {
              bs[j] = true;
              c++;
            }
          } else {
            bs[j] = true;
            c++;
          }
        }
        if (c == 0) {
          return true;
        }

        int k = 0;
        String[] rests = new String[c];
        for (int j = 0; j < strs.length; ++j) {
          if (bs[j])
            rests[k++] = strs[j];
        }
        event.data = rests;
      } else if (da.isSupportedData(event.data)) {
        if (da.run(event, targetEditor)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
	 */
  public void dragEnter(DropTargetEvent event) {
    TransferData data = null;
    Transfer[] ts = getTransfers();
    for (int i = 0; i < ts.length; i++) {
      for (int j = 0; j < event.dataTypes.length; j++) {
        if (ts[i].isSupportedType(event.dataTypes[j])) {
          data = event.dataTypes[j];
          break;
        }
      }
      if (data != null) {
        event.currentDataType = data;
        break;
      }
    }

    if (textViewer != null) {
      originalRange = textViewer.getSelectedRange();
    }
  }

  public void dragLeave(DropTargetEvent event) {
    if (textViewer != null) {
      textViewer.setSelectedRange(originalRange.x, originalRange.y);
    } else {
      originalRange = null;
    }
  }

  /**
   * Scroll the visible area as needed
   */
  public void dragOver(DropTargetEvent event) {
    event.operations &= ~DND.DROP_MOVE;
    event.detail = DND.DROP_COPY;
    event.feedback |= DND.FEEDBACK_SCROLL;
  }

  /**
	 */
  public void drop(DropTargetEvent event) {
    if (event.operations == DND.DROP_NONE)
      return;

    Transfer[] ts = getTransfers();
    for (int i = 0; i < ts.length; i++) {
      if (ts[i].isSupportedType(event.currentDataType)) {
        if (doDrop(ts[i], event)) {
          IEditorPart part = targetEditor;
          if (targetEditor instanceof StructuredTextEditor) {
            part = ((StructuredTextEditor) targetEditor).getEditorPart();
          }
          targetEditor.getSite().getPage().activate(part);
          break;
        }
      }
    }
  }

  public IEditorPart getTargetEditor() {
    return targetEditor;
  }

  public ITextViewer getTextViewer() {
    return textViewer;
  }

  /**
   * @return org.eclipse.swt.dnd.Transfer[]
   */
  public Transfer[] getTransfers() {
    if (transfers == null) {
      TransferBuilder tb = new TransferBuilder(useProxy);
      if (editorIds == null || editorIds.length == 0)
        transfers = tb.getDropTargetTransfers(getTargetEditor().getClass().getName());
      else
        transfers = tb.getDropTargetTransfers(editorIds);
    }
    return transfers;
  }

  /**
	 */
  public void setTargetEditor(IEditorPart targetEditor) {
    this.targetEditor = targetEditor;
  }

  public void setTargetIDs(String[] ids) {
    editorIds = ids;
  }

  public void setTextViewer(ITextViewer textViewer) {
    this.textViewer = textViewer;
  }

}
