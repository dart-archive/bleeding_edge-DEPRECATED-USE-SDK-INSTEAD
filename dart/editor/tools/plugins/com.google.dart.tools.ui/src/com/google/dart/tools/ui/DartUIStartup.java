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

import com.google.dart.tools.core.indexer.DartIndexer;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;

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
      // TODO(devoncarew): once the warmUpCompiler() method is performing useful work,
      // time it and find out how many calls gets us the best bang for the buck. If the
      // 3rd call speeds up significantly, then call warmUpCompiler() twice.

      //long start = System.currentTimeMillis();

      DartCompilerUtilities.warmUpCompiler();

      //System.out.println("compiler: " + (System.currentTimeMillis() - start) + "ms");
    }

    private void delay(int timeInMillis) throws InterruptedException {
      if (!getThread().isInterrupted()) {
        Thread.sleep(timeInMillis);
      }
    }

    private void indexerWarmup() throws InterruptedException {
      // This will initialize the Dart Tools Core plugin as well as the indexer plugin.
      DartModelManager.getInstance().getDartModel();

      delay(500);

      if (!getThread().isInterrupted()) {
        // Warm up the type cache.
        DartIndexer.warmUpIndexer();
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
