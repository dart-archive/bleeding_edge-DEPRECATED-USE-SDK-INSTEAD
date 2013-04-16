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

import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.tools.core.CmdLineOptions;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.analysis.AnalysisServer;
import com.google.dart.tools.core.index.NotifyCallback;
import com.google.dart.tools.core.instrumentation.InstrumentationLogger;
import com.google.dart.tools.core.internal.index.impl.InMemoryIndex;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;
import com.google.dart.tools.core.internal.perf.Performance;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.core.utilities.compiler.DartCompilerWarmup;
import com.google.dart.tools.ui.feedback.FeedbackUtils;
import com.google.dart.tools.ui.internal.text.editor.AutoSaveHelper;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;

import java.io.File;

/**
 * This early startup class is called after the main workbench window opens, and is used to warm up
 * various bits of compiler infrastructure.
 */
public class DartUIStartup implements IStartup {

  private class StartupJob extends Job {
    private CmdLineOptions options;

    public StartupJob() {
      super("Dart Editor Initialization");
      options = CmdLineOptions.getOptions();
      setSystem(true);
    }

    @Override
    protected void canceling() {
      getThread().interrupt();
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {

      //Pre-start the instrumentation logger if it's registered
      InstrumentationLogger.ensureLoggerStarted();
      InstrumentationBuilder instrumentation = Instrumentation.builder("DartUIStartup.run");
      try { //Instrumentation try - finally

        try {

          if (!getThread().isInterrupted()) {
            reportPlatformStatistics(); //Early report statistics to aid debugging in case of crash
            reportDartCoreDebug();
          }

          if (!getThread().isInterrupted()) {
            modelWarmup();
            instrumentation.metric("ModelWarmup", "Complete");

          }
          if (!getThread().isInterrupted()) {
            compilerWarmup();
            instrumentation.metric("Compiler", "Complete");

          }
          if (!getThread().isInterrupted()) {
            detectStartupComplete();
            instrumentation.metric("StartupComplete", "Complete");
          }
          if (!getThread().isInterrupted()) {
            new CmdLineFileProcessor(options).run();
            instrumentation.metric("OpenInitialFilesAndFolders", "Complete");
          }
          if (!getThread().isInterrupted()) {
            printPerformanceNumbers();

            //This is a special case as it's for recording from before when instrumentation was
            //available, so need to manually compute the time
            long delta = System.currentTimeMillis() - options.getStartTime();
            instrumentation.metric("DartUIStartup-Complete-FromInitialTimeApproximation", delta).log();

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
      } finally {
        instrumentation.log();

      }
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
     * If {@link CmdLineOptions#getMeasurePerformance()} is <code>true</code>, then record the
     * {@link Performance#TIME_TO_START_UI} metric.
     */
    private void detectStartupComplete() {
      if (options.getMeasurePerformance()) {
        Performance.TIME_TO_START_UI.log(options.getStartTime());
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
     * Print the performance numbers, if {@link CmdLineOptions#getMeasurePerformance()} is
     * <code>true</code>.
     */
    private void printPerformanceNumbers() {
      if (!options.getMeasurePerformance()) {
        return;
      }
      // wait for analysis is finished
      waitForAnalysis();
      // record the final performance number, and print key:value results in an asyncExec, to
      // ensure that the UI thread is not busy
      Display.getDefault().asyncExec(new Runnable() {
        @Override
        public void run() {
          Performance.TIME_TO_ANALYSIS_COMPLETE.log(options.getStartTime());
          InMemoryIndex.getInstance().notify(new NotifyCallback() {
            @Override
            public void done() {
              Performance.TIME_TO_INDEX_COMPLETE.log(options.getStartTime());
              Performance.printResults_keyValue();
              if (options.getAutoExit()) {
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

    private void reportDartCoreDebug() {
      InstrumentationBuilder instrumentation = Instrumentation.builder("DartUIStartup.reportDartCoreDebug");
      try {

        DartCoreDebug.record(instrumentation);

      } finally {
        instrumentation.log();

      }

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

  private static final String DART_INSTRUMENTATION_FLAGS_FILE_NAME = "dart_instrumentation_flags.txt";

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
    if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
      earlyStartupNewAnalysis();
    } else {
      synchronized (startupSync) {
        startupJob = new StartupJob();
        startupJob.schedule(500);
      }
    }
  }

  private void earlyStartupNewAnalysis() {

    //Pre-start the instrumentation logger if it's registered
    InstrumentationLogger.ensureLoggerStarted();
    InstrumentationBuilder instrumentation = Instrumentation.builder("DartUIStartup.earlyStartup");

    try {
      reportPlatformStatistics();
      reportDartCoreDebug();

      new CmdLineFileProcessor(CmdLineOptions.getOptions()).run();
      instrumentation.metric("OpenInitialFilesAndFolders", "Complete");

      AutoSaveHelper.start();
      instrumentation.metric("AutoSaveHelperStart", "Complete");

      // TODO (danrubel): performance measurements from old startup

    } catch (Throwable throwable) {
      // Catch any runtime exceptions that occur during warmup and log them.
      DartToolsPlugin.log("Exception occured during editor warmup", throwable);

    } finally {
      instrumentation.log();
    }
  }

  /**
   * Determine if the {@value #DART_INSTRUMENTATION_FLAGS_FILE_NAME} file exists in the user's dart
   * directory.
   * 
   * @return {@code true} if the file exists, else false
   */
  private boolean IsInstrumentationFlagFilePresent() {
    File dartDir = new File(DartCore.getUserDefaultDartFolder());
    return new File(dartDir, DART_INSTRUMENTATION_FLAGS_FILE_NAME).exists();
  }

  private void reportDartCoreDebug() {
    InstrumentationBuilder instrumentation = Instrumentation.builder("DartUIStartup.reportDartCoreDebug");
    try {
      DartCoreDebug.record(instrumentation);
    } finally {
      instrumentation.log();
    }
  }

  /**
   * Report core statistics about the executing platform to instrumentation system
   */
  private void reportPlatformStatistics() {
    InstrumentationBuilder instrumentation = Instrumentation.builder("DartUIStartup.reportPlatformStatistics");
    try {
      instrumentation.metric("BuildDate", DartCore.getBuildDate());
      instrumentation.metric("BuildID", DartCore.getBuildId());
      instrumentation.metric("Version", DartCore.getVersion());
      instrumentation.metric("SDKVersion", DartSdkManager.getManager().getSdk().getSdkVersion());
      instrumentation.metric("OSVersion", FeedbackUtils.getOSName());
      instrumentation.metric("IsInstrumentationFlagFilePresent", IsInstrumentationFlagFilePresent());
    } finally {
      instrumentation.log();
    }
  }
}
