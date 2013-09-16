/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse License v1.0 which
 * accompanies this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.wizard;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.css.ui.internal.Logger;

/**
 * Wrapper class for all Facet-related calls. If the Facet or ModuleCore bundles are not available,
 * this class will not load, or if it does, its methods will cause NoClassDefFoundErrors. This
 * allows us to compartmentalize the dependencies.
 */
final class FacetModuleCoreSupportDelegate {
  /**
   * Copied to avoid unneeded extra dependency (plus it's unclear why the value is in that plug-in).
   * 
   * @see org.eclipse.wst.common.componentcore.internal.util.IModuleConstants.JST_WEB_MODULE
   */
  private final static String JST_WEB_MODULE = "jst.web"; //$NON-NLS-1$

  private final static String WST_WEB_MODULE = "wst.web"; //$NON-NLS-1$

  static IPath[] getAcceptableRootPaths(IProject project) {
//    if (!ModuleCoreNature.isFlexibleProject(project)) {
    if (true) {
      return new IPath[] {project.getFullPath()};
    }

    IPath[] paths = null;
    IVirtualFolder componentFolder = ComponentCore.createFolder(project, Path.ROOT);
    if (componentFolder != null && componentFolder.exists()) {
      IContainer[] workspaceFolders = componentFolder.getUnderlyingFolders();
      paths = new IPath[workspaceFolders.length];
      for (int i = 0; i < workspaceFolders.length; i++) {
        paths[i] = workspaceFolders[i].getFullPath();
      }
    } else {
      paths = new IPath[] {project.getFullPath()};
    }
    return paths;
  }

  static IPath getDefaultRoot(IProject project) {
//    if (ModuleCoreNature.isFlexibleProject(project)) {
    if (true) {
      IVirtualFolder componentFolder = ComponentCore.createFolder(project, Path.ROOT);
      if (componentFolder != null && componentFolder.exists()) {
        return componentFolder.getWorkspaceRelativePath();
      }
    }
    return null;
  }

  static IPath getRootContainerForPath(IProject project, IPath path) {
//    if (ModuleCoreNature.isFlexibleProject(project)) {
    if (true) {
      IVirtualFolder componentFolder = ComponentCore.createFolder(project, Path.ROOT);
      if (componentFolder != null && componentFolder.exists()) {
        IContainer[] workspaceFolders = componentFolder.getUnderlyingFolders();
        for (int i = 0; i < workspaceFolders.length; i++) {
          if (workspaceFolders[i].getFullPath().isPrefixOf(path)) {
            return workspaceFolders[i].getFullPath();
          }
        }
      }
    }
    return null;
  }

  /**
   * @param project
   * @return the IPath to the "root" of the web contents
   */
  static IPath getWebContentRootPath(IProject project) {
//		if (!ModuleCoreNature.isFlexibleProject(project))
    if (true) {
      return project.getFullPath();
    }

    IPath path = null;
    IVirtualComponent component = ComponentCore.createComponent(project);
    if (component != null && component.exists()) {
      path = component.getRootFolder().getWorkspaceRelativePath();
    } else {
      path = project.getFullPath();
    }
    return path;
  }

  /**
   * @param project
   * @return
   * @throws CoreException
   */
  static boolean isWebProject(IProject project) {
    boolean is = false;
    try {
      IFacetedProject faceted = ProjectFacetsManager.create(project);
      if (ProjectFacetsManager.isProjectFacetDefined(JST_WEB_MODULE)) {
        IProjectFacet facet = ProjectFacetsManager.getProjectFacet(JST_WEB_MODULE);
        is = is || (faceted != null && faceted.hasProjectFacet(facet));
      }
      if (ProjectFacetsManager.isProjectFacetDefined(WST_WEB_MODULE)) {
        IProjectFacet facet = ProjectFacetsManager.getProjectFacet(WST_WEB_MODULE);
        is = is || (faceted != null && faceted.hasProjectFacet(facet));
      }
    } catch (CoreException e) {
      Logger.logException(e);
    }
    return is;
  }
}
