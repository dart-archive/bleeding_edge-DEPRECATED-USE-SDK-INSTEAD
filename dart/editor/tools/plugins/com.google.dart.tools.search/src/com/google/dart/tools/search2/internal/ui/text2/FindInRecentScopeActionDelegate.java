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
package com.google.dart.tools.search2.internal.ui.text2;

import com.google.dart.tools.search.ui.ISearchQuery;
import com.google.dart.tools.search.ui.text.TextSearchQueryProvider;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * @author markus.schorn@windriver.com
 */
abstract public class FindInRecentScopeActionDelegate extends RetrieverAction implements
    IWorkbenchWindowActionDelegate, IEditorActionDelegate {
  private IWorkbenchWindow fWindow;

  public FindInRecentScopeActionDelegate(String text) {
    setText(text);
  }

  // IWorkbenchWindowActionDelegate
  public void dispose() {
    fWindow = null;
  }

  // IWorkbenchWindowActionDelegate
  public void init(IWorkbenchWindow window) {
    fWindow = window;
  }

  // IEditorActionDelegate
  public void setActiveEditor(IAction action, IEditorPart targetEditor) {
    fWindow = null;
    if (targetEditor != null) {
      fWindow = targetEditor.getSite().getWorkbenchWindow();
    }
  }

  // IActionDelegate
  public void selectionChanged(IAction action, ISelection selection) {
  }

  // IActionDelegate
  final public void run(IAction action) {
    run();
  }

  // RetrieverAction
  protected IWorkbenchPage getWorkbenchPage() {
    if (fWindow != null) {
      return fWindow.getActivePage();
    }
    return null;
  }

  abstract protected ISearchQuery createQuery(TextSearchQueryProvider provider,
      String searchForString) throws CoreException;
}
