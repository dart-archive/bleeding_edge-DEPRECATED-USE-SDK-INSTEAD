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

import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.update.core.Revision;
import com.google.dart.tools.update.core.UpdateCore;
import com.google.dart.tools.update.core.internal.UpdateUtils;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

/**
 * A job to download the latest available update.
 */
public class DownloadUpdatesJob extends Job {

  private final Revision revision;

  /**
   * Create an instance.
   * 
   * @param revision the revision to download
   */
  public DownloadUpdatesJob(Revision revision) {
    super(UpdateJobMessages.DownloadUpdatesJob_job_label);
    this.revision = revision;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {

    //add checking
    try {
      if (DartCoreDebug.TRACE_UPDATE) {
        UpdateCore.logInfo("DownloadUpdatesJob.run()");//$NON-NLS-1$
      }
      downloadRevision(revision, monitor);
    } catch (IOException e) {
      return UpdateCore.createCancelStatus(e.getMessage());
    }

    return Status.OK_STATUS;
  }

  private SubMonitor createSubMonitor(IProgressMonitor monitor) {
    return SubMonitor.convert(monitor, UpdateJobMessages.DownloadUpdatesJob_progress_label, 100);
  }

  private void downloadInstaller(Revision revision, IProgressMonitor monitor) throws IOException {
    File installerFile = null;
    SubMonitor mon = createSubMonitor(monitor);
    File updateDir = UpdateUtils.getUpdateDir();

    try {

      installerFile = new File(updateDir, revision.toString() + ".msi"); //$NON-NLS-1$
      installerFile.createNewFile();

      //TODO (pquitslund): remove retry when the new scheme has settled in
      try {
        UpdateUtils.downloadFile(
            UpdateUtils.getInstallerUrl(revision, true),
            installerFile,
            NLS.bind(UpdateJobMessages.DownloadUpdatesJob_editor_rev_label, revision.toString()),
            mon);
      } catch (FileNotFoundException e) {
        // fall back to old bucket
        UpdateUtils.downloadFile(
            UpdateUtils.getInstallerUrl(revision, false),
            installerFile,
            NLS.bind(UpdateJobMessages.DownloadUpdatesJob_editor_rev_label, revision.toString()),
            mon);
      }

    } finally {
      if (installerFile != null) {
        // ensure installer file is valid
        if (installerFile.length() == 0) {
          installerFile.delete();
        }
      }

      monitor.done();
    }
  }

  private void downloadRevision(Revision revision, IProgressMonitor monitor) throws IOException {
    if (UpdateUtils.isInstallerPresent()) {
      downloadInstaller(revision, monitor);
    } else {
      downloadZip(revision, monitor);
    }
  }

  private void downloadZip(Revision revision, IProgressMonitor monitor) throws IOException,
      MalformedURLException {

    File updateFile = null;
    SubMonitor mon = createSubMonitor(monitor);
    File updateDir = UpdateUtils.getUpdateDir();

    try {

      updateFile = new File(updateDir, revision.toString() + ".zip"); //$NON-NLS-1$
      updateFile.createNewFile();

      //TODO (pquitslund): remove retry when the new scheme has settled in
      try {
        UpdateUtils.downloadFile(
            revision.getUrl(true),
            updateFile,
            NLS.bind(UpdateJobMessages.DownloadUpdatesJob_editor_rev_label, revision.toString()),
            mon);
      } catch (FileNotFoundException e) {
        // fall back to old bucket
        UpdateUtils.downloadFile(
            revision.getUrl(false),
            updateFile,
            NLS.bind(UpdateJobMessages.DownloadUpdatesJob_editor_rev_label, revision.toString()),
            mon);
      }

    } finally {
      if (updateFile != null) {
        //ensure file is valid
        if (!UpdateUtils.isZipValid(updateFile)) {

          if (DartCoreDebug.TRACE_UPDATE) {
            UpdateCore.logInfo(updateFile.getName() + " invalid -- deleting");//$NON-NLS-1$
          }

          updateFile.delete();
        }
      }

      monitor.done();
    }
  }
}
