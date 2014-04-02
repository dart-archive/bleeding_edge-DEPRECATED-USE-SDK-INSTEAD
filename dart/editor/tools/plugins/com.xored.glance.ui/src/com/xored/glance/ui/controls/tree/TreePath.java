/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.ui.controls.tree;

import com.xored.glance.ui.controls.decor.IPath;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.progress.PendingUpdateAdapter;

import java.util.ArrayList;
import java.util.List;

public class TreePath implements IPath {

  protected List<TreeNode> list = new ArrayList<TreeNode>();
  protected TreeContent content;

  private boolean cancel;

  public TreePath(TreeNode node) {
    content = node.getRoot();
    TreeNode cur = node;
    while (cur != null && cur != content) {
      list.add(0, cur);
      cur = cur.parent;
    }
  }

  @Override
  public void discardSelection() {
    this.cancel = true;
  }

  @Override
  public void select(Composite composite) {
    Tree tree = (Tree) composite;
    select(tree.getItems(), 0);
  }

  private void expand(TreeItem item) {
    Event event = new Event();
    event.item = item;
    event.type = SWT.Expand;
    event.widget = item.getParent();
    event.display = item.getDisplay();
    event.widget.notifyListeners(SWT.Expand, event);
    item.setExpanded(true);
  }

  private TreeNode getNode(TreeItem item) {
    TreeCell cell = new TreeCell(item, 0);
    TreeItemContent itemContent = content.getContent(cell);
    return itemContent != null ? itemContent.getNode() : null;
  }

  private void select(final TreeItem[] items, final int index) {
    if (cancel) {
      return;
    }
    TreeNode node = list.get(index);
    for (final TreeItem item : items) {
      if (getNode(item) == node) {
        final Tree tree = item.getParent();
        if (index == list.size() - 1) {
          // After tree item removes tree selection restores
          // we should set selection after this
          tree.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
              tree.setSelection(item);
              tree.showSelection();
            }
          });
        } else {
          if (!item.getExpanded()) {
            expand(item);
          }
          select(item.getItems(), index + 1);
        }
        return;
      }
    }
    if (items.length > 0) {
      TreeItem item = items[0];
      final TreeItem parent = item.getParentItem();
      if (item.getData() instanceof PendingUpdateAdapter) {
        item.addDisposeListener(new DisposeListener() {
          @Override
          public void widgetDisposed(DisposeEvent e) {
            select(parent.getItems(), index);
          }
        });
      }
    }
  }

}
