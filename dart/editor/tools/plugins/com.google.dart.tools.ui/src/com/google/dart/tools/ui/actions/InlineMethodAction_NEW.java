/*
 * Copyright (c) 2014, the Dart project authors.
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

import com.google.dart.server.generated.types.Element;
import com.google.dart.server.generated.types.ElementKind;
import com.google.dart.tools.ui.internal.actions.NewSelectionConverter;
import com.google.dart.tools.ui.internal.refactoring.InlineMethodWizard_NEW;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.refactoring.RefactoringSaveHelper;
import com.google.dart.tools.ui.internal.refactoring.ServerInlineMethodRefactoring;
import com.google.dart.tools.ui.internal.refactoring.actions.RefactoringStarter;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.SelectionChangedEvent;

/**
 * {@link Action} for "Inline Method" refactoring.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class InlineMethodAction_NEW extends AbstractRefactoringAction_NEW {
  public InlineMethodAction_NEW(DartEditor editor) {
    super(editor);
  }

  @Override
  public void run() {
    ServerInlineMethodRefactoring refactoring = new ServerInlineMethodRefactoring(
        file,
        selectionOffset,
        selectionLength);
    try {
      new RefactoringStarter().activate(
          new InlineMethodWizard_NEW(refactoring),
          getShell(),
          RefactoringMessages.InlineMethodAction_dialog_title,
          RefactoringSaveHelper.SAVE_NOTHING);
    } catch (Throwable e) {
      showError("Inline Method", e);
    }
  }

  @Override
  public void selectionChanged(SelectionChangedEvent event) {
    super.selectionChanged(event);
    setEnabled(false);
    Element[] elements = NewSelectionConverter.getNavigationTargets(file, selectionOffset);
    if (elements.length != 0) {
      Element element = elements[0];
      String kind = element.getKind();
      setEnabled(ElementKind.METHOD.equals(kind) || ElementKind.FUNCTION.equals(kind)
          || ElementKind.GETTER.equals(kind) || ElementKind.SETTER.equals(kind));
    }
  }

  @Override
  protected void init() {
  }
}
