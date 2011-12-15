package com.google.dart.tools.core.frog;

import com.google.dart.tools.core.DartCore;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Start and manage the frog compiler process.
 */
public class FrogProcess {
  private static final String FROG_STARTUP_TOKEN = "frog: accepting connections";

  /** @return the path to the the frog server script, relative to the frog source directory */
  private static final String FROG_SERVER_PATH = "server/frog_server.dart";

  private int port;

  private Process process;

  private boolean frogRunning;

  public FrogProcess() {
    this(-1);
  }

  public FrogProcess(int port) {
    this.port = port;

    if (this.port == -1) {
      this.port = FrogManager.DEFAULT_PORT;
    }
  }

  public int getPort() {
    return port;
  }

  public boolean isFrogRunning() {
    return frogRunning;
  }

  public void startProcess() throws IOException {
    ProcessBuilder builder = new ProcessBuilder();

    builder.command(FrogManager.getDartVmExecutablePath(), "--new_gen_heap_size=128",
        FROG_SERVER_PATH, FrogManager.LOCALHOST_ADDRESS, Integer.toString(getPort()));
    builder.directory(new File(FrogManager.getSdkDirectory(), "lib/frog"));
    builder.redirectErrorStream(true);

    process = builder.start();

    final CountDownLatch latch = new CountDownLatch(1);

    // We need to consume the text the VM writes to its stdout / stderr. For now we log it, in the
    // future we'll just silently ignore it.
    Thread readerThread = new Thread(new Runnable() {
      @Override
      public void run() {
        InputStream in = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        try {
          String line = reader.readLine();

          while (line != null) {
            if (line.contains(FROG_STARTUP_TOKEN)) {
              frogRunning = true;
              latch.countDown();
            }
            DartCore.logInformation("frog: [" + line.trim() + "]");

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
      latch.await(5000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException exception) {

    }

    if (!frogRunning) {
      throw new IOException("unable to start frog server");
    }
  }

  public void stopProcess() {
    if (process != null) {
      if (!hasStopped()) {
        process.destroy();
      }

      process = null;
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

}
