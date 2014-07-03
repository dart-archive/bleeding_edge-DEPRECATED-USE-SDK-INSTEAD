/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.utilities.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

/**
 * Execute the process created by the given process builder; collect the results and the exit code.
 * The process runs to completion before the run() method returns.
 */
public class ProcessRunner {
  private ProcessBuilder processBuilder;

  private int exitCode;
  private StringBuilder stdout = new StringBuilder();
  private StringBuilder stderr = new StringBuilder();

  private Thread processThread;
  private Thread stdoutThread;
  private Thread stderrThread;
  private Process process;

  public ProcessRunner(ProcessBuilder processBuilder) {
    this.processBuilder = processBuilder;
  }

  public ProcessRunner(String[] arguments) {
    this(new ProcessBuilder(arguments));
  }

  public void dispose() {
    if (process != null) {
      // This is set to null in runAsync().
      process.destroy();
    }
  }

  public int getExitCode() {
    return exitCode;
  }

  public String getStdErr() {
    return stderr.toString();
  }

  public String getStdOut() {
    return stdout.toString();
  }

  /**
   * Wait up to the specified time for the process to complete. This assumes that {@link #start()}
   * has already been called to launch the process, and that {@link #waitForComplete(long)} will be
   * called before retrieving stdout, stderr, or the exit code.
   * 
   * @param milliseconds the maximum number of milliseconds to wait for completion or zero to wait
   *          forever for the process to complete.
   * @return {@code true} if the process is complete, else {@code false}
   */
  public boolean isComplete(long milliseconds) throws InterruptedException {
    processThread.join(milliseconds);
    return !processThread.isAlive();
  }

  /**
   * Execute the process created by the process builder, wait up to the specified time for the
   * process to complete, and return the exit value.
   * 
   * @param milliseconds the maximum number of milliseconds to wait for completion or zero to wait
   *          forever for the process to complete.
   * @return the exit value or -1 if timed out waiting for the process to complete
   */
  public int runSync(long milliseconds) throws IOException {
    start();
    return waitForComplete(milliseconds);
  }

  /**
   * Launch the process created by the process builder.
   */
  public void start() throws IOException {
    exitCode = 0;
    stdout.setLength(0);
    stderr.setLength(0);

    final Process process = processBuilder.start();

    processThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          exitCode = process.waitFor();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    });

    stdoutThread = new Thread(new Runnable() {
      @Override
      public void run() {
        pipeOutput(process.getInputStream(), stdout);
      }
    });

    stderrThread = new Thread(new Runnable() {
      @Override
      public void run() {
        pipeOutput(process.getErrorStream(), stderr);
      }
    });

    processThread.start();
    stdoutThread.start();
    stderrThread.start();
  }

  /**
   * Wait up to the specified time for the process to complete, and return the exit value. This
   * assumes that {@link #start()} has already been called to launch the process.
   * 
   * @param milliseconds the maximum number of milliseconds to wait for completion or zero to wait
   *          forever for the process to complete.
   * @return the exit value or -1 if timed out waiting for the process to complete
   */
  public int waitForComplete(long milliseconds) throws IOException {
    try {
      if (!isComplete(milliseconds)) {
        exitCode = -1;
      } else {
        // Make sure we've read all the output.
        stdoutThread.join();
        stderrThread.join();
      }
      return exitCode;
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }

  protected void pipeOutput(InputStream in, StringBuilder builder) {
    try {
      Reader reader = new InputStreamReader(in, "UTF-8");
      char[] buffer = new char[512];
      int count = reader.read(buffer);
      while (count != -1) {
        builder.append(buffer, 0, count);
        count = reader.read(buffer);
      }
    } catch (UnsupportedEncodingException e) {
      //TODO (danrubel): better handle this
      e.printStackTrace();
    } catch (IOException e) {
      // This exception is expected.
    }
  }
}
