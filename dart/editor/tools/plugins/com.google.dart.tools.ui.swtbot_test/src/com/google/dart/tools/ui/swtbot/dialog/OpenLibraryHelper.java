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
package com.google.dart.tools.ui.swtbot.dialog;

import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.CreateAndRevealProjectAction;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;
import com.google.dart.tools.ui.swtbot.DartEditorHelper;
import com.google.dart.tools.ui.swtbot.DartLib;
import com.google.dart.tools.ui.swtbot.performance.Performance;

import static com.google.dart.tools.ui.swtbot.util.SWTBotUtil.editorWithTitle;
import static com.google.dart.tools.ui.swtbot.util.SWTBotUtil.waitForEditorWithTitle;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;

import static org.junit.Assert.fail;

/**
 * Helper for driving the "Open Folder..." command
 */
// TODO (jwren) rename to OpenFolder to match current UI
@SuppressWarnings("restriction")
public class OpenLibraryHelper {

  SWTWorkbenchBot bot;

  public OpenLibraryHelper(SWTWorkbenchBot bot) {
    this.bot = bot;
  }

  /**
   * Drive the "Open Library..." dialog to open the specified library. As a side-effect, the
   * {@link DartLib#editor} field is set after the library is opened.
   * 
   * @param lib the library (not <code>null</code>)
   */
  public void open(final DartLib lib) throws Exception {
    if (!lib.dartFile.exists()) {
      fail("Cannot open non existing file: " + lib.dartFile);
    }

    Display.getDefault().syncExec(new Runnable() {
      @Override
      public void run() {
        try {
          new CreateAndRevealProjectAction(DartEditorHelper.getWorkbenchWindow(),
              lib.dartFile.getParentFile().getAbsolutePath()).run();
          EditorUtility.openInEditor(ResourceUtil.getFile(lib.dartFile));
        } catch (Exception e) {
          DartToolsPlugin.log(e);
        }
      }
    });

    String title = lib.dartFile.getName();
    Performance.OPEN_LIB.logInBackground(waitForEditorWithTitle(title), lib.name);
    Performance.waitForResults(bot);

    lib.editor = editorWithTitle(bot, title);
  }
}
