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
import com.google.dart.tools.ui.internal.actions.NewSelectionConverter;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.functions.PositionElement;
import com.google.dart.tools.ui.internal.util.OpenTypeHierarchyUtil;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * {@link Action} to show "Type Hierarchy" view.
 */
public class OpenTypeHierarchyAction_NEW extends AbstractDartSelectionAction_NEW implements
    AnalysisServerNavigationListener {
  public OpenTypeHierarchyAction_NEW(DartEditor editor) {
    super(editor);
    DartCore.getAnalysisServerData().addNavigationListener(file, this);
  }

  @Override
  public void computedNavigation(String file, NavigationRegion[] regions) {
    updateSelectedElement();
  }

  @Override
  public void dispose() {
    DartCore.getAnalysisServerData().removeNavigationListener(file, this);
    super.dispose();
  }

  @Override
  public void run() {
    IWorkbenchWindow window = getWorkbenchWindow();
    PositionElement element = new PositionElement(file, selectionOffset);
    OpenTypeHierarchyUtil.open(element, window);
  }

  @Override
  public void selectionChanged(ISelection selection) {
    super.selectionChanged(selection);
    updateSelectedElement();
  }

  @Override
  protected void init() {
    setText(ActionMessages.OpenTypeHierarchyAction_label);
    setToolTipText(ActionMessages.OpenTypeHierarchyAction_tooltip);
    setDescription(ActionMessages.OpenTypeHierarchyAction_description);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.OPEN_TYPE_HIERARCHY_ACTION);
  }

  private void updateSelectedElement() {
    Element[] elements = NewSelectionConverter.getNavigationTargets(file, selectionOffset);
    setEnabled(elements.length != 0);
  }
}
