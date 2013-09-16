/*******************************************************************************
 * Copyright (c) 2007, 2008 Standards for Technology in Automotive Retail (STAR) and others. All
 * rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: David Carver/STAR -
 * dcarver@starstandard.org/d_a_carver@yahoo.com - bug 192568 Initial API - This implements the
 * functionality of the old Import Dialog for the XML Catalog.
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.common.ui.internal.viewers.SelectSingleFileView;

public class ImportXMLCatalogPage extends WizardPage {

  protected SelectSingleFileView selectSingleFileView;
  protected IStructuredSelection selection;

  public ImportXMLCatalogPage() {
    super(XMLWizardsMessages._UI_DIALOG_XMLCATALOG_IMPORT_TITLE);
    setTitle(XMLWizardsMessages._UI_DIALOG_XMLCATALOG_IMPORT_TITLE);
    selection = new StructuredSelection();
    selectSingleFileView = new SelectSingleFileView(selection, false) {
      public void createFilterControl(Composite composite) {
        ImportXMLCatalogPage.this.createFilterControl(composite);
      }
    };

  }

  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NULL);
    composite.setLayout(new GridLayout());
    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.widthHint = 350;
    gd.heightHint = 350;
    composite.setLayoutData(gd);
    String[] extensions = {".xmlcatalog", ".xml"}; //$NON-NLS-1$ //$NON-NLS-2$
    selectSingleFileView.addFilterExtensions(extensions);
    selectSingleFileView.createControl(composite);
    selectSingleFileView.setVisibleHelper(true);
    selectSingleFileView.addSelectionChangedTreeListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        ImportXMLCatalogPage.this.setPageComplete(selectSingleFileView.getFile() != null);
      }
    });
    setControl(composite);
  }

  public void createFilterControl(Composite composite) {
  }

  public IFile getFile() {
    return selectSingleFileView.getFile();
  }
}
