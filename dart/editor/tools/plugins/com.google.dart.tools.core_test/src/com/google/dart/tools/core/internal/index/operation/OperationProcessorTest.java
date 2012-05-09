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

import com.google.dart.tools.core.index.Resource;
import com.google.dart.tools.core.test.util.TestUtilities;

import junit.framework.TestCase;

public class OperationProcessorTest extends TestCase {
  public void test_OperationProcessor_create() {
    assertNotNull(new OperationProcessor(new OperationQueue()));
  }

  public void test_OperationProcessor_run() {
    final boolean[] wasRun = {false};
    OperationQueue queue = new OperationQueue();
    queue.enqueue(new IndexOperation() {
      @Override
      public void performOperation() {
        wasRun[0] = true;
      }

      @Override
      public boolean removeWhenResourceRemoved(Resource resource) {
        return false;
      }
    });
    final OperationProcessor processor = new OperationProcessor(queue);
    TestUtilities.wait(500, new TestUtilities.ThreadController() {
      @Override
      public void startThread() {
        processor.run();
      }

      @Override
      public boolean threadCompleted() {
        return wasRun[0];
      }
    });
    assertTrue("The operation was not run", wasRun[0]);
  }
}
