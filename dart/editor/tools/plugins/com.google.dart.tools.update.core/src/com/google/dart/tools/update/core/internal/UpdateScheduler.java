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
package com.google.dart.tools.update.core.internal;

import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.update.core.Revision;
import com.google.dart.tools.update.core.UpdateCore;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import java.util.concurrent.TimeUnit;

/**
 * Schedules update checks.
 */
public class UpdateScheduler {

  private class UpdateCheckTimer extends Job {

    public UpdateCheckTimer() {
      super("Update Checker");
      setSystem(true);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      if (monitor.isCanceled()) {
        return Status.CANCEL_STATUS;
      }

      if (DartCoreDebug.TRACE_UPDATE) {
        UpdateCore.logInfo("check timer pulse");//$NON-NLS-1$
      }

      try {
        if (shouldCheckForUpdates()) {
          scheduleUpdateCheck();
        }
      } finally {
        if (UpdateCore.isAutoUpdateCheckingEnabled()) {
          long delta = UpdateCore.getNextUpdateTime() - System.currentTimeMillis();
          schedule(Math.max(TimeUnit.HOURS.toMillis(1), delta));
        }
      }
      return Status.OK_STATUS;
    }
  }

  private UpdateCheckTimer autoUpdateTimer;

  private final UpdateModel model;
  private final DownloadManager downloadManager;

  /**
   * Create a scheduler instance.
   * 
   * @param model the update model
   * @param downloadManager the download manager
   */
  public UpdateScheduler(UpdateModel model, DownloadManager downloadManager) {
    this.model = model;
    this.downloadManager = downloadManager;
  }

  /**
   * Enable automatic update checking.
   * 
   * @param enable <code>true</code> to enable, <code>false</code> otherwise
   */
  public void enableAutoUpdateChecking(boolean enable) {
    if (enable) {
      start();
    } else {
      stop();
    }
  }

  /**
   * Schedule an update download.
   */
  public void scheduleDownload(Revision revision) {
    downloadManager.scheduleDownload(revision);
  }

  /**
   * Schedule an update check.
   */
  public void scheduleUpdateCheck() {
    downloadManager.scheduleUpdateCheck();
  }

  /**
   * Start the scheduler.
   */
  public void start() {
    if (autoUpdateTimer == null) {
      autoUpdateTimer = new UpdateCheckTimer();
      autoUpdateTimer.schedule();
    }
  }

  /**
   * Stop the scheduler.
   */
  public void stop() {
    if (autoUpdateTimer != null) {
      autoUpdateTimer.cancel();
      autoUpdateTimer = null;
    }
  }

  private boolean shouldCheckForUpdates() {
    return model.isIdle() && System.currentTimeMillis() >= UpdateCore.getNextUpdateTime();
  }
}
