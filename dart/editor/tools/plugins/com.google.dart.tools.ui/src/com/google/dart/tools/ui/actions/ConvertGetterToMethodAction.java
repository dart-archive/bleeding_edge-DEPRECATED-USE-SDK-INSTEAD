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

import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.services.refactoring.ConvertGetterToMethodRefactoring;
import com.google.dart.engine.services.refactoring.RefactoringFactory;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.refactoring.ConvertGetterToMethodWizard;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.refactoring.RefactoringSaveHelper;
import com.google.dart.tools.ui.internal.refactoring.RefactoringUtils;
import com.google.dart.tools.ui.internal.refactoring.ServiceConvertGetterToMethodRefactoring;
import com.google.dart.tools.ui.internal.refactoring.actions.RefactoringStarter;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartSelection;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

/**
 * {@link Action} for "Convert Getter To Method" refactoring.
 */
public class ConvertGetterToMethodAction extends AbstractRefactoringAction_OLD {
  /**
   * @return the {@link ConvertGetterToMethodRefactoring} to process given {@link Element}, may be
   *         {@code null} if {@link Element} cannot be processed.
   */
  private static ConvertGetterToMethodRefactoring newRefactoring(Element element) {
    // prepare PropertyAccessorElement
    if (!(element instanceof PropertyAccessorElement)) {
      return null;
    }
    PropertyAccessorElement accessorElement = (PropertyAccessorElement) element;
    // should be getter
    if (!accessorElement.isGetter()) {
      return null;
    }
    // should be explicit
    if (accessorElement.isSynthetic()) {
      return null;
    }
    // create ConvertMethodToGetterRefactoring
    SearchEngine searchEngine = DartCore.getProjectManager().newSearchEngine();
    return RefactoringFactory.createConvertGetterToMethodRefactoring(searchEngine, accessorElement);
  }

  public ConvertGetterToMethodAction(DartEditor editor) {
    super(editor);
  }

  public ConvertGetterToMethodAction(IWorkbenchSite site) {
    super(site);
  }

  @Override
  public void selectionChanged(DartSelection selection) {
    Element element = getSelectionElement(selection);
    selectionChanged(element);
  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
    Element element = getSelectionElement(selection);
    selectionChanged(element);
  }

  @Override
  protected void doRun(DartSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    Element element = getSelectionElement(selection);
    doRun(element);
  }

  @Override
  protected void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    Element element = getSelectionElement(selection);
    doRun(element);
  }

  @Override
  protected void init() {
    setText(RefactoringMessages.ConvertGetterToMethodAction_title);
    {
      String id = DartEditorActionDefinitionIds.CONVERT_GETTER_TO_METHOD;
      setId(id);
      setActionDefinitionId(id);
    }
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.CONVERT_GETTER_TO_METHOD_ACTION);
  }

  /**
   * Runs refactoring wizard to process given {@link Element}.
   */
  private void doRun(Element element) {
    if (!RefactoringUtils.waitReadyForRefactoring()) {
      return;
    }
    ConvertGetterToMethodRefactoring serviceRefactoring = newRefactoring(element);
    if (serviceRefactoring == null) {
      return;
    }
    try {
      ServiceConvertGetterToMethodRefactoring ltkRefactoring = new ServiceConvertGetterToMethodRefactoring(
          serviceRefactoring);
      new RefactoringStarter().activate(
          new ConvertGetterToMethodWizard(ltkRefactoring),
          getShell(),
          RefactoringMessages.ConvertGetterToMethodAction_dialog_title,
          RefactoringSaveHelper.SAVE_NOTHING);
    } catch (Throwable e) {
      ExceptionHandler.handle(
          e,
          getText(),
          "Unexpected exception occurred. See the error log for more details.");
    }
  }

  /**
   * Updates enablement for given {@link Element}.
   */
  private void selectionChanged(Element element) {
    // cannot operate on this editor
    if (!canOperateOn()) {
      setEnabled(false);
      return;
    }
    // validate Element
    ConvertGetterToMethodRefactoring refactoring = newRefactoring(element);
    if (refactoring != null) {
      try {
        if (!refactoring.checkAllConditions(null).hasError()) {
          setEnabled(true);
          return;
        }
      } catch (Throwable e) {
      }
    }
    // invalid Element
    setEnabled(false);
  }
}
