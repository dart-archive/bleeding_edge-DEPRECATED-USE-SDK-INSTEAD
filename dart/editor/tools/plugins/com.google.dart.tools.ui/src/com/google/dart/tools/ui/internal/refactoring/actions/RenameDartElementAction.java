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
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.tools.internal.corext.refactoring.RefactoringExecutionStarter;
import com.google.dart.tools.ui.actions.AbstractDartSelectionAction;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.refactoring.reorg.RenameLinkedMode;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartSelection;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchSite;

/**
 * {@link Action} for renaming {@link Element}.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class RenameDartElementAction extends AbstractDartSelectionAction {
  private DartEditor editor;

  public RenameDartElementAction(DartEditor editor) {
    super(editor);
    this.editor = editor;
    setEnabled(SelectionConverter.canOperateOn(editor));
  }

  public RenameDartElementAction(IWorkbenchSite site) {
    super(site);
  }

  @Override
  public void selectionChanged(DartSelection selection) {
    Element element = getSelectionElement(selection);
    setEnabled(element != null);
  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
    Element element = getSelectionElement(selection);
    setEnabled(element != null);
  }

  @Override
  protected void doRun(DartSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    RenameLinkedMode activeLinkedMode = RenameLinkedMode.getActiveLinkedMode();
    if (activeLinkedMode != null) {
      activeLinkedMode.startFullDialog();
    } else {
      AssistContext context = selection.getContext();
      if (context == null) {
        return;
      }
      new RenameLinkedMode(editor, context).start();
    }
  }

  @Override
  protected void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    Element element = getSelectionElement(selection);
    if (element == null) {
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

  @Override
  protected void init() {
  }
}
