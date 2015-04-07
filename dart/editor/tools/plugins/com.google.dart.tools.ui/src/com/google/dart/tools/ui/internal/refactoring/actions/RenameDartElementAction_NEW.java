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

import com.google.dart.server.generated.types.Element;
import com.google.dart.server.generated.types.NavigationRegion;
import com.google.dart.server.generated.types.NavigationTarget;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.AnalysisServerNavigationListener;
import com.google.dart.tools.ui.actions.AbstractRefactoringAction_NEW;
import com.google.dart.tools.ui.internal.actions.NewSelectionConverter;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.refactoring.RefactoringSaveHelper;
import com.google.dart.tools.ui.internal.refactoring.RenameWizard_NEW;
import com.google.dart.tools.ui.internal.refactoring.ServerRenameRefactoring;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;

/**
 * {@link Action} for renaming {@link Element}.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class RenameDartElementAction_NEW extends AbstractRefactoringAction_NEW implements
    AnalysisServerNavigationListener {
  public RenameDartElementAction_NEW(DartEditor editor) {
    super(editor);
    DartCore.getAnalysisServerData().addNavigationListener(getFile(), this);
  }

  @Override
  public void computedNavigation(String file, NavigationRegion[] regions) {
    updateSelectedElement();
  }

  @Override
  public void dispose() {
    DartCore.getAnalysisServerData().removeNavigationListener(getFile(), this);
    super.dispose();
  }

  @Override
  public void run() {
    if (!waitReadyForRefactoring()) {
      return;
    }
    ServerRenameRefactoring refactoring = new ServerRenameRefactoring(
        getFile(),
        selectionOffset,
        selectionLength);
    try {
      new RefactoringStarter().activate(
          new RenameWizard_NEW(refactoring),
          getShell(),
          "Rename",
          RefactoringSaveHelper.SAVE_NOTHING);
    } catch (Throwable e) {
      showError("Rename", e);
    }
  }

  @Override
  public void selectionChanged(ISelection selection) {
    super.selectionChanged(selection);
    updateSelectedElement();
  }

  @Override
  protected void init() {
    setText(RefactoringMessages.RenameAction_text);
  }

  private void updateSelectedElement() {
    NavigationTarget[] targets = NewSelectionConverter.getNavigationTargets(getFile(), selectionOffset);
    setEnabled(targets.length != 0);
  }
}
