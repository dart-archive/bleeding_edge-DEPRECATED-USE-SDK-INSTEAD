/*******************************************************************************
 * Copyright (c) 2001, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.wst.sse.core.internal.IExecutionDelegate;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;

import java.lang.reflect.InvocationTargetException;

class EditorExecutionContext implements IExecutionDelegate {

  /**
   * Reusable runnable for the Display execution queue to cut down on garbage creation. Will make
   * use of the progress service if possible.
   */
  private static class ReusableUIRunner implements Runnable, IRunnableWithProgress {
    private StructuredTextEditor editor;
    private ISafeRunnable fRunnable = null;

    ReusableUIRunner(StructuredTextEditor part) {
      super();
      editor = part;
    }

    /*
     * Expected to only be run by Display queue in the UI Thread
     * 
     * @see java.lang.Runnable#run()
     */
    public void run() {
      IWorkbenchPartSite site = editor.getEditorPart().getSite();
      final IWorkbenchWindow workbenchWindow = (site == null) ? null : site.getWorkbenchWindow();
      final IWorkbenchSiteProgressService jobService = (IWorkbenchSiteProgressService) ((site == null)
          ? null : site.getAdapter(IWorkbenchSiteProgressService.class));
      /*
       * Try to use the progress service so the workbench can give more feedback to the user
       * (although editors seem to make less use of the service than views -
       * https://bugs.eclipse.org/bugs/show_bug.cgi?id=86221 .
       */
      if (workbenchWindow != null && jobService != null) {
        /*
         * Doc is ambiguous, but it must be run from the UI thread -
         * https://bugs.eclipse.org/bugs/show_bug.cgi?id=165180
         */
        try {
          jobService.runInUI(workbenchWindow, this,
              (ISchedulingRule) editor.getEditorPart().getEditorInput().getAdapter(IResource.class));
        } catch (InvocationTargetException e) {
          Logger.logException(e);
        } catch (InterruptedException e) {
          Logger.logException(e);
        }
      } else {
        /*
         * Run it directly and direct the UI of the editor. See StructuredTextEditor's begin/end
         * background job for other activities to best accommodate (for example, there is a
         * "timed delay" before the editor itself leaves background-update mode). NOTE: this execute
         * method itself is always called from inside of an ILock block, so another block is not not
         * needed here for all these sycnExec's.
         */
        IWorkbench workbench = SSEUIPlugin.getInstance().getWorkbench();
        final Display display = workbench.getDisplay();
        if (display != null && !display.isDisposed()) {
          editor.beginBackgroundOperation();
          try {
            /*
             * Here's where the document update/modification occurs
             */
            SafeRunner.run(fRunnable);
          } finally {
            /*
             * This 'end' is just a signal to editor that this particular update is done. Its up to
             * the editor to decide exactly when to leave its "background mode"
             */
            editor.endBackgroundOperation();
          }
        }
        fRunnable = null;
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse
     * .core.runtime.IProgressMonitor)
     */
    public void run(IProgressMonitor monitor) throws InvocationTargetException,
        InterruptedException {
      if (fRunnable != null)
        SafeRunner.run(fRunnable);
    }

    void setRunnable(ISafeRunnable r) {
      fRunnable = r;
    }
  }

  StructuredTextEditor fEditor;
  private ReusableUIRunner fReusableRunner;

  public EditorExecutionContext(StructuredTextEditor editor) {
    super();
    fEditor = editor;
    fReusableRunner = new ReusableUIRunner(fEditor);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.sse.core.internal.IExecutionDelegate#execute(java.lang .Runnable)
   */
  public void execute(final ISafeRunnable runnable) {
    IWorkbench workbench = SSEUIPlugin.getInstance().getWorkbench();
    final Display display = workbench.getDisplay();
    if (display.getThread() == Thread.currentThread()) {
      // *If already in display thread, we can simply run, "as usual"*/
      SafeRunner.run(runnable);
    } else {
      // *otherwise run through the reusable runner */
      fReusableRunner.setRunnable(runnable);
      display.syncExec(fReusableRunner);
    }
  }
}
