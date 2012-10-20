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

package com.google.dart.tools.ui.actions;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.internal.corext.codemanipulation.OrganizeImportsOperation;
import com.google.dart.tools.internal.corext.refactoring.Checks;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableEx;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.internal.actions.MultiOrganizeImportAction;
import com.google.dart.tools.ui.internal.actions.WorkbenchRunnableAdapter;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.CompilationUnitEditor;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

/**
 * Organizes the import directives in a library compilation unit.
 * 
 * @coverage dart.editor.ui.code_manipulation
 */
public class OrganizeImportsAction extends SelectionDispatchAction {
  private static CompilationUnit getCompilationUnit(CompilationUnitEditor editor) {
    if (editor != null) {
      DartElement element = DartUI.getEditorInputDartElement(editor.getEditorInput());
      if (!(element instanceof CompilationUnit)) {
        return null;
      }
      return (CompilationUnit) element;
    }

    return null;
  }

  private final CompilationUnitEditor editor;
  private final MultiOrganizeImportAction fCleanUpDelegate;

  public OrganizeImportsAction(CompilationUnitEditor editor) {
    super(editor.getEditorSite());
    setText(ActionMessages.OrganizeImportsAction_label);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.ORGANIZE_IMPORTS_ACTION);
    this.editor = editor;
    this.fCleanUpDelegate = null;
  }

  public OrganizeImportsAction(IWorkbenchSite site) {
    super(site);
    setText(ActionMessages.OrganizeImportsAction_label);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.ORGANIZE_IMPORTS_ACTION);
    this.editor = null;
    this.fCleanUpDelegate = new MultiOrganizeImportAction(site);
  }

  @Override
  public void run(IStructuredSelection selection) {
    CompilationUnit[] cus = fCleanUpDelegate.getCompilationUnits(selection);
    if (cus.length == 0) {
      MessageDialog.openInformation(
          getShell(),
          ActionMessages.OrganizeImportsAction_EmptySelection_title,
          ActionMessages.OrganizeImportsAction_EmptySelection_description);
    } else if (cus.length == 1) {
      run(cus[0]);
    } else {
      fCleanUpDelegate.run(selection);
    }
  }

  @Override
  public void run(ITextSelection selection) {
    CompilationUnit cu = getCompilationUnit(editor);
    if (cu != null) {
      run(cu);
    }
  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
    fCleanUpDelegate.selectionChanged(selection);
    setEnabled(fCleanUpDelegate.isEnabled());
  }

  private void run(final CompilationUnit unit) {
    ExecutionUtils.runLog(new RunnableEx() {
      @Override
      public void run() throws Exception {
        runEx(unit);
      }
    });
  }

  private void runEx(CompilationUnit unit) throws Exception {
    if (!Checks.isAvailable(unit)) {
      return;
    }
    OrganizeImportsOperation op = new OrganizeImportsOperation(unit);
    IRunnableContext context = getSite().getWorkbenchWindow();
    context.run(false, false, new WorkbenchRunnableAdapter(op, op.getScheduleRule()));
  }
}
