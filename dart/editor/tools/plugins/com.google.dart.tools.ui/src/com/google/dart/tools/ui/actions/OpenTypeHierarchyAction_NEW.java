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

import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartSelection;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

/**
 * {@link Action} to show "Type Hierarchy" view.
 */
public class OpenTypeHierarchyAction_NEW extends AbstractDartSelectionAction_OLD {
  public OpenTypeHierarchyAction_NEW(DartEditor editor) {
    super(editor);
  }

  public OpenTypeHierarchyAction_NEW(IWorkbenchSite site) {
    super(site);
  }

  @Override
  public void selectionChanged(DartSelection selection) {
    boolean hasTarget = OpenAction.getNavigationTargets(selection).length != 0;
    setEnabled(hasTarget);
  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
    setEnabled(false);
    // TODO(scheglov) Analysis Server: implement (maybe)
//    Element element = getSelectionElement(selection);
//    setEnabled(element instanceof ClassElement);
  }

  @Override
  protected void doRun(DartSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    // TODO(scheglov) Analysis Server: implement for new API
//    Element[] targets = OpenAction.getNavigationTargets(selection);
//    if (targets.length != 0) {
//      OpenTypeHierarchyUtil.open(targets[0], getSite().getWorkbenchWindow());
//    }
  }

  @Override
  protected void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    // TODO(scheglov) Analysis Server: implement (maybe)
//    Element element = getSelectionElement(selection);
//    openElement(element);
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
}
