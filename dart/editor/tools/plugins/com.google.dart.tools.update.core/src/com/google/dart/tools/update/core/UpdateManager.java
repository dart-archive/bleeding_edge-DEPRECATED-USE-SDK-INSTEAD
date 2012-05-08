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
package com.google.dart.tools.update.core;

import com.google.dart.tools.update.core.internal.DownloadManager;
import com.google.dart.tools.update.core.internal.UpdateModel;
import com.google.dart.tools.update.core.internal.UpdateScheduler;
import com.google.dart.tools.update.core.internal.jobs.InstallUpdateAction;

import java.io.IOException;

/**
 * The main entry point for all update-related operations.
 */
public class UpdateManager {

  /**
   * The shared singleton instance.
   */
  private static final UpdateManager INSTANCE = new UpdateManager();

  /**
   * Get the shared update manager instance.
   * 
   * @return the shared instance
   */
  public static UpdateManager getInstance() {
    return INSTANCE;
  }

  /**
   * The shared model instance.
   */
  private final UpdateModel model;

  /**
   * The shared download manager.
   */
  private final DownloadManager downloadManager;

  /**
   * The shared update scheduler.
   */
  private final UpdateScheduler updateScheduler;

  /**
   * Create an update manager instance.
   */
  private UpdateManager() {
    model = new UpdateModel();
    downloadManager = new DownloadManager(model);
    updateScheduler = new UpdateScheduler(model, downloadManager);
  }

  /**
   * Add the given update listener.
   * 
   * @param listener the listener to add
   */
  public void addListener(UpdateListener listener) {
    model.addListener(listener);
  }

  /**
   * Enable automatic update checking.
   * 
   * @param enable <code>true</code> to enable, <code>false</code> otherwise
   */
  public void enableAutoUpdateChecking(boolean enable) {
    updateScheduler.enableAutoUpdateChecking(enable);
  }

  /**
   * Get the current changelog.
   * 
   * @return the changelog
   */
  public ChangeLog getChangeLog() {
    return new ChangeLog(UpdateCore.getChangeLogUrl());
  }

  /**
   * Get the latest revision number for update testing.
   */
  public Revision getLatestRevision() throws IOException {
    return downloadManager.getLatestRevision();
  }

  /**
   * Get the latest staged update.
   */
  public Revision getLatestStagedUpdate() {
    return downloadManager.getLatestStaged();
  }

  /**
   * Checks to see if an update is currently being downloaded.
   * 
   * @return <code>true</code> if an update is being downloaded, <code>false</code> otherwise
   */
  public boolean isDownloadingUpdate() {
    return model.isDownloadingUpdate();
  }

  /**
   * Checks to see if an update has been applied (implying we need a restart).
   * 
   * @return <code>true</code> if an update has been applied, <code>false</code> otherwise
   */
  public boolean isUpdateApplied() {
    return model.isUpdateApplied();
  }

  /**
   * Checks to see if an update is available for download.
   * 
   * @return <code>true</code> if an update is available, <code>false</code> otherwise
   */
  public boolean isUpdateAvailable() {
    return model.isUpdateAvailable();
  }

  /**
   * Checks to see if an update is downloaded and ready to be applied.
   * 
   * @return <code>true</code> if an update is ready to be applied, <code>false</code> otherwise
   */
  public boolean isUpdateReadyToBeApplied() {
    return model.isUpdateReadyToBeApplied() || downloadManager.isUpdateStaged();
  }

  /**
   * Remove the given update listener.
   * 
   * @param listener the listener to remove
   */
  public void removeListener(UpdateListener listener) {
    model.removeListener(listener);
  }

  /**
   * Schedule an update download.
   */
  public void scheduleDownload(Revision revision) {
    updateScheduler.scheduleDownload(revision);
  }

  /**
   * Schedule installation of a staged update.
   */
  public void scheduleInstall() {
    model.enterState(UpdateModel.State.INSTALLING);
    new InstallUpdateAction(this).run();
  }

  /**
   * Schedule an update check.
   */
  public void scheduleUpdateCheck() {
    updateScheduler.scheduleUpdateCheck();
  }

  /**
   * Start the update manager.
   */
  void start() {
    downloadManager.start();
    enableAutoUpdateChecking(UpdateCore.isAutoUpdateCheckingEnabled());
  }

  /**
   * Stop the update manager.
   */
  void stop() {
    updateScheduler.stop();
    downloadManager.stop();
  }

}
