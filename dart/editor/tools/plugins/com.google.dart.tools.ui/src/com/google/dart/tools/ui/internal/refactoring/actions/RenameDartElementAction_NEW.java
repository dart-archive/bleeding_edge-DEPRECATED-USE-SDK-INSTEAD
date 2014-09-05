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

import com.google.dart.engine.element.Element;
import com.google.dart.server.generated.types.RefactoringKind;
import com.google.dart.tools.internal.corext.refactoring.RefactoringExecutionStarter;
import com.google.dart.tools.ui.actions.AbstractRefactoringAction_NEW;
import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.refactoring.RefactoringUtils;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.jface.action.Action;

/**
 * {@link Action} for renaming {@link Element}.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class RenameDartElementAction_NEW extends AbstractRefactoringAction_NEW {
  public RenameDartElementAction_NEW(DartEditor editor) {
    super(editor, RefactoringKind.RENAME);
    setEnabled(SelectionConverter.canOperateOn(editor));
  }

//  @Override
//  public void selectionChanged(DartSelection selection) {
//    // cannot operate on this editor
//    if (!canOperateOn()) {
//      setEnabled(false);
//      return;
//    }
//    // validate element
//    Element element = getElementToRename(selection);
//    setEnabled(element != null);
//  }
//
//  @Override
//  public void selectionChanged(IStructuredSelection selection) {
//    Element element = getSelectionElement(selection);
//    setEnabled(element != null);
//  }
//
//  @Override
//  protected void doRun(DartSelection selection, Event event,
//      UIInstrumentationBuilder instrumentation) {
//    AssistContext context = getContextAfterBuild(selection);
//    if (context == null) {
//      return;
//    }
//    // prepare element
//    Element element = getElementToRename(selection);
//    if (element == null) {
//      return;
//    }
//    // Always rename using dialog. Eclipse implementation of the linked mode is very slow
//    // in case of the many occurrences in the single file. It is also done using asynchronous
//    // execution, in these 1-2-3 seconds user can type anything and damage source.
//    renameUsingDialog(element);
//  }
//
//  @Override
//  protected void doRun(IStructuredSelection selection, Event event,
//      UIInstrumentationBuilder instrumentation) {
//    Element element = getSelectionElement(selection);
//    renameUsingDialog(element);
//  }

  @Override
  public boolean isEnabled() {
    return super.isEnabled();
  }

  @Override
  public void run() {
    // TODO(scheglov)
    System.out.println("run!");
  }

  @Override
  protected void init() {
  }

  private void renameUsingDialog(Element element) {
    if (element == null) {
      return;
    }
    if (!RefactoringUtils.waitReadyForRefactoring()) {
      return;
    }
    try {
      RefactoringExecutionStarter.startRenameRefactoring(element, getShell());
    } catch (Throwable e) {
      ExceptionHandler.handle(
          e,
          RefactoringMessages.RenameDartElementAction_name,
          RefactoringMessages.RenameDartElementAction_exception);
    }
  }
}
