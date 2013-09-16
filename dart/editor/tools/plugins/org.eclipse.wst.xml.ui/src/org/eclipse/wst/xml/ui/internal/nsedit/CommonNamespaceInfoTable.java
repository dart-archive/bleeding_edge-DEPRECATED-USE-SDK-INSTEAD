/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.nsedit;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.wst.xml.core.internal.contentmodel.util.NamespaceInfo;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;

public class CommonNamespaceInfoTable extends TableViewer {

  /**
   * NamespaceInfoTableLabelProvider
   */
  protected class Provider extends LabelProvider implements ITableLabelProvider,
      IStructuredContentProvider {
    Viewer viewer;

    public Image getColumnImage(Object object, int columnIndex) {
      Image result = null;
      int columnCode = getColumnCode(columnIndex);
      if (columnCode == COLUMN_LOCATION_HINT) {
        NamespaceInfo info = (NamespaceInfo) object;
        if ((info.locationHint == null) || info.locationHint.trim().equals("")) { //$NON-NLS-1$
          // Comment this out until we solve the alignment/(space
          // for image being allocated
          // to prefix column) ......
          // result =
          // XMLEditorPluginImageHelper.getInstance().getImage(XMLEditorPluginImages.IMG_OBJ_WARNING_OBJ);
        }
      }

      return result;
    }

    public String getColumnText(Object object, int column) {
      NamespaceInfo info = (NamespaceInfo) object;
      String result = null;
      int columnCode = getColumnCode(column);
      switch (columnCode) {
        case COLUMN_PREFIX: {
          result = info.prefix;
          break;
        }
        case COLUMN_NAMESPACE_URI: {
          result = info.uri;
          break;
        }
        case COLUMN_CHECKBOX: {
          result = ""; // info.locationHint; //$NON-NLS-1$
          break;
        }
        case COLUMN_LOCATION_HINT: {
          result = info.locationHint;
          break;
        }
      }
      result = result != null ? result : ""; //$NON-NLS-1$
      if (result.equals("")) { //$NON-NLS-1$
        switch (columnCode) {
          case COLUMN_PREFIX: {
            result = XMLUIMessages._UI_NO_PREFIX;
            break;
          }
          case COLUMN_NAMESPACE_URI: {
            result = XMLUIMessages._UI_NO_NAMESPACE_NAME;
            break;
          }
        }
      }
      return result;
    }

    public Object[] getElements(Object inputElement) {
      List list = (List) viewer.getInput();
      return list != null ? list.toArray() : null;
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      this.viewer = viewer;
    }
  }

