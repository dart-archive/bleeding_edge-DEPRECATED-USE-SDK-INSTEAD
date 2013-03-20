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
package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.engine.element.Element;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.services.refactoring.RefactoringFactory;
import com.google.dart.engine.services.refactoring.RenameRefactoring;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.internal.refactoring.reorg.RenameRefactoringWizard;
import com.google.dart.tools.ui.internal.refactoring.reorg.RenameUserInterfaceStarter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.swt.widgets.Shell;

import java.lang.reflect.InvocationTargetException;

/**
 * Central access point to execute rename refactoring.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class RenameSupport {
  public static RenameSupport create(Element element, String newName) {
    SearchEngine searchEngine = DartCore.getProjectManager().newSearchEngine();
    RenameRefactoring refactoring = RefactoringFactory.createRenameRefactoring(
        searchEngine,
        element);
    if (refactoring == null) {
      return null;
    }
    return new RenameSupport(refactoring, newName);
  }

  //  private final org.eclipse.ltk.core.refactoring.Refactoring fRefactoring;
//  private final RenameRefactoring refactoring;
  private final org.eclipse.ltk.core.refactoring.Refactoring ltkRefactoring;

//  private RefactoringStatus fPreCheckStatus;

  private RenameSupport(RenameRefactoring refactoring, String newName) {
//    this.refactoring = refactoring;
    ltkRefactoring = new ServiceRenameRefactoring(refactoring);
    if (newName != null) {
      refactoring.setNewName(newName);
    }
  }

  /**
   * @return <code>true</code> if there are unresolved name references to the renaming
   *         {@link Element}, which may be OK, but may be not OK to change.
   */
  public boolean hasUnresolvedNameReferences() {
    // TODO(scheglov)
    return false;
//    return getDartRenameProcessor().hasUnresolvedNameReferences();
  }

  /**
   * Opens the refactoring dialog for this rename support.
   * 
   * @param parent a shell used as a parent for the refactoring dialog.
   * @throws CoreException if an unexpected exception occurs while opening the dialog.
   * @see #openDialog(Shell, boolean)
   */
  public void openDialog(Shell parent) throws CoreException {
    openDialog(parent, false);
  }

  /**
   * Opens the refactoring dialog for this rename support.
   * <p>
   * This method has to be called from within the UI thread.
   * </p>
   * 
   * @param parent a shell used as a parent for the refactoring, preview, or error dialog
   * @param showPreviewOnly if <code>true</code>, the dialog skips all user input pages and directly
   *          shows the preview or error page. Otherwise, shows all pages.
   * @return <code>true</code> if the refactoring has been executed successfully, <code>false</code>
   *         if it has been canceled or if an error has happened during initial conditions checking.
   * @throws CoreException if an error occurred while executing the operation.
   * @see #openDialog(Shell)
   */
  public boolean openDialog(Shell parent, boolean showPreviewOnly) throws CoreException {
//    ensureChecked();
    // TODO(scheglov)
//    if (fPreCheckStatus.hasFatalError()) {
//      showInformation(parent, fPreCheckStatus);
//      return false;
//    }

    UserInterfaceStarter starter;
    // TODO(scheglov)
    if (!showPreviewOnly) {
      starter = new RenameUserInterfaceStarter();
      RenameRefactoringWizard wizard = new RenameRefactoringWizard(
          ltkRefactoring,
          ltkRefactoring.getName(),
          null,
          null,
          null);
      wizard.setForcePreviewReview(showPreviewOnly);
      starter.initialize(wizard);
    } else {
      starter = new RenameUserInterfaceStarter();
      RenameRefactoringWizard wizard = new RenameRefactoringWizard(
          ltkRefactoring,
          ltkRefactoring.getName(),
          null,
          null,
          null) {
        @Override
        protected void addUserInputPages() {
          // nothing to add
        }
      };
      wizard.setForcePreviewReview(showPreviewOnly);
      starter.initialize(wizard);
    }
//  // TODO(scheglov)
    int saveMode = RefactoringSaveHelper.SAVE_ALL;
    return starter.activate(ltkRefactoring, parent, saveMode);
//    return starter.activate(ltkRefactoring, parent, getDartRenameProcessor().getSaveMode());
  }

