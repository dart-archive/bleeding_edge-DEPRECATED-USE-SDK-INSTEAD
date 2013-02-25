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

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.Bundle;

import java.text.MessageFormat;

/**
 * Common logger class that is associated with a plugin.
 */
public class Logger {
  private final Plugin plugin;

  public Logger(Plugin plugin) {
    this.plugin = plugin;
  }

  /**
   * Return <code>true</code> if tracing is enabled when using the given option name.
   * 
   * @return <code>true</code> if tracing is enabled
   */
  public boolean isTracing(String optionName) {
    if (optionName == null) {
      return true;
    }
    return TraceUtils.isTracing(qualifyOptionName(optionName));
  }

  /**
   * Log the specified error.
   * 
   * @param message a human-readable message, localized to the current locale.
   */
  public void logError(String message) {
    LogUtils.logError(this, null, message);
  }

  /**
   * Log the specified error.
   * 
   * @param message a human-readable message, localized to the current locale.
   * @param args message arguments.
   */
  public void logError(String message, Object... args) {
    LogUtils.logError(this, null, message, args);
  }

  /**
   * Log the specified error.
   * 
   * @param exception a low-level exception.
   */
  public void logError(Throwable exception) {
    LogUtils.logError(this, exception, "Unexpected Exception");
  }

  /**
   * Log the specified error.
   * 
   * @param exception a low-level exception, or <code>null</code> if not applicable.
   * @param message a human-readable message, localized to the current locale.
   */
  public void logError(Throwable exception, String message) {
    LogUtils.log(this, IStatus.ERROR, IStatus.OK, message, exception);
  }

  /**
   * Log the specified error.
   * 
   * @param exception a low-level exception, or <code>null</code> if not applicable.
   * @param message a human-readable message, localized to the current locale.
   * @param args message arguments.
   */
  public void logError(Throwable exception, String message, Object... args) {
    message = MessageFormat.format(message, args);
    LogUtils.log(this, IStatus.ERROR, IStatus.OK, message, exception);
  }

  /**
   * Log the specified information.
   * 
   * @param message a human-readable message, localized to the current locale.
   */
  public void logInfo(String message) {
    LogUtils.logInfo(this, message, new Object[0]);
  }

  /**
   * Log the specified information.
   * 
   * @param message a human-readable message, localized to the current locale.
   * @param args message arguments.
   */
  public void logInfo(String message, Object... args) {
    message = MessageFormat.format(message, args);
    LogUtils.log(this, IStatus.INFO, IStatus.OK, message, null);
  }

  /**
   * Log the specified warning.
   * 
   * @param message a human-readable message, localized to the current locale.
   */
  public void logWarning(String message) {
    LogUtils.log(this, IStatus.WARNING, IStatus.OK, message, null);
  }

  /**
   * Log the specified warning.
   * 
   * @param message a human-readable message, localized to the current locale.
   */
  public void logWarning(Throwable e, String message) {
    LogUtils.log(this, IStatus.WARNING, IStatus.OK, message, e);
  }

  /**
   * Trace the specified information.
   * 
   * @param message a human-readable message.
   */
  public void trace(String message) {
    trace(null, message);
  }

  /**
   * Trace the specified information.
   * 
   * @param optionName the option name
   * @param message a human-readable message.
   */
  public void trace(String optionName, String message) {
    TraceUtils.traceInfo(this, qualifyOptionName(optionName), message);
  }

  /**
   * Trace the specified information.
   * 
   * @param optionName the option name
   * @param e an exception whose stack trace is to be included in the output
   * @param message a human-readable message.
   */
  public void trace(String optionName, Throwable e, String message) {
    TraceUtils.traceError(this, qualifyOptionName(optionName), e, message);
  }

  Bundle getBundle() {
    return plugin.getBundle();
  }

  ILog getLog() {
    return plugin.getLog();
  }

  private String qualifyOptionName(String optionName) {
    Bundle bundle = getBundle();
    if (bundle == null) {
      return optionName;
    }
    String pluginId = bundle.getSymbolicName();
    if (pluginId == null) {
      return optionName;
    }

    // Check to see if the name is already fully qualified.
    if (optionName != null && optionName.startsWith(pluginId)) {
      return optionName;
    }

    if (optionName == null) {
      optionName = "";
    } else {
      optionName = "/" + optionName;
    }

    return pluginId + "/debug" + optionName;
  }

}