  class TableItemChecker extends MouseAdapter {
    public void mouseDown(MouseEvent e) {
      TableItem item = getTable().getItem(new Point(e.x, e.y));
      if (item != null) {
        Object obj = item.getData();
        if (obj != null) {
          NamespaceInfo info = (NamespaceInfo) obj;
          TableColumn col = getTable().getColumn(0);
          if (e.x < col.getWidth()) // if the point falls within
          // the
          // Select column then perform
          // check/uncheck
          {
            String currentState = (String) info.getProperty("checked"); //$NON-NLS-1$
            System.out.println("currentState" + currentState); //$NON-NLS-1$
            if ((currentState == null) || currentState.equals("false")) //$NON-NLS-1$
            {
              info.setProperty("checked", "true"); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
              info.setProperty("checked", "false"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            refresh();
          }
        }
      }
    }
  }

  protected static final int COLUMN_CHECKBOX = 1;
  protected static final int COLUMN_LOCATION_HINT = 4;
  protected static final int COLUMN_NAMESPACE_URI = 2;
  protected static final int COLUMN_PREFIX = 3;

  protected static final String LABEL_CHECKBOX = ""; //$NON-NLS-1$
  protected static final String LABEL_LOCATION_HINT = XMLUIMessages._UI_LABEL_LOCATION_HINT;
  protected static final String LABEL_NAMESPACE_URI = XMLUIMessages._UI_LABEL_NAMESPACE_NAME;
  protected static final String LABEL_PREFIX = XMLUIMessages._UI_LABEL_PREFIX;
  protected List checkedList = new ArrayList();

  // protected List namespaceInfoList = new ArrayList();
  protected int[] columnIndexMap;
  protected boolean showCheckBoxes = true;
  private boolean showLocationText = false;

  private Table table;
  protected int visibleRows = -1;

  public CommonNamespaceInfoTable(Composite parent, int visibleRows) {
    this(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER, visibleRows, false);
  }

  // protected CellEditor getCellEditor(int column)
  // {
  // return (column == COLUMN_CHECKBOX) ? checkBoxCellEditor :
  // textCellEditor;
  // }

  public CommonNamespaceInfoTable(Composite parent, int visibleRows, boolean showLocationText) {
    this(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER, visibleRows, showLocationText);
  }

  public CommonNamespaceInfoTable(Composite parent, int style, int visibleRows) {
    this(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER | style, visibleRows, false);
  }

  public CommonNamespaceInfoTable(Composite parent, int style, int visibleRows,
      boolean showLocationText) {
    super(new Table(parent, style));
    getTable().setLinesVisible(true);
    this.showCheckBoxes = (style & SWT.CHECK) != 0;
    columnIndexMap = createColumnIndexMap();
    this.showLocationText = showLocationText;

    Provider provider = new Provider();
    setContentProvider(provider);
    setLabelProvider(provider);

    String[] columnPropertiesArray = createColumnPropertiesArray();
    setColumnProperties(columnPropertiesArray);

    table = getTable();
    table.setHeaderVisible(true);
    table.setLayoutData(new GridData(GridData.FILL_BOTH));

    TableLayout layout = new TableLayout();

    for (int i = 0; i < columnPropertiesArray.length; i++) {
      TableColumn column = new TableColumn(table, i);
      if ((columnPropertiesArray[i]).equals(LABEL_LOCATION_HINT)) {
        if (showLocationText) {
          column.setText(columnPropertiesArray[i]);
        } else {
          // Comment this out until we solve the alignment/(space
          // for image being allocated
          // to prefix column) ......
          // column.setImage(XMLEditorPluginImageHelper.getInstance().getImage(XMLEditorPluginImages.IMG_OBJ_TXTEXT));
        }
      } else {
        column.setText(columnPropertiesArray[i]);
      }
      column.setAlignment(SWT.LEFT);
      layout.addColumnData(new ColumnWeightData(getColumnWidth(i), true));
    }
    table.setLayout(layout);

    this.visibleRows = visibleRows;
    // for (int i = 0; i < visibleRows; i++)
    // {
    // TableItem item = new TableItem(table, SWT.NONE);
    // item.setText("#######");
    // }
    // checkBoxCellEditor = new NamespaceInfoCheckboxCellEditor(table);
    // textCellEditor = new TextCellEditor(table);

    /*
     * CellEditor[] cellEditors = new CellEditor[columnPropertiesArray.length]; for (int i = 0; i <
     * columnPropertiesArray.length; i++) { cellEditors[i] = getCellEditor(i); }
     * setCellEditors(cellEditors);
     */
    // if (showCheckBoxes)
    // {
    // getTable().addMouseListener(new TableItemChecker());
    // }
  }

  // protected CellEditor checkBoxCellEditor;
  // protected CellEditor textCellEditor;

  protected int[] createColumnIndexMap() {
    int[] result = new int[showCheckBoxes ? 4 : 3];
    int i = 0;
    if (showCheckBoxes) {
      result[i++] = COLUMN_CHECKBOX;
    }
    result[i++] = COLUMN_PREFIX;
    result[i++] = COLUMN_NAMESPACE_URI;
    if (!showCheckBoxes) {
      result[i++] = COLUMN_LOCATION_HINT;
    }
    return result;
  }

  protected String[] createColumnPropertiesArray() {
    String[] result = new String[3];
    int i = 0;
    if (showCheckBoxes) {
      result[i++] = LABEL_CHECKBOX;
    }
    result[i++] = LABEL_PREFIX;
    result[i++] = LABEL_NAMESPACE_URI;
    if (!showCheckBoxes) {
      result[i++] = LABEL_LOCATION_HINT;
    }
    return result;
  }

  protected int getColumnCode(int column) {
    int result = 0;
    if (column < columnIndexMap.length) {
      result = columnIndexMap[column];
    }
    return result;
  }

  protected int getColumnWidth(int column) {
    int result = 0;
    switch (getColumnCode(column)) {
      case COLUMN_PREFIX: {
        result = 5;
        break;
      }
      case COLUMN_NAMESPACE_URI: {
        // Size columns differently when location hint text label is
        // displayed
        if (showLocationText) {
          result = 10;
        } else {
          result = 20;
        }
        break;
      }
      case COLUMN_CHECKBOX: {
        result = 1; // info.locationHint;
        break;
      }
      case COLUMN_LOCATION_HINT: {
        // Size columns differently when location hint text label is
        // displayed
        if (showLocationText) {
          result = 10;
        } else {
          result = 2;
        }
        break;
      }
    }
    return result;
  }
  /*
   * protected class NamespaceInfoCellModifier implements ICellModifier { public
   * NamespaceInfoCellModifier() { }
   * 
   * public boolean canModify(Object element, String property) { if
   * (property.equals(LABEL_CHECKBOX)) { return true; } else if (property.equals(LABEL_PREFIX)) {
   * return true; } return false; }
   * 
   * public Object getValue(Object element, String property) { int column = 0; if
   * (property.equals(LABEL_CHECKBOX)) { column = 0; } else if (property.equals(LABEL_PREFIX)) {
   * column = 1; } else if (property.equals(LABEL_NAMESPACE_URI)) { column = 2; }
   * 
   * //if (element instanceof TableElement) //{ // return provider.getColumnText(element, column);
   * //} //else //{ // return null; // } return "hello"; }
   * 
   * public void modify(Object element, String property, Object value) { } }
   * 
   * protected class NamespaceInfoCheckboxCellEditor extends CheckboxCellEditor implements
   * MouseListener { public NamespaceInfoCheckboxCellEditor(Composite parent) { super(parent); }
   * 
   * protected void doSetValue(Object value) { }
   * 
   * public void activate() { super.activate(); deactivate();
   * Display.getCurrent().getFocusControl().redraw(); }
   * 
   * public void mouseDown(MouseEvent e) { if (getTable().getItem(new Point(e.x, e.y)) != null) { }
   * } public void mouseDoubleClick(MouseEvent e) { } public void mouseUp(MouseEvent e) { } }
   */
}
