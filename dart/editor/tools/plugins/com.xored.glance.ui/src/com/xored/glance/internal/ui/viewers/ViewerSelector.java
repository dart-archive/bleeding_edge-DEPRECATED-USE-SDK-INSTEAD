/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.internal.ui.viewers;

import com.xored.glance.ui.controls.text.styled.TextSelector;

import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.custom.StyledText;

public class ViewerSelector extends TextSelector {

  private final TextViewer viewer;
  private final StyledText control;

  public ViewerSelector(TextViewer viewer) {
    this.viewer = viewer;
    this.control = viewer.getTextWidget();
    init();
  }

  @Override
  protected StyledText getControl() {
    return control;
  }

  @Override
  protected Region getSelection() {
    TextSelection selection = (TextSelection) viewer.getSelection();
    return new Region(selection.getOffset(), selection.getLength());
  }

  @Override
  protected void reveal(int offset, int length) {
    viewer.revealRange(offset, length);
  }

  @Override
  protected void setSelection(int offset, int length) {
    viewer.setSelection(new TextSelection(offset, length));
  }

}
