/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and Implementation (Yuri Strot)
 *******************************************************************************/
package com.xored.glance.ui.controls.table;

import com.xored.glance.ui.controls.items.ItemProvider;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * @author Yuri Strot
 */
public class TableItemProvider implements ItemProvider {

  private static TableItemProvider INSTANCE;

  public static TableItemProvider getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new TableItemProvider();
    }
    return INSTANCE;
  }

  public static TableItem getItem(Item item) {
    return (TableItem) item;
  }

  private TableItemProvider() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.xored.glance.ui.controls.items.ItemProvider#compare(org.eclipse.swt.widgets.Item,
   * org.eclipse.swt.widgets.Item)
   */
  @Override
  public int compare(Item item1, Item item2) {
    if (item1.equals(item2)) {
      return 0;
    }
    Table table = getItem(item1).getParent();
    TableItem[] items = table.getItems();
    for (TableItem item : items) {
      if (item1.equals(item)) {
        return -1;
      }
      if (item2.equals(item)) {
        return 1;
      }
    }
    return 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.xored.glance.internal.items.ItemProvider#getBackground(org.eclipse .swt.widgets.Item,
   * int)
   */
  @Override
  public Color getBackground(Item item, int index) {
    return getItem(item).getBackground(index);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.xored.glance.internal.items.ItemProvider#getBounds(org.eclipse.swt .widgets.Item, int)
   */
  @Override
  public Rectangle getBounds(Item item, int index) {
    return getItem(item).getBounds(index);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.xored.glance.internal.items.ItemProvider#getColumnCount(org.eclipse .swt.widgets.Item)
   */
  @Override
  public int getColumnCount(Item item) {
    return getItem(item).getParent().getColumnCount();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.xored.glance.internal.items.ItemProvider#getFont(org.eclipse.swt. widgets.Item, int)
   */
  @Override
  public Font getFont(Item item, int index) {
    return getItem(item).getFont(index);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.xored.glance.internal.items.ItemProvider#getForeground(org.eclipse .swt.widgets.Item,
   * int)
   */
  @Override
  public Color getForeground(Item item, int index) {
    return getItem(item).getForeground(index);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.xored.glance.internal.items.ItemProvider#getImage(org.eclipse.swt .widgets.Item, int)
   */
  @Override
  public Image getImage(Item item, int index) {
    return getItem(item).getImage(index);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.xored.glance.internal.items.ItemProvider#getImageBounds(org.eclipse .swt.widgets.Item,
   * int)
   */
  @Override
  public Rectangle getImageBounds(Item item, int index) {
    return getItem(item).getImageBounds(index);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.xored.glance.internal.items.ItemProvider#getText(org.eclipse.swt. widgets.Item, int)
   */
  @Override
  public String getText(Item item, int index) {
    return getItem(item).getText(index);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.xored.glance.internal.items.ItemProvider#getTextBounds(org.eclipse .swt.widgets.Item,
   * int)
   */
  @Override
  public Rectangle getTextBounds(Item item, int index) {
    return getItem(item).getTextBounds(index);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.xored.glance.internal.items.ItemProvider#select(org.eclipse.swt.widgets .Item)
   */
  @Override
  public void select(Item item) {
    TableItem tItem = getItem(item);
    tItem.getParent().setSelection(tItem);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.xored.glance.internal.items.ItemProvider#showItem(org.eclipse.swt .widgets.Item)
   */
  @Override
  public void show(Item item) {
    TableItem tItem = getItem(item);
    tItem.getParent().showItem(tItem);
  }

}
