/*******************************************************************************
 * Copyright (c) 2002, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.wizards;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.eclipse.ui.wizards.datatransfer.ZipFileStructureProvider;
import org.eclipse.wst.xml.ui.internal.Logger;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;
import org.osgi.framework.Bundle;

public class ExampleProjectCreationOperation implements IRunnableWithProgress {

  private IResource elementToOpen;

  private IOverwriteQuery overwriteQuery;

  private ExampleProjectCreationWizardPage[] pages;

  /**
   * Constructor for ExampleProjectCreationOperation
   */
  public ExampleProjectCreationOperation(ExampleProjectCreationWizardPage[] myPages,
      IOverwriteQuery myOverwriteQuery) {
    elementToOpen = null;
    pages = myPages;
    overwriteQuery = myOverwriteQuery;
  }

  private IProject configNewProject(IWorkspaceRoot root, String name, String[] natureIds,
      IProject[] referencedProjects, IProgressMonitor monitor) throws InvocationTargetException {
    try {
      IProject project = root.getProject(name);
      if (!project.exists()) {
        project.create(null);
      }
      if (!project.isOpen()) {
        project.open(null);
      }
      IProjectDescription desc = project.getDescription();
      desc.setLocation(null);
      desc.setNatureIds(natureIds);
      desc.setReferencedProjects(referencedProjects);

      project.setDescription(desc, new SubProgressMonitor(monitor, 1));

      return project;
    } catch (CoreException e) {
      throw new InvocationTargetException(e);
    }
  }

  private void createProject(IWorkspaceRoot root, ExampleProjectCreationWizardPage page,
      IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
    IConfigurationElement desc = page.getConfigurationElement();

    IConfigurationElement[] imports = desc.getChildren("import"); //$NON-NLS-1$
    IConfigurationElement[] natures = desc.getChildren("nature"); //$NON-NLS-1$
    IConfigurationElement[] references = desc.getChildren("references"); //$NON-NLS-1$
    int nImports = (imports == null) ? 0 : imports.length;
    int nNatures = (natures == null) ? 0 : natures.length;
    int nReferences = (references == null) ? 0 : references.length;

    monitor.beginTask(XMLWizardsMessages.ExampleProjectCreationOperation_op_desc_proj, nImports + 1);

    String name = page.getProjectName();

    String[] natureIds = new String[nNatures];
    for (int i = 0; i < nNatures; i++) {
      natureIds[i] = natures[i].getAttribute("id"); //$NON-NLS-1$
    }
    IProject[] referencedProjects = new IProject[nReferences];
    for (int i = 0; i < nReferences; i++) {
      referencedProjects[i] = root.getProject(references[i].getAttribute("id")); //$NON-NLS-1$
    }

    IProject proj = configNewProject(root, name, natureIds, referencedProjects, monitor);

    for (int i = 0; i < nImports; i++) {
      doImports(proj, imports[i], new SubProgressMonitor(monitor, 1));
    }

    String open = desc.getAttribute("open"); //$NON-NLS-1$
    if ((open != null) && (open.length() > 0)) {
      IResource fileToOpen = proj.findMember(new Path(open));
      if (fileToOpen != null) {
        elementToOpen = fileToOpen;
      }
    }

  }

  private void doImports(IProject project, IConfigurationElement curr, IProgressMonitor monitor)
      throws InvocationTargetException, InterruptedException {
    try {
      IPath destPath;
      String name = curr.getAttribute("dest"); //$NON-NLS-1$
      if ((name == null) || (name.length() == 0)) {
        destPath = project.getFullPath();
      } else {
        IFolder folder = project.getFolder(name);
        if (!folder.exists()) {
          folder.create(true, true, null);
        }
        destPath = folder.getFullPath();
      }
      String importPath = curr.getAttribute("src"); //$NON-NLS-1$
      if (importPath == null) {
        importPath = ""; //$NON-NLS-1$
        Logger.log(Logger.ERROR, "projectsetup descriptor: import missing"); //$NON-NLS-1$
        return;
      }

      ZipFile zipFile = getZipFileFromPluginDir(importPath, getContributingPlugin(curr));
      importFilesFromZip(zipFile, destPath, new SubProgressMonitor(monitor, 1));
    } catch (CoreException e) {
      throw new InvocationTargetException(e);
    }
  }

  private String getContributingPlugin(IConfigurationElement configurationElement) {
    Object parent = configurationElement;
    while (parent != null) {
      if (parent instanceof IExtension) {
        return ((IExtension) parent).getNamespace();
      }
      parent = ((IConfigurationElement) parent).getParent();
    }
    return null;
  }

  public IResource getElementToOpen() {
    return elementToOpen;
  }

  private ZipFile getZipFileFromPluginDir(String pluginRelativePath, String symbolicName)
      throws CoreException {
    try {
      Bundle bundle = Platform.getBundle(symbolicName);
      URL starterURL = new URL(bundle.getEntry("/"), pluginRelativePath); //$NON-NLS-1$
      return new ZipFile(Platform.asLocalURL(starterURL).getFile());
    } catch (IOException e) {
      String message = pluginRelativePath + ": " + e.getMessage(); //$NON-NLS-1$
      Status status = new Status(IStatus.ERROR, XMLUIPlugin.ID, IStatus.ERROR, message, e);
      throw new CoreException(status);
    }
  }

  private void importFilesFromZip(ZipFile srcZipFile, IPath destPath, IProgressMonitor monitor)
      throws InvocationTargetException, InterruptedException {
    ZipFileStructureProvider structureProvider = new ZipFileStructureProvider(srcZipFile);
    ImportOperation op = new ImportOperation(destPath, structureProvider.getRoot(),
        structureProvider, overwriteQuery);
    op.run(monitor);
  }

  /*
   * @see IRunnableWithProgress#run(IProgressMonitor)
   */
  public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
    if (monitor == null) {
      monitor = new NullProgressMonitor();
    }
    try {
      monitor.beginTask(XMLWizardsMessages.ExampleProjectCreationOperation_op_desc, pages.length);
      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

      for (int i = 0; i < pages.length; i++) {
        createProject(root, pages[i], new SubProgressMonitor(monitor, 1));
      }
    } finally {
      monitor.done();
    }
  }
}
