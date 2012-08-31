/*
 * Copyright 2012 Dart project authors.
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

package com.google.dart.tools.core.model;

import com.google.dart.tools.core.DartCore;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

// http://commondatastorage.googleapis.com/dart-editor-archive-integration/latest/dartsdk-macos-32.zip

// TOOD(devoncarew): refactor the download/unzip/copy code into utility methods (to be
// shared with the update code).

/**
 * The clearing house for getting the current SDK and listening for SDK changes.
 */
public class DartSdkManager {
  private static final String SDK_DIR_NAME = "dart-sdk";

  /**
   * A special Dart SDK instance signifying that no SDK is installed.
   */
  private static DartSdk NONE = new DartSdk(null) {
    @Override
    public File getDirectory() {
      return null;
    }
  };

  private static DartSdkManager manager = new DartSdkManager();

  public static DartSdkManager getManager() {
    return manager;
  }

  static File getEclipseInstallationDirectory() {
    return new File(Platform.getInstallLocation().getURL().getFile());
  }

  /**
   * The Editor looks for "dart-sdk" as a sibling to the installation directory.
   * 
   * @return
   */
  private static File getDefaultEditorSdkDirectory() {
    File parent = getEclipseInstallationDirectory().getParentFile();

    return new File(parent, SDK_DIR_NAME);
  }

  /**
   * The plugins build looks in the installation directory for "dart-sdk".
   * 
   * @return
   */
  private static File getDefaultPluginsSdkDirectory() {
    return new File(getEclipseInstallationDirectory(), SDK_DIR_NAME);
  }

  /**
   * @return one of 32 or 64
   */
  private static String getPlatformBititude() {
    if (DartCore.is32Bit()) {
      return "32";
    } else {
      return "64";
    }
  }

  /**
   * @return one of macos, windows, or linux
   */
  private static String getPlatformCode() {
    if (DartCore.isWindows()) {
      return "win32";
    } else if (DartCore.isMac()) {
      return "macos";
    } else if (DartCore.isLinux()) {
      return "linux";
    }

    return null;
  }

  private DartSdk sdk;

  private List<DartSdkListener> listeners = new ArrayList<DartSdkListener>();

  private DartSdkManager() {
    initSdk();
  }

  public void addSdkListener(DartSdkListener lisener) {
    listeners.add(lisener);
  }

  public DartSdk getSdk() {
    return sdk;
  }

  public boolean hasSdk() {
    return getSdk() != null && getSdk() != NONE;
  }

  public void removeSdkListener(DartSdkListener listener) {
    listeners.remove(listener);
  }

  /**
   * @param monitor
   */
  public IStatus upgrade(IProgressMonitor monitor) {
    try {
      upgradeImpl(monitor);

      return Status.OK_STATUS;
    } catch (IOException ioe) {
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, ioe.getMessage(), ioe);
    }
  }

  protected void notifyListeners() {
    for (DartSdkListener listener : listeners) {
      listener.sdkUpdated(getSdk());
    }
  }

  private void copyFile(File fromFile, File toFile, IProgressMonitor monitor) throws IOException {
    byte[] data = new byte[4096];

    InputStream in = new FileInputStream(fromFile);

    toFile.delete();

    OutputStream out = new FileOutputStream(toFile);

    monitor.beginTask("Copy " + fromFile.toString(), (int) fromFile.length());

    int count = in.read(data);

    while (count != -1) {
      out.write(data, 0, count);

      monitor.worked(count);

      count = in.read(data);
    }

    in.close();
    out.close();

    toFile.setLastModified(fromFile.lastModified());

    monitor.done();
  }

  private File copyNewSdk(IProgressMonitor monitor, File newSDK) throws IOException {
    File currentSDK = new File(getEclipseInstallationDirectory(), "dart-sdk.zip");

    copyFile(newSDK, currentSDK, monitor);

    return currentSDK;
  }

  private void copyStream(InputStream in, FileOutputStream out, IProgressMonitor monitor, int length)
      throws IOException {
    byte[] data = new byte[4096];

    int count = in.read(data);

    while (count != -1) {
      out.write(data, 0, count);

      if (length != -1) {
        monitor.worked(count);
      }

      count = in.read(data);
    }

    in.close();
    out.close();
  }

  private void deleteDirectory(File dir) {
    for (File file : dir.listFiles()) {
      if (file.isDirectory()) {
        deleteDirectory(file);
      } else {
        file.delete();
      }
    }

    dir.delete();
  }

  private File downloadFile(IProgressMonitor monitor) throws IOException {
    File tempFile = File.createTempFile(SDK_DIR_NAME, ".zip");
    tempFile.deleteOnExit();

    URI downloadURI = URI.create("http://commondatastorage.googleapis.com/dart-editor-archive-integration/latest/dartsdk-"
        + getPlatformCode() + "-" + getPlatformBititude() + ".zip");

    URLConnection connection = downloadURI.toURL().openConnection();

    int length = connection.getContentLength();

    FileOutputStream out = new FileOutputStream(tempFile);

    monitor.beginTask("Download SDK", length);

    copyStream(connection.getInputStream(), out, monitor, length);

    monitor.done();

    if (connection.getLastModified() != 0) {
      tempFile.setLastModified(connection.getLastModified());
    }

    return tempFile;
  }

  private void initSdk() {
    if (getDefaultEditorSdkDirectory().exists()) {
      sdk = new DartSdk(getDefaultEditorSdkDirectory());
    } else if (getDefaultPluginsSdkDirectory().exists()) {
      sdk = new DartSdk(getDefaultPluginsSdkDirectory());
    } else {
      sdk = NONE;
    }
  }

  private void unzip(File zipFile, File destination, IProgressMonitor monitor) throws IOException {
    monitor.beginTask("Unzip " + zipFile.getName(), (int) zipFile.length());

    final int BUFFER_SIZE = 4096;

    ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
    ZipEntry entry;

    while ((entry = zis.getNextEntry()) != null) {
      int count;
      byte data[] = new byte[BUFFER_SIZE];

      File outFile = new File(destination, entry.getName());

      if (entry.isDirectory()) {
        if (!outFile.exists()) {
          outFile.mkdirs();
        }
      } else {
        if (!outFile.getParentFile().exists()) {
          outFile.getParentFile().mkdirs();
        }

        OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));

        while ((count = zis.read(data, 0, BUFFER_SIZE)) != -1) {
          out.write(data, 0, count);

          monitor.worked(count);
        }

        out.flush();
        out.close();
      }
    }

    zis.close();

    monitor.done();
  }

  private void unzipNewSDK(File newSDK, IProgressMonitor monitor) throws IOException {
    File sdkDirectory = getDefaultPluginsSdkDirectory();

    if (sdkDirectory.exists()) {
      deleteDirectory(sdkDirectory);
    }

    unzip(newSDK, getDefaultPluginsSdkDirectory().getParentFile(), monitor);
  }

  private void upgradeImpl(IProgressMonitor monitor) throws IOException {
    try {
      // init progress
      SubMonitor mon = SubMonitor.convert(monitor, "Downloading Dart SDK", 100);

      // download to a temp file
      File tempFile = downloadFile(mon.newChild(80));

      // copy the new sdk
      File newSdk = copyNewSdk(mon.newChild(3), tempFile);

      tempFile.delete();

      // unzip
      unzipNewSDK(newSdk, mon.newChild(10));

      // swap out the new sdk for the old
      if (sdk != null) {
        sdk.dispose();
        sdk = null;
      }

      initSdk();

      // send upgrade notifications
      notifyListeners();
    } finally {
      monitor.done();
    }
  }

}
