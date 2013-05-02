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
import com.google.dart.tools.core.instrumentation.InstrumentationLogger;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.ui.feedback.FeedbackUtils;
import com.google.dart.tools.ui.internal.text.editor.AutoSaveHelper;

import org.eclipse.ui.IStartup;

import java.io.File;

/**
 * This early startup class is called after the main workbench window opens, and is used to warm up
 * various bits of compiler infrastructure.
 */
public class DartUIStartup implements IStartup {

  private static final String DART_INSTRUMENTATION_FLAGS_FILE_NAME = "dart_instrumentation_flags.txt";

  public static void cancelStartup() {
    // No-op : consider removing d
  }

  @Override
  public void earlyStartup() {
    doEarlyStartup();
  }

  private void doEarlyStartup() {

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
      // Catch any runtime exceptions that occur during warm up and log them.
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
