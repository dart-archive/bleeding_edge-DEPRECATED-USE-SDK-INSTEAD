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

import com.google.dart.engine.utilities.io.PrintStringWriter;

import java.io.File;

/**
 * Appends information about analysis to the console
 */
public class AnalysisDebug implements AnalysisListener, IdleListener {

  private final PrintStringWriter message = new PrintStringWriter();
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
      message.print(prefix);
      message.print("discarded ");
      File file = event.getLibraryFile();
      message.println(file != null ? file.getName() : "null");
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
      message.print(prefix);
      message.print("idle ");
      message.println(msg);
      message.notifyAll();
    }
  }

  @Override
  public void parsed(AnalysisEvent event) {
    synchronized (message) {
      File libFile = event.getLibraryFile();
      String libFileName = libFile != null ? libFile.getName() : "null";
      for (File file : event.getFiles()) {
        message.print(prefix);
        message.print("parsed ");
        message.print(libFileName);
        String fileName = file != null ? file.getName() : "null";
        if (!fileName.equals(libFileName)) {
          message.print(" @ ");
          message.print(fileName);
        }
        message.println();
      }
      message.notifyAll();
    }
  }

  @Override
  public void resolved(AnalysisEvent event) {
    synchronized (message) {
      message.print(prefix);
      message.print("resolved ");
      File file = event.getLibraryFile();
      message.println(file != null ? file.getName() : "null");
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
            if (message.getLength() == 0) {
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
