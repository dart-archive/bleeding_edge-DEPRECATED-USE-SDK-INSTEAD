package com.google.dart.tools.ui.actions;

import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.internal.corext.refactoring.RefactoringAvailabilityTester;
import com.google.dart.tools.internal.corext.refactoring.RefactoringExecutionStarter_OLD;
import com.google.dart.tools.internal.corext.refactoring.util.DartElementUtil;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.actions.ActionUtil;
import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartTextSelection;
import com.google.dart.tools.ui.internal.util.DartModelUtil;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

/**
 * Inlines a method.
 */
public class InlineMethodAction_OLD extends InstrumentedSelectionDispatchAction {

  private DartEditor fEditor;

  public InlineMethodAction_OLD(DartEditor editor) {
    this(editor.getEditorSite());
    fEditor = editor;
    setEnabled(SelectionConverter.canOperateOn(fEditor));
  }

  public InlineMethodAction_OLD(IWorkbenchSite site) {
    super(site);
    setText(RefactoringMessages.InlineMethodAction_inline_Method);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, DartHelpContextIds.INLINE_ACTION);
  }

  @Override
  public void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {

    try {
      Assert.isTrue(RefactoringAvailabilityTester.isInlineMethodAvailable(selection));
      Method method = (Method) selection.getFirstElement();
      SourceRange nameRange = method.getNameRange();
      instrumentation.data("MethodsName", method.getElementName());
      instrumentation.record(method.getCompilationUnit());

      run(nameRange.getOffset(), nameRange.getLength(), method.getCompilationUnit());
    } catch (DartModelException e) {
      ExceptionHandler.handle(
          e,
          getShell(),
          RefactoringMessages.InlineMethodAction_dialog_title,
          RefactoringMessages.InlineMethodAction_unexpected_exception);
    }
  }

  @Override
  public void doRun(ITextSelection selection, Event event, UIInstrumentationBuilder instrumentation) {
    DartElement element = SelectionConverter.getInput(fEditor);
    if (element == null) {
      instrumentation.metric("Problem", "Element was null");
      return;
    }
    CompilationUnit unit = element.getAncestor(CompilationUnit.class);
    if (unit == null) {
      instrumentation.metric("Problem", "CompilationUnit was null");
      return;
    }
    if (!DartElementUtil.isSourceAvailable(unit)) {
      instrumentation.metric("Problem", "SourceNotAvailable");
      return;
    }

    instrumentation.record(unit);
    run(selection.getOffset(), selection.getLength(), unit);
  }

  @Override
  public void selectionChanged(DartTextSelection selection) {
    try {
      setEnabled(RefactoringAvailabilityTester.isInlineMethodAvailable(selection));
    } catch (DartModelException e) {
      setEnabled(false);
    }
  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
    try {
      setEnabled(RefactoringAvailabilityTester.isInlineMethodAvailable(selection));
    } catch (DartModelException e) {
      if (DartModelUtil.isExceptionToBeLogged(e)) {
        DartToolsPlugin.log(e);
      }
    }
  }

  @Override
  public void selectionChanged(ITextSelection selection) {
    if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
      // TODO(scheglov)
      setEnabled(false);
    } else {
      setEnabled(true);
    }
  }

  public boolean tryInlineMethod(CompilationUnit unit, ITextSelection selection, Shell shell) {
    return RefactoringExecutionStarter_OLD.startInlineMethodRefactoring(
        unit,
        selection.getOffset(),
        selection.getLength(),
        shell);
  }

  private void run(int offset, int length, CompilationUnit unit) {
    if (!ActionUtil.isEditable(fEditor, getShell(), unit)) {
      return;
    }
    if (!RefactoringExecutionStarter_OLD.startInlineMethodRefactoring(
        unit,
        offset,
        length,
        getShell())) {
      MessageDialog.openInformation(
          getShell(),
          RefactoringMessages.InlineMethodAction_dialog_title,
          RefactoringMessages.InlineMethodAction_no_method_invocation_or_declaration_selected);
    }
  }
}
