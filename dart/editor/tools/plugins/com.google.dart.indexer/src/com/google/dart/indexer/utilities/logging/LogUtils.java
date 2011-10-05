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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;

import java.text.MessageFormat;

/**
 * Logging utilities.
 */
public class LogUtils {
  static final Object[] EMPTY = new Object[0];

  /**
   * Create a status object representing the specified information.
   * 
   * @param severity the severity; one of the following: <code>IStatus.OK</code> ,
   *          <code>IStatus.ERROR</code>, <code>IStatus.INFO</code>, or <code>IStatus.WARNING</code>
   *          .
   * @param code the plug-in-specific status code, or <code>OK</code>.
   * @param message a human-readable message, localized to the current locale.
   * @param exception a low-level exception, or <code>null</code> if not applicable.
   * @return the status object (not <code>null</code>).
   */
  public static IStatus createStatus(String bundleSymbolicName, int severity, int code,
      String message, Throwable exception) {
    return new Status(severity, bundleSymbolicName, code, message, exception);
  }

  /**
   * Log the specified information.
   * 
   * @param severity the severity; one of the following: <code>IStatus.OK</code> ,
   *          <code>IStatus.ERROR</code>, <code>IStatus.INFO</code>, or <code>IStatus.WARNING</code>
   *          .
   * @param code the plug-in-specific status code, or <code>OK</code>.
   * @param message a human-readable message, localized to the current locale.
   * @param exception a low-level exception, or <code>null</code> if not applicable.
   */
  static void log(Logger log, int severity, int code, String message, Throwable exception) {
    log(log, createStatus(getBundleSymbolicName(log), severity, code, message, exception));
  }

  /**
   * Log the given status.
   * 
   * @param status the status to log.
   */
  static void log(Logger log, IStatus status) {
    log.getLog().log(status);
  }

  /**
   * Log the specified error.
   * 
   * @param message a human-readable message, localized to the current locale.
   */
  static void logError(Logger log, String message) {
    logError(log, null, message);
  }

  /**
   * Log the specified error.
   * 
   * @param message a human-readable message, localized to the current locale.
   * @param args message arguments.
   */
  static void logError(Logger log, String message, Object... args) {
    logError(log, null, message, args);
  }

  /**
   * Log the specified error.
   * 
   * @param exception a low-level exception.
   */
  static void logError(Logger log, Throwable exception) {
    logError(log, exception, "Unexpected Exception");
  }

  /**
   * Log the specified error.
   * 
   * @param exception a low-level exception, or <code>null</code> if not applicable.
   * @param message a human-readable message, localized to the current locale.
   */
  static void logError(Logger log, Throwable exception, String message) {
    log(log, IStatus.ERROR, IStatus.OK, message, exception);
  }

  /**
   * Log the specified error.
   * 
   * @param exception a low-level exception, or <code>null</code> if not applicable.
   * @param message a human-readable message, localized to the current locale.
   * @param args message arguments.
   */
  static void logError(Logger log, Throwable exception, String message, Object... args) {
    message = MessageFormat.format(message, args);
    log(log, IStatus.ERROR, IStatus.OK, message, exception);
  }

  /**
   * Log the specified information.
   * 
   * @param message a human-readable message, localized to the current locale.
   */
  static void logInfo(Logger log, String message) {
    logInfo(log, message, EMPTY);
  }

  /**
   * Log the specified information.
   * 
   * @param message a human-readable message, localized to the current locale.
   * @param args message arguments.
   */
  static void logInfo(Logger log, String message, Object... args) {
    message = MessageFormat.format(message, args);
    log(log, IStatus.INFO, IStatus.OK, message, null);
  }

  /**
   * Log the specified warning.
   * 
   * @param message a human-readable message, localized to the current locale.
   */
  static void logWarning(Logger log, String message) {
    log(log, IStatus.WARNING, IStatus.OK, message, null);
  }

  private static String getBundleSymbolicName(Logger log) {
    Bundle bundle = log.getBundle();
    if (bundle == null) {
      return "com.google.gca.eclipse";
    }
    return bundle.getSymbolicName();
  }
}
