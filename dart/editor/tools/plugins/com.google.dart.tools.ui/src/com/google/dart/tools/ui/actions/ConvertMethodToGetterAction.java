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
import com.google.dart.tools.internal.corext.refactoring.RefactoringExecutionStarter_OLD;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

/**
 * Converts no-arguments method into getter.
 */
public class ConvertMethodToGetterAction extends InstrumentedSelectionDispatchAction {

  private DartEditor fEditor;

  public ConvertMethodToGetterAction(DartEditor editor) {
    super(editor.getEditorSite());
    setText(RefactoringMessages.ConvertMethodToGetterAction_title);
    fEditor = editor;
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.CONVERT_METHOD_TO_GETTER_ACTION);
    setEnabled(SelectionConverter.getInputAsCompilationUnit(fEditor) != null);
  }

  public ConvertMethodToGetterAction(IWorkbenchSite site) {
    super(site);
    setText(RefactoringMessages.ConvertMethodToGetterAction_title);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.CONVERT_METHOD_TO_GETTER_ACTION);
  }

  @Override
  public void doRun(ITextSelection selection, Event event, UIInstrumentationBuilder instrumentation) {

    if (!ActionUtil.isEditable(fEditor)) {
      instrumentation.metric("Problem", "Editor not editable");
      return;
    }

    CompilationUnit cu = SelectionConverter.getInputAsCompilationUnit(fEditor);
    instrumentation.record(cu);

    try {
      DartFunction function = DartModelUtil.findFunction(cu, selection.getOffset());
      boolean success = RefactoringExecutionStarter_OLD.startConvertMethodToGetterRefactoring(
          function,
          getShell());
      if (success) {
        return;
      }
      instrumentation.metric(
          "Problem",
          "RefactoringExecutionStarter.startConvertGetterToMethodRefactoring False");

    } catch (Throwable e) {
      instrumentation.record(e);
    }

    instrumentation.metric("Problem", "No valid selection, showing dialog");
    MessageDialog.openInformation(
        getShell(),
        RefactoringMessages.ConvertMethodToGetterAction_dialog_title,
        RefactoringMessages.ConvertMethodToGetterAction_select);
  }

  @Override
  public void selectionChanged(DartTextSelection selection) {
    try {
      setEnabled(RefactoringAvailabilityTester.isConvertMethodToGetterAvailable(selection));
    } catch (DartModelException e) {
      setEnabled(false);
    }
  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
    try {
      setEnabled(RefactoringAvailabilityTester.isConvertMethodToGetterAvailable(selection));
    } catch (DartModelException e) {
      setEnabled(false);
    }
  }

  @Override
  public void selectionChanged(ITextSelection selection) {
    setEnabled(true);
  }

  @Override
  protected void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {

    try {
      Object element = selection.getFirstElement();
      if (element instanceof DartFunction) {
        DartFunction function = (DartFunction) element;

        if (!RefactoringAvailabilityTester.isConvertMethodToGetterAvailable(function)) {
          instrumentation.metric("Problem", "RefactoringAvailabilityTester Returned false");
        }
        boolean success = RefactoringExecutionStarter_OLD.startConvertMethodToGetterRefactoring(
            function,
            getShell());

        if (success) {
          return;
        }

        instrumentation.metric(
            "Problem",
            "RefactoringExecutionStarter.startConvertMethodToGetterRefactoring returned False");

      }
    } catch (DartModelException e) {
      ExceptionHandler.handle(
          e,
          getShell(),
          RefactoringMessages.ConvertMethodToGetterAction_dialog_title,
          RefactoringMessages.InlineMethodAction_unexpected_exception);
    }
  }
}
