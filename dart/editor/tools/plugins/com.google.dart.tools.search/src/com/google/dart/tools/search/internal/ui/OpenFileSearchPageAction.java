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

import com.google.dart.tools.search.ui.NewSearchUI;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * Opens the Search Dialog.
 */
public class OpenFileSearchPageAction implements IWorkbenchWindowActionDelegate {

  private static final String TEXT_SEARCH_PAGE_ID = "com.google.dart.tools.search.internal.ui.text.TextSearchPage"; //$NON-NLS-1$

  private IWorkbenchWindow fWindow;

  public OpenFileSearchPageAction() {
  }

  public void init(IWorkbenchWindow window) {
    fWindow = window;
  }

  public void run(IAction action) {
    if (fWindow == null || fWindow.getActivePage() == null) {
      SearchPlugin.beep();
      logErrorMessage("Could not open the search dialog - for some reason the window handle was null"); //$NON-NLS-1$
      return;
    }
    NewSearchUI.openSearchDialog(fWindow, TEXT_SEARCH_PAGE_ID);
  }

  public void selectionChanged(IAction action, ISelection selection) {
    // do nothing since the action isn't selection dependent.
  }

  public void dispose() {
    fWindow = null;
  }

  public static void logErrorMessage(String message) {
    IStatus status = new Status(IStatus.ERROR, NewSearchUI.PLUGIN_ID, IStatus.ERROR, message, null);
    SearchPlugin.log(status);
  }
}
