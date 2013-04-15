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
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.source.Source;
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
  /**
   * @return {@code true} if given {@link DartSelection} looks valid and we can try to rename.
   */
  private static boolean isValidSelection(DartSelection selection) {
    Element element = getSelectionElement(selection);
    ASTNode node = getSelectionNode(selection);
    // can we rename this node at all?
    if (node instanceof SimpleIdentifier) {
      // usually
    } else if (node instanceof InstanceCreationExpression) {
      // "new X()" - to give name to unnamed constructor
    } else {
      return false;
    }
    // do we have interesting Element?
    if (!isInterestingElement(node, element)) {
      return false;
    }
    // we don't want to rename anything from SDK
    {
      Source source = element.getSource();
      if (source == null || source.isInSystemLibrary()) {
        return false;
      }
    }
    // OK
    return true;
  }

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
    setEnabled(isValidSelection(selection));
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
      // Unnamed constructor are special case - we don't have name to start linked mode.
      // Named constructors may become unnamed, this looks ugly because of analysis as you type.
      {
        Element element = context.getCoveredElement();
        // it is more logical to rename constructor, not type
        ASTNode node = context.getCoveredNode();
        if (node instanceof SimpleIdentifier && node.getParent() instanceof ConstructorDeclaration) {
          ConstructorDeclaration constructor = (ConstructorDeclaration) node.getParent();
          if (constructor.getName() == null && constructor.getReturnType() == node) {
            element = constructor.getElement();
          }
        }
        // is it constructor?
        if (element instanceof ConstructorElement) {
          renameUsingDialog(element);
          return;
        }
      }
      // start linked mode rename
      new RenameLinkedMode(editor, context).start();
    }
  }

  @Override
  protected void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    Element element = getSelectionElement(selection);
    renameUsingDialog(element);
  }

  @Override
  protected void init() {
  }

  private void renameUsingDialog(Element element) {
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
}
