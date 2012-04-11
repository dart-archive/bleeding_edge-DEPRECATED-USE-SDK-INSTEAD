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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * Represents the Dart SDK...
 * 
 * <pre>
 *    dart-sdk/
 *       Chromium    <-- Dartium
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
  private static Object lock = new Object();
  private static DartSdk defaultSdk;

  /**
   * @return the location where the Dart SDK is installed
   */
  public static File getInstallDirectory() {
    return new File(Platform.getInstallLocation().getURL().getFile());
  }

  /**
   * Answer the default SDK that ships with Dart Editor or <code>null</code> if the SDK is not
   * installed
   */
  public static DartSdk getInstance() {
    synchronized (lock) {
      if (defaultSdk == null) {
        File eclipseInstallDir = getInstallDirectory();
        File dir = new File(eclipseInstallDir, "dart-sdk");
        if (dir.exists()) {
          try {
            defaultSdk = new DartSdk(new Path(dir.getCanonicalPath()));
            defaultSdk.initializeSdk();
          } catch (IOException e) {
            DartCore.logError("Failed to resolve SDK path", e);
            // fall through
          }
        }
      }
    }
    return defaultSdk;
  }

  /**
   * @return whether the Dart SDK is installed
   */
  public static boolean isInstalled() {
    return getInstance() != null;
  }

  private final IPath sdkPath;

  private File vm;

  private File dartium;

  private DartSdk(IPath path) {
    sdkPath = path;
  }

  /**
   * Answer the VM executable or <code>null</code> if it does not exist
   */
  public File getDartiumExecutable() {
    synchronized (lock) {
      if (dartium == null) {
        File file = sdkPath.append(getDartiumBinaryName()).toFile();
        if (file.exists()) {
          dartium = file;
        }
      }
    }
    return dartium;
  }

  /**
   * @return the revision number of the SDK
   */
  public String getDartiumVersion() {
    File versionFile = new File(getDirectory(), "chromium.properties");

    try {
      Properties props = new Properties();
      props.load(new FileInputStream(versionFile));

      return props.getProperty("chromium.version");
    } catch (IOException ioe) {

    }

    return "0";
  }

  /**
   * Returns the directory where dartium can be found in the dart-sdk
   */
  public String getDartiumWorkingDirectory() {
    if (DartCore.isWindows() || DartCore.isMac()) {
      return sdkPath.toOSString();
    } else {
      return sdkPath.toOSString().concat("/chromium");
    }
  }

  /**
   * Answer the SDK directory
   */
  public File getDirectory() {
    return sdkPath.toFile();
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
    synchronized (lock) {
      if (vm == null) {
        File file = sdkPath.append("bin").append(getBinaryName()).toFile();
        if (file.exists()) {
          vm = file;
        }
      }
    }
    return vm;
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

    if (dartVm != null) {
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

  private String getDartiumBinaryName() {
    if (DartCore.isWindows()) {
      return "chromium/Chrome.exe";
    } else if (DartCore.isMac()) {
      return "Chromium.app/Contents/MacOS/Chromium";
    } else {
      return "chromium/chrome";
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
