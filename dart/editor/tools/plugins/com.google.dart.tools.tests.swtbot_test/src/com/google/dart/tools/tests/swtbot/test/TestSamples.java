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
import com.google.dart.tools.tests.swtbot.model.ProblemsBotView;
import com.google.dart.tools.tests.swtbot.model.WelcomePageEditor;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Verify the samples compile correctly then delete them. The test methods are named to ensure they
 * execute in the standard order for running samples.
 */
public class TestSamples extends EditorTestHarness {

  static class AngularTodoRunner extends SampleRunner {
    @Override
    void createSample(WelcomePageEditor page) {
      page.createAngularTodo();
    }
  }

  static class PolymerTodoRunner extends SampleRunner {
    @Override
    void createSample(WelcomePageEditor page) {
      page.createPolymerTodo();
    }
  }

  static class PopPopWinRunner extends SampleRunner {
    @Override
    void createSample(WelcomePageEditor page) {
      page.createPopPopWin();
    }
  }

  static class SunflowerRunner extends SampleRunner {
    @Override
    void createSample(WelcomePageEditor page) {
      page.createSunflower();
    }
  }

  private static abstract class SampleRunner {

    abstract void createSample(WelcomePageEditor page);

    void run(String projectName, int... args) {
      EditorBotWindow editor = new EditorBotWindow(bot);
      WelcomePageEditor page = editor.openWelcomePage();
      try {
        createSample(page);
        ProblemsBotView problems = editor.problemsView();
        assertTrue(problems.isEmpty(args));
      } finally {
        FilesBotView files = editor.filesView();
        files.deleteProject(projectName);
        assertTrue(files.isEmpty());
      }
    }
  }

  @Test
  public void test1Sunflower() throws Exception {
    new SunflowerRunner().run("sunflower");
  }

  @Test
  public void test2PopPopWin() throws Exception {
    new PopPopWinRunner().run("pop_pop_win");
  }

  @Test
  public void test3PolymerTodo() throws Exception {
    new PolymerTodoRunner().run("todomvc");
  }

  // Stop testing angular as  of 29 July 2014
//  @Test
  public void test4AngularTodo() throws Exception {
    new AngularTodoRunner().run("angular_todo", 1); // ignore one hint
  }
}
