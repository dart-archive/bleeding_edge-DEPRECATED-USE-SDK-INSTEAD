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

package com.google.dart.tools.core.frog;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.model.DartSdk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Start and manage the frog compiler process.
 */
public class FrogProcess {
  private static final String FROG_STARTUP_TOKEN = "frog: accepting connections";

  /** @return the path to the the frog server script, relative to the SDK directory */
  private static final String FROG_SERVER_PATH = "frog/server/frog_server.dart";

  // 20 seconds
  private static final int MAX_STARTUP_WAIT = 20000;

  private int port;

  private Process process;

  private boolean frogRunning;

  public FrogProcess() {
    this(FrogManager.AUTO_BIND_PORT);
  }

  public FrogProcess(int port) {
    if (port == -1) {
      throw new IllegalArgumentException("port cannot == -1");
    }

    this.port = port;
  }

  public int getPort() {
    return port;
  }

  public boolean isFrogRunning() {
    return frogRunning;
  }

  public void killProcess() {
    if (process != null) {
      if (!hasStopped()) {
        process.destroy();
      }

      process = null;
    }
  }

  public void startProcess() throws IOException {
    if (DartSdk.getInstance() == null) {
      throw new IOException("Unable to start Frog - no dart-sdk found");
    }

    ProcessBuilder builder = new ProcessBuilder();

    // Set the heap size to 128MB.
    builder.command(FrogManager.getDartVmExecutablePath(), "--new_gen_heap_size=128",
        FROG_SERVER_PATH, FrogManager.LOCALHOST_ADDRESS, Integer.toString(getPort()));
    builder.directory(DartSdk.getInstance().getLibraryDirectory());
    builder.redirectErrorStream(true);

    process = builder.start();

    final CountDownLatch latch = new CountDownLatch(1);

    // We need to consume the text the VM writes to its stdout / stderr so that the process does not
    // stall out trying to write to stdout.
    Thread readerThread = new Thread(new Runnable() {
      @Override
      public void run() {
        InputStream in = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        try {
          String line = reader.readLine();

          while (line != null) {
            if (DartCoreDebug.LOGGING_FROG) {
              DartCore.logInformation("frog: [" + line.trim() + "]");
            }

            if (line.contains(FROG_STARTUP_TOKEN)) {
              frogRunning = true;
              port = parseServerPort(line);
              latch.countDown();
            }

            // Log any internal frog server problems
            if (line.equals("Unhandled exception:") || line.contains("Error: line")) {
              StringWriter message = new StringWriter(1000);
              PrintWriter writer = new PrintWriter(message);
              writer.println("Internal frog server exception:");
              while (true) {
                writer.println(line);
                if (message.getBuffer().length() > 2000) {
                  break;
                }
                line = reader.readLine();
                if (line == null || line.trim().length() == 0) {
                  break;
                }
                if (DartCoreDebug.LOGGING_FROG) {
                  DartCore.logInformation("frog: [" + line.trim() + "]");
                }
              }
              DartCore.logError(message.toString());
            }

            line = reader.readLine();
          }

          latch.countDown();
        } catch (IOException exception) {
          // The process has terminated.

          latch.countDown();
        }
      }
    });

    readerThread.start();

    // If we didn't start up normally, then throw an exception.
    try {
      latch.await(MAX_STARTUP_WAIT, TimeUnit.MILLISECONDS);
    } catch (InterruptedException exception) {

    }

    if (!frogRunning) {
      throw new IOException("unable to start frog server");
    }

    if (port == -1) {
      // Kill the server - it is unreachable at this point.
      killProcess();

      throw new IOException("unable to retrieve server port");
    }
  }

  protected boolean hasStopped() {
    if (process == null) {
      return true;
    }

    try {
      process.exitValue();

      return true;
    } catch (IllegalThreadStateException exception) {
      // The process is still running.

      return false;
    }
  }

  protected int parseServerPort(String line) {
    // "FROG_STARTUP_TOKEN on host:port"

    final String START_TOKEN = " on ";

    int index = line.indexOf(START_TOKEN);

    if (index != -1) {
      line = line.substring(index + START_TOKEN.length());
    }

    String[] strs = line.split(":");

    if (strs.length >= 2) {
      try {
        return Integer.parseInt(strs[1]);
      } catch (NumberFormatException nfe) {

      }
    }

    return -1;
  }

}
