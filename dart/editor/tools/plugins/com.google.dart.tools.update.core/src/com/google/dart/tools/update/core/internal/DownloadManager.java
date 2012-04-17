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

/**
 * Manages the downloading of updates.
 */
public class DownloadManager {

  private class StateChangeListener extends UpdateAdapter {
    @Override
    public void updateAvailable() {
      if (UpdateCore.isAutoDownloadEnabled() && !model.isDownloadingUpdate()) {
        scheduleDownload();
      }
    }
  }

  //cached to ensure proper cancellation at workbench disposal (set to null after each use)
  private Job updateJob;

  private final UpdateModel model;

  /**
   * Create a manager instance.
   */
  public DownloadManager(UpdateModel model) {
    this.model = model;
    model.addListener(new StateChangeListener());
  }

  /**
   * Get the latest revision number for update testing.
   * 
   * @return the latest revision
   * @throws IOException if an error occurs accessing revision info
   */
  public Revision getLatestRevision() throws IOException {
    return UpdateUtils.getLatestRevision(UpdateCore.getUpdateUrl());
  }

  /**
   * Schedule an update download.
   */
  public void scheduleDownload() {
    //don't over-schedule a download
    if (!model.isDownloadingUpdate()) {
      doDownloadUpdate();
    }
  }

  /**
   * Schedule a check for available updates.
   */
  public void scheduleUpdateCheck() {
    //don't over-check
    if (!model.isUpdateAvailable() && model.isIdle()) {
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
        //TODO(pquitslund): sysout for debugging
        System.out.println("deleting invalid zip: " + zip.getName());
        zip.delete();
      }
    }
  }

  private void doCheckForUpdate() {

    //TODO(pquitslund): sysout for debgging
    System.out.println("UpdateManager.doCheckForUpdate()");

    //ensure jobs don't stack
    if (updateJob != null) {
      return;
    }

    updateJob = new CheckForUpdatesJob(this);
    updateJob.addJobChangeListener(new JobChangeAdapter() {
      @Override
      public void done(IJobChangeEvent event) {
        Revision latest = ((CheckForUpdatesJob) updateJob).getLatest();
        updateJob = null;
        model.enterState(State.CHECKED);
        if (latest.isMoreCurrentThan(UpdateCore.getCurrentRevision())) {
          Revision staged = getLatestStaged();
          if (latest.isMoreCurrentThan(staged)) {
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

  private void doDownloadUpdate() {

    //ensure jobs don't stack
    if (updateJob != null) {
      return;
    }

    updateJob = new DownloadUpdatesJob();
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

  private Revision getLatestStaged() {
    IPath updateDirPath = UpdateCore.getUpdateDirPath();
    File dir = updateDirPath.toFile();
    Revision latest = Revision.UNKNOWN;
    if (dir.exists() && dir.isDirectory()) {
      for (File file : dir.listFiles()) {
        Revision revision = Revision.forValue(file.getName().split(".zip")[0]);
        if (revision.isMoreCurrentThan(latest)) {
          latest = revision;
        }
      }
    }
    return latest;
  }

}
