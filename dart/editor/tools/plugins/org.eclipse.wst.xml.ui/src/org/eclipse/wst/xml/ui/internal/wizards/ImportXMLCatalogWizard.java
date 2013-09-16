/*******************************************************************************
 * Copyright (c) 2008 Standards for Technology in Automotive Retail (STAR) and others. All rights
 * reserved. This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: David Carver/STAR -
 * dcarver@starstandard.org/d_a_carver@yahoo.com - bug 192568 Initial API - This implements the
 * functionality of the old Import Dialog for the XML Catalog.
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.wst.xml.core.internal.XMLCorePlugin;
import org.eclipse.wst.xml.core.internal.catalog.CatalogSet;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalog;
import org.eclipse.wst.xml.core.internal.catalog.provisional.INextCatalog;
import org.eclipse.wst.xml.ui.internal.catalog.XMLCatalogMessages;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImageHelper;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImages;

public class ImportXMLCatalogWizard extends Wizard implements IImportWizard {

  protected ImportXMLCatalogPage importPage = null;
  protected ICatalog workingUserCatalog = null;
  protected ICatalog userCatalog = null;

  public ImportXMLCatalogWizard() {
    setWindowTitle(XMLWizardsMessages._UI_DIALOG_XMLCATALOG_IMPORT_TITLE);
    ImageDescriptor descriptor = XMLEditorPluginImageHelper.getInstance().getImageDescriptor(
        XMLEditorPluginImages.IMG_WIZBAN_GENERATEXML);
    setDefaultPageImageDescriptor(descriptor);
    ICatalog defaultCatalog = XMLCorePlugin.getDefault().getDefaultXMLCatalog();
    INextCatalog[] nextCatalogs = defaultCatalog.getNextCatalogs();
    for (int i = 0; i < nextCatalogs.length; i++) {
      INextCatalog catalog = nextCatalogs[i];
      ICatalog referencedCatalog = catalog.getReferencedCatalog();
      if (referencedCatalog != null) {
        if (XMLCorePlugin.USER_CATALOG_ID.equals(referencedCatalog.getId())) {
          userCatalog = referencedCatalog;
        }
      }
    }

    CatalogSet tempCatalogSet = new CatalogSet();
    workingUserCatalog = tempCatalogSet.lookupOrCreateCatalog("working", ""); //$NON-NLS-1$ //$NON-NLS-2$
    workingUserCatalog.addEntriesFromCatalog(userCatalog);
  }

  public boolean canFinish() {
    return importPage.isPageComplete();
  }

  public boolean performFinish() {
    IFile file = importPage.getFile();
    if (file != null) {
      String fileName = file.getLocation().toFile().toURI().toString();
      try {
        CatalogSet tempResourceSet = new CatalogSet();
        ICatalog newCatalog = tempResourceSet.lookupOrCreateCatalog("temp", fileName); //$NON-NLS-1$

        workingUserCatalog.addEntriesFromCatalog(newCatalog);
        userCatalog.clear();
        userCatalog.addEntriesFromCatalog(workingUserCatalog);
        userCatalog.save();
      } catch (Exception e) {
        return false;
      }
    }

    return true;
  }

  public void init(IWorkbench workbench, IStructuredSelection selection) {
    // TODO Auto-generated method stub

  }

  public void addPages() {
    importPage = new ImportXMLCatalogPage();
    importPage.setTitle(XMLCatalogMessages.UI_LABEL_IMPORT_DIALOG_HEADING);
    importPage.setDescription(XMLWizardsMessages._UI_DIALOG_XMLCATALOG_IMPORT_DESCRIPTION);
    importPage.setMessage(XMLCatalogMessages.UI_LABEL_IMPORT_DIALOG_MESSAGE);
    addPage(importPage);
    importPage.setPageComplete(false);
  }
}
