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

import org.eclipse.core.runtime.Platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Represents the Dart SDK...
 * 
 * <pre>
 *    Chromium/    <-- Dartium
 *    dart-sdk/
 *       bin/
 *          dart[.exe]  <-- VM   
 *       lib/
 *          core/
 *             core_runtime.dart
 *             ... other core library files ...
 *          ... other libraries ...
 *       util/
 *          ... Dart utilities ...
 */
public class DartSdk {

  /**
   * @return the location where the Dart SDK is installed
   */
  public static File getInstallDirectory() {
    return new File(Platform.getInstallLocation().getURL().getFile());
  }

  public static DartSdk getInstance() {
    return DartSdkManager.getManager().getSdk();
  }

  public static boolean isInstalled() {
    return DartSdkManager.getManager().hasSdk();
  }

  private final File sdkPath;

  private File dartium;

  protected DartSdk(File path) {
    sdkPath = path;

    if (sdkPath != null) {
      initializeSdk();
    }
  }

  /**
   * Answer the VM executable or <code>null</code> if it does not exist
   */
  public File getDartiumExecutable() {
    if (dartium == null) {
      File file = getDartiumBinary(getDartiumWorkingDirectory());

      if (file.exists()) {
        dartium = file;
      }
    }

    return dartium;
  }

  /**
   * Returns the directory where Dartium can be found.
   */
  public File getDartiumWorkingDirectory() {
    return getInstallDirectory();
  }

  /**
   * Answer the SDK directory
   */
  public File getDirectory() {
    return sdkPath;
  }

  /**
   * @return the SDK's documentation directory
   */
  public File getDocDirectory() {
    return new File(DartSdk.getInstance().getDirectory(), "docs");
  }

  /**
   * Return the auxiliary documentation file for the given library. Return null if no such file
   * exists.
   * 
   * @param libraryName
   * @return
   */
  public File getDocFileFor(String libraryName) {
    File dir = getDocDirectory();

    if (!dir.exists()) {
      return null;
    }

    File libDir = new File(dir, libraryName);

    File docFile = new File(libDir, libraryName + "_api.json");

    if (docFile.exists()) {
      return docFile;
    } else {
      return null;
    }
  }

  /**
   * @return the SDK's library directory path
   */
  public File getLibraryDirectory() {
    return new File(DartSdk.getInstance().getDirectory(), "lib");
  }

  /**
   * @return the revision number of the SDK
   */
  public String getSdkVersion() {
    File revisionFile = new File(getDirectory(), "revision");

    try {
      String revision = readFully(revisionFile);

      if (revision != null) {
        return revision;
      }
    } catch (IOException ioe) {

    }

    return "0";
  }

  /**
   * Answer the VM executable or <code>null</code> if it does not exist
   */
  public File getVmExecutable() {
    return new File(new File(sdkPath, "bin"), getBinaryName());
  }

  /**
   * @return whether this install of the SDK has documentation
   */
  public boolean hasDocumentation() {
    return getDocDirectory().exists();
  }

  /**
   * Checks if dartium binary is available
   */
  public boolean isDartiumInstalled() {
    if (getDartiumExecutable() != null) {
      return true;
    }

    return false;
  }

  protected void dispose() {

  }

  protected void initializeSdk() {
    if (!DartCore.isWindows()) {
      ensureVmIsExecutable();
    }
  }

  /**
   * Ensure that the dart vm is executable. If it is not, make it executable and log that it was
   * necessary for us to do so.
   */
  private void ensureVmIsExecutable() {
    File dartVm = getVmExecutable();

    if (dartVm != null && dartVm.exists()) {
      if (!dartVm.canExecute()) {
        makeExecutable(dartVm);

        DartCore.logError(dartVm.getPath() + " was not executable");
      }
    }
  }

  private String getBinaryName() {
    if (DartCore.isWindows()) {
      return "dart.exe";
    } else {
      return "dart";
    }
  }

  private File getDartiumBinary(File installDir) {
    if (DartCore.isWindows()) {
      return new File(new File(installDir, "chromium"), "Chrome.exe");
    } else if (DartCore.isMac()) {
      return new File(
          new File(new File(new File(installDir, "Chromium.app"), "Contents"), "MacOS"),
          "Chromium");
    } else {
      // Linux
      return new File(new File(installDir, "chromium"), "chrome");
    }
  }

  /**
   * Make the given file executable; returns true on success.
   * 
   * @param file
   * @return
   */
  private boolean makeExecutable(File file) {
    // First try and set executable for all users.
    if (file.setExecutable(true, false)) {
      // success

      return true;
    }

    // Then try only for the current user.
    return file.setExecutable(true, true);
  }

  private String readFully(File revisionFile) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(revisionFile));

    try {
      String line = reader.readLine();

      if (line != null) {
        return line.trim();
      } else {
        return null;
      }
    } finally {
      reader.close();
    }
  }

}
