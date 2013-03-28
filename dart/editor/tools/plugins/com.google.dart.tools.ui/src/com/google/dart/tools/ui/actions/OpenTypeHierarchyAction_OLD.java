/*
 * Copyright 2012 Dart project authors.
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

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.actions.ActionUtil;
import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;
import com.google.dart.tools.ui.internal.util.OpenTypeHierarchyUtil;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This action opens a type hierarchy on the selected type.
 * <p>
 * The action is applicable to selections containing elements of type <code>IType</code>.
 */
public class OpenTypeHierarchyAction_OLD extends InstrumentedSelectionDispatchAction {

  private static String getDialogTitle() {
    return ActionMessages.OpenTypeHierarchyAction_dialog_title;
  }

  private DartEditor fEditor;

  /**
   * Note: This constructor is for internal use only. Clients should not call this constructor.
   */
  public OpenTypeHierarchyAction_OLD(DartEditor editor) {
    this(editor.getEditorSite());
    fEditor = editor;
    setEnabled(SelectionConverter.canOperateOn(fEditor));
  }

  /**
   * Creates a new <code>OpenTypeHierarchyAction</code>. The action requires that the selection
   * provided by the site's selection provider is of type <code>
   * org.eclipse.jface.viewers.IStructuredSelection</code>.
   * 
   * @param site the site providing context information for this action
   */
  public OpenTypeHierarchyAction_OLD(IWorkbenchSite site) {
    super(site);
    setText(ActionMessages.OpenTypeHierarchyAction_label);
    setToolTipText(ActionMessages.OpenTypeHierarchyAction_tooltip);
    setDescription(ActionMessages.OpenTypeHierarchyAction_description);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.OPEN_TYPE_HIERARCHY_ACTION);
  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
    setEnabled(isEnabled(selection));
  }

  @Override
  public void selectionChanged(ITextSelection selection) {
  }

  @Override
  protected void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    instrumentation.metric("Problem", "NotYetImplemented in IStructuredSelection");
    // TODO(scheglov)
  }

  @Override
  protected void doRun(ITextSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    DartElement input = SelectionConverter.getInput(fEditor);
    if (!ActionUtil.isProcessable(getShell(), input)) {
      instrumentation.metric("Problem", "Editor not editable");
      return;
    }

    try {
      DartElement[] elements = SelectionConverter.codeResolveOrInputForked(fEditor);
      if (elements == null) {
        instrumentation.metric("Problem", "elements was null");
        return;
      }

      instrumentation.metric("elements-Length", elements.length);
      List<DartElement> candidates = new ArrayList<DartElement>(elements.length);
      for (int i = 0; i < elements.length; i++) {
        ActionInstrumentationUtilities.recordElement(elements[i], instrumentation);
        DartElement[] resolvedElements = OpenTypeHierarchyUtil.getCandidates(elements[i]);
        if (resolvedElements != null) {
          candidates.addAll(Arrays.asList(resolvedElements));
        }
      }

      instrumentation.metric("candidates-Length", candidates.size());
      for (DartElement candidate : candidates) {
        ActionInstrumentationUtilities.recordElement(candidate, instrumentation);
      }

      run(candidates.toArray(new DartElement[candidates.size()]));
    } catch (InvocationTargetException e) {
      ExceptionHandler.handle(
          e,
          getShell(),
          getDialogTitle(),
          ActionMessages.SelectionConverter_codeResolve_failed);
    } catch (InterruptedException e) {
      instrumentation.metric("Problem", "Cancelled");
      // cancelled
    }
  }

  private boolean isEnabled(IStructuredSelection selection) {
    Object[] elements = selection.toArray();
    if (elements.length == 0) {
      return false;
    }

    if (elements.length == 1) {
      Object input = elements[0];
      return input instanceof Type;
    }

    return false;
  }

  private void run(DartElement[] elements) {
    if (elements.length == 0) {
      getShell().getDisplay().beep();
      return;
    }
    OpenTypeHierarchyUtil.open(elements, getSite().getWorkbenchWindow());
  }
}
