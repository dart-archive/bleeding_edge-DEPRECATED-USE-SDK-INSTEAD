/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.indexer.utilities.logging;

import org.eclipse.core.runtime.Platform;

/**
 * Provides support for code using the runtime debug tracing facility.
 */
public class TraceUtils {

  /**
   * Setting DEV_MODE to true sends all tracing directly to System.out.
   */
  private static final boolean DEV_MODE = false;

  /**
   * Return <code>true</code> if debugging is enabled.
   * 
   * @return <code>true</code> if debugging is enabled
   */
  public static boolean isTracing(String optionName) {
    if (optionName == null) {
      return true;
    }
    return "true".equalsIgnoreCase(Platform.getDebugOption(optionName));
  }

  /**
   * If trace messages associated with the given trace option have been enabled, log the given
   * message to the debugging log file. The stack trace of the given exception will be included in
   * the log. Otherwise, if the detail object is non-<code>null</code>, its print string will be
   * included in the log.
   * 
   * @param log the log
   * @param optionName the name of the trace option used to determine whether the trace message
   *          should be written. Typically the optionName takes the form "plug-in-id/trace-option"
   * @param exception the exception to be included in the log file
   * @param message the trace message to be written
   */
  static void traceError(Logger log, String optionName, Throwable exception, String message) {
    LogUtils.logError(log, exception, message);
  }

  /**
   * If trace messages associated with the given trace option have been enabled, log the given
   * message to the debugging log file. If the given detail object is an exception, the stack trace
   * will be included in the log. Otherwise, if the detail object is non-<code>null</code>, its
   * print string will be included in the log.
   * 
   * @param log the log
   * @param optionName the name of the trace option used to determine whether the trace message
   *          should be written. Typically the optionName takes the form "plug-in-id/trace-option"
   * @param message the trace message to be written
   */
  static void traceInfo(Logger log, String optionName, String message) {
    if (!isTracing(optionName)) {
      return;
    }
    if (DEV_MODE) {
      System.out.println(message);
    } else {
      LogUtils.logInfo(log, message);
    }
  }
}
