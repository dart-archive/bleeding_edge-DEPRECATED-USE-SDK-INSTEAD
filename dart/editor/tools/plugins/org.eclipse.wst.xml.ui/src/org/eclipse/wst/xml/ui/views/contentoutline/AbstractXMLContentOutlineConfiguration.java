/*******************************************************************************
 * Copyright (c) 2001, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.views.contentoutline;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.util.TransferDragSourceListener;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.ui.internal.IReleasable;
import org.eclipse.wst.sse.ui.views.contentoutline.ContentOutlineConfiguration;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;
import org.eclipse.wst.xml.ui.internal.contentoutline.JFaceNodeContentProvider;
import org.eclipse.wst.xml.ui.internal.contentoutline.JFaceNodeLabelProvider;
import org.eclipse.wst.xml.ui.internal.contentoutline.XMLNodeActionManager;
import org.eclipse.wst.xml.ui.internal.dnd.DragNodeCommand;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;

/**
 * Basic Outline Configuration for generic XML support. Expects that the viewer's input will be the
 * DOM Model, and provides basic label and content providers.
 * 
 * @see org.eclipse.wst.sse.ui.views.contentoutline.ContentOutlineConfiguration
 * @since 3.1
 */
public abstract class AbstractXMLContentOutlineConfiguration extends ContentOutlineConfiguration {
  private class ActionManagerMenuListener implements IMenuListener, IReleasable {
    private XMLNodeActionManager fActionManager;
    private TreeViewer fTreeViewer;

