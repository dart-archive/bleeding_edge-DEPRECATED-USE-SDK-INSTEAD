/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.dialogs;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.ScanCallback;
import com.google.dart.tools.core.internal.builder.ScanCallbackProvider;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import java.io.File;
import java.io.IOException;

/**
 * Provides progress dialog for scanning and analysis that occurs when a folder is opened.
 */
public class ScanProgressUI {

  /**
   * A job that displays scanning progress and allows the user to cancel the operation.
   */
  private static final class ProgressJob extends Job implements ScanCallback {

    private static final int TOTAL_WORK = 10000;

    private Object lock = new Object();
    private int currentProgress = 0;
    private boolean canceled = false;

    ProgressJob() {
      super("Analyze folder content");
    }

    @Override
    public boolean isCanceled() {
      return canceled;
    }

    @Override
    public void progress(float progress) {
      synchronized (lock) {
        currentProgress = Math.round(progress * TOTAL_WORK);
        lock.notifyAll();
      }
    }

    @Override
    public void scanCanceled(final File rootFile) {
      Display.getDefault().asyncExec(new Runnable() {
        @Override
        public void run() {
          try {
            DartIgnoreManager.getInstance().addToIgnores(rootFile);
          } catch (IOException e) {
            DartCore.logError("Failed to ignore " + rootFile, e);
          }
        }
      });
    }

    @Override
    public void scanComplete() {
      synchronized (lock) {
        currentProgress = TOTAL_WORK;
        lock.notifyAll();
      }
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      int displayedProgress = 0;
      monitor.beginTask("Analyzing folder content", TOTAL_WORK);
      while (displayedProgress < TOTAL_WORK) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          //$FALL-THROUGH$
        }
        int work;
        synchronized (lock) {
          work = currentProgress - displayedProgress;
        }
        if (work > 0) {
          monitor.worked(work);
          displayedProgress = currentProgress;
        }
        if (monitor.isCanceled()) {
          canceled = true;
          return Status.CANCEL_STATUS;
        }
      }
      monitor.done();
      return Status.OK_STATUS;
    }
  }

  /**
   * Creates a new job and callback to monitor the scan progress.
   */
  private static final class Provider extends ScanCallbackProvider {
    @Override
    public ScanCallback newCallback() {
      final ProgressJob job = new ProgressJob();
      Display.getDefault().asyncExec(new Runnable() {
        @Override
        public void run() {
          PlatformUI.getWorkbench().getProgressService().showInDialog(null, job);
        }
      });
      job.schedule();
      return job;
    }
  }

  /**
   * Starts providing progress feedback for open folder operations.
   */
  public static void start() {
    ScanCallbackProvider.setProvider(new Provider());
  }

  /**
   * Stops providing progress feedback for open folder operations.
   */
  public static void stop() {
    ScanCallbackProvider.setProvider(null);
  }
}
