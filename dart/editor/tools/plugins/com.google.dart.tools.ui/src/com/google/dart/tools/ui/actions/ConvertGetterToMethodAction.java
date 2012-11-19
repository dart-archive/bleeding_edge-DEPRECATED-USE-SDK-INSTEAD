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
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.internal.corext.refactoring.RefactoringAvailabilityTester;
import com.google.dart.tools.internal.corext.refactoring.RefactoringExecutionStarter;
import com.google.dart.tools.ui.internal.actions.ActionUtil;
import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartTextSelection;
import com.google.dart.tools.ui.internal.util.DartModelUtil;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

/**
 * Converts getter into method.
 */
public class ConvertGetterToMethodAction extends SelectionDispatchAction {

  private DartEditor fEditor;

  public ConvertGetterToMethodAction(DartEditor editor) {
    super(editor.getEditorSite());
    setText(RefactoringMessages.ConvertGetterToMethodAction_title);
    fEditor = editor;
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.CONVERT_GETTER_TO_METHOD_ACTION);
    setEnabled(SelectionConverter.getInputAsCompilationUnit(fEditor) != null);
  }

  public ConvertGetterToMethodAction(IWorkbenchSite site) {
    super(site);
    setText(RefactoringMessages.ConvertGetterToMethodAction_title);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.CONVERT_GETTER_TO_METHOD_ACTION);
  }

  @Override
  public void run(IStructuredSelection selection) {
    try {
      Object element = selection.getFirstElement();
      if (element instanceof DartFunction) {
        DartFunction function = (DartFunction) element;
        if (RefactoringAvailabilityTester.isConvertGetterToMethodAvailable(function)) {
          boolean success = RefactoringExecutionStarter.startConvertGetterToMethodRefactoring(
              function,
              getShell());
          if (success) {
            return;
          }
        }
      }
    } catch (DartModelException e) {
      ExceptionHandler.handle(
          e,
          getShell(),
          RefactoringMessages.ConvertGetterToMethodAction_dialog_title,
          RefactoringMessages.InlineMethodAction_unexpected_exception);
    }
  }

  @Override
  public void run(ITextSelection selection) {
    if (!ActionUtil.isEditable(fEditor)) {
      return;
    }

    CompilationUnit cu = SelectionConverter.getInputAsCompilationUnit(fEditor);
    try {
      DartFunction function = DartModelUtil.findFunction(cu, selection.getOffset());
      boolean success = RefactoringExecutionStarter.startConvertGetterToMethodRefactoring(
          function,
          getShell());
      if (success) {
        return;
      }
    } catch (Throwable e) {
    }

    MessageDialog.openInformation(
        getShell(),
        RefactoringMessages.ConvertGetterToMethodAction_dialog_title,
        RefactoringMessages.ConvertGetterToMethodAction_select);
  }

  @Override
  public void selectionChanged(DartTextSelection selection) {
    try {
      setEnabled(RefactoringAvailabilityTester.isConvertGetterToMethodAvailable(selection));
    } catch (DartModelException e) {
      setEnabled(false);
    }
  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
    try {
      setEnabled(RefactoringAvailabilityTester.isConvertGetterToMethodAvailable(selection));
    } catch (DartModelException e) {
      setEnabled(false);
    }
  }

  @Override
  public void selectionChanged(ITextSelection selection) {
    setEnabled(true);
  }
}
