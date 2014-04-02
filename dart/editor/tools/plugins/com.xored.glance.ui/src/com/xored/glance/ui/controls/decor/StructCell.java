/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.ui.controls.decor;

import com.xored.glance.ui.utils.TextUtils;

import org.eclipse.jface.util.Policy;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Item;

public abstract class StructCell extends Cell {

  static final StyleRange[] NO_STYLES = new StyleRange[0];

  static final String KEY_TEXT_LAYOUT = Policy.JFACE + "styled_label_key_"; //$NON-NLS-1$

  public StyleRange[] styles = NO_STYLES;

  public StructCell(int column) {
    super(column);
  }

  public abstract Color getBackground();

  public abstract Rectangle getBounds();

  public abstract Font getFont();

  public abstract Color getForeground();

  public abstract Image getImage();

  public abstract Rectangle getImageBounds();

  public abstract String getText();

  public abstract Rectangle getTextBounds();

  public abstract boolean isSelected();

  @Override
  protected Object getElement() {
    return getItem();
  }

  protected abstract Item getItem();

  protected StyleRange[] nativeStyles() {
    String key = KEY_TEXT_LAYOUT + getColumn();
    Object data = getItem().getData(key);
    if (data instanceof StyleRange[]) {
      return TextUtils.copy((StyleRange[]) data);
    }
    return new StyleRange[0];
  }

}
