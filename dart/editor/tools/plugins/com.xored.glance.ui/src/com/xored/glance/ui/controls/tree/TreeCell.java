/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.ui.controls.tree;

import com.xored.glance.ui.controls.decor.StructCell;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.TreeItem;

public class TreeCell extends StructCell {

  private TreeItem item;

  public TreeCell(TreeItem item, int column) {
    super(column);
    this.item = item;
  }

  @Override
  public Color getBackground() {
    return item.getBackground(getColumn());
  }

  @Override
  public Rectangle getBounds() {
    return item.getBounds(getColumn());
  }

  @Override
  public Font getFont() {
    return item.getFont(getColumn());
  }

  @Override
  public Color getForeground() {
    return item.getForeground(getColumn());
  }

  @Override
  public Image getImage() {
    return item.getImage(getColumn());
  }

  @Override
  public Rectangle getImageBounds() {
    return item.getImageBounds(getColumn());
  }

  @Override
  public String getText() {
    return item.getText(getColumn());
  }

  @Override
  public Rectangle getTextBounds() {
    return item.getTextBounds(getColumn());
  }

  public TreeItem getTreeItem() {
    return item;
  }

  @Override
  public boolean isSelected() {
    TreeItem[] items = item.getParent().getSelection();
    for (TreeItem treeItem : items) {
      if (treeItem == item) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected Item getItem() {
    return item;
  }

}
