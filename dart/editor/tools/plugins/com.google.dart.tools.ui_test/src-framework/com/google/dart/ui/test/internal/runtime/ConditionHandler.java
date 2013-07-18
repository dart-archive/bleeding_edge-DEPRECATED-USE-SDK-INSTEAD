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
package com.google.dart.ui.test.internal.runtime;

import com.google.common.util.concurrent.Uninterruptibles;
import com.google.dart.ui.test.Condition;
import com.google.dart.ui.test.WaitTimedOutException;

import java.util.concurrent.TimeUnit;

/**
 * Condition handling support.
 */
public class ConditionHandler {
  public static ConditionHandler DEFAULT = new ConditionHandler(
      TimeUnit.SECONDS.toMillis(30),
      TimeUnit.MILLISECONDS.toMillis(1));

  private final long timeout;
  private final long delay;

  public ConditionHandler(long timeout, long delay) {
    this.timeout = timeout;
    this.delay = delay;
  }

  public void assertThat(Condition condition) throws WaitTimedOutException {
    waitFor(condition);
  }

  public void assertThat(String message, Condition condition) throws WaitTimedOutException {
    try {
      waitFor(condition);
    } catch (WaitTimedOutException e) {
      throw new WaitTimedOutException(message);
    }
  }

  public void waitFor(Condition condition) throws WaitTimedOutException {
    long start = System.currentTimeMillis();
    while (!condition.test()) {
      if (System.currentTimeMillis() - start > timeout) {
        throw new WaitTimedOutException("Timed out waiting for " + condition);
      }
      Uninterruptibles.sleepUninterruptibly(delay, TimeUnit.MILLISECONDS);
    }
  }
}