    public ActionManagerMenuListener(TreeViewer viewer) {
      fTreeViewer = viewer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.jface.action.IMenuListener#menuAboutToShow(org.eclipse.jface.action.IMenuManager)
     */
    public void menuAboutToShow(IMenuManager manager) {
      if (fActionManager == null) {
        fActionManager = createNodeActionManager(fTreeViewer);
      }
      if (fActionManager != null) {
        fActionManager.fillContextMenu(manager, fTreeViewer.getSelection());
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.wst.sse.ui.internal.IReleasable#release()
     */
    public void release() {
      fTreeViewer = null;
      if (fActionManager != null) {
        fActionManager.setModel(null);
      }
    }
  }

  private static class StatusLineLabelProvider extends JFaceNodeLabelProvider {
    public StatusLineLabelProvider() {
      super();
    }

    public String getText(Object element) {
      if (element == null)
        return null;

      if (!(element instanceof Node)) {
        return super.getText(element);
      }

      StringBuffer s = new StringBuffer();
      Node node = (Node) element;
      while (node != null) {
        if (node.getNodeType() != Node.DOCUMENT_NODE) {
          s.insert(0, super.getText(node));
        }

        if (node.getNodeType() == Node.ATTRIBUTE_NODE)
          node = ((Attr) node).getOwnerElement();
        else
          node = node.getParentNode();

        if (node != null && node.getNodeType() != Node.DOCUMENT_NODE) {
          s.insert(0, IPath.SEPARATOR);
        }
      }
      return s.toString();
    }
  }

  private IContentProvider fContentProvider = null;

  private ActionManagerMenuListener fContextMenuFiller = null;

  private ILabelProvider fLabelProvider = null;

  boolean fShowAttributes = false;

  private ILabelProvider fSimpleLabelProvider;
  private TransferDragSourceListener[] fTransferDragSourceListeners;

  private TransferDropTargetListener[] fTransferDropTargetListeners;

  /**
   * Create new instance of XMLContentOutlineConfiguration
   */
  public AbstractXMLContentOutlineConfiguration() {
    // Must have empty constructor to createExecutableExtension
    super();
  }

  /**
   * Returns the NodeActionManager to use for the given treeViewer.
   * <p>
   * Not API. May be removed in the future.
   * </p>
   * 
   * @param treeViewer the TreeViewer associated with this configuration
   * @return a node action manager for use with this tree viewer
   */
  protected XMLNodeActionManager createNodeActionManager(TreeViewer treeViewer) {
    return new XMLNodeActionManager((IStructuredModel) treeViewer.getInput(), treeViewer);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.wst.sse.ui.views.contentoutline.ContentOutlineConfiguration#getContentProvider(
   * org.eclipse.jface.viewers.TreeViewer)
   */
  public IContentProvider getContentProvider(TreeViewer viewer) {
    if (fContentProvider == null) {
      fContentProvider = new JFaceNodeContentProvider();
    }
    return fContentProvider;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.wst.sse.ui.views.contentoutline.ContentOutlineConfiguration#getLabelProvider(org
   * .eclipse.jface.viewers.TreeViewer)
   */
  public ILabelProvider getLabelProvider(TreeViewer viewer) {
    if (fLabelProvider == null) {
      fLabelProvider = new JFaceNodeLabelProvider();
    }
    return fLabelProvider;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.wst.sse.ui.views.contentoutline.ContentOutlineConfiguration#getMenuListener(org
   * .eclipse.jface.viewers.TreeViewer)
   */
  public IMenuListener getMenuListener(TreeViewer viewer) {
    if (fContextMenuFiller == null) {
      fContextMenuFiller = new ActionManagerMenuListener(viewer);
    }
    return fContextMenuFiller;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.wst.sse.ui.views.contentoutline.ContentOutlineConfiguration#getPreferenceStore()
   */
  protected IPreferenceStore getPreferenceStore() {
    return XMLUIPlugin.getDefault().getPreferenceStore();
  }

  public ILabelProvider getStatusLineLabelProvider(TreeViewer treeViewer) {
    if (fSimpleLabelProvider == null) {
      fSimpleLabelProvider = new StatusLineLabelProvider();
    }
    return fSimpleLabelProvider;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.sse.ui.views.contentoutline.ContentOutlineConfiguration#
   * getTransferDragSourceListeners(org.eclipse.jface.viewers.TreeViewer)
   */
  public TransferDragSourceListener[] getTransferDragSourceListeners(final TreeViewer treeViewer) {
    if (fTransferDragSourceListeners == null) {
      fTransferDragSourceListeners = new TransferDragSourceListener[] {new TransferDragSourceListener() {

        public void dragFinished(DragSourceEvent event) {
          LocalSelectionTransfer.getTransfer().setSelection(null);
        }

        public void dragSetData(DragSourceEvent event) {
        }

        public void dragStart(DragSourceEvent event) {
          LocalSelectionTransfer.getTransfer().setSelection(treeViewer.getSelection());
        }

        public Transfer getTransfer() {
          return LocalSelectionTransfer.getTransfer();
        }
      }};
    }

    return fTransferDragSourceListeners;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.sse.ui.views.contentoutline.ContentOutlineConfiguration#
   * getTransferDropTargetListeners(org.eclipse.jface.viewers.TreeViewer)
   */
  public TransferDropTargetListener[] getTransferDropTargetListeners(final TreeViewer treeViewer) {
    if (fTransferDropTargetListeners == null) {
      fTransferDropTargetListeners = new TransferDropTargetListener[] {new TransferDropTargetListener() {
        public void dragEnter(DropTargetEvent event) {
        }

        public void dragLeave(DropTargetEvent event) {
        }

        public void dragOperationChanged(DropTargetEvent event) {
        }

        public void dragOver(DropTargetEvent event) {
          event.feedback = DND.FEEDBACK_SELECT;
          float feedbackFloat = getHeightInItem(event);
          if (feedbackFloat > 0.75) {
            event.feedback = DND.FEEDBACK_INSERT_AFTER;
          } else if (feedbackFloat < 0.25) {
            event.feedback = DND.FEEDBACK_INSERT_BEFORE;
          }
          event.feedback |= DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL;
        }

        public void drop(DropTargetEvent event) {
          if (event.operations != DND.DROP_NONE
              && LocalSelectionTransfer.getTransfer().getSelection() != null
              && !LocalSelectionTransfer.getTransfer().getSelection().isEmpty()) {
            IStructuredSelection selection = (IStructuredSelection) LocalSelectionTransfer.getTransfer().getSelection();
            if (selection != null && !selection.isEmpty() && event.item != null
                && event.item.getData() != null) {
              /*
               * the command uses these numbers instead of the feedback constants (even though it
               * converts in the other direction as well)
               */
              float feedbackFloat = getHeightInItem(event);

              final DragNodeCommand command = new DragNodeCommand(event.item.getData(),
                  feedbackFloat, event.operations, event.detail, selection.toList(), treeViewer);
              if (command != null && command.canExecute()) {
                SafeRunnable.run(new SafeRunnable() {
                  public void run() throws Exception {
                    command.execute();
                  }
                });
              }
            }
          }
        }

        public void dropAccept(DropTargetEvent event) {
        }

        private float getHeightInItem(DropTargetEvent event) {
          if (event.item == null)
            return .5f;
          if (event.item instanceof TreeItem) {
            TreeItem treeItem = (TreeItem) event.item;
            Control control = treeItem.getParent();
            Point point = control.toControl(new Point(event.x, event.y));
            Rectangle bounds = treeItem.getBounds();
            return (float) (point.y - bounds.y) / (float) bounds.height;
          } else if (event.item instanceof TableItem) {
            TableItem tableItem = (TableItem) event.item;
            Control control = tableItem.getParent();
            Point point = control.toControl(new Point(event.x, event.y));
            Rectangle bounds = tableItem.getBounds(0);
            return (float) (point.y - bounds.y) / (float) bounds.height;
          } else {
            return 0.0F;
          }
        }

        public Transfer getTransfer() {
          return LocalSelectionTransfer.getTransfer();
        }

        public boolean isEnabled(DropTargetEvent event) {
          return getTransfer().isSupportedType(event.currentDataType);
        }
      }};
    }
    return fTransferDropTargetListeners;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.wst.sse.ui.views.contentoutline.ContentOutlineConfiguration#unconfigure(org.eclipse
   * .jface.viewers.TreeViewer)
   */
  public void unconfigure(TreeViewer viewer) {
    super.unconfigure(viewer);
    fTransferDragSourceListeners = null;
    fTransferDropTargetListeners = null;
    if (fContextMenuFiller != null) {
      fContextMenuFiller.release();
      fContextMenuFiller = null;
    }
  }
}
