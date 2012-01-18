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

import com.google.dart.tools.ui.swtbot.DartLib;
import com.google.dart.tools.ui.swtbot.Performance;
import com.google.dart.tools.ui.swtbot.matchers.EditorWithTitle;

import org.eclipse.swt.SWT;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;

import static org.eclipse.swtbot.eclipse.finder.waits.Conditions.waitForEditor;
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
  public void open(DartLib lib) {
    if (!lib.dartFile.exists()) {
      fail("Cannot open non existing file: " + lib.dartFile);
    }

    // Open the native dialog
    bot.menu("File").menu("Open...").click();
    waitForNativeShellShowing();

    // Type the absolute path to the library
    typeKeyCode(SWT.MOD1 | 'a'); // Select all
    bot.sleep(100);
    typeText(lib.dartFile.getAbsolutePath());
    bot.sleep(1000);

    // Press Enter and wait for the operation to complete
    typeChar(SWT.CR);
    lib.logFullCompileTime();
    EditorWithTitle matcher = new EditorWithTitle(lib.dartFile.getName());
    Performance.OPEN_LIB.log(bot, waitForEditor(matcher), lib.name);
    lib.editor = bot.editor(matcher).toTextEditor();
  }
}
