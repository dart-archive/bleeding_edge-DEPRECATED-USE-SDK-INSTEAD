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

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.element.Element;
import com.google.dart.tools.internal.corext.refactoring.RefactoringExecutionStarter;
import com.google.dart.tools.ui.actions.ActionInstrumentationUtilities;
import com.google.dart.tools.ui.actions.InstrumentedSelectionDispatchAction;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.actions.ActionUtil;
import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.refactoring.reorg.RenameLinkedMode;
import com.google.dart.tools.ui.internal.text.editor.CompilationUnitEditor;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchSite;

/**
 * {@link Action} for renaming {@link Element}.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class RenameDartElementAction extends InstrumentedSelectionDispatchAction implements
    RenameDartElementAction_I {

  private static Element getElement(IStructuredSelection selection) {
    if (selection.size() != 1) {
      return null;
    }
    Object first = selection.getFirstElement();
    if (!(first instanceof Element)) {
      return null;
    }
    return (Element) first;
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

  @Override
  public void doRun(Event event, UIInstrumentationBuilder instrumentation) {
    // may be linked mode rename, open dialog or cancel 
    RenameLinkedMode activeLinkedMode = RenameLinkedMode.getActiveLinkedMode();
    if (activeLinkedMode != null) {
      if (activeLinkedMode.isCaretInLinkedPosition()) {
        instrumentation.metric("ActiveLinkedMode", "CaretInLinkedPosition");
        activeLinkedMode.startFullDialog();
        return;
      } else {
        activeLinkedMode.cancel();
      }
    }
    // dispatch depending on selection
    super.doRun(event, instrumentation);
  }

  @Override
  public void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    Element element = getElement(selection);
    doRun(instrumentation, element, false);
  }

  @Override
  public void doRun(ITextSelection selection, Event event, UIInstrumentationBuilder instrumentation) {
    Element element = getElementFromEditor(selection);
    doRun(instrumentation, element, true);
  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
    // TODO(scheglov) implement when we will get selection notification for cursor movement
//    Element element = getElement(selection);
//    setEnabled(isRenameElementAvailable(element));
  }

  @Override
  public void selectionChanged(ITextSelection selection) {
    // TODO(scheglov) implement when we will get selection notification for cursor movement
//    Element element = getElementFromEditor(selection);
//    setEnabled(element != null);
  }

  private void doRun(UIInstrumentationBuilder instrumentation, Element element, boolean lightweight) {
    // check Element
    if (element == null) {
      instrumentation.metric("Problem", "Element was null");
      return;
    }
    ActionInstrumentationUtilities.recordElement(element, instrumentation);
    // check if can be modified
    if (!ActionUtil.isEditable(getShell(), element)) {
      instrumentation.metric("Problem", "Editor not editable");
      return;
    }
    // run rename
    try {
      run(element, lightweight);
    } catch (Throwable e) {
      ExceptionHandler.handle(
          e,
          RefactoringMessages.RenameDartElementAction_name,
          RefactoringMessages.RenameDartElementAction_exception);
    }
  }

//  private Element getElementFromEditor() {
//    ITextSelection selection = (ITextSelection) fEditor.getSelectionProvider().getSelection();
//    return getElementFromEditor(selection);
//  }

  private Element getElementFromEditor(ITextSelection selection) {
    CompilationUnit unit = fEditor.getInputUnit();
    int selectionOffset = selection.getOffset();
    ASTNode selectedNode = new NodeLocator(selectionOffset).searchWithin(unit);
    if (selectedNode instanceof SimpleIdentifier) {
      return ((SimpleIdentifier) selectedNode).getElement();
    }
    return null;
  }

  private void run(Element element, boolean lightweight) throws CoreException {
    if (lightweight && fEditor instanceof CompilationUnitEditor) {
      new RenameLinkedMode(element, (CompilationUnitEditor) fEditor).start();
    } else {
      RefactoringExecutionStarter.startRenameRefactoring(element, getShell());
    }
  }
}
