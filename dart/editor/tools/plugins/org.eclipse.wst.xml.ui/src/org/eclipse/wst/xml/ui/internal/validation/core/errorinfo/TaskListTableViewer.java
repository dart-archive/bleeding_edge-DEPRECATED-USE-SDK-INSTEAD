/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.validation.core.errorinfo;

import java.util.List;

import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.ISharedImages;
import org.eclipse.wst.validation.internal.provisional.core.IMessage;
import org.eclipse.wst.xml.core.internal.validation.core.ValidationMessage;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;
import org.eclipse.wst.xml.ui.internal.validation.XMLValidationUIMessages;

public class TaskListTableViewer extends TableViewer {
  protected static final int COLUMN_ICON = 0;
  protected static final int COLUMN_DESCRIPTION = 1;
  protected static final int COLUMN_RESOURCE = 3;
  protected static final int COLUMN_LOCATION = 2;

  protected static final String LABEL_ICON = ""; //$NON-NLS-1$
  protected static final String LABEL_DESCRIPTION = XMLValidationUIMessages.TaskListTableViewer_0;
  protected static final String LABEL_RESOURCE = XMLValidationUIMessages.TaskListTableViewer_1;
  protected static final String LABEL_LOCATION = XMLValidationUIMessages.TaskListTableViewer_2;

  protected int visibleRows = -1;

  protected int getColumnWidth(int column) {
    int result = 0;
    switch (column) {
      case COLUMN_ICON: {
        result = 1;
        break;
      }
      case COLUMN_DESCRIPTION: {
        result = 20;
        break;
      }
      case COLUMN_RESOURCE: {
        result = 3;
        break;
      }
      case COLUMN_LOCATION: {
        result = 3;
        break;
      }
    }
    return result;
  }

  public TaskListTableViewer(Composite parent, int visibleRows) {
    this(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER, visibleRows);
  }

  public TaskListTableViewer(Composite parent, int style, int visibleRows) {
    super(new Table(parent, style));
    getTable().setLinesVisible(true);

    Provider provider = new Provider();
    setContentProvider(provider);
    setLabelProvider(provider);

    String[] columnPropertiesArray = {LABEL_ICON, LABEL_DESCRIPTION, LABEL_LOCATION};
    setColumnProperties(columnPropertiesArray);

    Table table = getTable();
    table.setHeaderVisible(true);
    table.setLayoutData(new GridData(GridData.FILL_BOTH));

    TableLayout layout = new TableLayout();

    for (int i = 0; i < columnPropertiesArray.length; i++) {
      TableColumn column = new TableColumn(table, i);
      column.setText(columnPropertiesArray[i]);
      column.setAlignment(SWT.LEFT);
      layout.addColumnData(new ColumnWeightData(getColumnWidth(i), true));
    }

    table.setLayout(layout);

    this.visibleRows = visibleRows;
  }

  /**
   * NamespaceInfoTableLabelProvider
   */
  protected class Provider extends LabelProvider implements ITableLabelProvider,
      IStructuredContentProvider {
    Viewer viewer;
    Image errorImage;
    Image warnImage;

    public Provider() {
      errorImage = XMLUIPlugin.getInstance().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);

      warnImage = XMLUIPlugin.getInstance().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      this.viewer = viewer;
    }

    public Object[] getElements(Object inputElement) {
      List list = (List) viewer.getInput();
      return list != null ? list.toArray() : null;
    }

    public Image getColumnImage(Object object, int columnIndex) {
      ValidationMessage validationMessage = (ValidationMessage) object;
      Image result = null;
      if (columnIndex == 0) {
        int severity = validationMessage.getSeverity();
        if ((severity == IMessage.HIGH_SEVERITY) || (severity == IMessage.NORMAL_SEVERITY)) {
          result = errorImage;
        } else {
          result = warnImage;
        }
      }
      return result;
    }

    public String getColumnText(Object object, int column) {
      ValidationMessage errorMessage = (ValidationMessage) object;
      String result = ""; //$NON-NLS-1$
      switch (column) {
        case COLUMN_DESCRIPTION: {
          result = errorMessage.getMessage();
          break;
        }
        case COLUMN_LOCATION: {
          result = XMLValidationUIMessages.TaskListTableViewer_3 + errorMessage.getLineNumber();
          break;
        }
      }
      return result;
    }
  }
}
