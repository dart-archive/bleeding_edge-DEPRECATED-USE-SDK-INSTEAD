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

package com.google.dart.tools.core.dart2js;

import com.google.common.base.Joiner;
import com.google.dart.tools.core.DartCore;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

/**
 * Execute the process created by the given process builder; collect the results and the exit code.
 * The process runs to completion before the run() method returns.
 * 
 * @coverage dart.tools.core.dart2js
 */
public class ProcessRunner {
  private ProcessBuilder processBuilder;

  private int exitCode;
  private StringBuilder stdout = new StringBuilder();
  private StringBuilder stderr = new StringBuilder();

  private Thread processThread;
  private Process process;

  public ProcessRunner(ProcessBuilder processBuilder) {
    this.processBuilder = processBuilder;
  }

  /**
   * Wait for process termination.
   * 
   * @param millis
   * @throws IOException
   */
  public void await(IProgressMonitor monitor, int maxDelayMillis) throws IOException {
    long exitTime = maxDelayMillis > 0 ? System.currentTimeMillis() + maxDelayMillis : 0;

    try {
      // Run the process; check periodically for user cancellation.
      while (processThread.isAlive()) {
        if (monitor != null && monitor.isCanceled()) {
          return;
        }

        if (exitTime != 0 && System.currentTimeMillis() > exitTime) {
          return;
        }

        processThread.join(100);
      }
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
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
   * Run the process asynchronously, returning immediately.
   * 
   * @param monitor
   * @throws IOException
   */
  public void runAsync() throws IOException {
    exitCode = 0;
    stdout.setLength(0);
    stderr.setLength(0);

    process = processBuilder.start();

    // Read from stdout.
    final Thread stdoutThread = new Thread(new Runnable() {
      @Override
      public void run() {
        pipeStdout(process.getInputStream(), stdout);
      }
    });

    // Read from stderr.
    final Thread stderrThread = new Thread(new Runnable() {
      @Override
      public void run() {
        pipeStderr(process.getErrorStream(), stderr);
      }
    });

    processThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          exitCode = process.waitFor();

          process = null;

          stdoutThread.join();
          stderrThread.join();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    });

    processThread.start();
    stdoutThread.start();
    stderrThread.start();
  }

  /**
   * Execute the process created by the process builder; return the exit value. This call happens
   * synchronously. The monitor parameter is optional; if used, it is polled to see if the user
   * cancelled the operation.
   * 
   * @param monitor
   * @return
   * @throws IOException
   * @throws OperationCanceledException if the user cancelled the operation
   */
  public int runSync(IProgressMonitor monitor) throws IOException {
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

    // Read from stdout.
    Thread stdoutThread = new Thread(new Runnable() {
      @Override
      public void run() {
        pipeStdout(process.getInputStream(), stdout);
      }
    });

    // Read from stderr.
    Thread stderrThread = new Thread(new Runnable() {
      @Override
      public void run() {
        pipeStderr(process.getErrorStream(), stderr);
      }
    });

    processThread.start();
    stdoutThread.start();
    stderrThread.start();

    processStarted(process);

    try {
      // Run the process; check periodically for user cancellation.
      while (processThread.isAlive()) {
        if (monitor != null && monitor.isCanceled()) {
          process.destroy();

          throw new OperationCanceledException();
        }

        processThread.join(100);
      }

      // Make sure we've read all the output.
      stdoutThread.join();
      stderrThread.join();

      return exitCode;
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }

  @Override
  public String toString() {
    return Joiner.on(" ").join(processBuilder.command());
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
      DartCore.logError(e);
    } catch (IOException e) {
      // This exception is expected.

    }
  }

  protected void pipeStderr(InputStream in, StringBuilder builder) {
    pipeOutput(in, builder);
  }

  protected void pipeStdout(InputStream in, StringBuilder builder) {
    pipeOutput(in, builder);
  }

  /**
   * To be (optionally) implemented in subclasses. Getting a handle on the started process can be
   * useful, for example, if you want to pipe it stdin.
   * 
   * @param process the process
   * @throws IOException if an exception is thrown while interacting with the process
   */
  protected void processStarted(Process process) throws IOException {
  }
}
