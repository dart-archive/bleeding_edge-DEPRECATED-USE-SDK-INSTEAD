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
package com.google.dart.tools.ui.callhierarchy;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.ActionMessages;
import com.google.dart.tools.ui.actions.SelectionDispatchAction;
import com.google.dart.tools.ui.internal.actions.ActionUtil;
import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.callhierarchy.CallHierarchy;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This action opens a call hierarchy on the selected members.
 */
public class OpenCallHierarchyAction extends SelectionDispatchAction {

  private DartEditor editor;

  /**
   * Note: This constructor is for internal use only. Clients should not call this constructor.
   * 
   * @param editor internal
   */
  public OpenCallHierarchyAction(DartEditor editor) {
    this(editor.getEditorSite());
    this.editor = editor;
    setEnabled(SelectionConverter.canOperateOn(editor));
  }

  /**
   * Creates a new <code>OpenCallHierarchyAction</code>. The action requires that the selection
   * provided by the site's selection provider is of type
   * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
   * 
   * @param site the site providing context information for this action
   */
  public OpenCallHierarchyAction(IWorkbenchSite site) {
    super(site);
    setText(CallHierarchyMessages.OpenCallHierarchyAction_label);
    setToolTipText(CallHierarchyMessages.OpenCallHierarchyAction_tooltip);
    setDescription(CallHierarchyMessages.OpenCallHierarchyAction_description);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
        DartHelpContextIds.CALL_HIERARCHY_OPEN_ACTION);
  }

  @Override
  public void run(IStructuredSelection selection) {
    List<?> elements = selection.toList();
    if (!CallHierarchy.arePossibleInputElements(elements)) {
      elements = Collections.EMPTY_LIST;
    }

    TypeMember[] members = elements.toArray(new TypeMember[elements.size()]);
    if (!ActionUtil.areProcessable(getShell(), members)) {
      return;
    }

    CallHierarchyUI.openView(members, getSite().getWorkbenchWindow());
  }

  @Override
  public void run(ITextSelection selection) {
    CompilationUnit input = SelectionConverter.getInputAsCompilationUnit(editor);
    if (!ActionUtil.isProcessable(getShell(), input)) {
      return;
    }

    try {
      DartElement[] elements = SelectionConverter.codeResolveOrInputForked(editor);
      if (elements == null) {
        return;
      }
      List<DartElement> candidates = new ArrayList<DartElement>(elements.length);
      for (int i = 0; i < elements.length; i++) {
        DartElement element = elements[i];
        if (CallHierarchy.isPossibleInputElement(element)) {
          candidates.add(element);
        }
      }
      if (candidates.isEmpty()) {
        DartElement enclosingMethod = getEnclosingMethod(input, selection);
        if (enclosingMethod != null) {
          candidates.add(enclosingMethod);
        }
      }
      CallHierarchyUI.openSelectionDialog(candidates.toArray(new DartElement[candidates.size()]),
          getSite().getWorkbenchWindow());

    } catch (InvocationTargetException e) {
      ExceptionHandler.handle(e, getShell(),
          CallHierarchyMessages.OpenCallHierarchyAction_dialog_title,
          ActionMessages.SelectionConverter_codeResolve_failed);
    } catch (InterruptedException e) {
      // cancelled
    }
  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
    setEnabled(CallHierarchy.arePossibleInputElements(selection.toList()));
  }

  @Override
  public void selectionChanged(ITextSelection selection) {
    // Do nothing
  }

  private DartElement getEnclosingMethod(CompilationUnit input, ITextSelection selection) {
    try {
      DartElement enclosingElement = input.getElementAt(selection.getOffset());
      if (enclosingElement instanceof Method || enclosingElement instanceof Field) {
        // opening on the enclosing type would be too confusing (since the type resolves to the constructors)
        return enclosingElement;
      }
    } catch (DartModelException e) {
      DartToolsPlugin.log(e);
    }

    return null;
  }
}
