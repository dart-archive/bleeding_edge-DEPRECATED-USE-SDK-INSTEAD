/*
 * Copyright (c) 2013, the Dart project authors.
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

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.refactoring.RefactoringFactory;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.refactoring.ExtractMethodWizard;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.refactoring.RefactoringSaveHelper;
import com.google.dart.tools.ui.internal.refactoring.ServiceExtractMethodRefactoring;
import com.google.dart.tools.ui.internal.refactoring.actions.RefactoringStarter;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartSelection;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PlatformUI;

/**
 * {@link Action} for "Extract Method" refactoring.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class ExtractMethodAction extends AbstractRefactoringAction_OLD {
  public ExtractMethodAction(DartEditor editor) {
    super(editor);
  }

  @Override
  public void selectionChanged(DartSelection selection) {
    // cannot operate on this editor
    if (!canOperateOn()) {
      setEnabled(false);
      return;
    }
    // empty selection
    if (selection.getLength() == 0) {
      setEnabled(false);
      return;
    }
    // prepare context
    AssistContext context = selection.getContext();
    if (context == null) {
      setEnabled(false);
      return;
    }
    // prepare covered node
    AstNode coveredNode = context.getCoveredNode();
    if (coveredNode == null) {
      setEnabled(false);
      return;
    }
    // selection should be inside of executable node
    if (coveredNode.getAncestor(Block.class) == null) {
      setEnabled(false);
      return;
    }
    // OK
    setEnabled(true);
  }

  @Override
  protected void doRun(DartSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    AssistContext context = getContextAfterBuild(selection);
    try {
      com.google.dart.engine.services.refactoring.ExtractMethodRefactoring newRefactoring = RefactoringFactory.createExtractMethodRefactoring(context);
      ServiceExtractMethodRefactoring ltkRefactoring = new ServiceExtractMethodRefactoring(
          newRefactoring);
      new RefactoringStarter().activate(
          new ExtractMethodWizard(ltkRefactoring),
          getShell(),
          RefactoringMessages.ExtractMethodAction_dialog_title,
          RefactoringSaveHelper.SAVE_NOTHING);
      // TODO(scheglov) may be SAVE_NOTHING
    } catch (Throwable e) {
      ExceptionHandler.handle(
          e,
          "Extract Local",
          "Unexpected exception occurred. See the error log for more details.");
    }
  }

  @Override
  protected void init() {
    setText(RefactoringMessages.ExtractMethodAction_label);
    {
      String id = DartEditorActionDefinitionIds.EXTRACT_METHOD;
      setId(id);
      setActionDefinitionId(id);
    }
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.EXTRACT_METHOD_ACTION);
  }
}
