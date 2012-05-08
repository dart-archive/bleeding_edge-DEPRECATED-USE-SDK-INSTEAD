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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.ui.PlatformUI;

/**
 * This action selects all entries currently showing in view.
 */
public class SelectAllAction extends Action {

  private StructuredViewer fViewer;

  /**
   * Creates the action.
   */
  public SelectAllAction() {
    super("selectAll"); //$NON-NLS-1$
    setText(SearchMessages.SelectAllAction_label);
    setToolTipText(SearchMessages.SelectAllAction_tooltip);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, ISearchHelpContextIds.SELECT_ALL_ACTION);
  }

  public void setViewer(StructuredViewer viewer) {
    fViewer = viewer;
  }

  private void collectExpandedAndVisible(TreeItem[] items, List<TreeItem> result) {
    for (int i = 0; i < items.length; i++) {
      TreeItem item = items[i];
      result.add(item);
      if (item.getExpanded()) {
        collectExpandedAndVisible(item.getItems(), result);
      }
    }
  }

  /**
   * Selects all resources in the view.
   */
  public void run() {
    if (fViewer == null || fViewer.getControl().isDisposed()) {
      return;
    }
    if (fViewer instanceof TreeViewer) {
      ArrayList<TreeItem> allVisible = new ArrayList<TreeItem>();
      Tree tree = ((TreeViewer) fViewer).getTree();
      collectExpandedAndVisible(tree.getItems(), allVisible);
      tree.setSelection(allVisible.toArray(new TreeItem[allVisible.size()]));
    } else if (fViewer instanceof TableViewer) {
      ((TableViewer) fViewer).getTable().selectAll();
      // force viewer selection change
      fViewer.setSelection(fViewer.getSelection());
    }
  }
}
