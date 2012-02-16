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

package com.google.dart.tools.debug.core.webkit;

import com.google.dart.tools.debug.core.DartDebugCorePlugin;

/**
 * Log the call's result to the editor's log file.
 */
public class LoggingWebkitCallback<T> implements WebkitCallback<T> {
  private String label;

  public LoggingWebkitCallback(String label) {
    this.label = label;
  }

  @Override
  public void handleResult(WebkitResult<T> result) {
    DartDebugCorePlugin.logInfo(label + ": " + result);
  }

}
