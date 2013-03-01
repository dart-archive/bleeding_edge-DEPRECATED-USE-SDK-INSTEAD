package com.google.dart.tools.ui.actions;

import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.internal.corext.refactoring.RefactoringExecutionStarter;
import com.google.dart.tools.internal.corext.refactoring.RefactoringExecutionStarter_OLD;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

/**
 * Inlines the value of a local variable at all places where a read reference is used.
 */
public class InlineLocalAction extends InstrumentedSelectionDispatchAction {

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

//  @Override
//  public void doRun(IStructuredSelection selection, Event event,
//      UIInstrumentationBuilder instrumentation) {
//    instrumentation.metric("Problem", "InlineLocal called on StructuredSelection");
//    //do nothing
//  }
//
//  @Override
//  public void doRun(ITextSelection selection, Event event, UIInstrumentationBuilder instrumentation) {
//    if (!ActionUtil.isEditable(fEditor)) {
//      instrumentation.metric("Problem", "Editor not editable");
//      return;
//    }
//    try {
//      AssistContext assistContext = fEditor.getAssistContext();
//      RefactoringExecutionStarter.startInlineTempRefactoring(assistContext, getShell());
//    } catch (Throwable e) {
//      DartToolsPlugin.log(e);
//    }
//  }

  // TODO(scheglov) may be
//  @Override
//  public void selectionChanged(DartTextSelection selection) {
//    try {
//      setEnabled(RefactoringAvailabilityTester.isInlineTempAvailable(selection));
//    } catch (DartModelException e) {
//      setEnabled(false);
//    }
//  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
    setEnabled(false);
  }

  @Override
  public void selectionChanged(ITextSelection selection) {
    setEnabled(true);
  }

  public boolean tryInlineTemp(Shell shell) {
    try {
      if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
        AssistContext assistContext = fEditor.getAssistContext();
        return RefactoringExecutionStarter.startInlineTempRefactoring(assistContext, getShell());
      } else {
        CompilationUnit unit = SelectionConverter.getInputAsCompilationUnit(fEditor);
        ITextSelection selection = (ITextSelection) fEditor.getSelectionProvider().getSelection();
        return RefactoringExecutionStarter_OLD.startInlineTempRefactoring(unit, selection, shell);
      }
    } catch (Throwable e) {
      DartToolsPlugin.log(e);
      return false;
    }
  }
}
