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
package com.google.dart.tools.ui.actions;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.ui.internal.actions.ActionUtil;
import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

/**
 * Inlines a method, local variable or a static final field.
 */
public class InlineAction extends SelectionDispatchAction {

  private DartEditor fEditor;
  private final InlineLocalAction fInlineTemp;
  private final InlineMethodAction fInlineMethod;

//  private final InlineConstantAction fInlineConstant;

  public InlineAction(DartEditor editor) {
    //don't want to call 'this' here - it'd create useless action objects
    super(editor.getEditorSite());
    setText(RefactoringMessages.InlineAction_Inline);
    fEditor = editor;
    fInlineTemp = new InlineLocalAction(editor);
    fInlineMethod = new InlineMethodAction(editor);
//    fInlineConstant = new InlineConstantAction(editor);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, DartHelpContextIds.INLINE_ACTION);
    setEnabled(SelectionConverter.getInputAsCompilationUnit(fEditor) != null);
  }

  public InlineAction(IWorkbenchSite site) {
    super(site);
    setText(RefactoringMessages.InlineAction_Inline);
    fInlineTemp = new InlineLocalAction(site);
    fInlineMethod = new InlineMethodAction(site);
//    fInlineConstant = new InlineConstantAction(site);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, DartHelpContextIds.INLINE_ACTION);
  }

  @Override
  public void run(IStructuredSelection selection) {
//    if (fInlineConstant.isEnabled()) {
//      fInlineConstant.run(selection);
//    } else
    if (fInlineMethod.isEnabled()) {
      fInlineMethod.run(selection);
    } else {
      //inline temp will never be enabled on IStructuredSelection
      //don't bother running it
      Assert.isTrue(!fInlineTemp.isEnabled());
    }
  }

  @Override
  public void run(ITextSelection selection) {
    if (!ActionUtil.isEditable(fEditor)) {
      return;
    }

    // TODO(scheglov)
    CompilationUnit cu = SelectionConverter.getInputAsCompilationUnit(fEditor);
    if (fInlineTemp.isEnabled() && fInlineTemp.tryInlineTemp(cu, null, selection, getShell())) {
      return;
    }

//    ITypeRoot typeRoot = SelectionConverter.getInput(fEditor);
//    if (typeRoot == null) {
//      return;
//    }
//
//    CompilationUnit node = RefactoringASTParser.parseWithASTProvider(typeRoot, true, null);
//
//    if (typeRoot instanceof ICompilationUnit) {
//      ICompilationUnit cu = (ICompilationUnit) typeRoot;
//      if (fInlineTemp.isEnabled() && fInlineTemp.tryInlineTemp(cu, node, selection, getShell())) {
//        return;
//      }
//
//      if (fInlineConstant.isEnabled()
//          && fInlineConstant.tryInlineConstant(cu, node, selection, getShell())) {
//        return;
//      }
//    }

    //InlineMethod is last (also tries enclosing element):
    if (fInlineMethod.isEnabled() && fInlineMethod.tryInlineMethod(cu, selection, getShell())) {
      return;
    }
//    if (fInlineMethod.isEnabled()
//        && fInlineMethod.tryInlineMethod(typeRoot, node, selection, getShell())) {
//      return;
//    }

    MessageDialog.openInformation(
        getShell(),
        RefactoringMessages.InlineAction_dialog_title,
        RefactoringMessages.InlineAction_select);
  }

  @Override
  public void selectionChanged(ISelection selection) {
    fInlineTemp.update(selection);
    fInlineMethod.update(selection);
//    fInlineConstant.update(selection);
    setEnabled(fInlineTemp.isEnabled() || fInlineMethod.isEnabled() /*|| fInlineConstant.isEnabled()*/);
  }
}