//  /**
//   * Executes some light weight precondition checking. If the returned status is an error then the
//   * refactoring can't be executed at all. However, returning an OK status doesn't guarantee that
//   * the refactoring can be executed. It may still fail while performing the exhaustive precondition
//   * checking done inside the methods <code>openDialog</code> or <code>perform</code>. The method is
//   * mainly used to determine enable/disablement of actions.
//   * 
//   * @return the result of the light weight precondition checking.
//   * @throws CoreException if an unexpected exception occurs while performing the checking.
//   * @see #openDialog(Shell)
//   * @see #perform(Shell, IRunnableContext)
//   */
//  public IStatus preCheck() throws CoreException {
//    ensureChecked();
//    if (fPreCheckStatus.hasFatalError()) {
//      return fPreCheckStatus.getEntryMatchingSeverity(RefactoringStatus.FATAL).toStatus();
//    } else {
//      return Status.OK_STATUS;
//    }
//  }

//  private RenameSelectionState createSelectionState() {
//    RenameProcessor processor = (RenameProcessor) fRefactoring.getProcessor();
//    Object[] elements = processor.getElements();
//    RenameSelectionState state = elements.length == 1 ? new RenameSelectionState(elements[0])
//        : null;
//    return state;
//  }

//  private void ensureChecked() throws CoreException {
//    if (fPreCheckStatus == null) {
//      if (!fRefactoring.isApplicable()) {
//        fPreCheckStatus = RefactoringStatus.createFatalErrorStatus(DartUIMessages.RenameSupport_not_available);
//      } else {
//        fPreCheckStatus = new RefactoringStatus();
//      }
//    }
//  }

//  private DartRenameProcessor getDartRenameProcessor() {
//    return (DartRenameProcessor) fRefactoring.getProcessor();
//  }
//
//  private void restoreSelectionState(RenameSelectionState state) throws CoreException {
//    INameUpdating nameUpdating = (INameUpdating) fRefactoring.getAdapter(INameUpdating.class);
//    if (nameUpdating != null && state != null) {
//      Object newElement = nameUpdating.getNewElement();
//      if (newElement != null) {
//        state.restore(newElement);
//      }
//    }
//  }

  /**
   * Executes the rename refactoring without showing a dialog to gather additional user input (for
   * example the new name of the <tt>DartElement</tt>). Only an error dialog is shown (if necessary)
   * to present the result of the refactoring's full precondition checking.
   * <p>
   * The method has to be called from within the UI thread.
   * </p>
   * 
   * @param parent a shell used as a parent for the error dialog.
   * @param context a {@link IRunnableContext} to execute the operation.
   * @throws InterruptedException if the operation has been canceled by the user.
   * @throws InvocationTargetException if an error occurred while executing the operation.
   * @see #openDialog(Shell)
   * @see IRunnableContext#run(boolean, boolean, org.eclipse.jface.operation.IRunnableWithProgress)
   */
  public void perform(Shell parent, IRunnableContext context) throws InterruptedException,
      InvocationTargetException {
//    try {
    // TODO(scheglov)
//      ensureChecked();
//    if (fPreCheckStatus.hasFatalError()) {
//      showInformation(parent, fPreCheckStatus);
//      return;
//    }

    // TODO(scheglov)
//      RenameSelectionState state = createSelectionState();

//    // TODO(scheglov)
    int saveMode = RefactoringSaveHelper.SAVE_ALL;
    RefactoringExecutionHelper helper = new RefactoringExecutionHelper(
        ltkRefactoring,
        RefactoringCore.getConditionCheckingFailedSeverity(),
        saveMode,
        parent,
        context);
    helper.perform(true, true);

    // TODO(scheglov)
//      restoreSelectionState(state);
//    } catch (CoreException e) {
//      throw new InvocationTargetException(e);
//    }
  }

//  private void showInformation(Shell parent, RefactoringStatus status) {
//    String message = status.getMessageMatchingSeverity(RefactoringStatus.FATAL);
//    UserInteractions.openInformation.open(
//        parent,
//        DartUIMessages.RenameSupport_dialog_title,
//        message);
//  }
}
