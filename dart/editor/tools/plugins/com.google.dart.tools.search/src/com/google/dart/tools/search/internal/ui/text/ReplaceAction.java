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
package com.google.dart.tools.search.internal.ui.text;

import com.google.dart.tools.search.internal.ui.SearchMessages;

import org.eclipse.jface.action.Action;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;

public class ReplaceAction extends Action {

  public static class ReplaceWizard extends RefactoringWizard {
    public ReplaceWizard(ReplaceRefactoring refactoring) {
      super(refactoring, RefactoringWizard.DIALOG_BASED_USER_INTERFACE);
    }

    @Override
    protected void addUserInputPages() {
      addPage(new ReplaceConfigurationPage((ReplaceRefactoring) getRefactoring()));
    }
  }

  private final FileSearchResult fResult;
  private final Object[] fSelection;
  private final Shell fShell;

  /**
   * Creates the replace action.
   * 
   * @param shell the parent shell
   * @param result the file search page to
   * @param selection the selected entries or <code>null</code> to replace all
   */
  public ReplaceAction(Shell shell, FileSearchResult result, Object[] selection) {
    fShell = shell;
    fResult = result;
    fSelection = selection;
  }

  @Override
  public void run() {
    try {
      ReplaceRefactoring refactoring = new ReplaceRefactoring(fResult, fSelection);
      ReplaceWizard refactoringWizard = new ReplaceWizard(refactoring);
      if (fSelection == null) {
        refactoringWizard.setDefaultPageTitle(SearchMessages.ReplaceAction_title_all);
      } else {
        refactoringWizard.setDefaultPageTitle(SearchMessages.ReplaceAction_title_selected);
      }
      RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(refactoringWizard);
      op.run(fShell, SearchMessages.ReplaceAction_description_operation);
    } catch (InterruptedException e) {
      // refactoring got cancelled
    }
  }

}
