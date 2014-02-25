/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.ui.controls.text.styled;

import org.eclipse.jface.text.Region;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;

public class StyledTextSelector extends TextSelector {

  private final StyledText text;

  public StyledTextSelector(StyledText text) {
    this.text = text;
    init();
  }

  @Override
  protected Control getControl() {
    return text;
  }

  @Override
  protected Region getSelection() {
    final Point point = text.getSelection();
    return new Region(point.x, point.y - point.x);
  }

  @Override
  protected void reveal(int offset, int length) {
    Region region = getSelection();
    if (region.getOffset() == offset && region.getLength() == length) {
      text.showSelection();
    } else {
      setSelection(offset, length);
      try {
        text.showSelection();
      } finally {
        text.setSelectionRange(region.getOffset(), region.getLength());
      }
    }
  }

  @Override
  protected void setSelection(int offset, int length) {
    text.setSelectionRange(offset, length);
  }

}
