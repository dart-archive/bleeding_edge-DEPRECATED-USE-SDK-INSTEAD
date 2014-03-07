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

import com.google.common.collect.Lists;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.translation.DartOmit;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Instances of the {@link OperationQueue} represent a queue of operations against the index that
 * are waiting to be performed.
 * 
 * @coverage dart.engine.index
 */
@DartOmit
public class OperationQueue {
  /**
   * The non-query operations that are waiting to be performed.
   */
  private final LinkedList<IndexOperation> nonQueryOperations = Lists.newLinkedList();

  /**
   * The query operations that are waiting to be performed.
   */
  private final LinkedList<IndexOperation> queryOperations = Lists.newLinkedList();

  /**
   * {@code true} if query operations should be returned by {@link #dequeue(long)} or {code false}
   * if not.
   */
  private boolean processQueries = true;

  /**
   * Initialize a newly created operation queue to be empty.
   */
  public OperationQueue() {
    super();
  }

  /**
   * If this queue is not empty, then remove the next operation from the head of this queue and
   * return it. If this queue is empty (see {@link #setProcessQueries(boolean)}, then the behavior
   * of this method depends on the value of the argument. If the argument is less than or equal to
   * zero (<code>0</code>), then {@code null} will be returned immediately. If the argument is
   * greater than zero, then this method will wait until at least one operation has been added to
   * this queue or until the given amount of time has passed. If, at the end of that time, this
   * queue is empty, then {@code null} will be returned. If this queue is not empty, then the first
   * operation will be removed and returned.
   * <p>
   * Note that {@code null} can be returned, even if a positive timeout is given.
   * <p>
   * Note too that this method's timeout is not treated the same way as the timeout value used for
   * {@link Object#wait(long)}. In particular, it is not possible to cause this method to wait for
   * an indefinite period of time.
   * 
   * @param timeout the maximum number of milliseconds to wait for an operation to be available
   *          before giving up and returning {@code null}
   * @return the operation that was removed from the queue
   * @throws InterruptedException if the thread on which this method is running was interrupted
   *           while it was waiting for an operation to be added to the queue
   */
  public IndexOperation dequeue(long timeout) throws InterruptedException {
    synchronized (nonQueryOperations) {
      if (nonQueryOperations.isEmpty() && (!processQueries || queryOperations.isEmpty())) {
        if (timeout <= 0L) {
          return null;
        }
        waitForOperationAvailable(timeout);
      }
      if (!nonQueryOperations.isEmpty()) {
        return nonQueryOperations.removeFirst();
      }
      if (processQueries && !queryOperations.isEmpty()) {
        return queryOperations.removeFirst();
      }
      return null;
    }
  }

  /**
   * Add the given operation to the tail of this queue.
   * 
   * @param operation the operation to be added to the queue
   */
  public void enqueue(IndexOperation operation) {
    synchronized (nonQueryOperations) {
      if (operation instanceof RemoveSourceOperation) {
        Source source = ((RemoveSourceOperation) operation).getSource();
        removeForSource(source, nonQueryOperations);
        removeForSource(source, queryOperations);
      }
      if (operation.isQuery()) {
        queryOperations.add(operation);
      } else {
        nonQueryOperations.add(operation);
      }
      notifyOperationAvailable();
    }
  }

  /**
   * Return a list containing all of the operations that are currently on the queue. Modifying this
   * list will not affect the state of the queue.
   * 
   * @return all of the operations that are currently on the queue
   */
  public List<IndexOperation> getOperations() {
    List<IndexOperation> operations = Lists.newArrayList();
    synchronized (nonQueryOperations) {
      operations.addAll(nonQueryOperations);
      operations.addAll(queryOperations);
    }
    return operations;
  }

  /**
   * Set whether the receiver's {@link #dequeue(long)} method should return query operations.
   * 
   * @param processQueries {@code true} if the receiver's {@link #dequeue(long)} method should
   *          return query operations or {@code false} if query operations should be queued but not
   *          returned by the receiver's {@link #dequeue(long)} method until this method is called
   *          with a value of {@code true}.
   */
  public void setProcessQueries(boolean processQueries) {
    synchronized (nonQueryOperations) {
      if (this.processQueries != processQueries) {
        this.processQueries = processQueries;
        if (processQueries && !queryOperations.isEmpty()) {
          notifyOperationAvailable();
        }
      }
    }
  }

  /**
   * Return the number of operations on the queue.
   * 
   * @return the number of operations on the queue
   */
  public int size() {
    synchronized (nonQueryOperations) {
      return nonQueryOperations.size() + queryOperations.size();
    }
  }

  private void notifyOperationAvailable() {
    nonQueryOperations.notifyAll();
  }

  /**
   * Removes operations that should be removed when given {@link Source} is removed.
   */
  private void removeForSource(Source source, LinkedList<IndexOperation> operations) {
    for (Iterator<IndexOperation> iter = operations.listIterator(); iter.hasNext();) {
      IndexOperation indexOperation = iter.next();
      if (indexOperation.removeWhenSourceRemoved(source)) {
        iter.remove();
      }
    }
  }

  private void waitForOperationAvailable(long timeout) throws InterruptedException {
    nonQueryOperations.wait(timeout);
  }
}
