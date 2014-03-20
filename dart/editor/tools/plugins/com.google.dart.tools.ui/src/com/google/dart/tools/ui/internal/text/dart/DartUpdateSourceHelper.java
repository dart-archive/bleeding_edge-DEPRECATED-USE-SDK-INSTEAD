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

package com.google.dart.tools.ui.internal.text.dart;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.analysis.model.ContextManager;
import com.google.dart.tools.core.internal.builder.AnalysisManager;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Helper for updating in-memory content of {@link Source}s in an {@link AnalysisContext}.
 */
public class DartUpdateSourceHelper {

  private class DelayThread extends Thread {
    public DelayThread() {
      setName("DartUpdateSourceHelper-DelayThread");
      setDaemon(true);
    }

    @Override
    public void run() {
      while (true) {
        // prepare worker
        DelayWorker worker;
        try {
          worker = delayDeque.take();
          // if this worker is not ready yet, return it into the queue
          if (!worker.isReady()) {
            delayDeque.putFirst(worker);
            Uninterruptibles.sleepUninterruptibly(worker.getDelay(), TimeUnit.MILLISECONDS);
            continue;
          }
          // continue execution
        } catch (InterruptedException e) {
          continue;
        }
        // execute worker
        worker.perform();
        delayDequeEmpty = delayDeque.isEmpty();
      }
    }
  }

  private class DelayWorker {
    private final ContextManager manager;
    private final AnalysisContext context;
    private final Source source;
    private String content;
    private long contentTime;

    public DelayWorker(ContextManager manager, AnalysisContext context, Source source,
        String content) {
      this.manager = manager;
      this.context = context;
      this.source = source;
      setContent(content);
    }

    public void setContent(String content) {
      this.content = content;
      this.contentTime = System.currentTimeMillis();
    }

    /**
     * Return how many more milliseconds should be waited until this worker should be executed.
     */
    long getDelay() {
      long ageOfContent = System.currentTimeMillis() - contentTime;
      return DELAY_TIME - ageOfContent;
    }

    /**
     * Return {@code true} if enough time passed since last update, so that this worker should be
     * executed.
     */
    boolean isReady() {
      return getDelay() <= 0;
    }

    void perform() {
      context.setContents(source, content);
      AnalysisManager.getInstance().performAnalysisInBackground(manager, context);
    }
  }

  private class FastThread extends Thread {
    public FastThread() {
      setName("DartUpdateSourceHelper-FastThread");
      setDaemon(true);
    }

    @Override
    public void run() {
      while (true) {
        try {
          FastWorker worker = fastQueue.take();
          worker.perform();
        } catch (InterruptedException e) {
        }
        fastQueueEmpty = fastQueue.isEmpty();
      }
    }
  }

  private class FastWorker {
    private final AnalysisManager analysisManager;
    private final ContextManager manager;
    private final AnalysisContext context;
    private final Source source;
    private final String content;
    private final int offset;
    private final int oldLength;
    private final int newLength;

    public FastWorker(AnalysisManager analysisManager, ContextManager manager,
        AnalysisContext context, Source source, String content, int offset, int oldLength,
        int newLength) {
      this.analysisManager = analysisManager;
      this.manager = manager;
      this.context = context;
      this.source = source;
      this.content = content;
      this.offset = offset;
      this.oldLength = oldLength;
      this.newLength = newLength;
    }

    void perform() {
      if (offset != -1) {
        context.setChangedContents(source, content, offset, oldLength, newLength);
      } else {
        context.setContents(source, content);
      }
      analysisManager.performAnalysisInBackground(manager, context);
    }
  }

  private static long DELAY_TIME = 250;
  private static DartUpdateSourceHelper INSTANCE = new DartUpdateSourceHelper();

  /**
   * Returns the unique instance of the {@link DartUpdateSourceHelper}.
   */
  public static DartUpdateSourceHelper getInstance() {
    return INSTANCE;
  }

  private final BlockingQueue<FastWorker> fastQueue = new LinkedBlockingQueue<FastWorker>();
  private final BlockingDeque<DelayWorker> delayDeque = new LinkedBlockingDeque<DelayWorker>();
  private boolean fastQueueEmpty = true;
  private boolean delayDequeEmpty = true;

  private DartUpdateSourceHelper() {
    new FastThread().start();
    new DelayThread().start();
  }

  /**
   * Schedules update of the given {@link Source} in background and subsequent analysis.
   * <p>
   * Operation is executed ASAP, without any additional delay (but still it background).
   * 
   * @param manager the manager containing the context to be analyzed (not {@code null})
   * @param context the context to be analyzed (not {@code null})
   * @param source the source whose contents are being overridden
   * @param content the text to replace the range in the current contents
   */
  public void updateFast(AnalysisManager analysisManager, ContextManager manager,
      AnalysisContext context, Source source, String content) {
    updateFast(analysisManager, manager, context, source, content, -1, 0, 0);
  }

  /**
   * Schedules update of the given {@link Source} in background and subsequent analysis.
   * <p>
   * Operation is executed ASAP, without any additional delay (but still it background).
   * 
   * @param manager the manager containing the context to be analyzed (not {@code null})
   * @param context the context to be analyzed (not {@code null})
   * @param source the source whose contents are being overridden
   * @param content the text to replace the range in the current contents
   * @param offset the offset into the current contents, if {@code -1} then the whole source content
   *          is changes
   * @param oldLength the number of characters in the original contents that were replaced
   * @param newLength the number of characters in the replacement text
   */
  public void updateFast(AnalysisManager analysisManager, ContextManager manager,
      AnalysisContext context, Source source, String content, int offset, int oldLength,
      int newLength) {
    try {
      fastQueueEmpty = false;
      fastQueue.add(new FastWorker(
          analysisManager,
          manager,
          context,
          source,
          content,
          offset,
          oldLength,
          newLength));
    } catch (IllegalStateException e) {
      // Should never happen, "fastQueue" has a very high capacity.
    }
  }

  /**
   * Schedules update of the given {@link Source} in background and subsequent analysis.
   * <p>
   * Operation execution is delayed for some reasonable time.
   */
  public void updateWithDelay(ContextManager manager, AnalysisContext context, Source source,
      String content) {
    // if the same Source, update worker
    {
      DelayWorker lastWorker = delayDeque.peekLast();
      if (lastWorker != null && lastWorker.manager == manager && lastWorker.context == context
          && Objects.equal(lastWorker.source, source)) {
        lastWorker.setContent(content);
        return;
      }
    }
    // new Source - new worker
    delayDequeEmpty = false;
    delayDeque.add(new DelayWorker(manager, context, source, content));
  }

  /**
   * Waits for execution queue is empty.
   */
  @VisibleForTesting
  public void waitForEmptyQueue() {
    while (!fastQueueEmpty || !delayDequeEmpty) {
      Uninterruptibles.sleepUninterruptibly(5, TimeUnit.MILLISECONDS);
    }
  }
}
