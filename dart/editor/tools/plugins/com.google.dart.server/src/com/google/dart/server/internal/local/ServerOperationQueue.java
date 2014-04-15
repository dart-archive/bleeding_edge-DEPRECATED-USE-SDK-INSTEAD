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

package com.google.dart.server.internal.local;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Instances of the {@link ServerOperation} represent a queue of operations in a server.
 * 
 * @coverage dart.server.local
 */
public class ServerOperationQueue {
  /**
   * An array containing the operation queues in priority order.
   */
  private final LinkedList<ServerOperation>[] operationQueues;

  /**
   * The object used to synchronize access to {@link #operationQueues}.
   */
  private final Object operationsLock = new Object();

  /**
   * The last taken {@link ServerOperation} that hasn't been yet completed.
   */
  private ServerOperation lastOperation;

  @SuppressWarnings("unchecked")
  public ServerOperationQueue() {
    int queueCount = ServerOperationPriority.values().length;
    operationQueues = new LinkedList[queueCount];
    for (int i = 0; i < queueCount; i++) {
      operationQueues[i] = Lists.newLinkedList();
    }
  }

  /**
   * Add the given operation to this queue. The exact position in the queue depends on the priority
   * of the given operation relative to the priorities of the other operations in the queue. If
   * there is already an operation with which this one can be merge, it will be merged.
   */
  public void add(ServerOperation operation) {
    synchronized (operationsLock) {
      int queueIndex = operation.getPriority().ordinal();
      LinkedList<ServerOperation> operationQueue = operationQueues[queueIndex];
      // check if can be merged
      if (operation instanceof MergeableOperation) {
        for (ServerOperation existingOperation : operationQueue) {
          if (existingOperation instanceof MergeableOperation) {
            boolean merged = ((MergeableOperation) existingOperation).mergeWith(operation);
            if (merged) {
              return;
            }
          }
        }
      }
      // add to the end
      operationQueue.addLast(operation);
      operationsLock.notify();
    }
  }

  /**
   * Returns {@code true} if there are no queued or incomplete {@link ServerOperation}s.
   */
  public boolean isEmpty() {
    synchronized (operationsLock) {
      if (lastOperation != null) {
        return false;
      }
      for (LinkedList<ServerOperation> operationQueue : operationQueues) {
        if (!operationQueue.isEmpty()) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Marks the last {@link ServerOperation} returned from {@link #take(long)} as completed.
   */
  public void markLastOperationCompleted() {
    synchronized (operationsLock) {
      lastOperation = null;
    }
  }

  /**
   * Removes {@link ContextServerOperation} with the given identifier.
   */
  @VisibleForTesting
  public void removeWithContextId(String contextId) {
    for (LinkedList<ServerOperation> otherQueue : operationQueues) {
      for (Iterator<ServerOperation> iter = otherQueue.iterator(); iter.hasNext();) {
        ServerOperation otherOperation = iter.next();
        if (otherOperation instanceof ContextServerOperation) {
          String otherContextId = ((ContextServerOperation) otherOperation).getContextId();
          if (otherContextId.equals(contextId)) {
            iter.remove();
          }
        }
      }
    }
  }

  /**
   * Returns the next operation to perform or {@code null} if timeout happens.
   * 
   * @param timeout the maximum time to wait in milliseconds, if {@code 0} then wait infinitely
   */
  public ServerOperation take(long timeout) {
    synchronized (operationsLock) {
      while (true) {
        for (LinkedList<ServerOperation> operationQueue : operationQueues) {
          if (!operationQueue.isEmpty()) {
            lastOperation = operationQueue.removeFirst();
            return lastOperation;
          }
        }
        // wait for "queue is updated" notification
        long waitStart = System.currentTimeMillis();
        try {
          operationsLock.wait(timeout);
        } catch (InterruptedException e) {
        } finally {
          if (timeout != 0) {
            timeout -= System.currentTimeMillis() - waitStart;
            if (timeout <= 0) {
              return null;
            }
          }
        }
      }
    }
  }
}
