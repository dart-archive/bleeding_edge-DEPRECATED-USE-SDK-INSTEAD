package com.google.dart.tools.ui.internal.refactoring.actions;

import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.internal.corext.refactoring.RefactoringAvailabilityTester;
import com.google.dart.tools.internal.corext.refactoring.RefactoringExecutionStarter;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.actions.SelectionDispatchAction;
import com.google.dart.tools.ui.internal.actions.ActionUtil;
import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.refactoring.reorg.RenameLinkedMode;
import com.google.dart.tools.ui.internal.text.editor.CompilationUnitEditor;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartTextSelection;
import com.google.dart.tools.ui.internal.util.DartModelUtil;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;

public class RenameDartElementAction extends SelectionDispatchAction {

  private static boolean canEnable(IStructuredSelection selection) throws CoreException {
    DartElement element = getDartElement(selection);
    if (element == null) {
      return false;
    }
    return RefactoringAvailabilityTester.isRenameElementAvailable(element);
  }

  private static DartElement getDartElement(IStructuredSelection selection) {
    if (selection.size() != 1) {
      return null;
    }
    Object first = selection.getFirstElement();
    if (!(first instanceof DartElement)) {
      return null;
    }
    return (DartElement) first;
  }

  private DartEditor fEditor;

  public RenameDartElementAction(DartEditor editor) {
    this(editor.getEditorSite());
    fEditor = editor;
    setEnabled(SelectionConverter.canOperateOn(fEditor));
  }

  public RenameDartElementAction(IWorkbenchSite site) {
    super(site);
  }

  public boolean canRunInEditor() {
    // TODO(scheglov) linked mode support
    if (RenameLinkedMode.getActiveLinkedMode() != null) {
      return true;
    }

    try {
      DartElement element = getDartElementFromEditor();
      if (element == null) {
        return true;
      }

      return RefactoringAvailabilityTester.isRenameElementAvailable(element);
    } catch (DartModelException e) {
      if (DartModelUtil.isExceptionToBeLogged(e)) {
        DartToolsPlugin.log(e);
      }
    } catch (CoreException e) {
      DartToolsPlugin.log(e);
    }
    return false;
  }

  public void doRun() {
    // TODO(scheglov) linked mode support
    RenameLinkedMode activeLinkedMode = RenameLinkedMode.getActiveLinkedMode();
    if (activeLinkedMode != null) {
      if (activeLinkedMode.isCaretInLinkedPosition()) {
        activeLinkedMode.startFullDialog();
        return;
      } else {
        activeLinkedMode.cancel();
      }
    }

    try {
      DartElement element = getDartElementFromEditor();
      IPreferenceStore store = DartToolsPlugin.getDefault().getPreferenceStore();
      boolean lightweight = store.getBoolean(PreferenceConstants.REFACTOR_LIGHTWEIGHT);
      if (element != null && RefactoringAvailabilityTester.isRenameElementAvailable(element)) {
        run(element, lightweight);
        return;
      }
      // TODO(scheglov) linked mode support
//      else if (lightweight) {
//        // fall back to local rename:
//        CorrectionCommandHandler handler =
//            new CorrectionCommandHandler(fEditor, LinkedNamesAssistProposal.ASSIST_ID, true);
//        if (handler.doExecute()) {
//          fEditor.setStatusLineErrorMessage(RefactoringMessages.RenameJavaElementAction_started_rename_in_file);
//          return;
//        }
//      }
    } catch (CoreException e) {
      ExceptionHandler.handle(e, RefactoringMessages.RenameJavaElementAction_name,
          RefactoringMessages.RenameJavaElementAction_exception);
    }
    MessageDialog.openInformation(getShell(), RefactoringMessages.RenameJavaElementAction_name,
        RefactoringMessages.RenameJavaElementAction_not_available);
  }

  @Override
  public void run(IStructuredSelection selection) {
    DartElement element = getDartElement(selection);
    if (element == null) {
      return;
    }
    if (!ActionUtil.isEditable(getShell(), element)) {
      return;
    }
    try {
      run(element, false);
    } catch (CoreException e) {
      ExceptionHandler.handle(e, RefactoringMessages.RenameJavaElementAction_name,
          RefactoringMessages.RenameJavaElementAction_exception);
    }
  }

  @Override
  public void run(ITextSelection selection) {
    if (!ActionUtil.isEditable(fEditor)) {
      return;
    }
    if (canRunInEditor()) {
      doRun();
    } else {
      MessageDialog.openInformation(getShell(), RefactoringMessages.RenameAction_rename,
          RefactoringMessages.RenameAction_unavailable);
    }
  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
    try {
      if (selection.size() == 1) {
        setEnabled(canEnable(selection));
        return;
      }
    } catch (DartModelException e) {
      // http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
      if (DartModelUtil.isExceptionToBeLogged(e)) {
        DartToolsPlugin.log(e);
      }
    } catch (CoreException e) {
      DartToolsPlugin.log(e);
    }
    setEnabled(false);
  }

  @Override
  public void selectionChanged(ITextSelection selection) {
    if (selection instanceof DartTextSelection) {
      try {
        DartTextSelection dartTextSelection = (DartTextSelection) selection;
        DartElement[] elements = dartTextSelection.resolveElementAtOffset();
        if (elements.length == 1) {
          setEnabled(RefactoringAvailabilityTester.isRenameElementAvailable(elements[0]));
        } else {
          DartNode node = dartTextSelection.resolveCoveringNode();
          setEnabled(node instanceof DartIdentifier);
        }
      } catch (CoreException e) {
        setEnabled(false);
      }
    } else {
      setEnabled(true);
    }
  }

  private DartElement getDartElementFromEditor() throws DartModelException {
    DartElement[] elements = SelectionConverter.codeResolve(fEditor);
    if (elements == null || elements.length != 1) {
      return null;
    }
    return elements[0];
  }

  private void run(DartElement element, boolean lightweight) throws CoreException {
    // Work around for http://dev.eclipse.org/bugs/show_bug.cgi?id=19104
    if (!ActionUtil.isEditable(fEditor, getShell(), element)) {
      return;
    }
    // Workaround bug 31998
    if (ActionUtil.mustDisableDartModelAction(getShell(), element)) {
      return;
    }

    // TODO(scheglov) linked mode support
//    RefactoringExecutionStarter.startRenameRefactoring(element, getShell());
    if (lightweight && fEditor instanceof CompilationUnitEditor) {
      new RenameLinkedMode(element, (CompilationUnitEditor) fEditor).start();
    } else {
      RefactoringExecutionStarter.startRenameRefactoring(element, getShell());
    }
  }
}
