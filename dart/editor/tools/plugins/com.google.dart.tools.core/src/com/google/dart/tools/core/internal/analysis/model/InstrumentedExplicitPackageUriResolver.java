/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.tools.core.internal.analysis.model;

import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.ExplicitPackageUriResolver;
import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.engine.utilities.io.ProcessRunner;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.MessageConsole;

import java.io.File;
import java.io.IOException;

/**
 * A subclass of {@link ExplicitPackageUriResolver} that instruments calls to pub list and notifies
 * the user when those calls exceed the expected time threshold.
 */
public class InstrumentedExplicitPackageUriResolver extends ExplicitPackageUriResolver {

  /**
   * {@link MessageConsole} adapter for printing information about the pub list process.
   */
  class UserConsole {
    private MessageConsole console = null;

    void printElapseTime(long delta) {
      printSeparator();
      console.print("pub " + PUB_LIST_COMMAND + " took " + delta + " milliseconds to run");
    }

    void printFailed(int result, String stdErrOutput) {
      printSeparator();
      console.println("pub " + PUB_LIST_COMMAND + " failed: exit code " + result);
      console.println(stdErrOutput);
    }

    void printThresholdExceeded() {
      printSeparator();
      console.print(PUB_LIST_NAME + " taking longer than " + PUB_LIST_THRESHOLD
          + " milliseconds to complete");
    }

    private void printSeparator() {
      if (console == null) {
        console = DartCore.getConsole();
        console.printSeparator(PUB_LIST_NAME);
        @SuppressWarnings("resource")
        PrintStringWriter writer = new PrintStringWriter();
        writer.print("Running");
        for (String arg : getCommand()) {
          writer.print(" ");
          writer.print(arg);
        }
        writer.println();
        writer.print("  in ");
        writer.println(getRootDir());
      }
    }

  }

  private static final String PUB_LIST_NAME = "pub " + PUB_LIST_COMMAND;

  private static final int PUB_LIST_THRESHOLD = 2000;

  public InstrumentedExplicitPackageUriResolver(DirectoryBasedDartSdk sdk, File rootDir) {
    super(sdk, rootDir);
  }

  @Override
  protected int runProcess(ProcessRunner runner) throws IOException {
    UserConsole console = new UserConsole();
    InstrumentationBuilder instrumentation = Instrumentation.builder("InstrumentedExplicitPackageUriResolver.runPubList");
    try {
      long startTime = System.currentTimeMillis();
      runner.start();
      if (!runner.isComplete(PUB_LIST_THRESHOLD)) {
        console.printThresholdExceeded();
      }
      int result = runner.waitForComplete(0);
      long delta = System.currentTimeMillis() - startTime;
      if (delta > PUB_LIST_THRESHOLD) {
        console.printElapseTime(delta);
      }
      if (result != 0) {
        String stdErr = runner.getStdErr();
        console.printFailed(result, stdErr);
        instrumentation.metric("Exit-code", result);
        instrumentation.data("Std-err", stdErr);
      }
      return result;
    } catch (InterruptedException e) {
      instrumentation.record(e);
      throw new IOException(e);
    } catch (IOException e) {
      instrumentation.record(e);
      throw e;
    } finally {
      instrumentation.log();
    }
  }
}
