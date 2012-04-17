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
package com.google.dart.tools.update.core.internal.jobs;

import com.google.dart.tools.update.core.Revision;
import com.google.dart.tools.update.core.UpdateCore;
import com.google.dart.tools.update.core.internal.DownloadManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import java.io.IOException;

/**
 * A job that checks for updates.
 */
public class CheckForUpdatesJob extends Job {

  private final DownloadManager downloadManager;
  private Revision latest;

  /**
   * Create an instance.
   * 
   * @param downloadManager the download manager
   */
  public CheckForUpdatesJob(DownloadManager downloadManager) {
    super(UpdateJobMessages.CheckForUpdatesJob_job_label);
    this.downloadManager = downloadManager;
  }

  /**
   * Get the latest available update.
   * 
   * @return the latest update, or <code>null</code> if it has not been retrieved yet
   */
  public Revision getLatest() {
    return latest;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    try {
      latest = downloadManager.getLatestRevision();
    } catch (IOException e) {
      return UpdateCore.createErrorStatus("Unable to get latest revision: " + e.getMessage()); //$NON-NLS-1$
    }
    return Status.OK_STATUS;
  }

}
