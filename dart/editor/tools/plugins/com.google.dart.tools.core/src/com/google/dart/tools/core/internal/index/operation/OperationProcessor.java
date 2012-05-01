/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.core.internal.index.operation;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;

import java.text.DateFormat;
import java.util.GregorianCalendar;

/**
 * Instances of the class <code>OperationProcessor</code> process the operations on a single
 * {@link OperationQueue operation queue}. Each processor can be run one time on a single thread.
 */
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
      if (state != ProcessorState.READY) {
        // This processor is, or was, already running on a different thread.
        throw new IllegalStateException("Operation processors can only be run one time"); //$NON-NLS-1$
      }
      state = ProcessorState.RUNNING;
    }
    if (DartCoreDebug.TRACE_INDEX_PROCESSOR) {
      DartCore.logInformation("Started operation processor at " + DateFormat.getDateTimeInstance().format(new GregorianCalendar().getTime())); //$NON-NLS-1$
    }
    try {
      while (isRunning()) {
        IndexOperation operation = null;
        try {
          operation = queue.dequeue(WAIT_DURATION);
        } catch (InterruptedException exception) {
          synchronized (this) {
            if (state == ProcessorState.RUNNING) {
              state = ProcessorState.STOP_REQESTED;
            }
          }
        }
        if (operation != null) {
          if (DartCoreDebug.TRACE_INDEX_PROCESSOR) {
            DartCore.logInformation("Operation Processor: beginning " + operation); //$NON-NLS-1$
          }
          try {
            operation.performOperation();
          } catch (Throwable exception) {
            DartCore.logError("Exception in indexing operation: " + operation, exception); //$NON-NLS-1$
          }
          if (DartCoreDebug.TRACE_INDEX_PROCESSOR) {
            DartCore.logInformation("Operation Processor: completed " + operation); //$NON-NLS-1$
          }
        }
      }
    } finally {
      synchronized (this) {
        state = ProcessorState.STOPPED;
      }
      if (DartCoreDebug.TRACE_INDEX_PROCESSOR) {
        DartCore.logInformation("Stopped operation processor at " + DateFormat.getDateTimeInstance().format(new GregorianCalendar().getTime())); //$NON-NLS-1$
      }
    }
  }

  /**
   * Stop processing operations after the current operation has completed. If the argument is
   * <code>true</code> then this method will wait until the last operation has completed; otherwise
   * this method might return before the last operation has completed.
   * 
   * @param wait <code>true</code> if this method will wait until the last operation has completed
   *          before returning
   */
  public void stop(boolean wait) {
    synchronized (this) {
      if (state == ProcessorState.READY) {
        state = ProcessorState.STOPPED;
        return;
      } else if (state == ProcessorState.STOPPED) {
        return;
      } else if (state == ProcessorState.RUNNING) {
        state = ProcessorState.STOP_REQESTED;
      }
    }
    while (wait) {
      synchronized (this) {
        if (state == ProcessorState.STOPPED) {
          return;
        }
      }
      try {
        Thread.sleep(10);
      } catch (InterruptedException exception) {
        // Ignored
      }
    }
  }

  /**
   * Return <code>true</code> if the current state is {@link ProcessorState#RUNNING}.
   * 
   * @return <code>true</code> if this processor is running
   */
  private boolean isRunning() {
    synchronized (this) {
      return state == ProcessorState.RUNNING;
    }
  }
}
