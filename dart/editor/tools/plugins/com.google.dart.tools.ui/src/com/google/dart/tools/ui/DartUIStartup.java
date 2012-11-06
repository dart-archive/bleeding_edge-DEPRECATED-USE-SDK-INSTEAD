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
import com.google.dart.tools.core.analysis.AnalysisServer;
import com.google.dart.tools.core.index.NotifyCallback;
import com.google.dart.tools.core.internal.index.impl.InMemoryIndex;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;
import com.google.dart.tools.core.internal.perf.DartEditorCommandLineManager;
import com.google.dart.tools.core.internal.perf.Performance;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.utilities.compiler.DartCompilerWarmup;
import com.google.dart.tools.ui.actions.CreateAndRevealProjectAction;
import com.google.dart.tools.ui.internal.text.editor.AutoSaveHelper;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import java.io.File;
import java.util.ArrayList;

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
        if (!getThread().isInterrupted()) {
          modelWarmup();
        }
        if (!getThread().isInterrupted()) {
          compilerWarmup();
        }
        if (!getThread().isInterrupted()) {
          detectStartupComplete();
        }
        if (!getThread().isInterrupted()) {
          openInitialFilesAndFolders();
        }
        if (!getThread().isInterrupted()) {
          printPerformanceNumbers();
        }
        AutoSaveHelper.start();
      } catch (Throwable throwable) {
        // Catch any runtime exceptions that occur during warmup and log them.
        DartToolsPlugin.log("Exception occured during editor warmup", throwable);
      }

      synchronized (startupSync) {
        startupJob = null;
      }

      return Status.OK_STATUS;
    }

    /**
     * Load the DartC analysis classes
     */
    private void compilerWarmup() {
      long start = System.currentTimeMillis();
      DartCompilerWarmup.warmUpCompiler();
      if (DartCoreDebug.WARMUP) {
        long delta = System.currentTimeMillis() - start;
        DartCore.logInformation("Warmup Compiler : " + delta);
      }
    }

    /**
     * If {@link DartEditorCommandLineManager#MEASURE_PERFORMANCE} is <code>true</code>, then record
     * the {@link Performance#TIME_TO_START_UI} metric.
     */
    private void detectStartupComplete() {
      if (DartEditorCommandLineManager.MEASURE_PERFORMANCE) {
        Performance.TIME_TO_START_UI.log(DartEditorCommandLineManager.getStartTime());
      }
    }

    /**
     * Initialize the Dart Tools Core plugin.
     */
    private void modelWarmup() {
      long start = System.currentTimeMillis();
      DartModelManager.getInstance().getDartModel();
      if (DartCoreDebug.WARMUP) {
        long delta = System.currentTimeMillis() - start;
        DartCore.logInformation("Warmup Model : " + delta);
      }
    }

    /**
     * Loop through the files input on the command line, retrieved from
     * {@link DartEditorCommandLineManager#getFileSet()}, and open them in the Editor appropriately.
     */
    private void openInitialFilesAndFolders() {
      final ArrayList<File> fileSet = DartEditorCommandLineManager.getFileSet();
      if (fileSet == null || fileSet.isEmpty()) {
        return;
      }
      Display.getDefault().asyncExec(new Runnable() {
        @Override
        public void run() {
          for (File file : fileSet) {
            // verify that this file is not null, and exists
            if (file == null || !file.exists()) {
              continue;
            }
            final File fileToOpen = file;
            IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (fileToOpen.isFile()) {
              // If this File to open is a file, instead of a directory, then open the directory,
              // and then open the file in an editor
              String directoryToOpen = fileToOpen.getParentFile().getAbsolutePath();
              new CreateAndRevealProjectAction(workbenchWindow, directoryToOpen).run();
              try {
                EditorUtility.openInEditor(ResourceUtil.getFile(fileToOpen));
              } catch (PartInitException e) {
                e.printStackTrace();
              } catch (DartModelException e) {
                e.printStackTrace();
              }
            } else {
              // If this File to open is a directory, instead of a file, then just open the directory.
              String directoryToOpen = fileToOpen.getAbsolutePath();
              new CreateAndRevealProjectAction(workbenchWindow, directoryToOpen).run();
            }
          }
          if (DartEditorCommandLineManager.MEASURE_PERFORMANCE) {
            Performance.TIME_TO_OPEN.log(DartEditorCommandLineManager.getStartTime());
          }
        }
      });
    }

    /**
     * Print the performance numbers, if {@link DartEditorCommandLineManager#MEASURE_PERFORMANCE} is
     * <code>true</code>.
     */
    private void printPerformanceNumbers() {
      if (!DartEditorCommandLineManager.MEASURE_PERFORMANCE) {
        return;
      }
      // wait for analysis is finished
      waitForAnalysis();
      // record the final performance number, and print key:value results in an asyncExec, to
      // ensure that the UI thread is not busy
      Display.getDefault().asyncExec(new Runnable() {
        @Override
        public void run() {
          Performance.TIME_TO_ANALYSIS_COMPLETE.log(DartEditorCommandLineManager.getStartTime());
          InMemoryIndex.getInstance().notify(new NotifyCallback() {
            @Override
            public void done() {
              Performance.TIME_TO_INDEX_COMPLETE.log(DartEditorCommandLineManager.getStartTime());
              Performance.printResults_keyValue();
              if (DartEditorCommandLineManager.KILL_AFTER_PERF) {
                // From the UI thread, call PlatformUI.getWorkbench().close() to exit the Dart Editor
                Display.getDefault().syncExec(new Runnable() {
                  @Override
                  public void run() {
                    PlatformUI.getWorkbench().close();
                  }
                });
              }
            }
          });
        }
      });
    }

    /**
     * Wait for any background analysis to be complete
     */
    private void waitForAnalysis() {
      AnalysisServer server = PackageLibraryManagerProvider.getDefaultAnalysisServer();
      // Wait up to 2 minutes for the server to be idle
      if (!server.waitForIdle(120000)) {
        System.err.println("Stopped waiting for the analysis server.");
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
