/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.wizard;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

/**
 * This class encapsulates any used Module Core and Facets APIs along with fallbacks for use on
 * non-compliant projects and when those services are not available at runtime. Because ModuleCore
 * API calls can result in locks needing to be acquired, none of these methods should be called
 * while other thread locks have already been acquired.
 */
final class FacetModuleCoreSupport {
  static final boolean _dump_NCDFE = false;

  /**
   * @param project
   * @return the IPath to the "root" of the web contents
   */
  public static IPath getWebContentRootPath(IProject project) {
    if (project == null)
      return null;

    IPath path = null;
    try {
      path = FacetModuleCoreSupportDelegate.getWebContentRootPath(project);
    } catch (NoClassDefFoundError e) {
      if (_dump_NCDFE)
        e.printStackTrace();
    }
    return path;
  }

  /**
   * @param project
   * @return
   * @throws org.eclipse.core.runtime.CoreException
   */
  public static boolean isWebProject(IProject project) {
    if (project == null)
      return false;

    try {
      return FacetModuleCoreSupportDelegate.isWebProject(project);
    } catch (NoClassDefFoundError e) {
      if (_dump_NCDFE)
        e.printStackTrace();
    }
    return true;
  }

  /**
   * @param project
   * @return the IPaths to acceptable "roots" in a project
   */
  public static IPath[] getAcceptableRootPaths(IProject project) {
    if (project == null)
      return null;
    IPath[] paths = null;
    try {
      paths = FacetModuleCoreSupportDelegate.getAcceptableRootPaths(project);
    } catch (NoClassDefFoundError e) {
      if (_dump_NCDFE)
        e.printStackTrace();
      return new IPath[] {project.getFullPath()};
    }
    return paths;
  }

  /**
   * Gets the root container for the path in the project
   * 
   * @param project
   * @param path
   * @return
   */
  public static IPath getRootContainerForPath(IProject project, IPath path) {
    if (project == null)
      return null;
    IPath root = null;
    try {
      root = FacetModuleCoreSupportDelegate.getRootContainerForPath(project, path);
    } catch (NoClassDefFoundError e) {
      if (_dump_NCDFE)
        e.printStackTrace();
      return null;
    }
    return root;
  }

  /**
   * Gets the default root container for the project
   * 
   * @param project
   * @return
   */
  public static IPath getDefaultRootContainer(IProject project) {
    if (project == null)
      return null;
    IPath root = null;
    try {
      root = FacetModuleCoreSupportDelegate.getDefaultRoot(project);
    } catch (NoClassDefFoundError e) {
      if (_dump_NCDFE)
        e.printStackTrace();
      return null;
    }
    return root;
  }
}
