/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.ui.internal.refactoring.actions;

import com.google.dart.engine.element.Element;
import com.google.dart.server.generated.types.RefactoringKind;
import com.google.dart.tools.ui.actions.AbstractRefactoringAction_NEW;
import com.google.dart.tools.ui.internal.refactoring.RefactoringSaveHelper;
import com.google.dart.tools.ui.internal.refactoring.RenameWizard_NEW;
import com.google.dart.tools.ui.internal.refactoring.ServerRenameRefactoring;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.action.Action;

/**
 * {@link Action} for renaming {@link Element}.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class RenameDartElementAction_NEW extends AbstractRefactoringAction_NEW {
  public RenameDartElementAction_NEW(DartEditor editor) {
    super(editor, RefactoringKind.RENAME);
  }

  @Override
  public void run() {
    // TODO(scheglov)
    System.out.println("run!");
    ServerRenameRefactoring refactoring = new ServerRenameRefactoring(
        file,
        selectionOffset,
        selectionLength);
    try {
      new RefactoringStarter().activate(
          new RenameWizard_NEW(refactoring),
          getShell(),
          "Rename",
          RefactoringSaveHelper.SAVE_NOTHING);
    } catch (Throwable e) {
      showError("Extract Local", e);
    }
  }

  @Override
  protected void init() {
  }

//  private void renameUsingDialog(Element element) {
//    if (element == null) {
//      return;
//    }
//    if (!RefactoringUtils.waitReadyForRefactoring()) {
//      return;
//    }
//    try {
//      RefactoringExecutionStarter.startRenameRefactoring(element, getShell());
//    } catch (Throwable e) {
//      ExceptionHandler.handle(
//          e,
//          RefactoringMessages.RenameDartElementAction_name,
//          RefactoringMessages.RenameDartElementAction_exception);
//    }
//  }
}
