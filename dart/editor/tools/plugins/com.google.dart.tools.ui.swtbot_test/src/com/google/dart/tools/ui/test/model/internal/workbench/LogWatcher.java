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
package com.google.dart.tools.ui.test.model.internal.workbench;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A utility class that provides access to events recorded to the Eclipse log.
 */
public class LogWatcher {

  //TODO(pquitslund): move this behind a nicer model facade/entry-point
  //TODO(pquitslund): consider merging w/ DartCoreTestLog

  /**
   * A listener that caches logged errors.
   */
  private final class LogListener implements ILogListener {

    private final List<Throwable> loggedExceptions = new ArrayList<Throwable>();

    @Override
    public void logging(IStatus status, String plugin) {
      Throwable t = status.getException();
      if (t != null) {
        loggedExceptions.add(t);
      }
    }

    private Throwable[] getLoggedExceptions() {
      return loggedExceptions.toArray(new Throwable[] {});
    }

  }

  /** A listener for tracking logged exceptions */
  private final LogListener logListener = new LogListener();

  /**
   * Asserts that there are no exceptions logged to the platform log. Note that it is only legal to
   * invoke this method when the Platform is running (e.g., in workbench tests).
   * 
   * @throws AssertionFailedError if there are logged exceptions
   * @throws IllegalStateException if invoked when the Platform is not running
   */
  public void assertNoLoggedExceptions() {
    Throwable[] exceptions = getLoggedExceptions();
    if (exceptions.length > 0) {
      Assert.fail("Expected no logged exceptions but got: "
          + Arrays.toString(getLoggedExceptions()));
    }
  }

  /**
   * Get all exceptions logged to the Platform logging service (up to this point) in the execution
   * of this test. Note that it is only legal to invoke this method when the Platform is running
   * (e.g., in workbench tests).
   * 
   * @return an array of the logged exceptions
   * @throws IllegalStateException if invoked when the Platform is not running
   */
  public Throwable[] getLoggedExceptions() {
    assertPlatformRunning();
    if (logListener == null) {
      throw new IllegalStateException("Must call init before requesting logged exceptions");
    }
    return logListener.getLoggedExceptions();
  }

  /**
   * Start watching the log..
   */
  public void start() {
    assertPlatformRunning();
    //to ensure no duplicates we remove then add
    Platform.removeLogListener(logListener);
    Platform.addLogListener(logListener);
  }

  /**
   * Stop listening to the log.
   */
  public void stop() {
    if (Platform.isRunning()) {
      Platform.removeLogListener(logListener);
    }
  }

  private void assertPlatformRunning() {
    if (!Platform.isRunning()) {
      throw new IllegalStateException(
          "Logged exceptions can only be requested when the Platform is running");
    }
  }

}
