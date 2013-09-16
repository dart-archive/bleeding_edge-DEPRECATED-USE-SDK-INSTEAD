/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.dialogs;

import java.util.List;
import java.util.Vector;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.wst.xml.core.internal.contentmodel.util.NamespaceInfo;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;

public class NamespaceInfoTable extends Composite {

  /**
   * NamespaceInfoTableLabelProvider
   */
  protected class NamespaceInfoTableLabelProvider implements ITableLabelProvider,
      IStructuredContentProvider {

    public void addListener(ILabelProviderListener listener) {
    }

    public void dispose() {
    }

    public Image getColumnImage(Object object, int columnIndex) {
      return null;
    }

    public String getColumnText(Object object, int column) {
      NamespaceInfo info = (NamespaceInfo) object;
      String result = null;
      switch (column) {
        case 0: {
          result = info.uri;
          break;
        }
        case 1: {
          result = info.prefix;
          break;
        }
        case 2: {
          result = info.locationHint;
          break;
        }
      }
      result = result != null ? result : ""; //$NON-NLS-1$
      if (result.equals("")) { //$NON-NLS-1$
        switch (column) {
          case 0: {
            result = XMLUIMessages._UI_NO_NAMESPACE_NAME;
            break;
          }
          case 1: {
            result = XMLUIMessages._UI_NO_PREFIX;
            break;
          }
        }
      }
      return result;
    }

    String getDefaultPrefix() {
      String defaultPrefix = "p"; //$NON-NLS-1$
      if (namespaceInfoList == null) {
        return defaultPrefix;
      }
      Vector v = new Vector();
      for (int i = 0; i < namespaceInfoList.size(); i++) {
        NamespaceInfo nsinfo = (NamespaceInfo) namespaceInfoList.get(i);
        if (nsinfo.prefix != null) {
          v.addElement(nsinfo.prefix);
        }
      }
      if (v.contains(defaultPrefix)) {
        String s = defaultPrefix;
        for (int j = 0; v.contains(s); j++) {
          s = defaultPrefix + Integer.toString(j);
        }
        return s;
      } else {
        return defaultPrefix;
      }
    }

    public Object[] getElements(Object inputElement) {
      return namespaceInfoList.toArray();
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    public boolean isDeleted(Object element) {
      return false;
    }

    public boolean isLabelProperty(Object object, Object property) {
      return false;
    }

    public boolean isLabelProperty(Object element, String property) {
      return false;
    }

    public void removeListener(ILabelProviderListener listener) {
    }
  }

  protected static final String LOCATION_HINT = XMLUIMessages._UI_LABEL_LOCATION_HINT;
  protected static final String NAMESPACE_URI = XMLUIMessages._UI_LABEL_NAMESPACE_NAME;
  protected static final String PREFIX = XMLUIMessages._UI_LABEL_PREFIX;
  protected Button deleteButton;
  protected boolean dummyRowsRemoved = false;
  protected Button editButton;
  protected List namespaceInfoList = new Vector();
  protected Button newButton;
  protected NamespaceInfoTableLabelProvider provider;
  protected IPath resourceLocation;
  protected TableViewer tableViewer;
  protected UpdateListener updateListener;
  protected int visibleRows = -1;

  public NamespaceInfoTable(Composite parent) {
    this(parent, -1, -1, -1);
  }

  public NamespaceInfoTable(Composite parent, int visibleRows) {
    this(parent, -1, -1, visibleRows);
  }

  public NamespaceInfoTable(Composite parent, int widthHint, int heightHint) {
    this(parent, widthHint, heightHint, -1);
  }

  public NamespaceInfoTable(Composite parent, int widthHint, int heightHint, int visibleRows) {
    super(parent, SWT.NONE);
    setLayout(createGridLayout());
    setLayoutData(new GridData(GridData.FILL_BOTH));
    Group namespaceInfoGroup = new Group(this, SWT.NONE);
    namespaceInfoGroup.setText(XMLUIMessages._UI_LABEL_XML_SCHEMA_INFORMATION);
    namespaceInfoGroup.setLayout(new GridLayout());
    GridData gd = new GridData(GridData.FILL_BOTH);
    if (widthHint != -1) {
      gd.widthHint = widthHint;
    }
    if (heightHint != -1) {
      gd.heightHint = heightHint;
    }
    namespaceInfoGroup.setLayoutData(gd);
    // WorkbenchHelp.setHelp(namespaceInfoGroup, new
    // ControlContextComputer(namespaceInfoGroup,
    // XMLBuilderContextIds.XMLC_NAMESPACE_GROUP));
    String[] titleArray = {NAMESPACE_URI, PREFIX, LOCATION_HINT};
    tableViewer = new TableViewer(namespaceInfoGroup, SWT.FULL_SELECTION);
    provider = new NamespaceInfoTableLabelProvider();
    tableViewer.setContentProvider(provider);
    tableViewer.setLabelProvider(provider);
    tableViewer.setColumnProperties(titleArray);
    Table table = tableViewer.getTable();
    table.setHeaderVisible(true);
    table.setLayoutData(new GridData(GridData.FILL_BOTH));
    int[] widthArray = {50, 20, 30};
    TableLayout layout = new TableLayout();
    for (int i = 0; i < titleArray.length; i++) {
      TableColumn column = new TableColumn(table, i);
      column.setText(titleArray[i]);
      column.setAlignment(SWT.LEFT);
      layout.addColumnData(new ColumnWeightData(widthArray[i], true));
    }
    this.visibleRows = visibleRows;
    for (int i = 0; i < visibleRows; i++) {
      TableItem item = new TableItem(table, SWT.NONE);
      item.setText("#######"); //$NON-NLS-1$
    }
    table.setLayout(layout);
    CellEditor[] cellEditors = new CellEditor[titleArray.length];
    cellEditors[1] = new TextCellEditor(table);
    cellEditors[2] = new TextCellEditor(table);
    tableViewer.setCellEditors(cellEditors);
    MouseAdapter mouseAdapter = new MouseAdapter() {
      public void mouseDoubleClick(MouseEvent e) {
        if (tableViewer.getTable().getItem(new Point(e.x, e.y)) != null) {
          performEdit();
        }
      }
    };
    table.addMouseListener(mouseAdapter);
    createButtons(namespaceInfoGroup);
    ISelectionChangedListener selectionChangedListener = new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        updateButtonEnabledState();
      }
    };
    tableViewer.addSelectionChangedListener(selectionChangedListener);
  }

  protected void createButtons(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 2;
    gridLayout.marginHeight = 0;
    gridLayout.marginWidth = 0;
    composite.setLayout(gridLayout);
    Button hiddenButton = new Button(composite, SWT.NONE);
    hiddenButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    hiddenButton.setVisible(false);
    hiddenButton.setEnabled(false);
    SelectionListener selectionListener = new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (e.widget == newButton) {
          performNew();
        } else if (e.widget == editButton) {
          performEdit();
        } else if (e.widget == deleteButton) {
          performDelete();
        }
      }
    };
    // create a composite to hold the three buttons
    Composite buttonComposite = new Composite(composite, SWT.NONE);
    buttonComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    GridLayout buttonGridLayout = new GridLayout();
    buttonGridLayout.numColumns = 3;
    buttonGridLayout.makeColumnsEqualWidth = true;
    buttonComposite.setLayout(buttonGridLayout);
    // add the New button
    //
    newButton = new Button(buttonComposite, SWT.NONE);
    newButton.setText(XMLUIMessages._UI_BUTTON_NEW);
    newButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    newButton.addSelectionListener(selectionListener);
    // add the Edit button
    //
    editButton = new Button(buttonComposite, SWT.NONE);
    editButton.setText(XMLUIMessages._UI_BUTTON_EDIT);
    editButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    editButton.addSelectionListener(selectionListener);
    // add the Delete button
    //
    deleteButton = new Button(buttonComposite, SWT.NONE);
    deleteButton.setText(XMLUIMessages._UI_BUTTON_DELETE);
    deleteButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    deleteButton.addSelectionListener(selectionListener);
  }

  public GridLayout createGridLayout() {
    GridLayout gridLayout = new GridLayout();
    gridLayout.marginWidth = 0;
    gridLayout.horizontalSpacing = 0;
    return gridLayout;
  }

  public List getNamespaceInfoList() {
    return namespaceInfoList;
  }

  protected NamespaceInfo getTargetNamespaceInfo() {
    return ((namespaceInfoList != null) && (namespaceInfoList.size() > 0))
        ? (NamespaceInfo) namespaceInfoList.get(0) : null;
  }

  protected EditNamespaceInfoDialog invokeDialog(String title, NamespaceInfo info) {
    Shell shell = XMLUIPlugin.getInstance().getWorkbench().getActiveWorkbenchWindow().getShell();
    EditNamespaceInfoDialog dialog = new EditNamespaceInfoDialog(shell, info);
    dialog.create();
    dialog.getShell().setText(title);
    dialog.setBlockOnOpen(true);
    dialog.setResourceLocation(resourceLocation);
    dialog.open();
    return dialog;
  }

  protected void performDelayedUpdate() {
    Runnable delayedUpdate = new Runnable() {
      public void run() {
        update();
      }
    };
    getDisplay().asyncExec(delayedUpdate);
    if (updateListener != null) {
      updateListener.updateOccured(this, namespaceInfoList);
    }
  }

  public void performDelete() {
    ISelection selection = tableViewer.getSelection();
    Object selectedObject = (selection instanceof IStructuredSelection)
        ? ((IStructuredSelection) selection).getFirstElement() : null;
    if (selectedObject != null) {
      namespaceInfoList.remove(selectedObject);
      performDelayedUpdate();
    }
  }

  public void performEdit() {
    ISelection selection = tableViewer.getSelection();
    Object selectedObject = (selection instanceof IStructuredSelection)
        ? ((IStructuredSelection) selection).getFirstElement() : null;
    if (selectedObject instanceof NamespaceInfo) {
      invokeDialog(XMLUIMessages._UI_LABEL_NEW_NAMESPACE_INFORMATION,
          (NamespaceInfo) selectedObject);
      performDelayedUpdate();
    }
  }

  public void performNew() {
    NamespaceInfo info = new NamespaceInfo();
    EditNamespaceInfoDialog dialog = invokeDialog(
        XMLUIMessages._UI_LABEL_NEW_NAMESPACE_INFORMATION, info);
    if (dialog.getReturnCode() == Window.OK) {
      namespaceInfoList.add(info);
      performDelayedUpdate();
    }
  }

  public void setNamespaceInfoList(List namespaceInfoList) {
    this.namespaceInfoList = namespaceInfoList;
    update();
  }

  public void setResourceLocation(IPath resourceLocation) {
    this.resourceLocation = resourceLocation;
  }

  public void setUpdateListener(UpdateListener updateListener) {
    this.updateListener = updateListener;
  }

  public void update() {
    updateHelper(namespaceInfoList);
  }

  public void updateButtonEnabledState() {
    ISelection selection = tableViewer.getSelection();
    Object selectedObject = (selection instanceof IStructuredSelection)
        ? ((IStructuredSelection) selection).getFirstElement() : null;
    NamespaceInfo info = (NamespaceInfo) selectedObject;
    editButton.setEnabled(info != null);
    deleteButton.setEnabled((info != null) && (info.getProperty("unremovable") == null)); //$NON-NLS-1$
  }

  public void updateHelper(List namespaceInfoList) {
    if ((visibleRows != -1) && !dummyRowsRemoved) {
      dummyRowsRemoved = true;
      tableViewer.getTable().removeAll();
    }
    ISelection selection = tableViewer.getSelection();
    tableViewer.setInput(namespaceInfoList);
    if (selection.isEmpty()) {
      if (namespaceInfoList.size() > 0) {
        tableViewer.setSelection(new StructuredSelection(namespaceInfoList.get(0)));
      }
    } else {
      tableViewer.setSelection(selection);
    }
  }
}
