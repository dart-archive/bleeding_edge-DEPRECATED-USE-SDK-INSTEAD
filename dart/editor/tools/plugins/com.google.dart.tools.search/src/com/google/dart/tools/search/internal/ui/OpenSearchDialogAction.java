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
package com.google.dart.tools.search.internal.ui;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * Opens the Search Dialog.
 */
public class OpenSearchDialogAction extends Action implements IWorkbenchWindowActionDelegate {

  private IWorkbenchWindow fWindow;
  private String fPageId;

  public OpenSearchDialogAction() {
    super(SearchMessages.OpenSearchDialogAction_label);
    SearchPluginImages.setImageDescriptors(
        this,
        SearchPluginImages.T_TOOL,
        SearchPluginImages.IMG_TOOL_SEARCH);
    setToolTipText(SearchMessages.OpenSearchDialogAction_tooltip);
  }

  public OpenSearchDialogAction(IWorkbenchWindow window, String pageId) {
    this();
    fPageId = pageId;
    fWindow = window;
  }

  public void init(IWorkbenchWindow window) {
    fWindow = window;
  }

  public void run(IAction action) {
    run();
  }

  public void run() {
    if (getWorkbenchWindow().getActivePage() == null) {
      SearchPlugin.beep();
      return;
    }
    SearchDialog dialog = new SearchDialog(getWorkbenchWindow(), fPageId);
    dialog.open();
  }

  public void selectionChanged(IAction action, ISelection selection) {
    // do nothing since the action isn't selection dependent.
  }

  private IWorkbenchWindow getWorkbenchWindow() {
    if (fWindow == null)
      fWindow = SearchPlugin.getActiveWorkbenchWindow();
    return fWindow;
  }

  public void dispose() {
    fWindow = null;
  }
}
