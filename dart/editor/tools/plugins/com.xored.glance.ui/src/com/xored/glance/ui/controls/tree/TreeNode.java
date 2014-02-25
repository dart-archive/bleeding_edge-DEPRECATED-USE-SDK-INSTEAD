/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.ui.controls.tree;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.TreeItem;

import java.util.ArrayList;
import java.util.List;

public class TreeNode implements DisposeListener {

  public static final TreeNode[] EMPTY = new TreeNode[0];

  List<TreeNode> kids = new ArrayList<TreeNode>();
  List<TreeItemContent> items = new ArrayList<TreeItemContent>();
  TreeNode parent;
  int[] index;

  TreeItem item;

  public TreeNode(TreeItem item) {
    this.item = item;
    if (item != null) {
      item.addDisposeListener(this);
    }
  }

  public void add(TreeNode[] nodes) {
    for (TreeNode node : nodes) {
      doAdd(node);
    }
    notifyRoot(TreeNode.EMPTY, nodes);
  }

  public int compareTo(TreeNode that) {
    int[] index = that.index;
    for (int i = 0; i < index.length && i < this.index.length; i++) {
      int diff = this.index[i] - index[i];
      if (diff != 0) {
        return diff;
      }
    }
    return this.index.length - index.length;
  }

  public TreeNode[] getChildren() {
    return kids.toArray(new TreeNode[0]);
  }

  public void remove(TreeNode[] nodes) {
    doRemove(nodes);
    notifyRoot(nodes, TreeNode.EMPTY);
  }

  @Override
  public String toString() {
    int iMax = items.size() - 1;
    if (iMax == -1) {
      return "[]";
    }
    StringBuilder b = new StringBuilder();
    b.append('[');
    for (int i = 0;; i++) {
      b.append(items.get(i));
      if (i == iMax) {
        return b.append(']').toString();
      }
      b.append(", ");
    }
  }

  @Override
  public void widgetDisposed(DisposeEvent e) {
    close();
  }

  protected void close() {
    if (item != null) {
      if (!item.isDisposed()) {
        item.removeDisposeListener(this);
      }
      for (TreeNode node : kids) {
        node.close();
      }
      notifyRoot(new TreeNode[] {this}, TreeNode.EMPTY);
      item = null;
    }
  }

  void collect(TreeNode item, List<TreeNode> items) {
    items.add(item);
    for (TreeNode kid : item.kids) {
      collect(kid, items);
    }
  }

  TreeNode[] collect(TreeNode[] items) {
    List<TreeNode> result = new ArrayList<TreeNode>();
    for (TreeNode item : items) {
      collect(item, result);
    }
    return result.toArray(new TreeNode[0]);
  }

  void doAdd(TreeNode node) {
    node.recalc(index, kids.size());
    kids.add(node);
    node.parent = this;
  }

  void doRemove(TreeNode[] nodes) {
    int minPos = Integer.MAX_VALUE;
    for (TreeNode node : nodes) {
      int pos = kids.indexOf(node);
      if (pos >= 0) {
        kids.remove(pos);
        minPos = Math.min(minPos, pos);
      }
    }
    for (int i = minPos; i < kids.size(); i++) {
      kids.get(i).recalc(index, i);
    }
  }

  TreeItemContent[] getContent(TreeNode[] nodes) {
    List<TreeItemContent> content = new ArrayList<TreeItemContent>();
    nodes = collect(nodes);
    for (TreeNode node : nodes) {
      content.addAll(node.items);
    }
    return content.toArray(new TreeItemContent[0]);
  }

  TreeContent getRoot() {
    TreeNode node = this;
    while (node != null) {
      if (node instanceof TreeContent) {
        return (TreeContent) node;
      }
      node = node.parent;
    }
    return null;
  }

  void notifyRoot(TreeNode[] removed, TreeNode[] added) {
    TreeContent root = getRoot();
    if (root != null) {
      root.changed(getContent(removed), getContent(added));
    }
  }

  void recalc(int[] parent, int cur) {
    if (parent != null) {
      index = new int[parent.length + 1];
      System.arraycopy(parent, 0, index, 0, parent.length);
      index[parent.length] = cur;
      for (int i = 0; i < kids.size(); i++) {
        kids.get(i).recalc(index, i);
      }
    }
  }

}
