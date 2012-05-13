package com.google.dart.tools.ui.actions;

import com.google.dart.tools.internal.corext.refactoring.RefactoringAvailabilityTester;
import com.google.dart.tools.internal.corext.refactoring.code.ExtractLocalRefactoring;
import com.google.dart.tools.ui.internal.actions.ActionUtil;
import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.refactoring.ExtractLocalWizard;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.refactoring.RefactoringSaveHelper;
import com.google.dart.tools.ui.internal.refactoring.actions.RefactoringStarter;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartTextSelection;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.PlatformUI;

/**
 * Extracts an expression into a new local variable and replaces all occurrences of the expression
 * with the local variable.
 */
public class ExtractLocalAction extends SelectionDispatchAction {

  private final DartEditor editor;

  public ExtractLocalAction(DartEditor editor) {
    super(editor.getEditorSite());
    this.editor = editor;
    setText(RefactoringMessages.ExtractLocalAction_label);
    setEnabled(SelectionConverter.getInputAsCompilationUnit(editor) != null);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, DartHelpContextIds.EXTRACT_LOCAL_ACTION);
  }

  @Override
  public void run(ITextSelection selection) {
    if (!ActionUtil.isEditable(editor)) {
      return;
    }
    ExtractLocalRefactoring refactoring = new ExtractLocalRefactoring(
        SelectionConverter.getInputAsCompilationUnit(editor),
        selection.getOffset(),
        selection.getLength());
    new RefactoringStarter().activate(
        new ExtractLocalWizard(refactoring),
        getShell(),
        RefactoringMessages.ExtractLocalAction_dialog_title,
        RefactoringSaveHelper.SAVE_NOTHING);
  }

  @Override
  public void selectionChanged(DartTextSelection selection) {
    setEnabled(RefactoringAvailabilityTester.isExtractLocalAvailable(selection));
  }

  @Override
  public void selectionChanged(ITextSelection selection) {
    setEnabled(editor != null && SelectionConverter.getInputAsCompilationUnit(editor) != null);
  }
}
