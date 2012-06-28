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
import com.google.dart.tools.core.analysis.AnalysisServer;
import com.google.dart.tools.core.analysis.index.AnalysisIndexManager;
import com.google.dart.tools.core.model.DartSdk;

import java.io.File;

/**
 * The class <code>SystemLibraryManagerProvider</code> manages the {@link SystemLibraryManager
 * system library managers} used by the tools.
 */
public class SystemLibraryManagerProvider {
  private static final Object lock = new Object();
  private static EditorLibraryManager ANY_LIBRARY_MANAGER;

  /**
   * Return the manager for VM libraries
   */
  public static EditorLibraryManager getAnyLibraryManager() {
    synchronized (lock) {
      if (ANY_LIBRARY_MANAGER == null) {

        DartSdk sdk = DartSdk.getInstance();
        if (sdk == null) {
          DartCore.logError("Missing SDK");
          return null;
        }

        File sdkDir = sdk.getDirectory();
        if (!sdkDir.exists()) {
          DartCore.logError("Missing libraries directory: " + sdkDir);
          return null;
        }

        DartCore.logInformation("Reading bundled libraries from " + sdkDir);

        ANY_LIBRARY_MANAGER = new EditorLibraryManager(sdkDir, "any");
        String packageRoot = DartCore.getPlugin().getPrefs().get(
            DartCore.PACKAGE_ROOT_DIR_PREFERENCE,
            "");
        if (packageRoot != null && !packageRoot.isEmpty()) {
          ANY_LIBRARY_MANAGER.setPackageRoot(new File(packageRoot));
        }

      }
    }
    return ANY_LIBRARY_MANAGER;
  }

  /**
   * Answer the server used to analyze source against the "dart-sdk/lib" directory
   */
  public static AnalysisServer getDefaultAnalysisServer() {
    return AnalysisIndexManager.getServer();
  }

  /**
   * Return the default library manager.
   */
  public static EditorLibraryManager getSystemLibraryManager() {
    return getAnyLibraryManager();
  }
}
