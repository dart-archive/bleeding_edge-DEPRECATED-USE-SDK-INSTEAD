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

import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.refactoring.RefactoringFactory;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.internal.corext.refactoring.RefactoringAvailabilityTester;
import com.google.dart.tools.internal.corext.refactoring.code.ExtractMethodRefactoring_OLD;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.actions.ActionUtil;
import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.refactoring.ExtractMethodWizard;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.refactoring.RefactoringSaveHelper;
import com.google.dart.tools.ui.internal.refactoring.ServiceExtractMethodRefactoring;
import com.google.dart.tools.ui.internal.refactoring.actions.RefactoringStarter;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartTextSelection;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PlatformUI;

/**
 * Extracts the code selected inside a compilation unit editor into a new method. Necessary
 * arguments, exceptions and returns values are computed and an appropriate method signature is
 * generated.
 */
public class ExtractMethodAction extends InstrumentedSelectionDispatchAction {

  private final DartEditor editor;

  public ExtractMethodAction(DartEditor editor) {
    super(editor.getEditorSite());
    setText(RefactoringMessages.ExtractMethodAction_label);
    this.editor = editor;
    setEnabled(SelectionConverter.getInputAsCompilationUnit(editor) != null);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.EXTRACT_METHOD_ACTION);
  }

  @Override
  public void doRun(ITextSelection selection, Event event, UIInstrumentationBuilder instrumentation) {

    if (!ActionUtil.isEditable(editor)) {
      instrumentation.metric("Problem", "Editor not editable");
      return;
    }
    if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
      try {
        AssistContext context = editor.getAssistContext();
        if (context == null) {
          return;
        }
        com.google.dart.engine.services.refactoring.ExtractMethodRefactoring newRefactoring = RefactoringFactory.createExtractMethodRefactoring(context);
        ServiceExtractMethodRefactoring ltkRefactoring = new ServiceExtractMethodRefactoring(
            newRefactoring);
        new RefactoringStarter().activate(
            new ExtractMethodWizard(ltkRefactoring),
            getShell(),
            RefactoringMessages.ExtractMethodAction_dialog_title,
            RefactoringSaveHelper.SAVE_NOTHING);
      } catch (Throwable e) {
        DartToolsPlugin.log(e);
        instrumentation.metric("Problem", "Exception during activation.");
      }
    } else {
      ExtractMethodRefactoring_OLD refactoring = new ExtractMethodRefactoring_OLD(
          SelectionConverter.getInputAsCompilationUnit(editor),
          selection.getOffset(),
          selection.getLength());
      new RefactoringStarter().activate(
          new ExtractMethodWizard(refactoring),
          getShell(),
          RefactoringMessages.ExtractMethodAction_dialog_title,
          RefactoringSaveHelper.SAVE_NOTHING);
    }
  }

  @Override
  public void selectionChanged(DartTextSelection selection) {
    setEnabled(editor != null && editor.isEditable()
        && RefactoringAvailabilityTester.isExtractMethodAvailable(selection));
  }

  @Override
  public void selectionChanged(ITextSelection selection) {
    // no editor
    if (editor == null || !editor.isEditable()) {
      setEnabled(false);
      return;
    }
    // not selection
    if (selection.getLength() == 0) {
      setEnabled(false);
      return;
    }
    // has context?
    if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
      AssistContext context = editor.getAssistContext();
      if (context == null) {
        setEnabled(false);
        return;
      }
      setEnabled(true);
    } else {
      setEnabled(SelectionConverter.getInputAsCompilationUnit(editor) != null);
    }
  }
}
