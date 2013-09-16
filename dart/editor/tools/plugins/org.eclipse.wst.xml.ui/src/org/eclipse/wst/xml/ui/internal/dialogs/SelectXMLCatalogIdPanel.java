/*******************************************************************************
 * Copyright (c) 2001, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.dialogs;

import java.util.Collection;
import java.util.List;
import java.util.Vector;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalog;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalogEntry;
import org.eclipse.wst.xml.core.internal.catalog.provisional.INextCatalog;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;

public class SelectXMLCatalogIdPanel extends Composite {
  protected int catalogEntryType;
  protected boolean doTableSizeHack = false;

  protected XMLCatalogTableViewer tableViewer;
  protected ICatalog fXmlCatalog;

  public SelectXMLCatalogIdPanel(Composite parent, ICatalog xmlCatalog) {
    super(parent, SWT.NONE);
    this.fXmlCatalog = xmlCatalog;

    GridLayout gridLayout = new GridLayout();
    this.setLayout(gridLayout);
    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.heightHint = 200;
    gd.widthHint = 700;
    this.setLayoutData(gd);

    Label label = new Label(this, SWT.NONE);
    label.setText(XMLUIMessages._UI_LABEL_XML_CATALOG_COLON);

    tableViewer = createTableViewer(this);
    tableViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
    tableViewer.setInput("dummy"); //$NON-NLS-1$
  }

  protected XMLCatalogTableViewer createTableViewer(Composite parent) {
    String headings[] = new String[2];
    headings[0] = XMLUIMessages._UI_LABEL_KEY;
    headings[1] = XMLUIMessages._UI_LABEL_URI;

    XMLCatalogTableViewer theTableViewer = new XMLCatalogTableViewer(parent, headings) {

      protected void addXMLCatalogEntries(List list, ICatalogEntry[] entries) {
        for (int i = 0; i < entries.length; i++) {
          ICatalogEntry entry = entries[i];
          if (catalogEntryType == 0) {
            list.add(entry);
          } else if (catalogEntryType == entry.getEntryType()) {
            list.add(entry);
          }
        }
      }

      public Collection getXMLCatalogEntries() {
        List result = null;

        if ((fXmlCatalog == null) || doTableSizeHack) {
          // this lets us create a table with an initial height of
          // 10 rows
          // otherwise we get stuck with 0 row heigh table... that's
          // too small
          doTableSizeHack = false;
          result = new Vector();
          for (int i = 0; i < 6; i++) {
            result.add(""); //$NON-NLS-1$
          }
        } else {
          result = new Vector();
          processCatalog(result, fXmlCatalog);
        }
        return result;
      }

      private void processCatalog(List result, ICatalog catalog) {
        addXMLCatalogEntries(result, catalog.getCatalogEntries());
        INextCatalog[] nextCatalogs = catalog.getNextCatalogs();
        for (int i = 0; i < nextCatalogs.length; i++) {
          ICatalog nextCatalog = nextCatalogs[i].getReferencedCatalog();
          if (nextCatalog != null) {
            processCatalog(result, nextCatalog);
          }
        }
      }
    };
    return theTableViewer;
  }

  public String getId() {
    ICatalogEntry entry = getXMLCatalogEntry();
    return entry != null ? entry.getKey() : null;
  }

  public XMLCatalogTableViewer getTableViewer() {
    return tableViewer;
  }

  public String getURI() {
    ICatalogEntry entry = getXMLCatalogEntry();
    return entry != null ? entry.getURI() : null;
  }

  public ICatalogEntry getXMLCatalogEntry() {
    ICatalogEntry result = null;
    ISelection selection = tableViewer.getSelection();
    Object selectedObject = (selection instanceof IStructuredSelection)
        ? ((IStructuredSelection) selection).getFirstElement() : null;
    if (selectedObject instanceof ICatalogEntry) {
      result = (ICatalogEntry) selectedObject;
    }
    return result;
  }

  public void setCatalogEntryType(int catalogEntryType) {
    this.catalogEntryType = catalogEntryType;
    tableViewer.refresh();
  }
}
