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
import com.google.dart.tools.update.core.internal.UpdateUtils;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

import java.io.File;
import java.io.IOException;

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
      //TODO(pquitslund): sysout for testing
      System.out.println("DownloadUpdatesJob.run()"); //$NON-NLS-1$
      downloadRevision(revision, monitor);
    } catch (IOException e) {
      return UpdateCore.createCancelStatus(e.getMessage());
    }

    return Status.OK_STATUS;
  }

  private void downloadRevision(Revision revision, IProgressMonitor monitor) throws IOException {

    File updateFile = null;

    try {

      SubMonitor mon = SubMonitor.convert(
          monitor,
          UpdateJobMessages.DownloadUpdatesJob_progress_label,
          100);

      File updateDir = UpdateUtils.getUpdateDir();

      updateFile = new File(updateDir, revision.toString() + ".zip"); //$NON-NLS-1$

      updateFile.createNewFile();

      UpdateUtils.downloadFile(
          revision.getUrl(),
          updateFile,
          NLS.bind(UpdateJobMessages.DownloadUpdatesJob_editor_rev_label, revision.toString()),
          mon);

    } finally {
      if (updateFile != null) {
        //ensure file is valid
        if (!UpdateUtils.isZipValid(updateFile)) {
          //TODO(pquitslund): sysout for testing
          System.out.println(updateFile.getName() + " invalid -- deleting");
          updateFile.delete();
        }
      }

      monitor.done();
    }
  }

}
