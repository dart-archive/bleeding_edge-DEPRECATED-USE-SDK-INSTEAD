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
package com.google.dart.tools.tests.swtbot.model;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Model a code editor of Dart Editor.
 */
public class TextBotEditor extends AbstractBotView {

  private final String title;

  public TextBotEditor(SWTWorkbenchBot bot, String title) {
    super(bot);
    this.title = title;
  }

  @SuppressWarnings("unused")
  private IEditorReference editorReference() {
    // TODO for reference only; probably want to use SWTBotView
    return UIThreadRunnable.syncExec(new Result<IEditorReference>() {
      @Override
      public IEditorReference run() {
        IWorkbenchWindow bench = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IEditorReference[] refs = bench.getActivePage().getEditorReferences();
        for (IEditorReference ref : refs) {
          if (title.equals(ref.getTitle())) {
            return ref;
          }
        }
        return null;
      }
    });
  }
}
