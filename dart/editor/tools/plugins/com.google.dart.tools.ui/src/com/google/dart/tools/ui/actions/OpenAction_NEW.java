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
import com.google.dart.server.generated.types.NavigationRegion;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.AnalysisServerNavigationListener;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.internal.actions.NewSelectionConverter;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.PlatformUI;

/**
 * This action opens a {@link DartEditor} with declaration of {@link Element}.
 */
public class OpenAction_NEW extends AbstractDartSelectionAction_NEW implements
    AnalysisServerNavigationListener {

  public static void open(Element element) {
    if (element == null) {
      return;
    }
    try {
      DartUI.openInEditor(element, true);
    } catch (Throwable e) {
      ExceptionHandler.handle(e, "Open", "Exception during open.");
    }
  }

  public OpenAction_NEW(DartEditor editor) {
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
    Element[] elements = NewSelectionConverter.getNavigationTargets(file, selectionOffset);
    if (elements.length != 0) {
      Element element = elements[0];
      open(element);
    }
  }

  @Override
  public void selectionChanged(ISelection selection) {
    super.selectionChanged(selection);
    updateSelectedElement();
  }

  public void updateLabel() {
    //TODO (pquitslund): once there was logic here --- (re)assess and add back or remove
//  ISelection selection = fEditor.createElementSelection();
//  if (ActionUtil.isOpenDeclarationAvailable_OLD((DartElementSelection) selection)) {
//    update(selection);
//  } else {
//    setText(ActionMessages.OpenAction_declaration_label);
//    setEnabled(false);
//  }
  }

  @Override
  protected void init() {
    setText(ActionMessages.OpenAction_declaration_label);
    setToolTipText(ActionMessages.OpenAction_tooltip);
    setDescription(ActionMessages.OpenAction_description);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, DartHelpContextIds.OPEN_ACTION);
  }

  private void updateSelectedElement() {
    Element[] elements = NewSelectionConverter.getNavigationTargets(file, selectionOffset);
    setEnabled(elements.length != 0);
  }
}
