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
import com.google.dart.tools.update.core.UpdateAdapter;
import com.google.dart.tools.update.core.UpdateCore;
import com.google.dart.tools.update.core.internal.UpdateModel.State;
import com.google.dart.tools.update.core.internal.jobs.CheckForUpdatesJob;
import com.google.dart.tools.update.core.internal.jobs.DownloadUpdatesJob;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Manages the downloading of updates.
 */
public class DownloadManager {

  private class StateChangeListener extends UpdateAdapter {
    @Override
    public void updateAvailable(final Revision revision) {
      if (UpdateCore.isAutoDownloadEnabled()) {
        try {
          new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
              scheduleDownload(revision);
            }
          }, DOWNLOAD_DELAY);
        } catch (Exception e) {
          UpdateCore.logError(e);
        }
      }
    }
  }

  // Wait before initiating an auto DL (in ms)
  private static final int DOWNLOAD_DELAY = 3000;

  //cached to ensure proper cancellation at workbench disposal (set to null after each use)
  private Job updateJob;

  private final UpdateModel model;

  private UpdateResolver updateResolver;

  /**
   * Create a manager instance.
   */
  public DownloadManager(UpdateModel model) {
    this.model = model;
    model.addListener(new StateChangeListener());
    updateResolver = UpdateResolver.forIntegration();
  }

  /**
   * Get the latest revision number for update testing.
   * 
   * @return the latest revision
   * @throws IOException if an error occurs accessing revision info
   */
  public Revision getLatestRevision() throws IOException {
    return updateResolver.getLatest();
  }

  /**
   * Get the latest staged revision number.
   * 
   * @return the latest staged revision
   */
  public Revision getLatestStaged() {
    IPath updateDirPath = UpdateCore.getUpdateDirPath();
    File dir = updateDirPath.toFile();
    Revision latest = Revision.UNKNOWN;
    if (dir.exists() && dir.isDirectory()) {
      for (File file : dir.listFiles()) {
        Revision revision = getRevisionNumber(file);
        if (revision.isMoreCurrentThan(latest)) {
          latest = revision;
        }
      }
    }
    return latest;
  }

  /**
   * Check to see if there is an update staged and ready to be applied.
   * 
   * @return <code>true</code> if there is an update ready to be applied, <code>false</code>
   *         otherwise
   */
  public boolean isUpdateStaged() {
    Revision current = UpdateCore.getCurrentRevision();
    Revision staged = getLatestStaged();
    return staged.isMoreCurrentThan(current);
  }

  /**
   * Schedule an update download.
   */
  public void scheduleDownload(Revision revision) {
    //don't over-schedule a download
    if (!model.isDownloadingUpdate()) {
      doDownloadUpdate(revision);
    }
  }

  /**
   * Schedule a check for available updates.
   */
  public void scheduleUpdateCheck() {
    //don't over-check
    if (model.isIdle()) {
      doCheckForUpdate();
    }
  }

  /**
   * Start the download manager.
   */
  public void start() {
    cleanupStagingArea();
  }

  /**
   * Stop the download manager.
   */
  public void stop() {
    if (updateJob != null) {
      if (!updateJob.cancel()) {
        try {
          updateJob.join();
        } catch (InterruptedException e) {
          //TODO(pquitslund): sysout for debugging
          e.printStackTrace();
          //ignored since we're tearing down and there may be no log to write to!
        }
      }
    }
  }

  private void cleanupStagingArea() {
    //ensure last DL was sound
    Revision latestStaged = getLatestStaged();
    File zip = latestStaged.getLocalPath().toFile();
    if (zip.exists()) {
      if (!UpdateUtils.isZipValid(zip)) {

        if (DartCoreDebug.TRACE_UPDATE) {
          UpdateCore.logInfo("deleting invalid zip: " + zip.getName());//$NON-NLS-1$
        }

        zip.delete();
      }
    }
  }

  private void doCheckForUpdate() {

    if (DartCoreDebug.TRACE_UPDATE) {
      UpdateCore.logInfo("DownloadManager.doCheckForUpdate()");//$NON-NLS-1$
    }

    //ensure jobs don't stack
    if (updateJob != null) {

      if (DartCoreDebug.TRACE_UPDATE) {
        UpdateCore.logInfo("(update job active -- check canceled)");//$NON-NLS-1$
      }

      return;
    }

    updateJob = new CheckForUpdatesJob(this);
    updateJob.addJobChangeListener(new JobChangeAdapter() {
      @Override
      public void done(IJobChangeEvent event) {

        CheckForUpdatesJob updateCheckJob = (CheckForUpdatesJob) updateJob;

        //Signal that the job is finished
        updateJob = null;

        Revision latest = updateCheckJob.getLatest();
        if (latest == null) {
          model.setErrorMessage(updateCheckJob.getErrorMessage());
          model.enterState(State.FAILED);
        } else {

          UpdateCore.updateChecked();
          model.setLatestAvailableRevision(latest);
          model.enterState(State.CHECKED);

          Revision staged = getLatestStaged();
          if (latest.isEqualTo(staged)) {
            model.enterState(State.DOWNLOADED);
          } else if (latest.isMoreCurrentThan(UpdateCore.getCurrentRevision())) {
            model.enterState(State.AVAILABLE);
          }

        }

      }

      @Override
      public void running(IJobChangeEvent event) {
        model.enterState(State.CHECKING);
      }
    });
    updateJob.schedule();
  }

  private void doDownloadUpdate(Revision revision) {

    //ensure jobs don't stack
    if (updateJob != null) {
      return;
    }

    updateJob = new DownloadUpdatesJob(revision);
    updateJob.addJobChangeListener(new JobChangeAdapter() {

      @Override
      public void done(IJobChangeEvent event) {
        updateJob = null;
        IStatus result = event.getResult();
        if (result.isOK()) {
          model.enterState(State.DOWNLOADED);
        } else {
          UpdateCore.logWarning("Download cancelled [" + result.getMessage() + "]");
          model.enterState(State.DOWNLOAD_CANCELLED);
        }
      }

      @Override
      public void running(IJobChangeEvent event) {
        model.enterState(State.DOWNLOADING);
      }
    });
    updateJob.schedule();
  }

  private Revision getRevisionNumber(File file) {
    String name = file.getName();
    if (name.endsWith(".msi")) {
      return Revision.forValue(name.substring(0, name.length() - 4));
    }
    return Revision.forValue(name.split(".zip")[0]);
  }

}
