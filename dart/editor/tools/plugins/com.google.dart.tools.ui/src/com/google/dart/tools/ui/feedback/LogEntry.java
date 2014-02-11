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
package com.google.dart.tools.ui.feedback;

/**
 * An entry in the Eclipse log
 */
public class LogEntry {
  public static final String SESSION_TAG = "!SESSION";
  public static final String ENTRY_TAG = "!ENTRY";
  private static final String CRASH_MESSAGE = "!MESSAGE The workspace exited with unsaved changes";

  private String content;

  public LogEntry(String content) {
    this.content = content;
  }

  public String getContent() {
    return content;
  }

  public boolean isCrashMessage() {
    if (content.startsWith(ENTRY_TAG)) {
      if (content.indexOf(CRASH_MESSAGE, ENTRY_TAG.length()) > 0) {
        return true;
      }
    }
    return false;
  }

  public boolean isSessionStart() {
    return content.startsWith(SESSION_TAG);
  }
}
