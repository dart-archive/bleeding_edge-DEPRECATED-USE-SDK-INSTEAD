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

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartSelection;
import com.google.dart.tools.ui.internal.util.OpenTypeHierarchyUtil;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

/**
 * {@link Action} to show "Type Hierarchy" view.
 */
public class OpenTypeHierarchyAction_OLD extends AbstractDartSelectionAction_OLD {
  public OpenTypeHierarchyAction_OLD(DartEditor editor) {
    super(editor);
  }

  public OpenTypeHierarchyAction_OLD(IWorkbenchSite site) {
    super(site);
  }

  @Override
  public void selectionChanged(DartSelection selection) {
    Element element = getSelectionElement(selection);
    setEnabled(element instanceof ClassElement);
  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
    Element element = getSelectionElement(selection);
    setEnabled(element instanceof ClassElement);
  }

  @Override
  protected void doRun(DartSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    Element element = getSelectionElement(selection);
    openElement(element);
  }

  @Override
  protected void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    Element element = getSelectionElement(selection);
    openElement(element);
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

  private void openElement(Element element) {
    if (element instanceof ClassElement) {
      ClassElement type = (ClassElement) element;
      OpenTypeHierarchyUtil.open(type, getSite().getWorkbenchWindow());
    }
  }
}
