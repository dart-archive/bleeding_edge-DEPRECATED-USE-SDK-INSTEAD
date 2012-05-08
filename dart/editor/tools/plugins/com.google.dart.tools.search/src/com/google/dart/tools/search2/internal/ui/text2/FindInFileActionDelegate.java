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
import com.google.dart.tools.search2.internal.ui.SearchMessages;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;

public class FindInFileActionDelegate extends FindInRecentScopeActionDelegate {
  private IEditorPart fEditor = null;

  public FindInFileActionDelegate() {
    super(SearchMessages.FindInFileActionDelegate_text);
    setActionDefinitionId("com.google.dart.tools.search.ui.performTextSearchFile"); //$NON-NLS-1$
  }

  public void selectionChanged(IAction action, ISelection selection) {
    fEditor = null;
    IWorkbenchPage page = getWorkbenchPage();
    if (page != null) {
      IWorkbenchPart part = page.getActivePart();
      if (part instanceof IEditorPart) {
        IEditorPart editor = (IEditorPart) part;
        if (editor.getEditorInput() instanceof IFileEditorInput) {
          fEditor = editor;
        }
      }
    }
    action.setEnabled(fEditor != null);
  }

  public void setActiveEditor(IAction action, IEditorPart editor) {
    if (editor != null && editor.getEditorInput() instanceof IFileEditorInput) {
      fEditor = editor;
    } else {
      fEditor = null;
    }
    super.setActiveEditor(action, editor);
  }

  private IFile getFile() {
    if (fEditor != null) {
      IEditorInput ei = fEditor.getEditorInput();
      if (ei instanceof IFileEditorInput) {
        return ((IFileEditorInput) ei).getFile();
      }
    }
    return null;
  }

  protected ISearchQuery createQuery(TextSearchQueryProvider provider, String searchForString)
      throws CoreException {
    return provider.createQuery(searchForString, new IResource[] {getFile()});
  }

}
