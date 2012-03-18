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
import com.google.dart.tools.core.analysis.Target;
import com.google.dart.tools.core.model.DartSdk;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * The class <code>SystemLibraryManagerProvider</code> manages the {@link SystemLibraryManager
 * system library managers} used by the tools.
 */
public class SystemLibraryManagerProvider {
  private static final Object lock = new Object();
  private static EditorLibraryManager VM_LIBRARY_MANAGER;

  private static Target defaultTarget;
  private static AnalysisServer defaultAnalysisServer;

  //private static ResourceChangeListener defaultAnalysisChangeListener;

  /**
   * Answer the server used to analyze source against the "dart-sdk/lib" directory
   */
  public static AnalysisServer getDefaultAnalysisServer() {
    synchronized (lock) {
      if (defaultAnalysisServer == null) {
        defaultAnalysisServer = new AnalysisServer(getDefaultTarget());
        defaultAnalysisServer.addAnalysisListener(new AnalysisMarkerManager(defaultAnalysisServer));
        defaultAnalysisServer.addAnalysisListener(new AnalysisIndexManager());
        // TODO (danrubel) merge ResourceChangeListener with delta processor
        DartCore.notYetImplemented();
        //defaultAnalysisChangeListener = new ResourceChangeListener(defaultAnalysisServer);
        //defaultAnalysisChangeListener.start();
      }
    }
    return defaultAnalysisServer;
  }

  /**
   * Answer the default target, mapping "dart:<libname>" to files in the "dart-sdk/lib" directory
   */
  // TODO (danrubel): map "dart:<libname>" to files in "dart-sdk" directory
  public static Target getDefaultTarget() {
    synchronized (lock) {
      if (defaultTarget == null) {
        defaultTarget = new Target() {

          @Override
          public File resolvePath(String relPath) {
            URI relativeUri;
            try {
              relativeUri = new URI(relPath);
            } catch (URISyntaxException e) {
              DartCore.logError("Failed to create URI: " + relPath, e);
              return null;
            }
            URI resolveUri = getVmLibraryManager().resolveDartUri(relativeUri);
            if (resolveUri == null) {
              return null;
            }
            return new File(resolveUri.getPath());
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

          /**
           * Return the SDK "lib" directory
           */
          @Override
          public File getLibrariesDir() {
            DartSdk sdk = DartSdk.getInstance();
            if (sdk == null) {
              DartCore.logError("Missing SDK");
            }
            File libDir = sdk.getLibraryDirectory();
            if (!libDir.exists()) {
              DartCore.logError("Missing libraries directory: " + libDir);
            }
            File librariesDir = libDir;
            if (DartCoreDebug.DARTLIB) {
              DartCore.logInformation("Reading bundled libraries from " + librariesDir);
            }
            return librariesDir;
          }

          @Override
          protected String getPlatformName() {
            return "runtime";
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
        //defaultAnalysisChangeListener.stop();
        //defaultAnalysisChangeListener = null;
        defaultAnalysisServer.stop();
        defaultAnalysisServer = null;
      }
    }
  }
}
