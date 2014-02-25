/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.ui.controls.tree;

import com.xored.glance.ui.controls.decor.StructCell;
import com.xored.glance.ui.controls.decor.StructSource;
import com.xored.glance.ui.sources.ITextBlock;
import com.xored.glance.ui.sources.SourceSelection;

import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

public abstract class TreeStructSource extends StructSource {

  public TreeStructSource(Tree tree) {
    super(tree);
    tree.addSelectionListener(this);
  }

  @Override
  public void dispose() {
    Tree tree = getControl();
    try {
      super.dispose();
    } finally {
      if (tree != null && !tree.isDisposed()) {
        tree.removeSelectionListener(this);
      }
    }
  }

  @Override
  public Tree getControl() {
    return (Tree) super.getControl();
  }

  @Override
  protected StructCell createCell(Item item, int column) {
    return new TreeCell((TreeItem) item, column);
  }

  @Override
  protected abstract TreeContent createContent();

  @Override
  protected SourceSelection getSourceSelection() {
    TreeItem[] items = getControl().getSelection();
    if (items.length > 0) {
      ITextBlock block = content.getContent(createCell(items[0], 0));
      if (block != null) {
        return new SourceSelection(block, 0, block.getText().length());
      }
    }
    return null;
  }

}
