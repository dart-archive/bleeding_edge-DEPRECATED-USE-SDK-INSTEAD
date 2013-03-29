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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Represents the Dart SDK...
 * 
 * <pre>
 *    dart-sdk/
 *       bin/
 *          dart[.exe]  <-- VM   
 *          Chromium/   <-- Dartium
 *       lib/
 *          core/
 *             core.dart
 *             ... other core library files ...
 *          ... other libraries ...
 *       util/
 *          ... Dart utilities ...
 */
public class DartSdk {

  private final File sdkPath;

  protected DartSdk(File path) {
    sdkPath = path;

    if (sdkPath != null) {
      initializeSdk();
    }
  }

  /**
   * @return the path to the dart2js script in the bin/ directory
   */
  public File getDart2JsExecutable() {
    String dart2jsName = "dart2js" + (DartCore.isWindows() ? ".bat" : "");

    return new File(new File(sdkPath, "bin"), dart2jsName);
  }

  public File getDartDocExecutable() {
    String dartDocName = "dartdoc" + (DartCore.isWindows() ? ".bat" : "");

    return new File(new File(sdkPath, "bin"), dartDocName);
  }

  /**
   * Answer the OS-specific Dartium directory.
   * 
   * @param installDir the installation directory
   */
  public File getDartiumDir(File installDir) {
    return new File(installDir, "chromium");
  }

  /**
   * Answer the Dartium executable or <code>null</code> if it does not exist.
   */
  public File getDartiumExecutable() {
    File file = getDartiumBinary(getDartiumWorkingDirectory());

    if (file.exists()) {
      return file;
    } else {
      // As a fall-back, look in the directory where we used to install Dartium.
      file = getDartiumBinary(getDartiumWorkingDirectory_old());

      if (file.exists()) {
        return file;
      }
    }

    return null;
  }

  /**
   * Returns the directory where Dartium can be found.
   */
  public File getDartiumWorkingDirectory() {
    return new File(DartSdkManager.getEclipseInstallationDirectory(), "chromium");
  }

  /**
   * Returns the old location for Dartium.
   */
  @Deprecated
  public File getDartiumWorkingDirectory_old() {
    return DartSdkManager.getEclipseInstallationDirectory();
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
    return new File(getDirectory(), "docs");
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
    return new File(getDirectory(), "lib");
  }

  /**
   * @return the Dart Editor index file for the SDK libraries
   */
  public File getLibraryIndexFile() {
    return new File(new File(getLibraryDirectory(), "_internal"), "index.idx");
  }

  public File getPubExecutable() {
    String pubName = "pub" + (DartCore.isWindows() ? ".bat" : "");

    return new File(new File(sdkPath, "bin"), pubName);
  }

  /**
   * @return the revision number of the SDK
   */
  public String getSdkVersion() {
    File revisionFile = new File(getDirectory(), "version");

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
    String vmName = "dart" + (DartCore.isWindows() ? ".exe" : "");

    return new File(new File(sdkPath, "bin"), vmName);
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
      // TODO(devoncarew): these changes need to be moved into the create_sdk.py script.
      ensureExecutable(getVmExecutable());
      ensureExecutable(getDart2JsExecutable());
      ensureExecutable(getDartDocExecutable());
    }
  }

  /**
   * Ensure that the dart vm is executable. If it is not, make it executable and log that it was
   * necessary for us to do so.
   */
  private void ensureExecutable(File binary) {
    if (binary != null && binary.exists()) {
      if (!binary.canExecute()) {
        makeExecutable(binary);

        DartCore.logError(binary.getPath() + " was not executable");
      }
    }
  }

  private File getDartiumBinary(File dir) {
    if (DartCore.isWindows()) {
      return new File(dir, "Chrome.exe");
    } else if (DartCore.isMac()) {
      File appDir = new File(dir, "Chromium.app");

      return new File(new File(new File(appDir, "Contents"), "MacOS"), "Chromium");
    } else {
      // Linux
      return new File(dir, "chrome");
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
