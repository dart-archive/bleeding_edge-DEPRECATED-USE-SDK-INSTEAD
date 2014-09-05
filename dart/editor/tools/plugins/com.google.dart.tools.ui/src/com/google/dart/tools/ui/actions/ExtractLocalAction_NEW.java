/*
 * Copyright (c) 2014, the Dart project authors.
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

import com.google.dart.server.generated.types.RefactoringKind;
import com.google.dart.tools.ui.internal.refactoring.ExtractLocalWizard_NEW;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.refactoring.RefactoringSaveHelper;
import com.google.dart.tools.ui.internal.refactoring.ServerExtractLocalRefactoring;
import com.google.dart.tools.ui.internal.refactoring.actions.RefactoringStarter;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.PlatformUI;

/**
 * {@link Action} for "Extract Local" refactoring.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class ExtractLocalAction_NEW extends AbstractRefactoringAction_NEW {
//  private ServerExtractLocalRefactoring refactoring;

  public ExtractLocalAction_NEW(DartEditor editor) {
    super(editor, RefactoringKind.EXTRACT_LOCAL_VARIABLE);
  }

  @Override
  public void run() {
    // prepare refactoring
//    refactoring = null;
//    Control focusControl = Display.getCurrent().getFocusControl();
//    try {
//      IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
//      progressService.busyCursorWhile(new IRunnableWithProgress() {
//        @Override
//        public void run(IProgressMonitor pm) throws InterruptedException {
//          final CountDownLatch latch = new CountDownLatch(1);
//          ExtractLocalVariableOptions options = null;
//          DartCore.getAnalysisServer().edit_getRefactoring(
//              RefactoringKind.EXTRACT_LOCAL_VARIABLE,
//              file,
//              selectionOffset,
//              selectionLength,
//              false,
//              options,
//              new GetRefactoringConsumer() {
//                @Override
//                public void computedRefactorings(List<RefactoringProblem> problems,
//                    RefactoringFeedback feedback, SourceChange change, List<String> potentialEdits) {
//                  System.out.println("computedRefactorings: " + change);
//                }
//              });
//          while (true) {
//            if (pm.isCanceled()) {
//              throw new InterruptedException();
//            }
//            if (Uninterruptibles.awaitUninterruptibly(latch, 10, TimeUnit.MILLISECONDS)) {
//              break;
//            }
//          }
//        }
//      });
//    } catch (Throwable e) {
//      return;
//    } finally {
//      if (focusControl != null) {
//        focusControl.setFocus();
//      }
//    }
//    if (refactoring == null) {
//      return;
//    }
    // open dialog
    ServerExtractLocalRefactoring refactoring = new ServerExtractLocalRefactoring(
        file,
        selectionOffset,
        selectionLength);
    try {
      new RefactoringStarter().activate(
          new ExtractLocalWizard_NEW(refactoring),
          getShell(),
          RefactoringMessages.ExtractLocalAction_dialog_title,
          RefactoringSaveHelper.SAVE_NOTHING);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

//  @Override
//  public void run() {
//    RefactoringOptions options = new ExtractLocalVariableOptions("res", true);
//    DartCore.getAnalysisServer().edit_getRefactoring(
//        RefactoringKind.EXTRACT_LOCAL_VARIABLE,
//        file,
//        selectionOffset,
//        selectionLength,
//        false,
//        options,
//        new GetRefactoringConsumer() {
//          @Override
//          public void computedRefactorings(List<RefactoringProblem> problems,
//              RefactoringFeedback feedback, SourceChange change, List<String> potentialEdits) {
//            System.out.println("computedRefactorings: " + change);
//          }
//        });
//  }

  @Override
  public void selectionChanged(SelectionChangedEvent event) {
    super.selectionChanged(event);
    // TODO
    setEnabled(true);
  }

//  @Override
//  protected void doRun(DartSelection selection, Event event,
//      UIInstrumentationBuilder instrumentation) {
//    // TODO(scheglov) restore or remove for the new API
//    final String contextId = selection.getEditor().getInputAnalysisContextId();
//    final Source source = selection.getEditor().getInputSource();
//    if (contextId == null || source == null) {
//      return;
//    }
//    final int offset = selection.getOffset();
//    final int length = selection.getLength();
//    // prepare refactoring
//    refactoring = null;
//    Control focusControl = Display.getCurrent().getFocusControl();
//    try {
//      IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
//      progressService.busyCursorWhile(new IRunnableWithProgress() {
//        @Override
//        public void run(IProgressMonitor pm) throws InterruptedException {
//          final CountDownLatch latch = new CountDownLatch(1);
//          DartCore.getAnalysisServer().createRefactoringExtractLocal(
//              contextId,
//              source,
//              offset,
//              length,
//              new RefactoringExtractLocalConsumer() {
//                @Override
//                public void computed(String refactoringId, RefactoringStatus status,
//                    boolean hasSeveralOccurrences, String[] proposedNames) {
//                  refactoring = new ServerExtractLocalRefactoring(
//                      refactoringId,
//                      status,
//                      hasSeveralOccurrences,
//                      proposedNames);
//                  latch.countDown();
//                }
//              });
//          while (true) {
//            if (pm.isCanceled()) {
//              throw new InterruptedException();
//            }
//            if (Uninterruptibles.awaitUninterruptibly(latch, 10, TimeUnit.MILLISECONDS)) {
//              break;
//            }
//          }
//        }
//      });
//    } catch (Throwable e) {
//      return;
//    } finally {
//      if (focusControl != null) {
//        focusControl.setFocus();
//      }
//    }
//    if (refactoring == null) {
//      return;
//    }
//    // open dialog
//    try {
//      new RefactoringStarter().activate(
//          new ExtractLocalWizard_NEW(refactoring),
//          getShell(),
//          RefactoringMessages.ExtractLocalAction_dialog_title,
//          RefactoringSaveHelper.SAVE_NOTHING);
//    } catch (Throwable e) {
//      ExceptionHandler.handle(
//          e,
//          "Extract Local",
//          "Unexpected exception occurred. See the error log for more details.");
//    }
//  }

  @Override
  protected void init() {
    setText(RefactoringMessages.ExtractLocalAction_label);
    {
      String id = DartEditorActionDefinitionIds.EXTRACT_LOCAL_VARIABLE;
      setId(id);
      setActionDefinitionId(id);
    }
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, DartHelpContextIds.EXTRACT_LOCAL_ACTION);
  }
}
