/*******************************************************************************
 * Copyright (c) 2007, 2008 Standards for Technology in Automotive Retail (STAR) and others. All
 * rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: David Carver/STAR -
 * dcarver@starstandard.org/d_a_carver@yahoo.com - bug 192568 Initial API - This implements the
 * functionality of the old Export Dialog for the XML Catalog.
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.wizards;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.ui.wizards.newresource.BasicNewFileResourceWizard;
import org.eclipse.wst.xml.core.internal.XMLCorePlugin;
import org.eclipse.wst.xml.core.internal.catalog.CatalogSet;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalog;
import org.eclipse.wst.xml.core.internal.catalog.provisional.INextCatalog;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImageHelper;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImages;

public class ExportXMLCatalogWizard extends BasicNewFileResourceWizard implements IExportWizard {

  protected WizardNewFileCreationPage exportPage = null;
  protected ICatalog workingUserCatalog = null;
  protected ICatalog userCatalog = null;

  public ExportXMLCatalogWizard() {
    setWindowTitle(XMLWizardsMessages._UI_DIALOG_XMLCATALOG_EXPORT_TITLE);
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

  public boolean performFinish() {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IWorkspaceRoot workspaceRoot = workspace.getRoot();
    String workspacePath = workspaceRoot.getLocation().toOSString();
    String fullPath = workspacePath + exportPage.getContainerFullPath().toOSString();
    String requiredString = fullPath + File.separator + exportPage.getFileName();
    try {
      IFile file = exportPage.createNewFile();
      workingUserCatalog.setLocation(requiredString);
      workingUserCatalog.save();
      file.refreshLocal(IResource.DEPTH_ZERO, null);
    } catch (Exception ex) {
      return false;
    }
    return true;
  }

  public void addPages() {
    exportPage = new WizardNewFileCreationPage("XML Catalog Export", getSelection());
    exportPage.setTitle(XMLWizardsMessages._UI_DIALOG_XMLCATALOG_EXPORT_TITLE);
    exportPage.setDescription(XMLWizardsMessages._UI_DIALOG_XMLCATALOG_EXPORT_DESCRIPTION);
    exportPage.setFileExtension("xml"); //$NON-NLS-1$

    addPage(exportPage);
  }

  public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
    super.init(workbench, currentSelection);
    setWindowTitle(XMLWizardsMessages._UI_DIALOG_XMLCATALOG_EXPORT_TITLE);
    ImageDescriptor descriptor = XMLEditorPluginImageHelper.getInstance().getImageDescriptor(
        XMLEditorPluginImages.IMG_WIZBAN_GENERATEXML);
    setDefaultPageImageDescriptor(descriptor);
  }
}
