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
import com.google.dart.server.generated.types.NavigationRegion;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.AnalysisServerNavigationListener;
import com.google.dart.tools.ui.internal.actions.NewSelectionConverter;
import com.google.dart.tools.ui.internal.refactoring.ConvertMethodToGetterWizard_NEW;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.refactoring.RefactoringSaveHelper;
import com.google.dart.tools.ui.internal.refactoring.ServerConvertMethodToGetterRefactoring;
import com.google.dart.tools.ui.internal.refactoring.actions.RefactoringStarter;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.PlatformUI;

/**
 * {@link Action} for "Inline Local" refactoring.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class ConvertMethodToGetterAction_NEW extends AbstractRefactoringAction_NEW implements
    AnalysisServerNavigationListener {
  public ConvertMethodToGetterAction_NEW(DartEditor editor) {
    super(editor);
    DartCore.getAnalysisServerData().subscribeNavigation(file, this);
  }

  @Override
  public void computedNavigation(String file, NavigationRegion[] regions) {
    updateSelectedElement();
  }

  @Override
  public void dispose() {
    DartCore.getAnalysisServerData().unsubscribeNavigation(file, this);
    super.dispose();
  }

  @Override
  public void run() {
    ServerConvertMethodToGetterRefactoring refactoring = new ServerConvertMethodToGetterRefactoring(
        file,
        selectionOffset);
    try {
      new RefactoringStarter().activate(
          new ConvertMethodToGetterWizard_NEW(refactoring),
          getShell(),
          RefactoringMessages.ConvertMethodToGetterAction_dialog_title,
          RefactoringSaveHelper.SAVE_NOTHING);
    } catch (Throwable e) {
      showError("Inline Local", e);
    }
  }

  @Override
  public void selectionChanged(SelectionChangedEvent event) {
    super.selectionChanged(event);
    updateSelectedElement();
  }

  @Override
  protected void init() {
    setText(RefactoringMessages.ConvertMethodToGetterAction_title);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.CONVERT_METHOD_TO_GETTER_ACTION);
  }

  private void updateSelectedElement() {
    setEnabled(false);
    Element[] elements = NewSelectionConverter.getNavigationTargets(file, selectionOffset);
    if (elements.length != 0) {
      Element element = elements[0];
      String kind = element.getKind();
      setEnabled(kind.equals(ElementKind.FUNCTION) || kind.equals(ElementKind.METHOD));
    }
  }
}
