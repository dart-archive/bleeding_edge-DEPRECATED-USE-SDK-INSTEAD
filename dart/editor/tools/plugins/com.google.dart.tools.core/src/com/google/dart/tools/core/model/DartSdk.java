/*
 * Copyright 2011 Dart project authors.
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

import java.io.File;
import java.io.IOException;

/**
 * Represents the Dart SDK...
 * 
 * <pre>
 *    dart-sdk/
 *       bin/
 *          dart  <-- VM
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

  private DartSdk(IPath path) {
    sdkPath = path;
  }

  /**
   * Answer the SDK directory
   */
  public File getDirectory() {
    return sdkPath.toFile();
  }

  /**
   * Answer the VM executable or <code>null</code> if it does not exist
   */
  public File getVm() {
    synchronized (lock) {
      if (vm == null) {
        File file = sdkPath.append("bin").append("dart").toFile();
        if (file.exists()) {
          vm = file;
        }
      }
    }
    return vm;
  }
}
