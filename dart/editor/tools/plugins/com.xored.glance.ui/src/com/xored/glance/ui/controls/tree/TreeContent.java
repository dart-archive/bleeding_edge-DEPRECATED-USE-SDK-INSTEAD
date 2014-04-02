/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.ui.controls.tree;

import com.xored.glance.ui.controls.decor.IPath;
import com.xored.glance.ui.controls.decor.IStructContent;
import com.xored.glance.ui.controls.decor.StructCell;
import com.xored.glance.ui.sources.ITextBlock;
import com.xored.glance.ui.sources.ITextSourceListener;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;

import java.util.HashSet;
import java.util.Set;

public abstract class TreeContent extends TreeNode implements IStructContent {

  private ListenerList listeners = new ListenerList();

  private Set<TreeItemContent> blocks = new HashSet<TreeItemContent>();

  public TreeContent() {
    super(null);
    index = new int[0];
  }

  @Override
  public void addListener(ITextSourceListener listener) {
    listeners.add(listener);
  }

  @Override
  public void dispose() {
  }

  @Override
  public ITextBlock[] getBlocks() {
    return blocks.toArray(new TreeItemContent[0]);
  }

  @Override
  public final ITextBlock getContent(StructCell cell) {
    return getContent((TreeCell) cell);
  }

  public abstract TreeItemContent getContent(TreeCell cell);

  @Override
  public ITextSourceListener[] getListeners() {
    Object[] objects = listeners.getListeners();
    ITextSourceListener[] listeners = new ITextSourceListener[objects.length];
    System.arraycopy(objects, 0, listeners, 0, objects.length);
    return listeners;
  }

  @Override
  public IPath getPath(ITextBlock block) {
    TreeItemContent content = (TreeItemContent) block;
    return new TreePath(content.getNode());
  }

  @Override
  public abstract void index(IProgressMonitor monitor);

  @Override
  public void removeListener(ITextSourceListener listener) {
    listeners.remove(listener);
  }

  void changed(TreeItemContent[] removed, TreeItemContent[] added) {
    for (TreeItemContent item : removed) {
      blocks.remove(item);
    }
    for (TreeItemContent item : added) {
      blocks.add(item);
    }
    for (ITextSourceListener listener : getListeners()) {
      listener.blocksChanged(removed, added);
    }
  }

}
