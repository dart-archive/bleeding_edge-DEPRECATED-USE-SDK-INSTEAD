/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.ui.controls.tree;

import com.xored.glance.ui.sources.ITextBlock;
import com.xored.glance.ui.sources.ITextBlockListener;
import com.xored.glance.ui.sources.TextChangedEvent;

import org.eclipse.core.runtime.ListenerList;

public class TreeItemContent implements ITextBlock {

  private TreeNode node;
  private String text;
  private ListenerList listeners = new ListenerList();
  private int column;

  public TreeItemContent(TreeNode node, String text, int column) {
    this.node = node;
    node.items.add(this);
    this.text = text;
    this.column = column;
  }

  @Override
  public void addTextBlockListener(ITextBlockListener listener) {
    listeners.add(listener);
  }

  @Override
  public int compareTo(ITextBlock that) {
    TreeItemContent item = (TreeItemContent) that;
    int diff = this.node.compareTo(item.node);
    if (diff != 0) {
      return diff;
    }
    return column - item.column;
  }

  public TreeNode getNode() {
    return node;
  }

  @Override
  public String getText() {
    return text;
  }

  @Override
  public void removeTextBlockListener(ITextBlockListener listener) {
    listeners.remove(listener);
  }

  public void setText(String text) {
    if (text.equals(this.text)) {
      return;
    }
    int length = text.length();
    TextChangedEvent event = new TextChangedEvent(0, length, this.text);
    this.text = text;
    for (Object object : listeners.getListeners()) {
      ITextBlockListener listener = (ITextBlockListener) object;
      listener.textChanged(event);
    }
  }

  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("(");
    buffer.append(text);
    buffer.append(", ");
    buffer.append(column);
    buffer.append(")");
    return buffer.toString();
  }

}
