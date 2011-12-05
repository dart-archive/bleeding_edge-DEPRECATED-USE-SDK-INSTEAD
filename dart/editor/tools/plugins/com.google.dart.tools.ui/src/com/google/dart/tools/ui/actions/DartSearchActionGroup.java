/*
 * Copyright (c) 2011, the Dart project authors.
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

import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.part.Page;

/**
 * Action group that adds the Dart search actions to a context menu and the global menu bar.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class DartSearchActionGroup extends ActionGroup {

  private final ReferencesSearchGroup referencesGroup;

  /**
   * Note: This constructor is for internal use only. Clients should not call this constructor.
   * 
   * @param editor the Dart editor
   * @noreference This constructor is not intended to be referenced by clients.
   */
  public DartSearchActionGroup(DartEditor editor) {
    Assert.isNotNull(editor);
    referencesGroup = new ReferencesSearchGroup(editor);
  }

  /**
   * Creates a new <code>DartSearchActionGroup</code>. The group requires that the selection
   * provided by the part's selection provider is of type
   * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
   * 
   * @param part the view part that owns this action group
   */
  public DartSearchActionGroup(IViewPart part) {
    this(part.getViewSite());
  }

  /**
   * Creates a new <code>DartSearchActionGroup</code>. The group requires that the selection
   * provided by the given selection provider is of type {@link IStructuredSelection}.
   * 
   * @param site the site that will own the action group.
   * @param specialSelectionProvider the selection provider used instead of the sites selection
   *          provider.
   */
  public DartSearchActionGroup(IWorkbenchSite site, ISelectionProvider specialSelectionProvider) {
    referencesGroup = new ReferencesSearchGroup(site, specialSelectionProvider);
  }

  /**
   * Creates a new <code>DartSearchActionGroup</code>. The group requires that the selection
   * provided by the page's selection provider is of type
   * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
   * 
   * @param page the page that owns this action group
   */
  public DartSearchActionGroup(Page page) {
    this(page.getSite());
  }

  private DartSearchActionGroup(IWorkbenchSite site) {
    referencesGroup = new ReferencesSearchGroup(site);
  }

  @Override
  public void dispose() {
    referencesGroup.dispose();
    super.dispose();
  }

  @Override
  public void fillActionBars(IActionBars actionBar) {
    super.fillActionBars(actionBar);
    referencesGroup.fillActionBars(actionBar);
  }

  @Override
  public void fillContextMenu(IMenuManager menu) {
    super.fillContextMenu(menu);
    menu.add(new Separator());
    referencesGroup.fillContextMenu(menu);
  }

  @Override
  public void setContext(ActionContext context) {
    referencesGroup.setContext(context);
  }
}
