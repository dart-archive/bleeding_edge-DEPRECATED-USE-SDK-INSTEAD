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

import com.google.common.collect.Lists;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionGroup;

import java.util.Collections;
import java.util.List;

/**
 * {@link ActionGroup} for selection-based actions.
 */
public class AbstractDartSelectionActionGroup extends ActionGroup {
  private final DartEditor editor;
  private final IWorkbenchSite site;
  private final List<IAction> actions = Lists.newArrayList();

  public AbstractDartSelectionActionGroup(DartEditor editor) {
    this.editor = editor;
    this.site = editor.getSite();
  }

  protected AbstractDartSelectionActionGroup(IWorkbenchSite site) {
    this.editor = null;
    this.site = site;
  }

  @Override
  public void dispose() {
    for (IAction action : actions) {
      removeActionSelectionListeners(action);
    }
    disposeActions();
    actions.clear();
    super.dispose();
  }

  /**
   * Registers added actions with {@link #site}'s {@link ISelectionProvider}.
   */
  protected final void addActionDartSelectionListeners() {
    ISelectionProvider selectionProvider = site.getSelectionProvider();
    ISelection selection = selectionProvider.getSelection();
    for (IAction action : actions) {
      if (action instanceof ISelectionChangedListener) {
        updateAction(action, selection);
        editor.addDartSelectionListener((ISelectionChangedListener) action);
      }
    }
  }

  /**
   * Adds given {@link IAction}s to this {@link ActionGroup}.
   */
  protected final void addActions(IAction... actions) {
    Collections.addAll(this.actions, actions);
  }

  /**
   * Registers added actions with {@link #site}'s {@link ISelectionProvider}.
   */
  protected final void addActionSelectionListeners() {
    ISelectionProvider selectionProvider = site.getSelectionProvider();
    ISelection selection = selectionProvider.getSelection();
    for (IAction action : actions) {
      addActionSelectionListener(action, selectionProvider, selection);
    }
  }

  /**
   * Appends all registered {@link IAction}s to group, if enabled.
   */
  protected final void appendToGroup(IMenuManager menu, String groupName) {
    for (IAction action : actions) {
      appendToGroup(menu, groupName, action);
    }
  }

  /**
   * Appends given {@link IAction} is it is enabled.
   */
  protected final void appendToGroup(IMenuManager menu, String groupName, IAction action) {
    if (action.isEnabled()) {
      menu.appendToGroup(groupName, action);
    }
  }

  /**
   * Removes the {@link IAction} from the {@link ISelectionProvider}.
   */
  protected final void removeActionSelectionListeners(IAction action) {
    ISelectionProvider provider = site.getSelectionProvider();
    if (provider != null && action instanceof ISelectionChangedListener) {
      provider.removeSelectionChangedListener((ISelectionChangedListener) action);
    }
  }

  /**
   * Update previously registered actions.
   */
  protected final void updateActions(ISelection selection) {
    for (IAction action : actions) {
      updateAction(action, selection);
    }
  }

  /**
   * Registers given {@link IAction} in {@link ISelectionProvider}.
   */
  private void addActionSelectionListener(IAction action, ISelectionProvider provider,
      ISelection selection) {
    updateAction(action, selection);
    if (provider != null && action instanceof ISelectionChangedListener) {
      ISelectionChangedListener listener = (ISelectionChangedListener) action;
      provider.addSelectionChangedListener(listener);
    }
  }

  /**
   * Dispose previously registered actions.
   */
  private void disposeActions() {
    for (IAction action : actions) {
      if (action instanceof AbstractDartSelectionAction_NEW) {
        AbstractDartSelectionAction_NEW dartAction = (AbstractDartSelectionAction_NEW) action;
        dartAction.dispose();
      }
    }
  }

  /**
   * Updates {@link IAction} selection.
   */
  private void updateAction(IAction action, ISelection selection) {
    if (action instanceof InstrumentedSelectionDispatchAction) {
      ((InstrumentedSelectionDispatchAction) action).update(selection);
    }
  }
}
