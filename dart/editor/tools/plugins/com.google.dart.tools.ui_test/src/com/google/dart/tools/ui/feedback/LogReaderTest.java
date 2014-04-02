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

import junit.framework.TestCase;

import java.util.List;

public class LogReaderTest extends TestCase {

  static final String EOL = System.getProperty("line.separator");

  static final String LOG_SESSION_START = //
  "!SESSION 2013-10-04 07:13:59.705 -----------------------------------------------"
      + EOL
      + "eclipse.buildId=M20130131-0800"
      + EOL
      + "java.version=1.6.0_51"
      + EOL
      + "java.vendor=Apple Inc."
      + EOL
      + "BootLoader constants: OS=macosx, ARCH=x86_64, WS=cocoa, NL=en_US"
      + EOL
      + "Framework arguments:  -keyring /Users/username/.eclipse_keyring -showlocation"
      + EOL
      + "Command-line arguments:  -os macosx -ws cocoa -arch x86_64 -clean -keyring /Users/username/.eclipse_keyring -showlocation";

  static final String LOG_MESSAGE = //
  "!ENTRY org.eclipse.update.configurator 4 0 2013-10-04 07:17:07.861" + EOL
      + "!MESSAGE Unable to find feature.xml in directory: /foo/bar";

  static final String LOG_CRASH_MESSAGE = //
  "!ENTRY org.eclipse.core.resources 2 10035 2013-05-16 22:07:36.551"
      + "!MESSAGE The workspace exited with unsaved changes in the previous session; refreshing workspace to recover changes.";

  static final String LOG_EXCEPTION = //
  "!ENTRY org.eclipse.core.resources 4 567 2013-10-04 07:58:49.135"
      + EOL
      + "!MESSAGE Workspace restored, but some problems occurred."
      + EOL
      + "!SUBENTRY 1 org.eclipse.core.resources 4 567 2013-10-04 07:58:49.135"
      + EOL
      + "!MESSAGE Could not read metadata for 'com.google.dart.tools.instrumentation'."
      + EOL
      + "!STACK 1"
      + EOL
      + "org.eclipse.core.internal.resources.ResourceException: The project description file"
      + EOL
      + "  at org.eclipse.core.internal.localstore.FileSystemResourceManager.read(FileSystemResourceManager.java:851)"
      + EOL
      + "  at org.eclipse.core.internal.resources.SaveManager.restoreMetaInfo(SaveManager.java:874)"
      + EOL
      + "  at org.eclipse.core.internal.resources.SaveManager.restoreMetaInfo(SaveManager.java:854)";

  static final String LOG_CONTENTS = LOG_SESSION_START + EOL + EOL + LOG_MESSAGE + EOL + EOL
      + LOG_EXCEPTION;

  public void test_parseEntries() throws Exception {
    List<LogEntry> result = LogReader.parseEntries(LOG_CONTENTS);
    assertEquals(3, result.size());
    assertTrue(result.get(0).getContent().startsWith("!SESSION"));
    assertTrue(result.get(1).getContent().startsWith("!ENTRY"));
    assertTrue(result.get(2).getContent().startsWith("!ENTRY"));
    assertTrue(result.get(2).getContent().contains("!STACK"));
  }

  public void test_parseEntries_exception() throws Exception {
    List<LogEntry> result = LogReader.parseEntries(LOG_EXCEPTION);
    assertEquals(1, result.size());
    assertTrue(result.get(0).getContent().startsWith("!ENTRY"));
    assertTrue(result.get(0).getContent().contains("!STACK"));
  }

  public void test_parseEntries_message() throws Exception {
    List<LogEntry> result = LogReader.parseEntries(LOG_MESSAGE);
    assertEquals(1, result.size());
    assertTrue(result.get(0).getContent().startsWith("!ENTRY"));
  }

  public void test_parseEntries_partial() throws Exception {
    List<LogEntry> result = LogReader.parseEntries("some stuff" + EOL + EOL + LOG_CONTENTS);
    assertEquals(4, result.size());
    assertTrue(result.get(0).getContent().equals("some stuff"));
    assertTrue(result.get(1).getContent().startsWith("!SESSION"));
    assertTrue(result.get(2).getContent().startsWith("!ENTRY"));
    assertTrue(result.get(3).getContent().startsWith("!ENTRY"));
    assertTrue(result.get(3).getContent().contains("!STACK"));
  }

  public void test_parseEntries_session() throws Exception {
    List<LogEntry> result = LogReader.parseEntries(LOG_SESSION_START);
    assertEquals(1, result.size());
    assertTrue(result.get(0).getContent().startsWith("!SESSION"));
  }
}
