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
import com.google.dart.tools.tests.swtbot.model.FilesBotView;

import org.junit.Test;

/**
 * Delete samples and reset anything that needs to be initialized.
 */
public class TestInit extends EditorTestHarness {

  @Test
  public void testInit() throws Exception {
    EditorBotWindow editor = new EditorBotWindow(bot);
    FilesBotView files = editor.filesView();
    files.deleteExistingProject("sunflower");
    files.deleteExistingProject("pop_pop_win");
    files.deleteExistingProject("todomvc");
    files.deleteExistingProject("angular_todo");
  }
}
