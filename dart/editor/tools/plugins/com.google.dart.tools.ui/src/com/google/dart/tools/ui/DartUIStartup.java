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
package com.google.dart.tools.ui;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.indexer.DartIndexer;
import com.google.dart.tools.core.internal.index.impl.InMemoryIndex;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;
import com.google.dart.tools.core.utilities.compiler.DartCompilerWarmup;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IStartup;

/**
 * This early startup class is called after the main workbench window opens, and is used to warm up
 * various bits of compiler infrastructure.
 */
public class DartUIStartup implements IStartup {

  private class StartupJob extends Job {

    public StartupJob() {
      super("Dart Editor Initialization");

      setSystem(true);
    }

    @Override
    protected void canceling() {
      getThread().interrupt();
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      try {
        indexerWarmup();
        if (DartCoreDebug.ANALYSIS_SERVER) {
          SystemLibraryManagerProvider.getDefaultAnalysisServer();
        }
        if (!getThread().isInterrupted()) {
          compilerWarmup();
        }
      } catch (InterruptedException ie) {

      } catch (Throwable throwable) {
        // Catch any runtime exceptions that occur during warmup and log them.
        DartToolsPlugin.log("Exception occured during editor warmup", throwable);
      }

      synchronized (startupSync) {
        startupJob = null;
      }

      return Status.OK_STATUS;
    }

    private void compilerWarmup() {
      long start = System.currentTimeMillis();
      DartCompilerWarmup.warmUpCompiler();
      if (DartCoreDebug.WARMUP) {
        long delta = System.currentTimeMillis() - start;
        DartCore.logInformation("Warmup Compiler : " + delta);
      }
    }

    private void indexerWarmup() throws InterruptedException {
      //
      // Initialize the indexer.
      //
      if (!getThread().isInterrupted()) {
        // Warm up the type cache.
        long start = System.currentTimeMillis();
        if (DartCoreDebug.NEW_INDEXER) {
          InMemoryIndex.getInstance().initializeIndex();
        } else {
          DartIndexer.warmUpIndexer();
        }
        if (DartCoreDebug.WARMUP) {
          long delta = System.currentTimeMillis() - start;
          DartCore.logInformation("Warmup Indexer : " + delta);
        }
      }
      //
      // Initialize the Dart Tools Core plugin.
      //
      if (!getThread().isInterrupted()) {
        long start = System.currentTimeMillis();
        DartModelManager.getInstance().getDartModel();
        if (DartCoreDebug.WARMUP) {
          long delta = System.currentTimeMillis() - start;
          DartCore.logInformation("Warmup Model : " + delta);
        }
      }
    }
  }

  private static StartupJob startupJob;
  private static final Object startupSync = new Object();

  public static void cancelStartup() {
    synchronized (startupSync) {
      if (startupJob != null) {
        startupJob.cancel();
      }
    }
  }

  @Override
  public void earlyStartup() {
    synchronized (startupSync) {
      startupJob = new StartupJob();
      startupJob.schedule(500);
    }
  }

}
