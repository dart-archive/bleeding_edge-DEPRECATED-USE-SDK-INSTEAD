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
   * Initialize a newly created operation queue to be empty.
   */
  public OperationQueue() {
    super();
  }

  public IndexOperation dequeue() {
    synchronized (operations) {
      if (operations.isEmpty()) {
        return null;
      }
      return operations.remove(0);
    }
  }

  public void enqueue(IndexOperation operation) {
    synchronized (operations) {
      operations.add(operation);
    }
  }
}
