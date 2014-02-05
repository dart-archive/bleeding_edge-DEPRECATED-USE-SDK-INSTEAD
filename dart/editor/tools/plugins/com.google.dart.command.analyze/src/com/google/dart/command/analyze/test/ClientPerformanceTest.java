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
package com.google.dart.command.analyze.test;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.general.TimeCounter.TimeCounterHandle;

/**
 * Simulates a client running on a separate thread requesting information via
 * {@link AnalysisContext#getKindOf(com.google.dart.engine.source.Source)}.
 */
public class ClientPerformanceTest {

  private final PerformanceMonitor monitor;
  private AnalysisContext context;
  private Source source;
  private Thread thread;
  private boolean running;

  public ClientPerformanceTest(PerformanceMonitor monitor) {
    this.monitor = monitor;
  }

  /**
   * Start another thread that periodically calls
   * {@link AnalysisContext#getKindOf(com.google.dart.engine.source.Source)}
   * 
   * @param context the context to be called, not {@code null}.
   * @param source the source, not {@code null}
   */
  public void start(AnalysisContext context, Source source) {
    if (thread != null) {
      throw new IllegalStateException();
    }
    this.context = context;
    this.source = source;
    thread = new Thread(new Runnable() {
      @Override
      public void run() {
        while (running) {
          callContext();
          try {
            Thread.sleep(5);
          } catch (InterruptedException e) {
            //$FALL-THROUGH$
          }
        }
      }
    });
    running = true;
    thread.start();
  }

  /**
   * Stop the thread making calls to the analysis context
   */
  public void stop() {
    running = false;
  }

  private void callContext() {
    TimeCounterHandle timer = monitor.start();
    context.getKindOf(source);
    timer.stop();
  }
}
