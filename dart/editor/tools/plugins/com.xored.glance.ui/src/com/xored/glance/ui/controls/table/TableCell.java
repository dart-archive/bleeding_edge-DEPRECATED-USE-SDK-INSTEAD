/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.ui.controls.table;

import com.xored.glance.ui.controls.decor.StructCell;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.TableItem;

public class TableCell extends StructCell {

  private TableItem item;

  public TableCell(TableItem item, int column) {
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

  public TableItem getTableItem() {
    return item;
  }

  @Override
  public String getText() {
    return item.getText(getColumn());
  }

  @Override
  public Rectangle getTextBounds() {
    return item.getTextBounds(getColumn());
  }

  @Override
  public boolean isSelected() {
    TableItem[] items = item.getParent().getSelection();
    for (TableItem treeItem : items) {
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
