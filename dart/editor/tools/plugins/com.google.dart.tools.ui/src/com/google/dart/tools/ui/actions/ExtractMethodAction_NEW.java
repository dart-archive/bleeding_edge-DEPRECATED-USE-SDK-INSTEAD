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
package com.google.dart.tools.ui.actions;

import com.google.dart.tools.ui.internal.refactoring.ExtractMethodWizard_NEW;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.refactoring.RefactoringSaveHelper;
import com.google.dart.tools.ui.internal.refactoring.ServerExtractMethodRefactoring;
import com.google.dart.tools.ui.internal.refactoring.actions.RefactoringStarter;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.PlatformUI;

/**
 * {@link Action} for "Extract Method" refactoring.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class ExtractMethodAction_NEW extends AbstractRefactoringAction_NEW {
  public ExtractMethodAction_NEW(DartEditor editor) {
    super(editor);
  }

  @Override
  public void run() {
    if (!waitReadyForRefactoring()) {
      return;
    }
    ServerExtractMethodRefactoring refactoring = new ServerExtractMethodRefactoring(
        file,
        selectionOffset,
        selectionLength);
    try {
      new RefactoringStarter().activate(
          new ExtractMethodWizard_NEW(refactoring),
          getShell(),
          RefactoringMessages.ExtractMethodAction_dialog_title,
          RefactoringSaveHelper.SAVE_NOTHING);
    } catch (Throwable e) {
      showError("Extract Method", e);
    }
  }

  @Override
  public void selectionChanged(ISelection selection) {
    super.selectionChanged(selection);
    setEnabled(selectionLength != 0);
  }

  @Override
  protected void init() {
    setText(RefactoringMessages.ExtractLocalAction_label);
    {
      String id = DartEditorActionDefinitionIds.EXTRACT_LOCAL_VARIABLE;
      setId(id);
      setActionDefinitionId(id);
    }
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, DartHelpContextIds.EXTRACT_LOCAL_ACTION);
  }
}
