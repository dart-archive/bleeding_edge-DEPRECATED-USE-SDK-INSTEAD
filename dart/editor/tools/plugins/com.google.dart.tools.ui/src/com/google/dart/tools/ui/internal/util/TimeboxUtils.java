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

package com.google.dart.tools.ui.internal.util;

import com.google.common.util.concurrent.Uninterruptibles;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class TimeboxUtils {
  /**
   * Executes given {@link Runnable} with timeout.
   * 
   * @return {@code true} if the {@link Runnable} finished within the timeout.
   */
  public static boolean run(final Runnable runnable, long timeout, TimeUnit unit) {
    final boolean done[] = {false};
    Thread thread = new Thread("TimeboxUtils.run") {
      @Override
      public void run() {
        runnable.run();
        done[0] = true;
      }
    };
    thread.start();
    Uninterruptibles.joinUninterruptibly(thread, timeout, unit);
    return done[0];
  }

  /**
   * Executes the given {@link RunnableObject} with timeout.
   * 
   * @return the result of the {@link RunnableObject} execution or "timeoutValue" if not finished
   *         within the timeout.
   */
  public static <T> T runObject(final RunnableObject<T> runnable, T timeoutValue, long timeout,
      TimeUnit unit) {
    final AtomicReference<T> result = new AtomicReference<T>(timeoutValue);
    Thread thread = new Thread("TimeboxUtils.runObject") {
      @Override
      public void run() {
        result.set(runnable.runObject());
      }
    };
    thread.start();
    Uninterruptibles.joinUninterruptibly(thread, timeout, unit);
    return result.get();
  }
}
