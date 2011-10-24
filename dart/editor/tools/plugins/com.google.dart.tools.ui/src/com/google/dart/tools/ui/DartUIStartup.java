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
package com.google.dart.tools.ui;

import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.indexer.DartIndexer;
import com.google.dart.tools.core.internal.model.DartModelManager;
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

        delay(500);

        if (!getThread().isInterrupted()) {
          compilerWarmup();
        }
      } catch (InterruptedException ie) {

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
        DartCoreDebug.log("Warmup Compiler : " + delta);
      }
    }

    private void delay(int timeInMillis) throws InterruptedException {
      if (!getThread().isInterrupted()) {
        Thread.sleep(timeInMillis);
      }
    }

    private void indexerWarmup() throws InterruptedException {
      // This will initialize the Dart Tools Core plugin as well as the indexer plugin.
      long start = System.currentTimeMillis();
      DartModelManager.getInstance().getDartModel();
      if (DartCoreDebug.WARMUP) {
        long delta = System.currentTimeMillis() - start;
        DartCoreDebug.log("Warmup Model : " + delta);
      }

      delay(500);

      if (!getThread().isInterrupted()) {
        // Warm up the type cache.
        start = System.currentTimeMillis();
        DartIndexer.warmUpIndexer();
        if (DartCoreDebug.WARMUP) {
          long delta = System.currentTimeMillis() - start;
          DartCoreDebug.log("Warmup Indexer : " + delta);
        }
      }
    }
  }

  private static StartupJob startupJob;
  private static Object startupSync = new Object();

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
