/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.model;

import com.google.dart.compiler.SystemLibraryManager;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;

import org.eclipse.core.runtime.Platform;

import java.io.File;

/**
 * The class <code>SystemLibraryManagerProvider</code> manages the {@link SystemLibraryManager
 * system library managers} used by the tools.
 */
public class SystemLibraryManagerProvider {
  private static final Object lock = new Object();
  private static EditorLibraryManager DARTC_LIBRARY_MANAGER;
  private static EditorLibraryManager VM_LIBRARY_MANAGER;

  /**
   * Return the manager for DartC libraries
   */
  public static EditorLibraryManager getDartCLibraryManager() {
    synchronized (lock) {
      if (DARTC_LIBRARY_MANAGER == null) {
        DARTC_LIBRARY_MANAGER = new EditorLibraryManager() {

          @Override
          protected String getPlatformName() {
            return "compiler";
          }

          /**
           * Return the "libraries" directory containing DartC libraries
           */
          @Override
          File getLibrariesDir() {
            File installDir = new File(Platform.getInstallLocation().getURL().getFile());
            File libDir = new File(installDir, "libraries");
            if (!libDir.exists()) {
              DartCore.logError("Missing libraries directory: " + libDir);
            }
            if (DartCoreDebug.DARTLIB) {
              DartCore.logInformation("Reading bundled libraries from " + libDir);
            }
            return libDir;
          }
        };
      }
    }
    return DARTC_LIBRARY_MANAGER;
  }

  /**
   * Return the default library manager.
   */
  public static EditorLibraryManager getSystemLibraryManager() {
    return getDartCLibraryManager();
  }

  /**
   * Return the manager for VM libraries
   */
  public static EditorLibraryManager getVmLibraryManager() {
    synchronized (lock) {
      if (VM_LIBRARY_MANAGER == null) {
        VM_LIBRARY_MANAGER = new EditorLibraryManager() {

          @Override
          protected String getPlatformName() {
            return "runtime";
          }

          /**
           * Return the SDK "lib" directory
           */
          @Override
          File getLibrariesDir() {
            File installDir = new File(Platform.getInstallLocation().getURL().getFile());
            File libDir = new File(new File(installDir, "sdk"), "lib");
            if (!libDir.exists()) {
              DartCore.logError("Missing libraries directory: " + libDir);
            }
            if (DartCoreDebug.DARTLIB) {
              DartCore.logInformation("Reading bundled libraries from " + libDir);
            }
            return libDir;
          }
        };
      }
    }
    return VM_LIBRARY_MANAGER;
  }
}
