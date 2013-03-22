package com.google.dart.tools.ui.actions;

import com.google.dart.tools.internal.corext.refactoring.RefactoringAvailabilityTester;
import com.google.dart.tools.internal.corext.refactoring.code.ExtractLocalRefactoring_OLD;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PlatformUI;

/**
 * Extracts an expression into a new local variable and replaces all occurrences of the expression
 * with the local variable.
 */
public class ExtractLocalAction_OLD extends InstrumentedSelectionDispatchAction {

  private final DartEditor editor;

  public ExtractLocalAction_OLD(DartEditor editor) {
    super(editor.getEditorSite());
    this.editor = editor;
    setText(RefactoringMessages.ExtractLocalAction_label);
    setEnabled(SelectionConverter.getInputAsCompilationUnit(editor) != null);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, DartHelpContextIds.EXTRACT_LOCAL_ACTION);
  }

  @Override
  public void selectionChanged(DartTextSelection selection) {
    setEnabled(editor != null && editor.isEditable()
        && RefactoringAvailabilityTester.isExtractLocalAvailable(selection));
  }

  @Override
  public void selectionChanged(ITextSelection selection) {
    if (editor == null || !editor.isEditable()) {
      setEnabled(false);
      return;
    }
    setEnabled(SelectionConverter.getInputAsCompilationUnit(editor) != null);
  }

  @Override
  protected void doRun(ITextSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    if (!ActionUtil.isEditable(editor)) {
      instrumentation.metric("Problem", "Editor not editable");
      return;
    }

    ExtractLocalRefactoring_OLD refactoring = new ExtractLocalRefactoring_OLD(
        SelectionConverter.getInputAsCompilationUnit(editor),
        selection.getOffset(),
        selection.getLength());
    new RefactoringStarter().activate(
        new ExtractLocalWizard(refactoring),
        getShell(),
        RefactoringMessages.ExtractLocalAction_dialog_title,
        RefactoringSaveHelper.SAVE_ALL);
    // TODO(scheglov) replace with SAVE_NOTHING, when parsing working copy will be fixed by Dan
  }
}
