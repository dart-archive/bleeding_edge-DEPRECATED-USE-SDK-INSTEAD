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
package com.google.dart.tools.core.internal.model;

import com.google.dart.compiler.SystemLibraryManager;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.analysis.AnalysisIndexManager;
import com.google.dart.tools.core.analysis.AnalysisMarkerManager;
import com.google.dart.tools.core.analysis.AnalysisServer;
import com.google.dart.tools.core.analysis.ResourceChangeListener;
import com.google.dart.tools.core.analysis.Target;

import org.eclipse.core.runtime.Platform;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * The class <code>SystemLibraryManagerProvider</code> manages the {@link SystemLibraryManager
 * system library managers} used by the tools.
 */
public class SystemLibraryManagerProvider {
  private static final Object lock = new Object();
  private static EditorLibraryManager DARTC_LIBRARY_MANAGER;
  private static EditorLibraryManager VM_LIBRARY_MANAGER;

  private static Target defaultTarget;
  private static AnalysisServer defaultAnalysisServer;
  private static ResourceChangeListener defaultAnalysisChangeListener;

  /**
   * Return the manager for DartC libraries
   */
  public static EditorLibraryManager getDartCLibraryManager() {
    synchronized (lock) {
      if (DARTC_LIBRARY_MANAGER == null) {
        DARTC_LIBRARY_MANAGER = new EditorLibraryManager() {

          @Override
          protected String getPlatformName() {
            return DartCoreDebug.getPlatformName();
          }

          /**
           * Return the "libraries" directory containing DartC libraries
           */
          @Override
          File getLibrariesDir() {
            File installDir = new File(Platform.getInstallLocation().getURL().getFile());
            String libPath = DartCoreDebug.getLibrariesPath();
            File libDir;
            if (libPath.startsWith(File.separator)) {
              libDir = new File(libPath);
            } else {
              libDir = new File(installDir, libPath);
            }
            try {
              libDir = libDir.getCanonicalFile();
            } catch (IOException e) {
              DartCore.logError("Failed to get canonical path for libraries directory " + libDir);
              // Fall through to check existence
            }
            if (!libDir.exists()) {
              File defaultLibDir = new File(installDir, "libraries");
              DartCore.logError("Specified libraries directory does not exist: " + libDir
                  + "\n  using default libraries directory instead: " + defaultLibDir);
              libDir = defaultLibDir;
            }
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
   * Answer the server used to analyze source against the "libraries" directory
   */
  public static AnalysisServer getDefaultAnalysisServer() {
    synchronized (lock) {
      if (defaultAnalysisServer == null) {
        defaultAnalysisServer = new AnalysisServer(getDefaultTarget());
        defaultAnalysisServer.addAnalysisListener(new AnalysisMarkerManager());
        defaultAnalysisServer.addAnalysisListener(new AnalysisIndexManager());
        defaultAnalysisChangeListener = new ResourceChangeListener(defaultAnalysisServer);
      }
    }
    return defaultAnalysisServer;
  }

  /**
   * Answer the default target, mapping "dart:<libname>" to files in the "libraries" directory
   */
  // TODO (danrubel): map "dart:<libname>" to files in "dart-sdk" directory
  public static Target getDefaultTarget() {
    synchronized (lock) {
      if (defaultTarget == null) {
        File installDir = new File(Platform.getInstallLocation().getURL().getFile());
        File librariesDir = new File(installDir, "libraries");
        final HashMap<String, File> dartLibraries = new HashMap<String, File>();
        for (String libName : librariesDir.list()) {
          File libDir = new File(librariesDir, libName);
          if (!libDir.isDirectory()) {
            continue;
          }
          File libFile = new File(libDir, libName + ".dart");
          if (!libFile.isFile()) {

            // Handle odd cases
            if (libName.equals("core")) {
              libFile = new File(libDir, "corelib.dart");
            } else if (libName.equals("coreimpl")) {
              libFile = new File(libDir, "corelib_impl.dart");
            }

            if (!libFile.isFile()) {
              continue;
            }
          }
          dartLibraries.put("dart:" + libName, libFile);
        }
        defaultTarget = new Target() {

          @Override
          public File resolveImport(String relPath) {
            return dartLibraries.get(relPath);
          }
        };
      }
    }
    return defaultTarget;
  }

  /**
   * Return the default library manager.
   */
  public static EditorLibraryManager getSystemLibraryManager() {
    return getVmLibraryManager();
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
            File libDir = new File(new File(installDir, "dart-sdk"), "lib");
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

  /**
   * Stop any active analysis servers
   */
  public static void stop() {
    synchronized (lock) {
      if (defaultAnalysisServer != null) {
        defaultAnalysisChangeListener.stop();
        defaultAnalysisChangeListener = null;
        defaultAnalysisServer.stop();
        defaultAnalysisServer = null;
      }
    }
  }
}
