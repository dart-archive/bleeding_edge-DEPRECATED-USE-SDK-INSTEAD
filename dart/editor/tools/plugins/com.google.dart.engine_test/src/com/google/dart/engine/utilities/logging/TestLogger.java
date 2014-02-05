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
package com.google.dart.engine.utilities.logging;

/**
 * Instances of the class {@code TestLogger} implement a logger that can be used by tests.
 */
public class TestLogger implements Logger {
  /**
   * The number of error messages that were logged.
   */
  private long errorCount = 0L;

  /**
   * The number of informational messages that were logged.
   */
  private long infoCount = 0L;

  /**
   * Return the number of error messages that were logged.
   * 
   * @return the number of error messages that were logged
   */
  public long getErrorCount() {
    return errorCount;
  }

  /**
   * Return the number of informational messages that were logged.
   * 
   * @return the number of informational messages that were logged
   */
  public long getInfoCount() {
    return infoCount;
  }

  @Override
  public void logError(String message) {
    errorCount++;
  }

  @Override
  public void logError(String message, Throwable exception) {
    errorCount++;
  }

  @Override
  public void logInformation(String message) {
    infoCount++;
  }

  @Override
  public void logInformation(String message, Throwable exception) {
    infoCount++;
  }
}
