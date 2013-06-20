/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and Implementation (Yuri Strot)
 *******************************************************************************/
package com.xored.glance.ui.controls.items;

import org.eclipse.jface.util.Policy;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Item;

import com.xored.glance.ui.sources.ITextBlock;
import com.xored.glance.ui.sources.ITextBlockListener;
import com.xored.glance.ui.utils.TextUtils;

/**
 * @author Yuri Strot
 */
public class ItemCell implements ITextBlock {

  private Item item;
  private int index;
  private ItemProvider provider;

  public static final String KEY_TEXT_LAYOUT = Policy.JFACE + "styled_label_key_"; //$NON-NLS-1$

  public ItemCell(Item item, int index, ItemProvider provider) {
    this.item = item;
    this.index = index;
    this.provider = provider;
  }

  public Image getImage() {
    return provider.getImage(item, index);
  }

  public Object getKey() {
    Object data = item.getData();
    if (data != null)
      return data;
    return item;
  }

  /**
   * @return the item
   */
  public Item getItem() {
    return item;
  }

  public String getText() {
    return provider.getColumnCount(item) == 0 ? item.getText() : provider.getText(item, index);
  }

  public int getLength() {
    return getText().length();
  }

  public StyleRange[] getStyles() {
    String key = KEY_TEXT_LAYOUT + index;
    Object data = item.getData(key);
    if (data instanceof StyleRange[]) {
      return TextUtils.copy((StyleRange[]) data);
    }
    return new StyleRange[0];
  }

  /**
   * @return the index
   */
  public int getIndex() {
    return index;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return item.hashCode() ^ index;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    ItemCell item = (ItemCell) obj;
    return item.item.equals(this.item) && item.index == index;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(ITextBlock block) {
    ItemCell cell = (ItemCell) block;
    return provider.compare(item, cell.item);
  }

  public void addTextBlockListener(ITextBlockListener listener) {
  }

  public void removeTextBlockListener(ITextBlockListener listener) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("{");
    buffer.append(item);
    buffer.append(", ");
    buffer.append(index);
    buffer.append("}");
    return buffer.toString();
  }

}
