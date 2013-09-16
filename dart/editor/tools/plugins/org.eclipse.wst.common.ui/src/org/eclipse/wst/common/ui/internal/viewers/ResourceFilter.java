/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - Initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.common.ui.internal.viewers;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import java.util.Collection;

public class ResourceFilter extends ViewerFilter {
  protected String[] fExtensions;
  protected IFile[] fExcludedFiles;
  protected Collection fExcludes;

  public ResourceFilter(String[] extensions, Collection exclude) {
    fExtensions = extensions;
    fExcludes = exclude;
    fExcludedFiles = null;
  }

  public ResourceFilter(String[] extensions, IFile[] excludedFiles, Collection exclude) {
    fExtensions = extensions;
    fExcludes = exclude;
    fExcludedFiles = excludedFiles;
  }

  public boolean isFilterProperty(Object element, Object property) {
    return false;
  }

  public boolean select(Viewer viewer, Object parent, Object element) {
    if (element instanceof IFile) {
      if (fExcludes != null && fExcludes.contains(element)) {
        return false;
      }
      String name = ((IFile) element).getName();
      if (fExcludedFiles != null) {
        for (int j = 0; j < fExcludedFiles.length; j++) {
          if (((IFile) element).getLocation().toOSString().compareTo(
              (fExcludedFiles[j]).getLocation().toOSString()) == 0)
            return false;
        }
      }
      if (fExtensions.length == 0) {
        // assume that we don't want to filter any files based on 
        // extension
        return true;
      }
      for (int i = 0; i < fExtensions.length; i++) {
        if (name.endsWith(fExtensions[i])) {
          return true;
        }
      }
      return false;
    } else if (element instanceof IContainer) { // IProject, IFolder
      try {
        IResource[] resources = ((IContainer) element).members();
        for (int i = 0; i < resources.length; i++) {
          // recursive!
          if (select(viewer, parent, resources[i])) {
            return true;
          }
        }
      } catch (CoreException e) {
      }
    }
    return false;
  }
}
