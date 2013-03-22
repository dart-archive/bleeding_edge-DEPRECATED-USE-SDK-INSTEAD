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

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.internal.corext.refactoring.RefactoringAvailabilityTester;
import com.google.dart.tools.internal.corext.refactoring.RefactoringExecutionStarter_OLD;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartTextSelection;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

/**
 * Inlines the value of a local variable at all places where a read reference is used.
 */
public class InlineLocalAction_OLD extends InstrumentedSelectionDispatchAction {

  private DartEditor fEditor;

  public InlineLocalAction_OLD(DartEditor editor) {
    this(editor.getEditorSite());
    fEditor = editor;
    setEnabled(SelectionConverter.canOperateOn(fEditor));
  }

  InlineLocalAction_OLD(IWorkbenchSite site) {
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

  @Override
  public void selectionChanged(DartTextSelection selection) {
    try {
      setEnabled(RefactoringAvailabilityTester.isInlineTempAvailable(selection));
    } catch (DartModelException e) {
      setEnabled(false);
    }
  }

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
      CompilationUnit unit = SelectionConverter.getInputAsCompilationUnit(fEditor);
      ITextSelection selection = (ITextSelection) fEditor.getSelectionProvider().getSelection();
      return RefactoringExecutionStarter_OLD.startInlineTempRefactoring(unit, selection, shell);
    } catch (Throwable e) {
      DartToolsPlugin.log(e);
      return false;
    }
  }
}
