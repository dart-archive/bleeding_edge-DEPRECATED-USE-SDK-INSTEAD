/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.designer.editor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

/**
 * Horizontal or vertical line, line {@link Label} separator, but without highlight.
 * 
 * @author scheglov_ke
 * @coverage XML.editor
 */
public final class LineControl extends Canvas {
  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public LineControl(Composite parent, int style) {
    super(parent, style);
    addListener(SWT.Paint, new Listener() {
      @Override
      public void handleEvent(Event event) {
        GC gc = event.gc;
        gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
        if (isHorizontal()) {
          gc.drawLine(0, 0, getSize().x, 0);
        } else {
          gc.drawLine(0, 0, 0, getSize().y);
        }
      }
    });
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Control
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public Point computeSize(int wHint, int hHint, boolean changed) {
    if (isHorizontal()) {
      return new Point(wHint, 1);
    } else {
      return new Point(1, hHint);
    }
  }

  private boolean isHorizontal() {
    return (getStyle() & SWT.HORIZONTAL) == SWT.HORIZONTAL;
  }
}
