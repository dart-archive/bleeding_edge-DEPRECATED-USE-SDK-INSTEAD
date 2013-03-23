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
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.actions.ActionUtil;
import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

/**
 * Inlines a method, local variable or a static final field.
 */
public class InlineAction_OLD extends InstrumentedSelectionDispatchAction {

  private DartEditor fEditor;
  private final InlineLocalAction_OLD fInlineTemp;
  private final InlineMethodAction_OLD fInlineMethod;

//  private final InlineConstantAction fInlineConstant;

  public InlineAction_OLD(DartEditor editor) {
    //don't want to call 'this' here - it'd create useless action objects
    super(editor.getEditorSite());
    setText(RefactoringMessages.InlineAction_Inline);
    fEditor = editor;
    fInlineTemp = new InlineLocalAction_OLD(editor);
    fInlineMethod = new InlineMethodAction_OLD(editor);
//    fInlineConstant = new InlineConstantAction(editor);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, DartHelpContextIds.INLINE_ACTION);
    setEnabled(SelectionConverter.getInputAsCompilationUnit(fEditor) != null);
  }

  public InlineAction_OLD(IWorkbenchSite site) {
    super(site);
    setText(RefactoringMessages.InlineAction_Inline);
    fInlineTemp = new InlineLocalAction_OLD(site);
    fInlineMethod = new InlineMethodAction_OLD(site);
//    fInlineConstant = new InlineConstantAction(site);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, DartHelpContextIds.INLINE_ACTION);
  }

  @Override
  public void doRun(ISelection selection, Event event, UIInstrumentationBuilder instrumentation) {
//    if (fInlineConstant.isEnabled()) {
//      fInlineConstant.run(selection);
//    } else
    if (fInlineMethod.isEnabled()) {
      fInlineMethod.doRun(selection, event, instrumentation);
    } else {
      instrumentation.metric("Problem", "InlineMethodAction not enabled");
    }
  }

  @Override
  public void doRun(ITextSelection selection, Event event, UIInstrumentationBuilder instrumentation) {
    if (!ActionUtil.isEditable(fEditor)) {
      instrumentation.metric("Problem", "Editor not editable");
      return;
    }

    // TODO(scheglov)
    if (fInlineTemp.isEnabled() && fInlineTemp.tryInlineTemp(getShell())) {
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
    CompilationUnit cu = SelectionConverter.getInputAsCompilationUnit(fEditor);
    if (fInlineMethod.isEnabled() && fInlineMethod.tryInlineMethod(cu, selection, getShell())) {
      return;
    }
//    if (fInlineMethod.isEnabled()
//        && fInlineMethod.tryInlineMethod(typeRoot, node, selection, getShell())) {
//      return;
//    }

    instrumentation.metric("Problem", "No valid selection, showing dialog");
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
