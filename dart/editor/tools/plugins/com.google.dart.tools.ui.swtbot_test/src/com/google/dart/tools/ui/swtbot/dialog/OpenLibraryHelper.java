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

import com.google.dart.tools.ui.internal.text.editor.EditorUtility;
import com.google.dart.tools.ui.swtbot.DartLib;
import com.google.dart.tools.ui.swtbot.performance.Performance;

import static com.google.dart.tools.ui.swtbot.util.SWTBotUtil.editorWithTitle;
import static com.google.dart.tools.ui.swtbot.util.SWTBotUtil.waitForEditorWithTitle;

import org.eclipse.swt.SWT;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;

import static org.eclipse.swtbot.swt.finder.utils.SWTUtils.isMac;
import static org.junit.Assert.fail;

/**
 * Helper for driving the "Open Library.." command
 */
public class OpenLibraryHelper extends NativeDialogHelper {
  public OpenLibraryHelper(SWTWorkbenchBot bot) {
    super(bot);
  }

  /**
   * Drive the "Open Library..." dialog to open the specified library. As a side-effect, the
   * {@link DartLib#editor} field is set after the library is opened.
   * 
   * @param lib the library (not <code>null</code>)
   */
  public void open(DartLib lib) throws Exception {
    if (!lib.dartFile.exists()) {
      fail("Cannot open non existing file: " + lib.dartFile);
    }

    // Open the native dialog
    bot.menu("File").menu("Open Folder...").click();

    try {
      waitForNativeShellShowing();
      bot.sleep(500);

      // On Mac, open the "Go to Folder" popup
      if (isMac()) {
        typeKeyCode(SWT.COMMAND | SWT.SHIFT | 'g');
      }
      typeKeyCode(SWT.MOD1 | 'a'); // Select all

      // Type the absolute path to the library
      bot.sleep(500);
      typeText(lib.dir.getAbsolutePath());
      bot.sleep(1000);

      // On Mac, extra CR to close the "Go to Folder" popup
      if (isMac()) {
        typeChar(SWT.CR);
        bot.sleep(250);
      }

      // Press Enter and wait for the operation to complete
      typeChar(SWT.CR);
      waitForNativeShellClosed();
      lib.logFullAnalysisTime();
      String title = lib.dartFile.getName();
      Performance.OPEN_LIB.logInBackground(waitForEditorWithTitle(title), lib.name);
      EditorUtility.openInEditor(lib.dartFile);
      Performance.waitForResults(bot);
      lib.editor = editorWithTitle(bot, title);

    } finally {
      ensureNativeShellClosed();
    }
  }
}
