/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal;

import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Convenience class for error exceptions thrown inside JavaScriptUI plugin.
 */
public class DartUiStatus extends Status {

  public static IStatus createError(int code, String message, Throwable throwable) {
    return new DartUiStatus(IStatus.ERROR, code, message, throwable);
  }

  public static IStatus createError(int code, Throwable throwable) {
    String message = throwable.getMessage();
    if (message == null) {
      message = throwable.getClass().getName();
    }
    return new DartUiStatus(IStatus.ERROR, code, message, throwable);
  }

  public static IStatus createInfo(int code, String message, Throwable throwable) {
    return new DartUiStatus(IStatus.INFO, code, message, throwable);
  }

  public static IStatus createWarning(int code, String message, Throwable throwable) {
    return new DartUiStatus(IStatus.WARNING, code, message, throwable);
  }

  private DartUiStatus(int severity, int code, String message, Throwable throwable) {
    super(severity, DartToolsPlugin.getPluginId(), code, message, throwable);
  }
}
