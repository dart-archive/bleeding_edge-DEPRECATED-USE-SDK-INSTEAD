/*
 * Copyright 2014 Dart project authors.
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
package com.google.dart.tools.tests.swtbot.test;

import com.google.dart.tools.tests.swtbot.harness.EditorTestHarness;
import com.google.dart.tools.tests.swtbot.model.EditorBotWindow;
import com.google.dart.tools.tests.swtbot.model.RemoteConnectionBotView;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestRemoteConnection extends EditorTestHarness {

  @BeforeClass
  public static void setUpTest() {
    assertNotNull(bot); // initialized in superclass
  }

  @Test
  public void testRemoteConnection() throws Exception {
    EditorBotWindow editor = new EditorBotWindow(bot);
    editor.menu("Run").menu("Remote Connection...").click();
    RemoteConnectionBotView conn = new RemoteConnectionBotView(bot);
    conn.useVM();
    conn.host("candy", "8080");
    conn.useChrome();
    conn.host("cake", "7235");
    conn.usePubServe(false);
    conn.close();
    editor.menu("Run").menu("Remote Connection...").click();
    conn = new RemoteConnectionBotView(bot);
    assertEquals("cake", conn.host());
    assertEquals("7235", conn.port());
//    assertFalse(conn.usingPub()); // this value is not saved since there is no connection
    conn.close();
  }
}
