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
package com.google.dart.tools.core.analysis.index;

import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.analysis.AnalysisDebug;
import com.google.dart.tools.core.analysis.AnalysisServer;
import com.google.dart.tools.core.analysis.SavedContext;
import com.google.dart.tools.core.internal.index.impl.InMemoryIndex;
import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

import java.io.File;

/**
 * Manages startup and shutdown of {@link AnalysisServer} and {@link InMemoryIndex}
 */
public class AnalysisIndexManager {
  private static final Object lock = new Object();

  private static AnalysisServer server;
  private static AnalysisMarkerManager markerManager;
  private static AnalysisDebug analysisDebugSaved;
  private static AnalysisDebug analysisDebugEdit;

  private static boolean indexing = false;

  /**
   * Answer the default analysis server for Dart Editor, creating and initializing it as necessary
   */
  public static AnalysisServer getServer() {
    synchronized (lock) {
      if (server == null) {
        server = new AnalysisServer(PackageLibraryManagerProvider.getAnyLibraryManager());
        initServerListeners();
        initServerDebug();
        initServerContent();

        // Ensure index is initialized before starting analysis server
        startIndexing();

        server.start();
      }
    }
    return server;
  }

  /**
   * Load the indexer's cached state and start indexing in the background
   */
  public static void startIndexing() {
    synchronized (lock) {
      if (!indexing) {
        indexing = true;
        InMemoryIndex.getInstance().initializeIndex();
        new Thread(new Runnable() {
          @Override
          public void run() {
            InMemoryIndex.getInstance().getOperationProcessor().run();
          }
        }, "Index Operation Processor").start(); //$NON-NLS-1$
      }
    }
  }

  /**
   * Stop the default analysis server and write its current state to disk
   */
  public static void stopServerAndIndexing() {
    synchronized (lock) {
      if (indexing) {
        InMemoryIndex index = InMemoryIndex.getInstance();
        File[] libraryFiles = index.getOperationProcessor().stop(true);
        if (server != null) {
          server.stop();
          SavedContext context = server.getSavedContext();
          for (File file : libraryFiles) {
            context.resolve(file, null);
          }
          stopServerDebug();
          stopServerListener();
          server.writeCache();
          server = null;
        }
        index.shutdown();
        indexing = false;
      }
    }
  }

  private static void initServerContent() {
    if (!server.readCache()) {
      for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
        server.scan(project.getLocation().toFile(), true);
      }
    }
  }

  private static void initServerDebug() {
    if (DartCoreDebug.DEBUG_ANALYSIS) {
      analysisDebugSaved = new AnalysisDebug("Saved");
      server.addIdleListener(analysisDebugSaved);
      server.getSavedContext().addAnalysisListener(analysisDebugSaved);
      analysisDebugEdit = new AnalysisDebug("Edit");
      server.getEditContext().addAnalysisListener(analysisDebugEdit);
    }
  }

  private static void initServerListeners() {
    AnalysisIndexListener listener = new AnalysisIndexListener();
    server.getSavedContext().addAnalysisListener(listener);
    server.addIdleListener(listener);
    markerManager = new AnalysisMarkerManager();
    server.getSavedContext().addAnalysisListener(markerManager);
  }

  private static void stopServerDebug() {
    if (analysisDebugSaved != null) {
      analysisDebugSaved.stop();
      analysisDebugSaved = null;
    }
    if (analysisDebugEdit != null) {
      analysisDebugEdit.stop();
      analysisDebugEdit = null;
    }
  }

  private static void stopServerListener() {
    markerManager.stop();
    markerManager = null;
  }
}
