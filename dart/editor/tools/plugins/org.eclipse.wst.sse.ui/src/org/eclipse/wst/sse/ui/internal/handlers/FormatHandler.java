/*******************************************************************************
 * Copyright (c) 2008 Standards for Technology in Automotive Retail and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: David Carver - initial API and
 * implementation - bug 212330 - Based off FormatActionDelegate.java
 *******************************************************************************/

package org.eclipse.wst.sse.ui.internal.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.wst.sse.core.internal.exceptions.MalformedInputExceptionWithDetail;
import org.eclipse.wst.sse.core.internal.format.IStructuredFormatProcessor;
import org.eclipse.wst.sse.ui.internal.FormatProcessorsExtensionReader;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;

import java.io.IOException;

public class FormatHandler extends AbstractHandler implements IHandler {

  protected IStructuredSelection fSelection;

  public void dispose() {
    // nulling out just in case
  }

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

  public Object execute(ExecutionEvent event) throws ExecutionException {

    ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event).getSelectionService().getSelection();

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
    }

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

    return null;
  }

  class FormatJob extends Job {

    public FormatJob(String name) {
      super(name);
    }

    protected IStatus run(IProgressMonitor monitor) {
      IStatus status = Status.OK_STATUS;

      Object[] elements = fSelection.toArray();
      monitor.beginTask("", elements.length); //$NON-NLS-1$
      for (int i = 0; i < elements.length; i++) {
        if (elements[i] instanceof IResource) {
          process(new SubProgressMonitor(monitor, 1), (IResource) elements[i]);
        } else {
          monitor.worked(1);
        }
      }
      monitor.done();

      if (fErrorStatus.getChildren().length > 0) {
        status = fErrorStatus;
        fErrorStatus = new MultiStatus(SSEUIPlugin.ID, IStatus.ERROR,
            SSEUIMessages.FormatActionDelegate_errorStatusMessage, null); //$NON-NLS-1$
      }

      return status;
    }

  }

  private MultiStatus fErrorStatus = new MultiStatus(SSEUIPlugin.ID, IStatus.ERROR,
      SSEUIMessages.FormatActionDelegate_errorStatusMessage, null); //$NON-NLS-1$

  protected void format(IProgressMonitor monitor, IFile file) {
    if (monitor == null || monitor.isCanceled())
      return;

    try {
      monitor.beginTask("", 100);
      IContentDescription contentDescription = file.getContentDescription();
      monitor.worked(5);
      if (contentDescription != null) {
        IContentType contentType = contentDescription.getContentType();
        IStructuredFormatProcessor formatProcessor = getFormatProcessor(contentType.getId());
        if (formatProcessor != null && (monitor == null || !monitor.isCanceled())) {
          String message = NLS.bind(SSEUIMessages.FormatActionDelegate_3,
              new String[] {file.getFullPath().toString().substring(1)});
          monitor.subTask(message);
          formatProcessor.setProgressMonitor(new SubProgressMonitor(monitor, 95));
          formatProcessor.formatFile(file);
        }
      }
      monitor.done();
    } catch (MalformedInputExceptionWithDetail e) {
      String message = NLS.bind(SSEUIMessages.FormatActionDelegate_5,
          new String[] {file.getFullPath().toString()});
      fErrorStatus.add(new Status(IStatus.ERROR, SSEUIPlugin.ID, IStatus.ERROR, message, e));
    } catch (IOException e) {
      String message = NLS.bind(SSEUIMessages.FormatActionDelegate_4,
          new String[] {file.getFullPath().toString()});
      fErrorStatus.add(new Status(IStatus.ERROR, SSEUIPlugin.ID, IStatus.ERROR, message, e));
    } catch (CoreException e) {
      String message = NLS.bind(SSEUIMessages.FormatActionDelegate_4,
          new String[] {file.getFullPath().toString()});
      fErrorStatus.add(new Status(IStatus.ERROR, SSEUIPlugin.ID, IStatus.ERROR, message, e));
    }
  }

  protected void format(final IProgressMonitor monitor, IResource resource) {
    if (resource instanceof IFile) {
      final IFile file = (IFile) resource;

      // BUG 178598 - If the resource is shared, and it's possible to
      // get the workbench Display, the UI thread is asked to execute the
      // format of the file when it can
      monitor.beginTask("", 20); //$NON-NLS-1$
      try {
        ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
        ITextFileBuffer buffer = null;

        try {
          if (manager != null) {
            manager.connect(file.getFullPath(), LocationKind.IFILE, new SubProgressMonitor(monitor,
                1));
            buffer = manager.getTextFileBuffer(resource.getFullPath(), LocationKind.IFILE);
          }

          if (buffer != null && buffer.isShared()) {
            Display display = getDisplay();
            if (display != null) {
              display.syncExec(new Runnable() {
                public void run() {
                  format(new SubProgressMonitor(monitor, 18), file);
                }
              });
            }
          } else
            format(new SubProgressMonitor(monitor, 18), file);
        } finally {
          if (manager != null)
            manager.disconnect(file.getFullPath(), LocationKind.IFILE, new SubProgressMonitor(
                monitor, 1));
        }
        monitor.done();
      } catch (CoreException e) {
        String message = NLS.bind(SSEUIMessages.FormatActionDelegate_4,
            new String[] {file.getFullPath().toString()});
        fErrorStatus.add(new Status(IStatus.ERROR, SSEUIPlugin.ID, IStatus.ERROR, message, e));
      } finally {
        if (monitor != null)
          monitor.done();
      }

    } else if (resource instanceof IContainer) {
      IContainer container = (IContainer) resource;

      try {
        IResource[] members = container.members();
        monitor.beginTask("", members.length); //$NON-NLS-1$
        for (int i = 0; i < members.length; i++) {
          if (monitor != null && !monitor.isCanceled())
            format(new SubProgressMonitor(monitor, 1), members[i]);
        }
        monitor.done();
      } catch (CoreException e) {
        String message = NLS.bind(SSEUIMessages.FormatActionDelegate_4,
            new String[] {resource.getFullPath().toString()});
        fErrorStatus.add(new Status(IStatus.ERROR, SSEUIPlugin.ID, IStatus.ERROR, message, e));
      }
    }
  }

  private Display getDisplay() {

    // Note: the workbench should always have a display
    // (unless running headless), whereas Display.getCurrent()
    // only returns the display if the currently executing thread
    // has one.
    if (PlatformUI.isWorkbenchRunning())
      return PlatformUI.getWorkbench().getDisplay();
    else
      return null;
  }

  protected IStructuredFormatProcessor getFormatProcessor(String contentTypeId) {
    return FormatProcessorsExtensionReader.getInstance().getFormatProcessor(contentTypeId);
  }

  protected Job getJob() {
    return new FormatJob(SSEUIMessages.FormatActionDelegate_jobName); //$NON-NLS-1$
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.sse.ui.edit.util.ResourceActionDelegate#process(org.eclipse.core.runtime.
   * IProgressMonitor, org.eclipse.core.resources.IResource)
   */
  protected void process(IProgressMonitor monitor, IResource resource) {
    monitor.beginTask("", 100);
    format(new SubProgressMonitor(monitor, 98), resource);

    try {
      resource.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 2));
    } catch (CoreException e) {
      String message = NLS.bind(SSEUIMessages.FormatActionDelegate_4,
          new String[] {resource.getFullPath().toString()});
      fErrorStatus.add(new Status(IStatus.ERROR, SSEUIPlugin.ID, IStatus.ERROR, message, e));
    }
    monitor.done();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.wst.sse.ui.internal.actions.ResourceActionDelegate#processorAvailable(org.eclipse
   * .core.resources.IResource)
   */
  protected boolean processorAvailable(IResource resource) {
    boolean result = false;
    if (resource.isAccessible()) {
      try {
        if (resource instanceof IFile) {
          IFile file = (IFile) resource;

          IStructuredFormatProcessor formatProcessor = null;
          IContentDescription contentDescription = file.getContentDescription();
          if (contentDescription != null) {
            IContentType contentType = contentDescription.getContentType();
            formatProcessor = getFormatProcessor(contentType.getId());
          }
          if (formatProcessor != null)
            result = true;
        } else if (resource instanceof IContainer) {
          IContainer container = (IContainer) resource;
          IResource[] members;
          members = container.members();
          for (int i = 0; i < members.length; i++) {
            boolean available = processorAvailable(members[i]);

            if (available) {
              result = true;
              break;
            }
          }
        }
      } catch (CoreException e) {
        Logger.logException(e);
      }
    }

    return result;
  }

}
