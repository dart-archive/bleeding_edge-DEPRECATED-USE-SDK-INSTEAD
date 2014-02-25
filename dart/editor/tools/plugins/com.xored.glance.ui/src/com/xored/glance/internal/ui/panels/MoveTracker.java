/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and Implementation (Yuri Strot)
 *******************************************************************************/
package com.xored.glance.internal.ui.panels;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * @author Yuri Strot
 */
public class MoveTracker implements Listener {

  private Cursor cursor;

  private Point iLocation;

  private Control control;

  public MoveTracker(Control control) {
    this.control = control;
    cursor = new Cursor(control.getDisplay(), SWT.CURSOR_SIZEALL);
    control.setCursor(cursor);
    control.addListener(SWT.MouseDown, this);
    control.addListener(SWT.Dispose, this);
  }

  @Override
  public void handleEvent(Event event) {
    if (control != null && !control.isDisposed()) {
      switch (event.type) {
        case SWT.MouseDown:
          Point point = control.getDisplay().getCursorLocation();
          handleClick(point.x, point.y);
          break;
        case SWT.MouseMove:
          point = control.getDisplay().getCursorLocation();
          handleDrag(point.x - iLocation.x, point.y - iLocation.y);
          break;
        case SWT.Dispose:
          cursor.dispose();
          cursor = null;
        case SWT.MouseUp:
          control.getDisplay().removeFilter(SWT.MouseMove, this);
          control.getDisplay().removeFilter(SWT.MouseUp, this);
      }

    }
  }

  protected void handleClick(int x, int y) {
    iLocation = new Point(x, y);
    control.getDisplay().addFilter(SWT.MouseMove, this);
    control.getDisplay().addFilter(SWT.MouseUp, this);
  }

  protected void handleDrag(int dx, int dy) {
  }

}
