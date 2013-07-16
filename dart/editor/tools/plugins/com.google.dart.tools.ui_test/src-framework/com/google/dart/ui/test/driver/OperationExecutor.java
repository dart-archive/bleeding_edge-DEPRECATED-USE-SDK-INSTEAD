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

package com.google.dart.ui.test.driver;

import com.google.common.collect.Lists;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.ui.test.util.UiContext;

import org.eclipse.swt.widgets.Display;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Executes a sequence of {@link Operation}s.
 */
public class OperationExecutor {
  private final Display display = Display.getDefault();
  private final UiContext context = new UiContext();
  private final LinkedList<Operation> operations = Lists.newLinkedList();
  private final AtomicBoolean operationsDone = new AtomicBoolean();
  private Throwable exception = null;

  /**
   * Schedules the {@link Operation} for execution.
   */
  public void addOperation(Operation operation) {
    operations.add(operation);
  }

  /**
   * Runs the scheduled {@link Operation}s, waits for the given time at most.
   */
  public void runUiOperations(long waitFor, TimeUnit unit) throws Exception {
    display.timerExec(5, new Runnable() {
      private LinkedList<Operation> finishOperations = Lists.newLinkedList();

      @Override
      public void run() {
        // are we done?
        if (operationsDone.get()) {
          return;
        }
        // schedule again
        display.timerExec(5, this);
        // run single operation
        try {
          // wait for current operation done
          if (!finishOperations.isEmpty()) {
            Operation operation = finishOperations.getFirst();
            if (operation.isDone(context)) {
              finishOperations.removeFirst();
              operation.done(context);
              maybeDone();
            }
            return;
          }
          // prepare new operation
          if (operations.isEmpty()) {
            return;
          }
          Operation operation = operations.getFirst();
          // wait for new operation ready
          if (operation.isReady(context)) {
            operations.removeFirst();
            try {
              operation.run(context);
              // done operation
              if (operation.isDone(context)) {
                operation.done(context);
              } else {
                finishOperations.addLast(operation);
              }
              // may be done execution
              maybeDone();
            } catch (Throwable e) {
              operation.onError(context);
              ExecutionUtils.propagate(e);
            }
          }
        } catch (Throwable e) {
          if (exception == null) {
            exception = e;
          }
          // we are done - with failure
          operationsDone.set(true);
        }
      }

      private void maybeDone() {
        if (operations.isEmpty() && finishOperations.isEmpty()) {
          operationsDone.set(true);
        }
      }
    });
    // wait for successful completion or failure
    {
      long end = System.nanoTime() + unit.toNanos(waitFor);
      while (!operationsDone.get()) {
        if (System.nanoTime() >= end) {
          throw new TimeoutException();
        }
        UiContext.runEventLoop(10);
      }
    }
    // check for exception
    if (exception != null) {
      ExecutionUtils.propagate(exception);
    }
    // OK
  }
}
