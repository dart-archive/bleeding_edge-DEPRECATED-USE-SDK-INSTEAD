/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - Initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.common.ui.internal.viewers;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableTreeViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableCursor;
import org.eclipse.swt.custom.TableTreeItem;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * Adds a TableCursor to a StructuredViewer - for keyboard navigation of the table The intent of
 * this class is to provide the standard listeners for using F2 to activate cell editors. Due to a
 * current bug in the TableCursor, TableViewers using this class must make a call similar to the
 * TableNavigator method moveCellEditorsAbove(cellEditors) whenever a setCellEditors call is made in
 * the StructuredViewer. This is so that the cell editor control shows up above the table cursor
 * control.
 */

public class TableNavigator extends TableCursor {
  private static final String TABLETREEITEM_ID = "TableTreeItemID";

  final Table table;

  public TableNavigator(Table table, StructuredViewer viewer) {
    super(table, SWT.NONE);
    this.table = table;
    final Table currentTable = table;
    final StructuredViewer sViewer = viewer;

    // Linux index out of bounds fix.  See defect 253429, 253433, and more
    setVisible(false);

    addPaintListener(viewer);
    addKeyListeners(viewer);
    addMouseListeners(viewer);
    addSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(SelectionEvent)
       */
      public void widgetSelected(SelectionEvent e) {
        super.widgetSelected(e);
        if (sViewer instanceof TableTreeViewer) {
          TableTreeItem tableTreeItem = (TableTreeItem) getRow().getData(TABLETREEITEM_ID);
          StructuredSelection selection = new StructuredSelection(tableTreeItem.getData());
          sViewer.setSelection(selection, true);
        }
      }

    });
    addFocusListener(new FocusAdapter() {
      public void focusGained(FocusEvent e) {
        // if e.source is not a child of the table then set selection - this is for tab into viewer
        Object eventSource = e.getSource();
        if (eventSource instanceof Control) {
          if (!isChild(currentTable, (Control) eventSource)) {
            if (currentTable.getItemCount() > 0 && currentTable.getSelectionCount() <= 0) {
              if (sViewer instanceof TableTreeViewer) {
                TableTreeItem tableTreeItem = (TableTreeItem) getRow().getData(TABLETREEITEM_ID);
                StructuredSelection selection = new StructuredSelection(tableTreeItem.getData());
                sViewer.setSelection(selection, true);
              } else {
                currentTable.setSelection(0);
                setSelection(0, 0);
              }
            }
          } else {
            if (currentTable.getItems().length > 0) {
              // cursor can end up on a non-existent table row 
              //   currently no way to get the current table cursor row 
              //   so for now just catch the exception since it doesn't
              //   cause any side effects.
              try {
                setVisible(true);
              } catch (Exception ee) {
                currentTable.setSelection(0);
                setSelection(0, 0);
              }
            } else // do not show table cursor if there are no elements in the table - avoid repaint
            {
              setVisible(false);
            }
          }
        }
      }

      protected boolean isChild(Control parent, Control child) {
        Control tempChild = child;
        while (tempChild != null) {
          if (tempChild == parent) {
            return true;
          }
          tempChild = tempChild.getParent();
        }
        return false;
      }

      /**
       * @see org.eclipse.swt.events.FocusAdapter#focusLost(FocusEvent)
       */
      public void focusLost(FocusEvent e) {
        // Set the table navigator to be not visible if the the table
        // is not in focus and a child of the table is not in focus
        // note that we do this asynchronously so we don't mess up the
        // current focus handling.
        Display.getDefault().asyncExec(new Runnable() {
          /**
           * @see java.lang.Runnable#run()
           */
          public void run() {
            if (currentTable != null && !currentTable.isDisposed()
                && !currentTable.isFocusControl()
                && !isChild(currentTable, Display.getDefault().getFocusControl())) {
              setVisible(false);
            }
          }
        });
      }

    });

    table.addFocusListener(new FocusAdapter() {
      /**
       * @see org.eclipse.swt.events.FocusListener#focusGained(FocusEvent)
       */
      public void focusGained(FocusEvent e) {
        // only display navigator if there are items in the table 
        // and if the focus wasn't gained from our own table navigator
        // (ie focus came from outside)
        if (currentTable.getItemCount() > 0 && (Display.getDefault().getFocusControl() != null)
            && !Display.getDefault().getFocusControl().equals(TableNavigator.this)) {
          // note that we do this asynchronously so we don't mess up the
          // current focus handling.
          Display.getDefault().asyncExec(new Runnable() {
            /**
             * @see java.lang.Runnable#run()
             */
            public void run() {
              if (!isVisible()) {
                try {
                  setVisible(true);
                  setFocus();
                } catch (Exception e) {
                  // catch IllegalArgumentExceptions here - index out of bounds on tableviewer
                  if (currentTable.getItemCount() > 0) {
                    currentTable.setSelection(0);
                    setSelection(0, 0);
                  } else // do not show table cursor if there are no elements in the table - avoid repaint
                  {
                    setVisible(false);
                  }
                }
              }
            }
          });
        }
      }
    });
  }

  public Table getTable() {
    return table;
  }

  public void addPaintListener(StructuredViewer viewer) {

    addPaintListener(new PaintListener() {
      public void paintControl(PaintEvent e) {
        TableItem[] selection = table.getSelection();
        final TableItem row = (selection.length == 0) ? table.getItem(table.getTopIndex())
            : selection[0];
        final String cellText = row.getText(getColumn());
        final Image cellImage = row.getImage(getColumn());
        final int col = getColumn();

        Display.getCurrent().asyncExec(new Runnable() {
          public void run() {
            if (!row.isDisposed()) {
              String newText = row.getText(getColumn());
              if (!newText.equals(cellText) || !(row.getImage(col) == cellImage)) {
                redraw();
              }
            }
          }
        });
      }
    });
  }

  public SelectionKeyAdapter getKeyAdapter(StructuredViewer viewer) {
    if (keyAdapter == null) {
      return new SelectionKeyAdapter(viewer);
    } else
      return keyAdapter;
  }

  public void setKeyAdapter(SelectionKeyAdapter kAdapter) {
    keyAdapter = kAdapter;
  }

  protected SelectionKeyAdapter keyAdapter = null;

  public class SelectionKeyAdapter extends KeyAdapter {
    StructuredViewer structuredViewer;

    public SelectionKeyAdapter(StructuredViewer viewer) {
      super();
      this.structuredViewer = viewer;
    }

    int lastKeyPressed = -1; // used to cache the last key for key combos

    public void keyPressed(KeyEvent e) {
      TableItem row = getRow();
      int column = getColumn();

      // hack to emulate SHIFT+F10 popup menu - otherwise table cursor
      //   obscures the table popup mechanism and it doesn't work.
      if (lastKeyPressed == SWT.SHIFT && e.keyCode == SWT.F10) {
        Menu popup = getTable().getMenu();
        popup.setVisible(true);
      }
      lastKeyPressed = e.keyCode;

      //jvh - look for + or - key
      // column == 0
      if (row.getData(TABLETREEITEM_ID) instanceof TableTreeItem) {
        if (column == 0 && e.character == '+') {
          TableTreeItem tableTreeItem = (TableTreeItem) row.getData(TABLETREEITEM_ID);
          ((TableTreeViewer) structuredViewer).setExpandedState(tableTreeItem.getData(), true);
          refresh();
        } else if (column == 0 && e.character == '-') {
          TableTreeItem tableTreeItem = (TableTreeItem) row.getData(TABLETREEITEM_ID);
          ((TableTreeViewer) structuredViewer).setExpandedState(tableTreeItem.getData(), false);
          refresh();
        }
      }
      // use F2 to invoke editing for a cell
      if (e.keyCode == SWT.F2) {
        if (structuredViewer instanceof TableViewer) {
          ((TableViewer) structuredViewer).editElement(row.getData(), column);
        } else if (structuredViewer instanceof TableTreeViewer) {
          TableTreeItem tableTreeItem = (TableTreeItem) row.getData(TABLETREEITEM_ID);
          ((TableTreeViewer) structuredViewer).editElement(tableTreeItem.getData(), column);
        }
      }
    }
  }

  public void addKeyListeners(StructuredViewer viewer) {
    final StructuredViewer structuredViewer = viewer;

    addKeyListener(getKeyAdapter(structuredViewer));
  }

  public void addMouseListeners(StructuredViewer viewer) {
    final StructuredViewer structuredViewer = viewer;

    addMouseListener(new MouseAdapter() {

      public void mouseUp(MouseEvent e) {
        TableItem row = getRow();
        int column = getColumn();

        // use mouse button 1 to invoke editing for a cell
        if (e.button == 1) {
          if (structuredViewer instanceof TableViewer) {
            ((TableViewer) structuredViewer).editElement(row.getData(), column);
          } else if (structuredViewer instanceof TableTreeViewer && column == 1) {
            TableTreeItem tableTreeItem = (TableTreeItem) row.getData(TABLETREEITEM_ID);
            ((TableTreeViewer) structuredViewer).editElement(tableTreeItem.getData(), column);
          }

          if (structuredViewer instanceof TableTreeViewer
              && row.getData(TABLETREEITEM_ID) instanceof TableTreeItem) {
            if (column == 0) {
              TableTreeItem tableTreeItem = (TableTreeItem) row.getData(TABLETREEITEM_ID);
              boolean expandState = tableTreeItem.getExpanded();
              ((TableTreeViewer) structuredViewer).setExpandedState(tableTreeItem.getData(),
                  !expandState);
              refresh();
            }
          }
        }
      }
    });
  }

  /**
   * Ensure that cell editor control shows up above the table cursor control. Should be called
   * whenever the table viewer makes a new call to setCellEditors i.e. in constructor and in
   * refreshCellEditors
   * 
   * @param - array of cell editors for the StructuredViewer
   */

  public void moveCellEditorsAbove(CellEditor[] editorArray) {
    for (int i = 0; i < editorArray.length; i++) {
      CellEditor cEd = editorArray[i];
      if (cEd != null && cEd.getControl() != null) {
        cEd.getControl().moveAbove(null);
      }
    }
  }

  public void refresh() {
    Display.getCurrent().asyncExec(new Runnable() {
      public void run() {
        if (!isDisposed() && isVisible())
          redraw();
      }
    });
  }
}
