/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.ui.swtbot;

import com.google.dart.tools.ui.swtbot.performance.SwtBotPerformance;
import com.google.dart.tools.ui.test.model.Workspace;
import com.google.dart.tools.ui.test.model.Workspace.Project;
import com.google.dart.tools.ui.test.model.Workspace.Project.Type;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * New Application tests for the Dart Editor.
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public final class NewApplicationActionTest extends AbstractDartEditorTest {

  private Project project;

  @After
  public void cleanup() throws CoreException {
    if (project != null) {
      project.delete();
    }
  }

  @Test
  public void testNewApplicationWizard_server() throws Exception {
    createApp("NewAppServer", Type.SERVER);
    //TODO (pquitslund): verify content and launch
  }

  @Test
  public void testNewApplicationWizard_web() throws Exception {
    createApp("NewAppWeb", Type.WEB);
    //TODO (pquitslund): verify content and launch
  }

  private void createApp(String name, Type type) throws CoreException {
    //lib = new NewApplicationHelper(bot).create(name, type);
    project = Workspace.createProject(name, type);
    //may be unnecessary
    SwtBotPerformance.waitForResults(bot);
  }

}
