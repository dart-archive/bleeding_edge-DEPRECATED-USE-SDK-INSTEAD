/*******************************************************************************
 * Copyright (c) 2001, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.nsedit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wst.xml.core.internal.contentmodel.util.NamespaceInfo;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.eclipse.wst.xml.ui.internal.dialogs.EditNamespaceInfoDialog;
import org.eclipse.wst.xml.ui.internal.dialogs.NamespaceInfoErrorHelper;

public class CommonEditNamespacesDialog {
  protected Composite commonComposite;
  protected Button deleteButton;
  protected Button editButton;

  protected Label errorMessageLabel;
  protected int heightHint = 250;
  protected List namespaceInfoList = new ArrayList();

  protected Button newButton;
  protected IPath resourceLocation;

  private boolean showLocationText = false;
  protected String tableLabel = ""; //$NON-NLS-1$
  protected CommonNamespaceInfoTable tableViewer;

  protected Composite topComposite;
  protected boolean useGroup;
  protected int widthHint = 500;

  public CommonEditNamespacesDialog(Composite parent, IPath resourceLocation,
      String stringTableLabel) {
    this(parent, resourceLocation, stringTableLabel, false, false);
  }

  public CommonEditNamespacesDialog(Composite parent, IPath resourceLocation,
      String stringTableLabel, boolean useGroup, boolean showLocText) {
    this.resourceLocation = resourceLocation;
    tableLabel = stringTableLabel;
    this.useGroup = useGroup;
    showLocationText = showLocText;

    GridData gd = new GridData(GridData.FILL_BOTH);
    if (widthHint != -1) {
      gd.widthHint = widthHint;
    }
    if (heightHint != -1) {
      gd.heightHint = heightHint;
    }

    // Set GridData and GridLayout for the parent Composite
    parent.setLayoutData(gd);
    parent.setLayout(new GridLayout());

    // Create the top Composite
    topComposite = new Composite(parent, SWT.NONE);
    GridData topData = new GridData(GridData.FILL_HORIZONTAL);
    topData.heightHint = 0;
    topComposite.setLayoutData(topData);
    topComposite.setLayout(new GridLayout());

    // Create the 'common'/middle Composite
    if (useGroup) {
      commonComposite = new Group(parent, SWT.NONE);
    } else {
      commonComposite = new Composite(parent, SWT.NONE);
    }
    commonComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
    commonComposite.setLayout(new GridLayout(3, false));

    // Add the error Message Label
    errorMessageLabel = new Label(parent, SWT.NONE);
    errorMessageLabel.setLayoutData(createHorizontalFill());
    Color color = new Color(errorMessageLabel.getDisplay(), 200, 0, 0);
    errorMessageLabel.setForeground(color);

    createControlArea();
  }

  protected void createButtons(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayoutData(new GridData(GridData.FILL_VERTICAL));
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 1;
    gridLayout.marginHeight = 0;
    gridLayout.marginWidth = 0;
    composite.setLayout(gridLayout);

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
    buttonComposite.setLayoutData(createHorizontalFill());
    GridLayout buttonGridLayout = new GridLayout();
    // buttonGridLayout.numColumns = 3;
    // buttonGridLayout.makeColumnsEqualWidth = true;
    buttonComposite.setLayout(buttonGridLayout);

    // add the New button
    //
    newButton = new Button(buttonComposite, SWT.NONE);
    // newButton.setText(" " +
    // XMLCommonUIPlugin.getInstance().getString("_UI_BUTTON_NEW") + " ");
    newButton.setText("   " + XMLUIMessages.CommonEditNamespacesDialog_0 + "   "); //$NON-NLS-1$ //$NON-NLS-2$ 
    newButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL)); // ViewUtility.createHorizontalFill());
    newButton.addSelectionListener(selectionListener);

    // add the Edit button
    //
    // gd = new GridData();
    // gd.horizontalAlignment = gd.FILL;
    // gd.grabExcessHorizontalSpace = true;

    editButton = new Button(buttonComposite, SWT.NONE);
    editButton.setText(XMLUIMessages._UI_BUTTON_EDIT);
    editButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL)); // ViewUtility.createHorizontalFill());
    editButton.addSelectionListener(selectionListener);

    // add the Delete button
    //
    // gd = new GridData();
    // gd.horizontalAlignment = gd.FILL;
    // gd.grabExcessHorizontalSpace = true;

    deleteButton = new Button(buttonComposite, SWT.NONE);
    deleteButton.setText(XMLUIMessages._UI_BUTTON_DELETE);
    deleteButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL)); // ViewUtility.createHorizontalFill());
    deleteButton.addSelectionListener(selectionListener);
  }

  private void createControlArea() {
    if (useGroup) {
      ((Group) commonComposite).setText(tableLabel);
    } else {
      Label label = new Label(commonComposite, SWT.NONE);
      label.setText(tableLabel);
      label.setLayoutData(createGridData(false, 3));
    }

    tableViewer = new CommonNamespaceInfoTable(commonComposite, 6, showLocationText);
    tableViewer.getControl().setLayoutData(createGridData(true, 2));
    createButtons(commonComposite);

    tableViewer.setInput(namespaceInfoList);
    updateButtonEnabledState();
    ISelectionChangedListener selectionChangedListener = new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        updateButtonEnabledState();
      }
    };
    tableViewer.addSelectionChangedListener(selectionChangedListener);
  }

  protected GridData createGridData(boolean both, int span) {
    GridData gd = new GridData(both ? GridData.FILL_BOTH : GridData.FILL_HORIZONTAL);
    gd.horizontalSpan = 2;
    return gd;
  }

  private GridData createHorizontalFill() {
    GridData gd = new GridData();
    gd.horizontalAlignment = GridData.FILL;
    gd.grabExcessHorizontalSpace = true;
    return gd;
  }

  public NamespaceInfo getNamespaceInfo(String namespace) {
    NamespaceInfo result = null;
    for (Iterator i = namespaceInfoList.iterator(); i.hasNext();) {
      NamespaceInfo info = (NamespaceInfo) i.next();
      if ((info.uri != null) && info.uri.equals(namespace)) {
        result = info;
        break;
      }
    }
    return result;
  }

  protected Object getSelection(ISelection selection) {
    if (selection == null) {
      return null;
    } // end of if ()

    Object result = null;
    if (selection instanceof IStructuredSelection) {
      IStructuredSelection es = (IStructuredSelection) selection;
      Iterator i = es.iterator();
      if (i.hasNext()) {
        result = i.next();
      }
    }
    return result;
  }

  /*
   * Use the returned Composite to add content above the 'common contents'. Note: The GridData for
   * the returned Composite has a heightHint = 0. This means when using the returned Composite, the
   * GridData must be reset, else the Composite and it's contents will not appear.
   */
  protected Composite getTopComposite() {
    return topComposite;
  }

  protected EditNamespaceInfoDialog invokeDialog(String title, NamespaceInfo info) {
    EditNamespaceInfoDialog dialog = new EditNamespaceInfoDialog(topComposite.getShell(), info);
    dialog.create();
    dialog.getShell().setText(title);
    dialog.setBlockOnOpen(true);
    dialog.setResourceLocation(resourceLocation);
    dialog.open();
    return dialog;
  }

  protected void performDelayedUpdate() {
    tableViewer.refresh();
    /*
     * Runnable delayedUpdate = new Runnable() { public void run() { tableViewer.refresh(); } };
     * Display.getCurrent().asyncExec(delayedUpdate);
     */
    // if (updateListener != null)
    // {
    // updateListener.updateOccured(this, namespaceInfoList);
    // }
  }

  public void performDelete() {
    ISelection selection = tableViewer.getSelection();
    if (selection instanceof IStructuredSelection) {
      IStructuredSelection structuredSelection = (IStructuredSelection) selection;
      namespaceInfoList.removeAll(structuredSelection.toList());
      updateErrorMessage(namespaceInfoList);
      performDelayedUpdate();
    }
  }

  public void performEdit() {
    Object selection = getSelection(tableViewer.getSelection());
    if (selection != null) {
      invokeDialog(XMLUIMessages._UI_LABEL_NEW_NAMESPACE_INFORMATION, (NamespaceInfo) selection);
      updateErrorMessage(namespaceInfoList);
      performDelayedUpdate();
    }
  }

  public void performNew() {
    CommonAddNamespacesDialog dialog = new CommonAddNamespacesDialog(topComposite.getShell(),
        XMLUIMessages._UI_ADD_NAMESPACE_DECLARATIONS, resourceLocation, namespaceInfoList);
    dialog.createAndOpen();
    if (dialog.getReturnCode() == Window.OK) {
      namespaceInfoList.addAll(dialog.getNamespaceInfoList());
      updateErrorMessage(namespaceInfoList);
      performDelayedUpdate();
    }
  }

  public void setNamespaceInfoList(List list) {
    namespaceInfoList = list;
    tableViewer.setInput(namespaceInfoList);
  }

  public void updateButtonEnabledState() {
    Object selection = getSelection(tableViewer.getSelection());
    NamespaceInfo info = (NamespaceInfo) selection;
    editButton.setEnabled(info != null);
    deleteButton.setEnabled((info != null) && (info.getProperty("unremovable") == null)); //$NON-NLS-1$
  }

  public void updateErrorMessage(List namespaceInfoList) {
    NamespaceInfoErrorHelper helper = new NamespaceInfoErrorHelper();
    String errorMessage = helper.computeErrorMessage(namespaceInfoList, null);
    errorMessageLabel.setText(errorMessage != null ? errorMessage : ""); //$NON-NLS-1$
  }

  public void setResourcePath(IPath path) {
    resourceLocation = path;
  }
}
