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

/**
 * Instances of the class <code>OperationProcessor</code> process the operations on a single
 * {@link OperationQueue operation queue}.
 */
public class OperationProcessor {
  /**
   * The queue containing the operations to be processed.
   */
  private OperationQueue queue;

  /**
   * A flag indicating whether the processor should continue to process operations.
   */
  private boolean running = false;

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
   * until after the processor has been stopped from a different thread.
   */
  public void run() {
    synchronized (this) {
      if (running) {
        // This processor is already running on a different thread.
        return;
      }
      running = true;
    }
    while (running) {
      IndexOperation operation = queue.dequeue();
      if (operation == null) {
        try {
          Thread.sleep(500);
        } catch (InterruptedException exception) {
          running = false;
        }
      } else {
        operation.performOperation();
      }
    }
  }

  /**
   * Stop processing operations after the current operation has completed. This method can return
   * before the last operation has completed.
   */
  public void stop() {
    running = false;
  }
}
