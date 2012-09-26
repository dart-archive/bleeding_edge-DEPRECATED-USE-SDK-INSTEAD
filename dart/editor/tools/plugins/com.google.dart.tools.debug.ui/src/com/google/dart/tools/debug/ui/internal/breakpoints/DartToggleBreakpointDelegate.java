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
package com.google.dart.tools.debug.ui.internal.breakpoints;

import org.eclipse.debug.ui.actions.ToggleBreakpointAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.AbstractRulerActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Toggles a breakpoint when ruler is double-clicked. This action delegate can be contributed to an
 * editor with the <code>editorActions</code> extension point. This action is as a factory that
 * creates another action that performs the actual breakpoint toggling. The created action acts on
 * the editor's <code>IToggleBreakpointsTagret</code> to toggle breakpoints.
 */
public class DartToggleBreakpointDelegate extends AbstractRulerActionDelegate implements
    IActionDelegate2 {
  private ToggleBreakpointAction actionDelegate = null;
  private IEditorPart editor = null;

  public DartToggleBreakpointDelegate() {

  }

  @Override
  public void dispose() {
    if (actionDelegate != null) {
      actionDelegate.dispose();
    }
    actionDelegate = null;
    editor = null;
  }

  @Override
  public void init(IAction action) {

  }

  @Override
  public void runWithEvent(IAction action, Event event) {
    run(action);
  }

  @Override
  public void setActiveEditor(IAction callerAction, IEditorPart targetEditor) {
    if (editor != null) {
      if (actionDelegate != null) {
        actionDelegate.dispose();
        actionDelegate = null;
      }
    }
    editor = targetEditor;
    super.setActiveEditor(callerAction, targetEditor);
  }

  @Override
  protected IAction createAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
    actionDelegate = new ToggleBreakpointAction(editor, null, rulerInfo);
    return actionDelegate;
  }

}
