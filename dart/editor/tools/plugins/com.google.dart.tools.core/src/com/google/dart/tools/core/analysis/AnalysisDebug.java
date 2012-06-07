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
package com.google.dart.tools.core.analysis;

import java.io.File;

/**
 * Appends information about analysis to the console
 */
public class AnalysisDebug implements AnalysisListener, IdleListener {
  private static final String LINE_SEPARATOR = System.getProperty("line.separator");

  private final StringBuilder message = new StringBuilder(20000);
  private final String prefix;
  private boolean debug;
  private long lastIdleTime;

  public AnalysisDebug(String contextName) {
    this.prefix = "[analysis " + contextName + "] ";
    start();
  }

  @Override
  public void discarded(AnalysisEvent event) {
    synchronized (message) {
      message.append(prefix);
      message.append("discarded ");
      File file = event.getLibraryFile();
      message.append(file != null ? file.getName() : "null");
      message.append(LINE_SEPARATOR);
      message.notifyAll();
    }
  }

  @Override
  public void idle(boolean idle) {
    String msg;
    if (idle) {
      long elapseTime = System.currentTimeMillis() - lastIdleTime;
      msg = "true " + elapseTime + " ms ---------------------";
    } else {
      lastIdleTime = System.currentTimeMillis();
      msg = "false";
    }
    synchronized (message) {
      message.append(prefix);
      message.append("idle ");
      message.append(msg);
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
        message.append(prefix);
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
      message.append(prefix);
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
    lastIdleTime = System.currentTimeMillis();
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
