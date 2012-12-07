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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;

/**
 * {@link Canvas} to show a single centered SWT {@link Image}, may be with scrolling
 */
class ImageCanvas extends Canvas {

  private Image image;

  public ImageCanvas(Composite parent, int style) {
    super(parent, style | SWT.H_SCROLL | SWT.V_SCROLL);

    ScrollBar sb = getHorizontalBar();
    sb.setIncrement(20);
    sb.addListener(SWT.Selection, new Listener() {
      @Override
      public void handleEvent(Event e) {
        repaint();
      }
    });

    sb = getVerticalBar();
    sb.setIncrement(20);
    sb.addListener(SWT.Selection, new Listener() {
      @Override
      public void handleEvent(Event e) {
        repaint();
      }
    });

    addListener(SWT.Resize, new Listener() {
      @Override
      public void handleEvent(Event e) {
        updateScrollbars();
      }
    });

    addListener(SWT.Paint, new Listener() {
      @Override
      public void handleEvent(Event event) {
        paint(event.gc);
      }
    });
  }

  public void repaint() {
    if (!isDisposed()) {
      GC gc = new GC(this);
      paint(gc);
      gc.dispose();
    }
  }

  public void setImage(Image img) {
    image = img;
    if (!isDisposed()) {
      getHorizontalBar().setSelection(0);
      getVerticalBar().setSelection(0);
      updateScrollbars();
      getParent().layout();
      redraw();
    }
  }

  void paint(GC gc) {
    if (image != null) {
      Rectangle bounds = image.getBounds();
      Rectangle clientArea = getClientArea();

      int x;
      if (bounds.width < clientArea.width) {
        x = (clientArea.width - bounds.width) / 2;
      } else {
        x = -getHorizontalBar().getSelection();
      }

      int y;
      if (bounds.height < clientArea.height) {
        y = (clientArea.height - bounds.height) / 2;
      } else {
        y = -getVerticalBar().getSelection();
      }

      gc.drawImage(image, x, y);
    }
  }

  private void updateScrollbars() {
    Rectangle bounds = image != null ? image.getBounds() : new Rectangle(0, 0, 0, 0);
    Point size = getSize();
    Rectangle clientArea = getClientArea();

    ScrollBar horizontal = getHorizontalBar();
    if (bounds.width <= clientArea.width) {
      horizontal.setVisible(false);
      horizontal.setSelection(0);
    } else {
      horizontal.setPageIncrement(clientArea.width - horizontal.getIncrement());
      int max = bounds.width + (size.x - clientArea.width);
      horizontal.setMaximum(max);
      horizontal.setThumb(size.x > max ? max : size.x);
      horizontal.setVisible(true);
    }

    ScrollBar vertical = getVerticalBar();
    if (bounds.height <= clientArea.height) {
      vertical.setVisible(false);
      vertical.setSelection(0);
    } else {
      vertical.setPageIncrement(clientArea.height - vertical.getIncrement());
      int max = bounds.height + (size.y - clientArea.height);
      vertical.setMaximum(max);
      vertical.setThumb(size.y > max ? max : size.y);
      vertical.setVisible(true);
    }
  }

}
