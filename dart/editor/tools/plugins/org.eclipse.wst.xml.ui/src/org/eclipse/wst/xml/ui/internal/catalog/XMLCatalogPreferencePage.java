/*******************************************************************************
 * Copyright (c) 2002, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring David Carver/STAR
 * Standard - d_a_carver@yahoo.com - bug 192568 Removed Advanced button - Functionality is now in
 * the Import/Export XML Catalog Wizards.
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.catalog;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.wst.xml.core.internal.XMLCorePlugin;
import org.eclipse.wst.xml.core.internal.catalog.CatalogSet;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalog;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalogElement;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalogEvent;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalogListener;
import org.eclipse.wst.xml.core.internal.catalog.provisional.INextCatalog;

public class XMLCatalogPreferencePage extends PreferencePage implements IWorkbenchPreferencePage,
    ICatalogListener {

  protected XMLCatalogEntriesView catalogEntriesView;

  protected ICatalog workingUserCatalog;

  protected ICatalog systemCatalog;

  protected ICatalog userCatalog;

  protected ICatalog defaultCatalog;

  protected Button advancedButton;

  public XMLCatalogPreferencePage() {
    defaultCatalog = XMLCorePlugin.getDefault().getDefaultXMLCatalog();
    INextCatalog[] nextCatalogs = defaultCatalog.getNextCatalogs();
    for (int i = 0; i < nextCatalogs.length; i++) {
      INextCatalog catalog = nextCatalogs[i];
      ICatalog referencedCatalog = catalog.getReferencedCatalog();
      if (referencedCatalog != null) {
        if (XMLCorePlugin.SYSTEM_CATALOG_ID.equals(referencedCatalog.getId())) {
          systemCatalog = referencedCatalog;
        } else if (XMLCorePlugin.USER_CATALOG_ID.equals(referencedCatalog.getId())) {
          userCatalog = referencedCatalog;
        }
      }
    }
  }

  public void dispose() {
    super.dispose();
    workingUserCatalog.removeListener(this);
  }

  /**
   * Refresh the view in responce to an event sent by the Catalog
   */
  public void catalogChanged(ICatalogEvent event) {
    catalogEntriesView.updatePage();
  }

  /**
   * Creates preference page controls on demand.
   * 
   * @param parent the parent for the preference page
   */
  protected Control createContents(Composite parent) {
    // we create a working copy of the 'User Settings' for the Catalog
    // that we can modify
    CatalogSet tempCatalogSet = new CatalogSet();
    workingUserCatalog = tempCatalogSet.lookupOrCreateCatalog("working", ""); //$NON-NLS-1$ //$NON-NLS-2$

    // TODO: add entries from the nested catalogs as well
    workingUserCatalog.addEntriesFromCatalog(userCatalog);
    workingUserCatalog.addListener(this);
    noDefaultAndApplyButton();
    Composite composite = new Composite(parent, SWT.NULL);
    // WorkbenchHelp.setHelp(composite, new
    // ControlContextComputer(composite,
    // XMLBuilderContextIds.XMLP_CATALOG_PAGE));
    composite.setLayout(new GridLayout());
    composite.setLayoutData(new GridData(GridData.FILL_BOTH));
    createCatalogEntriesView(composite);
    createCatalogDetailsView(composite);
    //createAdvancedButton(composite);
    // catalogEntriesView.updatePage();
    applyDialogFont(composite);

    return composite;
  }

  /**
   * @deprecated
   * @param composite
   */
  protected void createAdvancedButton(Composite composite) {
    Composite panel = new Composite(composite, SWT.NONE);
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 3;
    panel.setLayout(gridLayout);
    panel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    // TODO... is there a better way to expand the page width?
    // This invisible label is created to force the width of the page to
    // be
    // wide enough to show
    // the rather long uri and key fields of a catalog entry.
    Label widthFudger = new Label(panel, SWT.NONE);
    String widthFudgerString = ""; //$NON-NLS-1$
    for (int i = 0; i < 55; i++) {
      widthFudgerString += "x"; //$NON-NLS-1$
    }
    widthFudger.setText(widthFudgerString);
    widthFudger.setVisible(false);
    Composite placeHolder = new Composite(panel, SWT.NONE);
    placeHolder.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    advancedButton = new Button(panel, SWT.NONE);
    advancedButton.setText(XMLCatalogMessages.UI_LABEL_ADVANCED);
    SelectionListener selectionListener = new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        AdvancedOptionsDialog dialog = new AdvancedOptionsDialog(getShell(), workingUserCatalog);
        dialog.create();
        dialog.getShell().setText(XMLCatalogMessages.UI_LABEL_ADVANCED_XML_CATALOG_PREFS);
        dialog.setBlockOnOpen(true);
        dialog.open();
      }
    };
    advancedButton.addSelectionListener(selectionListener);
  }

  public boolean isSameFileName(String a, String b) {
    boolean result = false;
    if ((a == null) && (b == null)) {
      result = true;
    } else if ((a != null) && (b != null)) {
      result = a.equals(b);
    }
    return result;
  }

  protected void createCatalogEntriesView(Composite parent) {
    Group group = new Group(parent, SWT.NONE);
    group.setLayout(new GridLayout());
    group.setLayoutData(new GridData(GridData.FILL_BOTH));
    group.setText(XMLCatalogMessages.UI_LABEL_USER_ENTRIES);
    group.setToolTipText(XMLCatalogMessages.UI_LABEL_USER_ENTRIES_TOOL_TIP);
    // WorkbenchHelp.setHelp(userEntriesGroup, new
    // ControlContextComputer(userEntriesGroup,
    // XMLBuilderContextIds.XMLP_CATALOG_USER_GROUP));
    catalogEntriesView = new XMLCatalogEntriesView(group, workingUserCatalog, systemCatalog);
    catalogEntriesView.setLayoutData(new GridData(GridData.FILL_BOTH));
  }

  protected void createCatalogDetailsView(Composite parent) {
    Group detailsGroup = new Group(parent, SWT.NONE);
    detailsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    detailsGroup.setLayout(new GridLayout());
    detailsGroup.setText(XMLCatalogMessages.UI_LABEL_DETAILS);
    final XMLCatalogEntryDetailsView detailsView = new XMLCatalogEntryDetailsView(detailsGroup);
    ISelectionChangedListener listener = new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        ISelection selection = event.getSelection();
        Object selectedObject = (selection instanceof IStructuredSelection)
            && ((IStructuredSelection) selection).size() == 1
            ? ((IStructuredSelection) selection).getFirstElement() : null;
        if (selectedObject instanceof ICatalogElement) {
          detailsView.setCatalogElement((ICatalogElement) selectedObject);
        } else {
          detailsView.setCatalogElement(null);
        }
      }
    };
    catalogEntriesView.getViewer().addSelectionChangedListener(listener);
  }

  /**
   * Do anything necessary because the OK button has been pressed.
   * 
   * @return whether it is okay to close the preference page
   */
  public boolean performOk() {
    return storeValues();
  }

  /**
   * @see IWorkbenchPreferencePage
   */
  public void init(IWorkbench workbench) {
  }

  /**
   * Stores the values of the controls back to the preference store.
   */
  private boolean storeValues() {
    // dw Object fileObject = null;
    try {
      // update the userCatalog so that its the same as the working
      // catalog
      userCatalog.clear();
      // TODO add entries from the nested catalogs?
      userCatalog.addEntriesFromCatalog(workingUserCatalog);
      // now save the userCatalog
      userCatalog.save();
    } catch (Exception e) {
    }
    return true;
  }

}
