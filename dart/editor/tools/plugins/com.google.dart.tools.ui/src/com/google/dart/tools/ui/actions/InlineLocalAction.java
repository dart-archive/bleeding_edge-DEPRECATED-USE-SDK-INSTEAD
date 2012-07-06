package com.google.dart.tools.ui.actions;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.internal.corext.refactoring.RefactoringAvailabilityTester;
import com.google.dart.tools.internal.corext.refactoring.RefactoringExecutionStarter;
import com.google.dart.tools.ui.internal.actions.ActionUtil;
import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartTextSelection;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

/**
 * Inlines the value of a local variable at all places where a read reference is used.
 */
public class InlineLocalAction extends SelectionDispatchAction {

  private DartEditor fEditor;

  public InlineLocalAction(DartEditor editor) {
    this(editor.getEditorSite());
    fEditor = editor;
    setEnabled(SelectionConverter.canOperateOn(fEditor));
  }

  InlineLocalAction(IWorkbenchSite site) {
    super(site);
    setText(RefactoringMessages.InlineLocalAction_label);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, DartHelpContextIds.INLINE_ACTION);
  }

  @Override
  public void run(IStructuredSelection selection) {
    //do nothing
  }

  @Override
  public void run(ITextSelection selection) {
    CompilationUnit input = SelectionConverter.getInputAsCompilationUnit(fEditor);
    if (!ActionUtil.isEditable(fEditor)) {
      return;
    }
    RefactoringExecutionStarter.startInlineTempRefactoring(input, null, selection, getShell());
  }

  @Override
  public void selectionChanged(DartTextSelection selection) {
    try {
      setEnabled(RefactoringAvailabilityTester.isInlineTempAvailable(selection));
    } catch (DartModelException e) {
      setEnabled(false);
    }
  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
    setEnabled(false);
  }

  @Override
  public void selectionChanged(ITextSelection selection) {
    setEnabled(true);
  }

  boolean tryInlineTemp(CompilationUnit unit, DartUnit node, ITextSelection selection, Shell shell) {
    return RefactoringExecutionStarter.startInlineTempRefactoring(unit, node, selection, shell);
  }
}
