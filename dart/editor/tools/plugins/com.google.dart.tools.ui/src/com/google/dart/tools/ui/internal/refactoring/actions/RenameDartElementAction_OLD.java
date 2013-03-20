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
package com.google.dart.tools.ui.internal.refactoring.actions;

import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.internal.corext.refactoring.RefactoringAvailabilityTester;
import com.google.dart.tools.internal.corext.refactoring.RefactoringExecutionStarter_OLD;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.actions.ActionInstrumentationUtilities;
import com.google.dart.tools.ui.actions.InstrumentedSelectionDispatchAction;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.actions.ActionUtil;
import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.refactoring.reorg.RenameLinkedMode_OLD;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchSite;

/**
 * @coverage dart.editor.ui.refactoring.ui
 */
public class RenameDartElementAction_OLD extends InstrumentedSelectionDispatchAction {

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

  public RenameDartElementAction_OLD(DartEditor editor) {
    this(editor.getEditorSite());
    fEditor = editor;
    setEnabled(SelectionConverter.canOperateOn(fEditor));
  }

  public RenameDartElementAction_OLD(IWorkbenchSite site) {
    super(site);
  }

  public boolean canRunInEditor() {
    if (RenameLinkedMode_OLD.getActiveLinkedMode() != null) {
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

  @Override
  public void doRun(Event event, UIInstrumentationBuilder instrumentation) {
    RenameLinkedMode_OLD activeLinkedMode = RenameLinkedMode_OLD.getActiveLinkedMode();
    if (activeLinkedMode != null) {
      if (activeLinkedMode.isCaretInLinkedPosition()) {
        instrumentation.metric("ActiveLinkedMode", "CaretInLinkedPosition");
        activeLinkedMode.startFullDialog();
        return;
      } else {
        activeLinkedMode.cancel();
      }
    }

    try {
      DartElement element = getDartElementFromEditor();
      ActionInstrumentationUtilities.recordElement(element, instrumentation);
      IPreferenceStore store = DartToolsPlugin.getDefault().getPreferenceStore();
      boolean lightweight = store.getBoolean(PreferenceConstants.REFACTOR_LIGHTWEIGHT);
      if (element != null && RefactoringAvailabilityTester.isRenameElementAvailable(element)) {
        run(element, lightweight);
        return;
      }
    } catch (CoreException e) {
      ExceptionHandler.handle(
          e,
          RefactoringMessages.RenameDartElementAction_name,
          RefactoringMessages.RenameDartElementAction_exception);
    }
    instrumentation.metric("Problem", "Rename Action not valid here, showing dialog");
    MessageDialog.openInformation(
        getShell(),
        RefactoringMessages.RenameDartElementAction_name,
        RefactoringMessages.RenameDartElementAction_not_available);
  }

  @Override
  public void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    DartElement element = getDartElement(selection);
    if (element == null) {
      instrumentation.metric("Problem", "Element was null");
      return;
    }
    ActionInstrumentationUtilities.recordElement(element, instrumentation);

    if (!ActionUtil.isEditable(getShell(), element)) {
      instrumentation.metric("Problem", "Editor not editable");
      return;
    }
    try {
      run(element, false);
    } catch (CoreException e) {
      ExceptionHandler.handle(
          e,
          RefactoringMessages.RenameDartElementAction_name,
          RefactoringMessages.RenameDartElementAction_exception);
    }
  }

  @Override
  public void doRun(ITextSelection selection, Event event, UIInstrumentationBuilder instrumentation) {
    if (!ActionUtil.isEditable(fEditor)) {
      instrumentation.metric("Problem", "Editor not editable");
      return;
    }
    if (canRunInEditor()) {
      doRun(event, instrumentation);
    } else {
      instrumentation.metric("Problem", "Rename Action not valid here, showing dialog");
      MessageDialog.openInformation(
          getShell(),
          RefactoringMessages.RenameAction_rename,
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

    if (lightweight && fEditor instanceof CompilationUnitEditor) {
      new RenameLinkedMode_OLD(element, (CompilationUnitEditor) fEditor).start();
    } else {
      RefactoringExecutionStarter_OLD.startRenameRefactoring(element, getShell());
    }
  }
}
