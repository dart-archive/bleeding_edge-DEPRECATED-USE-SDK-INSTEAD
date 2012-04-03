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

import java.util.ArrayList;

/**
 * Instances of the class <code>OperationQueue</code> represent a queue of operations against the
 * index that are waiting to be performed.
 */
public class OperationQueue {
  /**
   * The operations that are waiting to be performed.
   */
  private ArrayList<IndexOperation> operations = new ArrayList<IndexOperation>();

  /**
   * An array containing a single boolean indicating whether the receiver has an idle processor
   * waiting for new operations to be queued. Synchronize against this array before accessing it. If
   * you wait on this array to be notified then do not synchronize against {@link #operations}.
   */
  private boolean[] isIdle = new boolean[1];

  /**
   * Initialize a newly created operation queue to be empty.
   */
  public OperationQueue() {
    super();
  }

  /**
   * If this queue is not empty, then remove the next operation from the head of this queue and
   * return it. If this queue is empty, then the behavior of this method depends on the value of the
   * argument. If the argument is less than or equal to zero (<code>0</code>), then
   * <code>null</code> will be returned immediately. If the argument is greater than zero, then this
   * method will wait until at least one operation has been added to this queue or until the given
   * amount of time has passed. If, at the end of that time, this queue is empty, then
   * <code>null</code> will be returned. If this queue is not empty, then the first operation will
   * be removed and returned.
   * <p>
   * Note that <code>null</code> can be returned, even if a positive timeout is given.
   * <p>
   * Note too that this method's timeout is not treated the same way as the timeout value used for
   * {@link Object#wait(long)}. In particular, it is not possible to cause this method to wait for
   * an indefinite period of time.
   * 
   * @param timeout the maximum number of milliseconds to wait for an operation to be available
   *          before giving up and returning <code>null</code>
   * @return the operation that was removed from the queue
   * @throws InterruptedException if the thread on which this method is running was interrupted
   *           while it was waiting for an operation to be added to the queue
   */
  public IndexOperation dequeue(long timeout) throws InterruptedException {
    synchronized (operations) {
      if (operations.isEmpty()) {
        // Notify any objects waiting for the receiver to be idle
        synchronized (isIdle) {
          isIdle[0] = true;
          isIdle.notifyAll();
        }
        if (timeout <= 0L) {
          return null;
        }
        try {
          operations.wait(timeout);
        } finally {
          synchronized (isIdle) {
            isIdle[0] = false;
          }
        }
        if (operations.isEmpty()) {
          return null;
        }
      }
      return operations.remove(0);
    }
  }

  /**
   * Add the given operation to the tail of this queue.
   * 
   * @param operation the operation to be added to the queue
   */
  public void enqueue(IndexOperation operation) {
    synchronized (operations) {
      operations.add(operation);
      operations.notifyAll();
    }
  }

  /**
   * Wait up to the specified number of milliseconds for the receiver to have an idle processor
   * waiting for new operations to be queued. If the number of milliseconds specified is less than
   * or equal to zero, then this method returns immediately.
   * 
   * @param milliseconds the maximum number of milliseconds to wait for idle.
   * @return <code>true</code> if the receiver is idle or <code>false</code> if the specified number
   *         of milliseconds has passed and the receiver is still not idle.
   */
  public boolean waitForIdle(int milliseconds) {
    long end = System.currentTimeMillis() + milliseconds;
    synchronized (isIdle) {
      while (!isIdle[0]) {
        long delta = end - System.currentTimeMillis();
        if (delta <= 0) {
          return false;
        }
        try {
          isIdle.wait(delta);
        } catch (InterruptedException e) {
          //$FALL-THROUGH$
        }
      }
    }
    return true;
  }
}
