/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.eclipse.core.jobs;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.mobile.AndroidSdkManager;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.core.utilities.download.DownloadUtilities;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * Downloads the android.zip containing the binaries needed for mobile support.
 */
public class AndroidZipDownloadJob extends Job {

  private static final String ANDROID_ZIP = "latest/editor/android.zip";

  public AndroidZipDownloadJob() {
    super("Downloading android.zip");
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {

    String updateLocation = DartSdkManager.getManager().getUpdateChannelUrl();

    try {
      downloadImpl(updateLocation, monitor);
    } catch (IOException ioe) {
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, ioe.getMessage(), ioe);
    }

    return Status.OK_STATUS;
  }

  private File copyNewZip(IProgressMonitor monitor, File newZip) throws IOException {
    File currentZip = new File(DartSdkManager.getEclipseInstallationDirectory(), "android.zip");

    DownloadUtilities.copyFile(newZip, currentZip, monitor);

    return currentZip;
  }

  private void downloadImpl(String channel, IProgressMonitor monitor) throws IOException {
    try {
      // init progress
      SubMonitor mon = SubMonitor.convert(monitor, "Downloading android.zip", 100);

      URI downloadURI = URI.create(channel + ANDROID_ZIP);

      // download to a temp file
      File tempFile = DownloadUtilities.downloadZipFile(downloadURI, "android",
          "Downloading android.zip", mon.newChild(80));

      // copy the new zip
      File newZip = copyNewZip(mon.newChild(3), tempFile);

      tempFile.delete();

      // unzip
      unzipNewAndriodZip(newZip, mon.newChild(10));

      newZip.delete();

      AndroidSdkManager.getManager().notifyMobileUpdateListeners();

      DartCore.getConsole().printSeparator("Download mobile");
      DartCore.getConsole().println("Dart Content Shell apk and Adb executable downloaded");

    } finally {
      monitor.done();
    }
  }

  private void unzipNewAndriodZip(File newSDK, IProgressMonitor monitor) throws IOException {
    File androidDirectory = AndroidSdkManager.getManager().getDefaultPluginsAndroidDirectory();

    if (androidDirectory.exists()) {
      DownloadUtilities.deleteDirectory(androidDirectory);
    }

    DownloadUtilities.unzip(newSDK, androidDirectory, monitor);
  }

}
