package com.google.dart.tools.core.analysis;

import java.io.File;

/**
 * Appends information about analysis to the console
 */
public class AnalysisDebug implements AnalysisListener {
  private static final String ANALYSIS_PREFIX = "[analysis] ";
  private static final String LINE_SEPARATOR = System.getProperty("line.separator");
  private final StringBuilder message = new StringBuilder(20000);
  private boolean debug;

  public AnalysisDebug() {
    start();
  }

  @Override
  public void discarded(AnalysisEvent event) {
    synchronized (message) {
      message.append(ANALYSIS_PREFIX);
      message.append("discarded ");
      File file = event.getLibraryFile();
      message.append(file != null ? file.getName() : "null");
      message.append(LINE_SEPARATOR);
      message.notifyAll();
    }
  }

  @Override
  public void idle(boolean idle) {
    synchronized (message) {
      message.append(ANALYSIS_PREFIX);
      message.append("idle ");
      message.append(idle);
      message.append(LINE_SEPARATOR);
      message.notifyAll();
    }
  }

  @Override
  public void parsed(AnalysisEvent event) {
    synchronized (message) {
      File libFile = event.getLibraryFile();
      String libFileName = libFile != null ? libFile.getName() : "null";
      for (File file : event.getFiles()) {
        message.append(ANALYSIS_PREFIX);
        message.append("parsed ");
        message.append(libFileName);
        String fileName = file != null ? file.getName() : "null";
        if (!fileName.equals(libFileName)) {
          message.append(" @ ");
          message.append(fileName);
        }
        message.append(LINE_SEPARATOR);
      }
      message.notifyAll();
    }
  }

  @Override
  public void resolved(AnalysisEvent event) {
    synchronized (message) {
      message.append(ANALYSIS_PREFIX);
      message.append("resolved ");
      File file = event.getLibraryFile();
      message.append(file != null ? file.getName() : "null");
      message.append(LINE_SEPARATOR);
      message.notifyAll();
    }
  }

  public void stop() {
    debug = false;
    synchronized (message) {
      message.notifyAll();
    }
  }

  /**
   * Start a background thread that prints analysis information as it becomes available.
   */
  private void start() {
    debug = true;
    new Thread(getClass().getSimpleName()) {
      @Override
      public void run() {
        while (debug) {
          String lines;
          synchronized (message) {
            if (message.length() == 0) {
              try {
                message.wait();
              } catch (InterruptedException e) {
                //$FALL-THROUGH$
              }
            }
            lines = message.toString();
            message.setLength(0);
          }
          if (debug) {
            System.out.print(lines);
          }
        }
      };
    }.start();
  }
}
