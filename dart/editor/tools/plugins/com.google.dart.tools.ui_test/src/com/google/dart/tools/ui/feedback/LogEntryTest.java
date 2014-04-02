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

import static com.google.dart.tools.ui.feedback.LogReaderTest.LOG_CRASH_MESSAGE;
import static com.google.dart.tools.ui.feedback.LogReaderTest.LOG_EXCEPTION;
import static com.google.dart.tools.ui.feedback.LogReaderTest.LOG_MESSAGE;
import static com.google.dart.tools.ui.feedback.LogReaderTest.LOG_SESSION_START;

import junit.framework.TestCase;

public class LogEntryTest extends TestCase {

  public void test_getContent() {
    LogEntry entry = new LogEntry(LOG_EXCEPTION);
    assertEquals(LOG_EXCEPTION, entry.getContent());
  }

  public void test_isCrashMessage() throws Exception {
    LogEntry entry = new LogEntry(LOG_CRASH_MESSAGE);
    assertTrue(entry.isCrashMessage());
  }

  public void test_isCrashMessage_not() throws Exception {
    LogEntry entry = new LogEntry("random");
    assertFalse(entry.isCrashMessage());
  }

  public void test_isSessionStart_exception() {
    LogEntry entry = new LogEntry(LOG_EXCEPTION);
    assertFalse(entry.isSessionStart());
  }

  public void test_isSessionStart_message() {
    LogEntry entry = new LogEntry(LOG_MESSAGE);
    assertFalse(entry.isSessionStart());
  }

  public void test_isSessionStart_other() {
    LogEntry entry = new LogEntry("some random stuff");
    assertFalse(entry.isSessionStart());
  }

  public void test_isSessionStart_start() {
    LogEntry entry = new LogEntry(LOG_SESSION_START);
    assertTrue(entry.isSessionStart());
  }
}
