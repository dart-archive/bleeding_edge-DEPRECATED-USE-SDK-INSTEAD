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
package com.google.dart.engine.internal.index.operation;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.translation.DartOmit;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Instances of the {@link OperationProcessor} process the operations on a single
 * {@link OperationQueue operation queue}. Each processor can be run one time on a single thread.
 * 
 * @coverage dart.engine.index
 */
@DartOmit
public class OperationProcessor {
  /**
   * The enumeration <code>ProcessorState</code> represents the possible states of an operation
   * processor.
   */
  private enum ProcessorState {
    /**
     * The processor is ready to be run (has not been run before).
     */
    READY,

    /**
     * The processor is currently performing operations.
     */
    RUNNING,

    /**
     * The processor is currently performing operations but has been asked to stop.
     */
    STOP_REQESTED,

    /**
     * The processor has stopped performing operations and cannot be used again.
     */
    STOPPED;
  }

  /**
   * The queue containing the operations to be processed.
   */
  private OperationQueue queue;

  /**
   * The current state of the processor.
   */
  private ProcessorState state = ProcessorState.READY;

  /**
   * The number of milliseconds for which the thread on which the processor is running will wait for
   * an operation to become available if there are no operations ready to be processed.
   */
  private static long WAIT_DURATION = 100L;

  /**
   * Initialize a newly created operation processor to process the operations on the given queue.
   * 
   * @param queue the queue containing the operations to be processed
   */
  public OperationProcessor(OperationQueue queue) {
    this.queue = queue;
  }

  /**
   * Start processing operations. If the processor is already running on a different thread, then
   * this method will return immediately with no effect. Otherwise, this method will not return
   * until after the processor has been stopped from a different thread or until the thread running
   * the processor has been interrupted.
   */
  public void run() {
    synchronized (this) {
      // This processor is, or was, already running on a different thread.
      if (state != ProcessorState.READY) {
        throw new IllegalStateException("Operation processors can only be run one time: " + state); //$NON-NLS-1$
      }
      // OK, run.
      state = ProcessorState.RUNNING;
    }
    try {
      while (isRunning()) {
        // wait for operation
        IndexOperation operation = null;
        try {
          operation = queue.dequeue(WAIT_DURATION);
        } catch (InterruptedException exception) {
          // ignore
        }
        // perform operation
        if (operation != null) {
          try {
            operation.performOperation();
          } catch (Throwable exception) {
            AnalysisEngine.getInstance().getLogger().logError(
                "Exception in indexing operation: " + operation, exception); //$NON-NLS-1$
          }
        }
      }
    } finally {
      synchronized (this) {
        state = ProcessorState.STOPPED;
      }
    }
  }

  /**
   * Stop processing operations after the current operation has completed. If the argument is
   * {@code true} then this method will wait until the last operation has completed; otherwise this
   * method might return before the last operation has completed.
   * 
   * @param wait {@code true} if this method will wait until the last operation has completed before
   *          returning
   * @return the library files for the libraries that need to be analyzed when a new session is
   *         started.
   */
  public Source[] stop(boolean wait) {
    synchronized (this) {
      if (state == ProcessorState.READY) {
        state = ProcessorState.STOPPED;
        return getUnanalyzedSources();
      } else if (state == ProcessorState.STOPPED) {
        return getUnanalyzedSources();
      } else if (state == ProcessorState.RUNNING) {
        state = ProcessorState.STOP_REQESTED;
      }
    }
    while (wait) {
      synchronized (this) {
        if (state == ProcessorState.STOPPED) {
          return getUnanalyzedSources();
        }
      }
      waitOneMs();
    }
    return getUnanalyzedSources();
  }

  /**
   * Waits until processors will switch from "ready" to "running" state.
   * 
   * @return {@code true} if processor is now actually in "running" state, e.g. not in "stopped"
   *         state.
   */
  public boolean waitForRunning() {
    while (state == ProcessorState.READY) {
      threadYield();
    }
    return state == ProcessorState.RUNNING;
  }

  /**
   * @return the {@link Source}s that are not indexed yet.
   */
  private Source[] getUnanalyzedSources() {
    Set<Source> sources = Sets.newHashSet();
    for (IndexOperation operation : queue.getOperations()) {
      if (operation instanceof IndexUnitOperation) {
        Source source = ((IndexUnitOperation) operation).getSource();
        sources.add(source);
      }
    }
    return sources.toArray(new Source[sources.size()]);
  }

  /**
   * Return {@code true} if the current state is {@link ProcessorState#RUNNING}.
   * 
   * @return {@code true} if this processor is running
   */
  private boolean isRunning() {
    synchronized (this) {
      return state == ProcessorState.RUNNING;
    }
  }

  private void threadYield() {
    Thread.yield();
  }

  private void waitOneMs() {
    Uninterruptibles.sleepUninterruptibly(1, TimeUnit.MILLISECONDS);
  }
}
