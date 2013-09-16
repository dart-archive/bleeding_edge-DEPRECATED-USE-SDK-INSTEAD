/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - Initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.common.ui.internal.dnd;

import org.eclipse.core.commands.Command;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.custom.TableTreeItem;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * This implementation of a drop target listener is designed to turn a drag and drop operation into
 * a {@link Command} based on the model objects of an {@link EditingDomain} and created by
 * {@link DragAndDropManager#create}. It is designed to do early data transfer so the the enablement
 * and feedback of the drag and drop interaction can intimately depend on the state of the model
 * objects involed.
 * <p>
 * The base implementation of this class should be sufficient for most applications. Any change in
 * behaviour is typically accomplished by overriding {@link ItemProviderAdapter}
 * .createDragAndDropCommand to return a derived implementation of {@link DragAndDropCommand}. This
 * is how one these adapters is typically hooked up:
 * 
 * <pre>
 *   viewer.addDropSupport
 *     (DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_LINK,
 *      new Transfer [] { ObjectTransfer.getInstance() },
 *      EditingDomainViewerDropAdapter(viewer));
 * </pre>
 * <p>
 * This implementation prefers to use a {@link ObjectTransfer}, which short-circuits the transfer
 * process for simple transfers within the workbench, the method {@link #getDragSource} can be
 * overriden to change the behaviour. The implementation also only handles an
 * {@link IStructuredSelection}, but the method {@link #extractDragSource} can be overriden to
 * change the behaviour.
 * <p>
 * You can call {@link #setHoverThreshold} to set the amount of time, in milliseconds, to hover over
 * an item before {@link #hover} is called; the default is 1500 milliseconds.
 */
public class ViewerDropAdapter extends DropTargetAdapter {
  /**
   * This is the viewer for which this is a drop target listener.
   */
  protected Viewer viewer;

  /**
   * This is the collection of source objects being dragged.
   */
  protected Collection source;

  /**
   * This is the command created during dragging which provides the feedback and will carry out the
   * action upon completion.
   */
  //  protected Command command;
  protected DragAndDropCommand command;

  /**
   * This records the object for which the {@link #command} was created.
   */
  protected Object commandTarget;

  /**
   * The amount of time to hover over a tree item before expanding it
   */
  protected int hoverThreshold = 1500;

  /**
   * The is the time the mouse first started hovering over the current item.
   */
  protected long hoverStart = 0;

  /**
   * This keeps track of the most recent item for the {@link #hoverStart}.
   */
  protected Widget previousItem;

  /**
   * This keeps track of the original operation that was in effect before we set the event.detail in
   * here.
   */
  protected int originalOperation;

  /**
   * This keeps track of the information used to create the current command.
   */
  protected DragAndDropCommandInformation dragAndDropCommandInformation;

  protected DragAndDropManager dragAndDropManager;

  /**
   * This creates and instance of the given domain and viewer.
   */
  public ViewerDropAdapter(Viewer viewer, DragAndDropManager dragAndDropManager) {
    this.viewer = viewer;
    this.dragAndDropManager = dragAndDropManager;
  }

  /**
   * This is called when the mouse first enters or starts dragging in the viewer.
   */
  public void dragEnter(DropTargetEvent event) {
    originalOperation = event.detail;
    helper(event);
  }

  /**
   * This is called when the mouse leaves or stops dragging in the viewer
   */
  public void dragLeave(DropTargetEvent event) {
    // Clean up the command if there is one.
    //
    if (command != null) {
//        command.dispose();
      command = null;
      commandTarget = null;
    }

    // Reset the other values.
    //
    previousItem = null;
    hoverStart = 0;
    source = null;
  }

  /**
   * This is called when the operation has changed in some way, typically because the user changes
   * keyboard modifiers.
   */
  public void dragOperationChanged(DropTargetEvent event) {
    originalOperation = event.detail;
    helper(event);
  }

  /**
   * This is called repeated when the mouse over the viewer.
   */
  public void dragOver(DropTargetEvent event) {
    helper(event);
  }

  /**
   * This is called just as the mouse is released over the viewer to initiate a drop.
   */
  public void dropAccept(DropTargetEvent event) {
    // There seems to be a bug in SWT that the view may have scrolled.
    // helper(event);
  }

  /**
   * This is called to indate that the drop action should be invoked.
   */
  public void drop(DropTargetEvent event) {
    // There seems to be a bug in SWT that the view may have scrolled.
    // helper(event);
    if (dragAndDropCommandInformation != null) {
      command = dragAndDropCommandInformation.createCommand();

      // Execute the command
      command.execute();

      // Clean up the state.
      //
      command = null;
      commandTarget = null;
      previousItem = null;
      hoverStart = 0;
      source = null;
    }
  }

  /**
   * This method is called the same way for each of the
   * {@link org.eclipse.swt.dnd.DropTargetListener} methods, except during leave.
   */
  protected void helper(DropTargetEvent event) {
    // Try to get the source if there isn't one.
    //
    if (source == null) {
      source = getDragSource(event);
    } else if (event.currentDataType == null) {
      setCurrentDataType(event);
    }

    // If there's still no source, wait until the next time to try again.
    //
    if (source == null) {
      event.detail = DND.DROP_NONE;
      event.feedback = DND.FEEDBACK_SELECT;
    }
    // Otherwise, if we need to scroll...
    //
    else if (scrollIfNeeded(event)) {
      // In the case that we scroll, we just do all the work on the next event and only just scroll now.
      //
      event.feedback = DND.FEEDBACK_SELECT;
    } else {
      // Get the data from the item, if there is one.
      //
      Object target = event.item == null ? null : event.item.getData();
      if (target instanceof TableTreeItem) {
        target = ((TableTreeItem) target).getData();
      }

      // Do the logic to determine the hover information.
      // If we're over a new item from before.
      //
      if (event.item != previousItem) {
        // Remember the item and the time.
        //
        previousItem = event.item;
        hoverStart = event.time;
      } else if (target != null) {
        if (event.time - hoverStart > hoverThreshold) {
          hover(target);

          // We don't need to hover over this guy again.
          //
          hoverStart = Integer.MAX_VALUE;
        }
      }

      // Determine if we can create a valid command at the current mouse location.
      //
      boolean valid = false;

      // If we don't have a previous cached command...
      //
      if (command == null) {
        // Create the command and test if it is executable.
        //
        commandTarget = target;
        command = dragAndDropManager.createCommand(target, getLocation(event), event.operations,
            event.detail, source);
        if (command != null) {
          valid = command.canExecute();
        }
      } else {
        int operation = originalOperation != event.detail ? originalOperation : event.detail;

        // Check if the cached command is able to provide drag and drop feedback.
        //
        if (target == commandTarget)// && command instanceof DragAndDropFeedback)
        {
          float location = getLocation(event);

          dragAndDropCommandInformation = new DragAndDropCommandInformation(target, location,
              event.operations, operation, source);

          // If so, revalidate the command.
          //
          command.reinitialize(target, location, event.operations, operation, source);
          if (command != null) {
            valid = command.canExecute();
          }
        } else {
          // If not, dispose the current command and create a new one.
          //
          //          command.dispose();
          commandTarget = target;

          dragAndDropCommandInformation = new DragAndDropCommandInformation(target,
              getLocation(event), event.operations, operation, source);

          // DragAndDropManager.create(domain, target, getLocation(event), event.operations, operation, source);
          //
          command = dragAndDropCommandInformation.createCommand();

          if (command != null) {
            valid = command.canExecute();
          }
        }
      }

      // If this command can provide detailed drag and drop feedback...
      //
      //if (command instanceof DragAndDropCommand)
      if (command != null) {
        // Use the feedback for the operation and mouse point from the command.
        //
        event.detail = command.getOperation();
        event.feedback = command.getFeedback();
      } else if (valid) {
        // All we can know is to provide selection feedback.
        //
        event.feedback = DND.FEEDBACK_SELECT;
      } else {
        // There is no executable command, so we'd better nix the whole deal.
        //
        event.detail = DND.DROP_NONE;
        event.feedback = DND.FEEDBACK_SELECT;
      }
    }
  }

  protected void setCurrentDataType(DropTargetEvent event) {
    ObjectTransfer objectTransfer = ObjectTransfer.getInstance();
    TransferData[] dataTypes = event.dataTypes;
    for (int i = 0; i < dataTypes.length; ++i) {
      TransferData transferData = dataTypes[i];
      // If the local tansfer supports this datatype, switch to that data type
      //
      if (objectTransfer.isSupportedType(transferData)) {
        event.currentDataType = transferData;
      }
    }
  }

  /**
   * This attempts to extract the drag source from the event early, i.e., before the drop method.
   * This implementation tries to use a
   * {@link org.eclipse.wst.common.ui.internal.dnd.ObjectTransfer}.
   */
  protected Collection getDragSource(DropTargetEvent event) {
    // Check whether the current data type can be transfered locally.
    //
    ObjectTransfer objectTransfer = ObjectTransfer.getInstance();
    if (!objectTransfer.isSupportedType(event.currentDataType)) {
      // Iterate over the data types to see if there is a datatype that supports a local transfer.
      //
      setCurrentDataType(event);
      return null;
    } else {
      // Transfer the data and extract it.
      //
      Object object = objectTransfer.nativeToJava(event.currentDataType);
      if (object == null) {
        return null;
      } else {
        return extractDragSource(object);
      }
    }
  }

  /**
   * This extracts a collection of dragged source objects from the given object retrieved from the
   * transfer agent. This default implementation converts a structured selection into a collection
   * of elements.
   */
  protected Collection extractDragSource(Object object) {
    // Transfer the data and convert the structured selection to a collection of objects.
    //
    if (object instanceof IStructuredSelection) {
      Collection result = new ArrayList();
      for (Iterator elements = ((IStructuredSelection) object).iterator(); elements.hasNext();) {
        result.add(elements.next());
      }
      return result;
    } else {
      return Collections.EMPTY_LIST;
    }
  }

  /**
   * This gets the amount of time, in milliseconds, to hover over an item before {@link #hover} is
   * called.
   */
  public int getHoverThreshold() {
    return hoverThreshold;
  }

  /**
   * This set the amount of time, in milliseconds, to hover over an item before {@link #hover} is
   * called.
   */
  public void setHoverThreshold(int hoverThreshold) {
    this.hoverThreshold = hoverThreshold;
  }

  /**
   * This is called when the cursor has hovered over the given target for longer than
   * {@link #hoverThreshold}.
   */
  protected void hover(Object target) {
    if (viewer instanceof AbstractTreeViewer) {
      ((AbstractTreeViewer) viewer).expandToLevel(target, 1);
    }
  }

  /**
   * This returns whether a scroll was performed based on the given drag coordinates.
   */
  protected boolean scrollIfNeeded(DropTargetEvent event) {
    // By default we'll not scroll
    //
    boolean result = false;

    // We only handle a tree item right now.
    //
    if (event.item instanceof TreeItem) {
      // Tree items have special data that will help.
      //
      TreeItem treeItem = (TreeItem) event.item;

      // We need need the point in the coordinates of the control and the control's bounds.
      //
      Tree tree = treeItem.getParent();
      Point point = tree.toControl(new Point(event.x, event.y));
      Rectangle bounds = tree.getClientArea();

      // This is the distance in pixels from the top or bottom that will cause scrolling.
      //
      int scrollEpsilon = Math.min(treeItem.getBounds().height, bounds.height / 3);

      // This will be the item that should be scrolled into the view.
      //
      TreeItem scrollTreeItem = null;

      // If we should scroll up.
      //
      if (point.y < scrollEpsilon) {
        // Determine the parent to find the sibling.
        //
        TreeItem parent = treeItem.getParentItem();
        // Walk through the siblings.
        //
        TreeItem[] children = parent == null ? tree.getItems() : parent.getItems();
        for (int i = 0; i < children.length; ++i) {
          // Is this a match.
          //
          if (children[i] == treeItem) {
            // If there is a previous sibling...
            //
            if (i > 0) {
              scrollTreeItem = children[i - 1];

              // Get the last deepest descendent of this previous sibling.
              //
              for (;;) {
                children = scrollTreeItem.getItems();
                if (children != null && children.length != 0 && scrollTreeItem.getExpanded()) {
                  scrollTreeItem = children[children.length - 1];
                } else {
                  break;
                }
              }
            } else {
              // The parent must be the previous.
              //
              scrollTreeItem = parent;
            }

            // We're done after the match.
            //
            break;
          }
        }
      }
      // If we should scroll down...
      //
      else if (bounds.height - point.y < scrollEpsilon) {
        // If this thing has visible children, then the first child must be next.
        //
        TreeItem[] children = treeItem.getItems();
        if (children != null && children.length != 0 && treeItem.getExpanded()) {
          scrollTreeItem = children[0];
        } else {
          // We need the parent to determine siblings and will walk up the tree if we are the last sibling.
          //
          while (scrollTreeItem == null) {
            // If there's no parent, we're done.
            //
            TreeItem parent = treeItem.getParentItem();
            // Walk the children.
            //
            children = parent == null ? tree.getItems() : parent.getItems();
            for (int i = 0; i < children.length; ++i) {
              // When we find the child.
              //
              if (children[i] == treeItem) {
                // If the next index is a valid index...
                //
                if (++i < children.length) {
                  // We've found the item.
                  //
                  scrollTreeItem = children[i];
                }

                // We're done with this parent.
                //
                break;
              }
            }

            if (parent == null) {
              break;
            }

            // Walk up.
            //
            treeItem = parent;
          }
        }
      }

      // If we should scroll.
      //
      if (scrollTreeItem != null) {
        // Only scroll if we're on an item for a while.
        //
        if (previousItem != null && event.time - hoverStart > 200) {
          ScrollBar verticalScrollBar = tree.getVerticalBar();
          if (verticalScrollBar != null) {
            int before = verticalScrollBar.getSelection();

            // Make sure the item is scrolled in place.
            //
            tree.showItem(scrollTreeItem);

            // Make sure we don't scroll again quickly.
            //
            previousItem = null;

            // Indicate that we've done a scroll and nothing else should be done.
            //
            result = before != verticalScrollBar.getSelection();
          }
        } else {
          // If the item changes, reset the timer information.
          //
          if (event.item != previousItem) {
            previousItem = event.item;
            hoverStart = event.time;
          }
        }
      }
    } else if (event.item instanceof TableItem) {
      // Table items have special data that will help.
      //
      TableItem tableItem = (TableItem) event.item;

      // We need need the point in the coordinates of the control and the control's bounds.
      //
      Table table = tableItem.getParent();
      Point point = table.toControl(new Point(event.x, event.y));
      Rectangle bounds = table.getClientArea();
      if (table.getHeaderVisible()) {
        int offset = table.getItemHeight();
        bounds.y += offset;
        bounds.height -= offset;
        point.y -= offset;
      }

      // The position of this item.
      //
      int index = table.indexOf(tableItem);

      // This is the distance in pixels from the top or bottom that will cause scrolling.
      //
      int scrollEpsilon = Math.min(tableItem.getBounds(0).height, bounds.height / 3);

      // This will be the item that should be scrolled into the view.
      //
      TableItem scrollTableItem = null;

      // If we should scroll up.
      //
      if (point.y < scrollEpsilon) {
        if (index > 0) {
          scrollTableItem = table.getItems()[index - 1];
        }
      }
      // If we should scroll down...
      //
      else if (bounds.height - point.y < scrollEpsilon) {
        if (index + 1 < table.getItems().length) {
          scrollTableItem = table.getItems()[index + 1];
        }
      }

      // If we should scroll.
      //
      if (scrollTableItem != null) {
        // Only scroll if we're on an item for a while.
        //
        if (previousItem != null && event.time - hoverStart > 200) {
          ScrollBar verticalScrollBar = table.getVerticalBar();
          if (verticalScrollBar != null) {
            int before = verticalScrollBar.getSelection();

            // Make sure the item is scrolled in place.
            //
            table.showItem(scrollTableItem);

            // Make sure we don't scroll again quickly.
            //
            previousItem = null;

            // Indicate that we've done a scroll and nothing else should be done.
            //
            result = before != verticalScrollBar.getSelection();
          }
        } else {
          // If the item changes, reset the timer information.
          //
          if (event.item != previousItem) {
            previousItem = event.item;
            hoverStart = event.time;
          }
        }
      }
    }

    return result;
  }

  protected static float getLocation(DropTargetEvent event) {
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

  protected class DragAndDropCommandInformation {
    //    protected EditingDomain domain;
    protected Object target;
    protected float location;
    protected int operations;
    protected int operation;
    protected Collection source;

    public DragAndDropCommandInformation(Object target, float location, int operations,
        int operation, Collection source) {
      this.target = target;
      this.location = location;
      this.operations = operations;
      this.operation = operation;
      this.source = new ArrayList(source);
    }

    public DragAndDropCommand createCommand() {
      return dragAndDropManager.createCommand(target, location, operations, operation, source);
    }
  }
}
