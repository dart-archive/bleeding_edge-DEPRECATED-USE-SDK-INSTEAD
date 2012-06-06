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
package com.google.dart.tools.ui.internal.appsview;

import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.ui.internal.filesview.LinkWithEditorAction;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IWorkbenchPage;

public class LinkAppsViewWithEditorAction extends LinkWithEditorAction {

  AppsViewContentProvider provider;

  public LinkAppsViewWithEditorAction(IWorkbenchPage page, TreeViewer treeViewer) {
    super(page, treeViewer);
    provider = (AppsViewContentProvider) treeViewer.getContentProvider();
  }

  @Override
  protected void syncEditorToSelection(Object element) {
    if (element instanceof ElementTreeNode) {
      try {
        element = ((ElementTreeNode) element).getModelElement().getCorrespondingResource();
      } catch (DartModelException ex) {
        // fall through
      }
    }
    super.syncEditorToSelection(element);
  }

  @Override
  protected void syncSelectionToEditor(Object file) {
    ElementTreeNode node = provider.findNode(file);
    super.syncSelectionToEditor(node);
  }
}
