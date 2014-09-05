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

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.internal.index.IndexContributor;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.internal.corext.refactoring.RefactoringExecutionStarter;
import com.google.dart.tools.ui.actions.AbstractRefactoringAction_OLD;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.refactoring.RefactoringUtils;
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
public class RenameDartElementAction_OLD extends AbstractRefactoringAction_OLD {
  /**
   * @return the {@link Element} to rename {@code true}, may be {@code null} if invalid selection.
   */
  private static Element getElementToRename(DartSelection selection) {
    Element element = getSelectionElement(selection);
    AstNode node = getSelectionNode(selection);
    // 'library x;' or 'part of x;'
    if (node != null) {
      Directive directive = node.getAncestor(Directive.class);
      if (directive != null) {
        Element directiveElement = directive.getElement();
        if (directiveElement instanceof LibraryElement) {
          return directiveElement;
        }
        if (directiveElement instanceof ImportElement) {
          return directiveElement;
        }
      }
    }
    // may be PrefixElement, rename ImportElement instead
    if (element instanceof PrefixElement) {
      element = IndexContributor.getImportElement((SimpleIdentifier) node);
    }
    // do we have interesting Element?
    if (!isInterestingElement(node, element)) {
      return null;
    }
    // we don't want to rename anything from SDK
    {
      Source source = element.getSource();
      if (source == null || source.isInSystemLibrary()) {
        return null;
      }
    }
    // OK
    return element;
  }

  public RenameDartElementAction_OLD(DartEditor editor) {
    super(editor);
    setEnabled(SelectionConverter.canOperateOn(editor));
  }

  public RenameDartElementAction_OLD(IWorkbenchSite site) {
    super(site);
  }

  @Override
  public void selectionChanged(DartSelection selection) {
    // cannot operate on this editor
    if (!canOperateOn()) {
      setEnabled(false);
      return;
    }
    // validate element
    Element element = getElementToRename(selection);
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
    AssistContext context = getContextAfterBuild(selection);
    if (context == null) {
      return;
    }
    // prepare element
    Element element = getElementToRename(selection);
    if (element == null) {
      return;
    }
    // Always rename using dialog. Eclipse implementation of the linked mode is very slow
    // in case of the many occurrences in the single file. It is also done using asynchronous
    // execution, in these 1-2-3 seconds user can type anything and damage source.
    renameUsingDialog(element);
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
