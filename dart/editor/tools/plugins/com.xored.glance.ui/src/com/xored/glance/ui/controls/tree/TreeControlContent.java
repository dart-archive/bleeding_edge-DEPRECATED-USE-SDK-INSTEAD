/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.ui.controls.tree;

import com.xored.glance.ui.sources.ConfigurationManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TreeControlContent extends TreeContent {

  private Tree tree;
  private final Map<TreeCell, TreeItemContent> cellToContent = new HashMap<TreeCell, TreeItemContent>();

  public TreeControlContent(Tree tree) {
    this.tree = tree;
    collectCells(this, tree.getItems());
  }

  @Override
  public void dispose() {
    tree = null;
  }

  @Override
  public TreeItemContent getContent(TreeCell cell) {
    TreeItemContent content = cellToContent.get(cell);
    if (content == null) {
      TreeItem parent = null;
      do {
        parent = cell.getTreeItem().getParentItem();
        if (parent == null) {
          break;
        }
        TreeCell parentCell = new TreeCell(parent, 0);
        cell = parentCell;
        content = cellToContent.get(parentCell);
      } while (content == null);
      if (content != null && parent != null) {
        collectCells(content.getNode(), parent.getItems());
        content = cellToContent.get(cell);
      }
    }
    return content;
  }

  @Override
  public void index(final IProgressMonitor monitor) {
    if (tree == null || tree.isDisposed()) {
      monitor.done();
      return;
    }
    tree.getDisplay().asyncExec(new Runnable() {
      @Override
      public void run() {
        try {
          if (tree == null || tree.isDisposed()) {
            return;
          }
          final LinkedList<TreeItem> items = new LinkedList<TreeItem>();
          for (TreeItem item : tree.getItems()) {
            items.add(item);
          }
          if (items.size() > 0) {
            expand(items, monitor);
          }
        } finally {
          monitor.done();
        }
      }
    });
  }

  private void collectCells(TreeNode node, TreeItem[] items) {
    if (items.length > 0) {
      int columns = items[0].getParent().getColumnCount();
      if (columns == 0) {
        columns = 1;
      }
      List<TreeNode> nodes = new ArrayList<TreeNode>(items.length);
      for (int i = 0; i < items.length; i++) {
        TreeItem item = items[i];
        TreeItemContent c = getContent(item);
        if (c != null) {
          continue;
        }
        TreeNode child = new TreeNode(item);
        for (int j = 0; j < columns; j++) {
          TreeCell cell = new TreeCell(item, j);
          TreeItemContent itemContent = new TreeItemContent(child, item.getText(j), j);
          cellToContent.put(cell, itemContent);
        }
        if (item.getExpanded()) {
          collectCells(child, item.getItems());
        }
        nodes.add(child);
      }
      if (nodes.size() > 0) {
        node.add(nodes.toArray(new TreeNode[nodes.size()]));
      }
    }
  }

  private void expand(final LinkedList<TreeItem> items, final IProgressMonitor monitor) {
    if (tree == null || tree.isDisposed()) {
      return;
    }
    final Display display = tree.getDisplay();
    BusyIndicatorUtils.withoutIndicator(display, new Runnable() {
      @Override
      public void run() {
        int maxIndexingDepth = ConfigurationManager.getInstance().getMaxIndexingDepth();
        int level = 1;
        TreeItem lastInLevel = items.getLast();
        monitor.beginTask("1/" + maxIndexingDepth, items.size());
        while (true) {
          if (tree == null || tree.isDisposed()
              || (maxIndexingDepth >= 0 && level >= maxIndexingDepth)) {
            return;
          }
          if (monitor.isCanceled()) {
            return;
          }
          TreeItem item = items.poll();
          if (item == null) {
            return;
          }
          try {
            if (item.isDisposed()) {
              continue;
            }
            if (!item.getExpanded()) {
              Event event = new Event();
              event.item = item;
              event.type = SWT.Expand;
              event.widget = item.getParent();
              event.display = display;
              event.widget.notifyListeners(SWT.Expand, event);
            }
            TreeItem[] kids = item.getItems();
            TreeItemContent content = getContent(item);
            if (content != null) {
              collectCells(content.getNode(), item.getItems());
            }
            for (TreeItem child : kids) {
              items.addLast(child);
            }
            while (display.readAndDispatch()) {
              ;
            }
          } finally {
            monitor.worked(1);
            if (item == lastInLevel && items.size() > 0) {
              lastInLevel = items.getLast();
              level++;
              int total = items.size();
              monitor.beginTask(level + "/?", total);
            }
          }
        }
      }
    });
  }

  private TreeItemContent getContent(TreeItem item) {
    return cellToContent.get(new TreeCell(item, 0));
  }

}
