/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and Implementation (Yuri Strot)
 *******************************************************************************/
package com.xored.glance.ui.controls.items;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Item;

/**
 * @author Yuri Strot
 */
public interface ItemProvider {

  public int compare(Item item1, Item item2);

  public Color getBackground(Item item, int index);

  public Rectangle getBounds(Item item, int index);

  public int getColumnCount(Item item);

  public Font getFont(Item item, int index);

  public Color getForeground(Item item, int index);

  public Image getImage(Item item, int index);

  public Rectangle getImageBounds(Item item, int index);

  public String getText(Item item, int index);

  public Rectangle getTextBounds(Item item, int index);

  public void select(Item item);

  public void show(Item item);

}
