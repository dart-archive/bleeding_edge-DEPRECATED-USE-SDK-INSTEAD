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
import java.io.FileReader;
import java.io.IOException;

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
   * Answer the default SDK that ships with Dart Editor or <code>null</code> if the SDK is not
   * installed
   */
  public static DartSdk getInstance() {
    synchronized (lock) {
      if (defaultSdk == null) {
        File eclipseInstallDir = new File(Platform.getInstallLocation().getURL().getFile());
        File dir = new File(eclipseInstallDir, "dart-sdk");
        if (dir.exists()) {
          try {
            defaultSdk = new DartSdk(new Path(dir.getCanonicalPath()));
          } catch (IOException e) {
            DartCore.logError("Failed to resolve SDK path", e);
            // fall through
          }
        }
      }
    }
    return defaultSdk;
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
   * Returns the directory where dartium can be found in the dart-sdk
   */
  public String getDartiumWorkingDirectory() {
    if (isWindows() || isMac()) {
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

  private String getBinaryName() {
    if (isWindows()) {
      return "dart.exe";
    } else {
      return "dart";
    }
  }

  private String getDartiumBinaryName() {
    if (isWindows()) {
      return "Chromium.exe";
    } else if (isMac()) {
      return "Chromium.app/Contents/MacOS/Chromium";
    } else {
      return "chromium/chrome";
    }
  }

  private boolean isMac() {
    // Look for the "Mac" OS name.
    return System.getProperty("os.name").toLowerCase().startsWith("mac");
  }

  private boolean isWindows() {
    // Look for the "Windows" OS name.
    return System.getProperty("os.name").toLowerCase().startsWith("win");
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
