/*******************************************************************************
 * Copyright (c) 2001, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.dnd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.common.ui.internal.dnd.DefaultDragAndDropCommand;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.ui.internal.Logger;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class DragNodeCommand extends DefaultDragAndDropCommand {
  private List fSelections;
  private TreeViewer fTreeViewer;

  public DragNodeCommand(Object target, float location, int operations, int operation,
      Collection sources, TreeViewer treeViewer) {
    super(target, location, operations, operation, sources);
    fTreeViewer = treeViewer;
    fSelections = new ArrayList();
  }

  private void beginModelChange(Node node, boolean batchUpdate) {
    IStructuredModel structuredModel = getStructuredModel(node);
    if (structuredModel != null) {
      String undoDesc = new String();
      if (getOperation() == DND.DROP_MOVE) {
        undoDesc = XMLUIMessages.DragNodeCommand_0;
      } else if (getOperation() == DND.DROP_COPY) {
        undoDesc = XMLUIMessages.DragNodeCommand_1;
      }

      structuredModel.beginRecording(this, undoDesc);
      if (batchUpdate) {
        // structuredModel.aboutToChangeModel();
      }
    }
  }

  public boolean canExecute() {
    return executeHelper(true);
  }

  private boolean doModify(Node source, Node parentNode, Node refChild, boolean testOnly) {
    boolean result = false;
    if (source.getNodeType() == Node.ATTRIBUTE_NODE) {
      Attr sourceAttribute = (Attr) source;
      Element sourceAttributeOwnerElement = sourceAttribute.getOwnerElement();
      if ((parentNode.getNodeType() == Node.ELEMENT_NODE)
          && (sourceAttributeOwnerElement != parentNode)) {
        result = true;
        if (!testOnly) {
          try {
            if (getOperation() == DND.DROP_MOVE) {
              Element targetElement = (Element) parentNode;
              sourceAttributeOwnerElement.removeAttributeNode(sourceAttribute);
              targetElement.getAttributes().setNamedItem(sourceAttribute);
              fSelections.add(sourceAttribute);
            } else if (getOperation() == DND.DROP_COPY) {
              Attr cloneAttribute = (Attr) sourceAttribute.cloneNode(false);
              Element targetElement = (Element) parentNode;
              targetElement.getAttributes().setNamedItem(cloneAttribute);
              fSelections.add(cloneAttribute);
            }
          } catch (Exception e) {
            Logger.log(Logger.WARNING_DEBUG, e.getMessage(), e);
          }
        }
      }
    } else {
      if (((parentNode.getNodeType() == Node.ELEMENT_NODE) || (parentNode.getNodeType() == Node.DOCUMENT_NODE))
          && !(refChild instanceof Attr)) {
        result = true;

        if (!testOnly) {
          try {
            if (isAncestor(source, parentNode)) {
              // System.out.println("can not perform this drag drop
              // operation.... todo... pop up dialog");
            } else {
              // defect 221055 this test is required or else the
              // node will
              // be removed from the tree and the insert will fail
              if (source != refChild) {
                if (getOperation() == DND.DROP_MOVE) {
                  Node oldParent = source.getParentNode();
                  Node oldSibling = source.getNextSibling();
                  oldParent.removeChild(source);
                  try {
                    parentNode.insertBefore(source, refChild);
                  } catch (DOMException e) {
                    // bug151692 - if unable to move node to new location, reinsert back to old location
                    oldParent.insertBefore(source, oldSibling);
                  }
                  fSelections.add(source);
                } else if (getOperation() == DND.DROP_COPY) {
                  Node nodeClone = source.cloneNode(true);
                  parentNode.insertBefore(nodeClone, refChild);
                  fSelections.add(nodeClone);
                }
              }
            }
          } catch (Exception e) {
            Logger.log(Logger.WARNING_DEBUG, e.getMessage(), e);
          }
        }
      }
    }
    return result;
  }

  private void endModelChange(Node node, boolean batchUpdate) {
    IStructuredModel structuredModel = getStructuredModel(node);
    if (structuredModel != null) {
      structuredModel.endRecording(this);
      if (batchUpdate) {
        // structuredModel.changedModel();
      }
    }
  }

  public void execute() {
    executeHelper(false);

    // Make our selection if the treeViewer != null
    if (fTreeViewer != null) {
      StructuredSelection structuredSelection = new StructuredSelection(fSelections);
      fTreeViewer.setSelection(structuredSelection);
    }
  }

  private boolean executeHelper(boolean testOnly) {
    boolean result = true;
    if (target instanceof Node) {
      Node targetNode = (Node) target;
      if (!testOnly && fTreeViewer != null
          && !validateEdit(getStructuredModel(targetNode), fTreeViewer.getControl().getShell()))
        return false;

      Node parentNode = getParentForDropPosition(targetNode);
      Node refChild = getRefChild(targetNode);

      Vector sourcesList = new Vector();
      sourcesList.addAll(sources);

      removeMemberDescendants(sourcesList);
      boolean performBatchUpdate = sourcesList.size() > 5;

      if (!testOnly) {
        beginModelChange(targetNode, performBatchUpdate);
      }
      for (Iterator i = sourcesList.iterator(); i.hasNext();) {
        Object source = i.next();
        if (source instanceof Node) {
          if (!((refChild == null) && (targetNode instanceof Attr)) && (parentNode != null)) {
            result = doModify((Node) source, parentNode, refChild, testOnly);
          } else {
            result = false;
          }
          if (!result) {
            break;
          }
        }
      }
      if (!testOnly) {
        endModelChange(targetNode, performBatchUpdate);
      }
    } else {
      result = false;
    }
    return result;
  }

  /**
   * Checks that the resource backing the model is writeable utilizing <code>validateEdit</code> on
   * a given <tt>IWorkspace</tt>.
   * 
   * @param model the model to be checked
   * @param context the shell context for which <code>validateEdit</code> will be run
   * @return boolean result of checking <code>validateEdit</code>. If the resource is unwriteable,
   *         <code>status.isOK()</code> will return true; otherwise, false.
   */
  private boolean validateEdit(IStructuredModel model, Shell context) {
    if (model != null && model.getBaseLocation() != null) {
      IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(
          new Path(model.getBaseLocation()));
      return !file.isAccessible()
          || ResourcesPlugin.getWorkspace().validateEdit(new IFile[] {file}, context).isOK();
    }
    return false; //$NON-NLS-1$
  }

  public int getFeedback() {
    int result = DND.FEEDBACK_SELECT;
    if (location > 0.75) {
      result = DND.FEEDBACK_INSERT_AFTER;
    } else if (location < 0.25) {
      result = DND.FEEDBACK_INSERT_BEFORE;
    }
    return result;
  }

  private Node getParentForDropPosition(Node node) {
    Node result = null;

    int feedback = getFeedback();
    if (feedback == DND.FEEDBACK_SELECT) {
      result = node;
    } else {
      result = getParentOrOwner(node);
    }
    return result;
  }

  private Node getParentOrOwner(Node node) {
    return (node.getNodeType() == Node.ATTRIBUTE_NODE) ? ((Attr) node).getOwnerElement()
        : node.getParentNode();
  }

  private Node getRefChild(Node node) {
    Node result = null;

    int feedback = getFeedback();

    if (feedback == DND.FEEDBACK_INSERT_BEFORE) {
      result = node;
    } else if (feedback == DND.FEEDBACK_INSERT_AFTER) {
      result = node.getNextSibling();
    }
    return result;
  }

  private IStructuredModel getStructuredModel(Node node) {
    IStructuredModel result = null;
    if (node instanceof IDOMNode) {
      result = ((IDOMNode) node).getModel();
    }
    return result;
  }

  // returns true if a is an ancestore of b
  //
  private boolean isAncestor(Node a, Node b) {
    boolean result = false;
    for (Node parent = b; parent != null; parent = parent.getParentNode()) {
      if (parent == a) {
        result = true;
        break;
      }
    }
    return result;
  }

  /**
   * This method removes members of the list that have ancestors that are also members of the list.
   */
  private void removeMemberDescendants(List list) {
    Hashtable table = new Hashtable();
    for (Iterator i = list.iterator(); i.hasNext();) {
      Object node = i.next();
      table.put(node, node);
    }

    for (int i = list.size() - 1; i >= 0; i--) {
      Object node = list.get(i);
      if (node instanceof Node) {
        for (Node parent = getParentOrOwner((Node) node); parent != null; parent = getParentOrOwner(parent)) {
          if (table.get(parent) != null) {
            list.remove(i);
            break;
          }
        }
      }
    }
  }
}
