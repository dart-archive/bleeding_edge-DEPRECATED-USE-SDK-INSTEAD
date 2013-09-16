/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.actions;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

public class ResourceActionDelegate implements IActionDelegate {
  protected IStructuredSelection fSelection;

  private IWorkbenchSiteProgressService getActiveProgressService() {
    IWorkbenchSiteProgressService service = null;
    if (PlatformUI.isWorkbenchRunning()) {
      IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
      if (activeWorkbenchWindow != null) {
        IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
        if (activePage != null) {
          IWorkbenchPart activePart = activePage.getActivePart();
          if (activePart != null) {
            service = (IWorkbenchSiteProgressService) activePart.getSite().getAdapter(
                IWorkbenchSiteProgressService.class);
          }
        }
      }
    }
    return service;
  }

  protected Job getJob() {
    // ResourceActionDelegate does not create background job
    // subclass creates the background job for the action
    return null;
  }

  protected boolean processorAvailable(IResource resource) {
    // ResourceActionDelegate returns false by default
    // subclass returns true if processor is available; false otherwise
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
   */
  public void run(IAction action) {
    if (fSelection != null && !fSelection.isEmpty()) {
      Job job = getJob();
      if (job != null) {
        IWorkbenchSiteProgressService progressService = getActiveProgressService();
        if (progressService != null) {
          progressService.schedule(job);
        } else {
          job.schedule();
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
   * org.eclipse.jface.viewers.ISelection)
   */
  public void selectionChanged(IAction action, ISelection selection) {
    if (selection instanceof IStructuredSelection) {
      fSelection = (IStructuredSelection) selection;
      boolean available = false;

      Object[] elements = fSelection.toArray();
      for (int i = 0; i < elements.length; i++) {
        if (elements[i] instanceof IResource) {
          available = processorAvailable((IResource) elements[i]);

          if (available)
            break;
        }
      }

      action.setEnabled(available);
    }
  }

}
