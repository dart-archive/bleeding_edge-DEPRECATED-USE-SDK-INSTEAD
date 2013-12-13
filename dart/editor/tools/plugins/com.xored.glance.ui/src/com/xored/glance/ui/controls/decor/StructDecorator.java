/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.ui.controls.decor;

import com.xored.glance.ui.sources.ColorManager;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;

import java.lang.reflect.Field;

public class StructDecorator implements Listener {

  private Composite composite;
  private IStructProvider provider;

  private TextLayout textLayout;

  private Listener[] eraseListeners;

  private Listener[] paintListeners;

  private boolean disposed;

  public StructDecorator(Composite composite, IStructProvider provider) {
    this.composite = composite;
    this.provider = provider;
    init();
  }

  public void dispose() {
    if (!disposed) {
      disposed = true;
      if (!composite.isDisposed()) {
        composite.removeListener(SWT.EraseItem, StructDecorator.this);
        for (Listener listener : eraseListeners) {
          composite.addListener(SWT.EraseItem, listener);
        }

        composite.removeListener(SWT.PaintItem, StructDecorator.this);
        for (Listener listener : paintListeners) {
          composite.addListener(SWT.PaintItem, listener);
        }

        if ("gtk".equalsIgnoreCase(Platform.getWS()) && composite instanceof Tree) {
          try {
            Field field = composite.getClass().getDeclaredField("drawForeground");
            field.setAccessible(true);
            if (field.get(composite) != null) {
              // System.out.println("Fixed tree drawForeground bug");
              field.set(composite, null);
            }
          } catch (Exception e) {
            // ignore if no such field
          }
        }

        redraw();
      }
    }
  }

  public void erase(Event event) {
    int style = SWT.BACKGROUND | SWT.FOREGROUND;
    if (!ColorManager.getInstance().isUseNative()) {
      style |= SWT.SELECTED | SWT.HOT;
    }

    event.detail &= ~style;
  }

  @Override
  public void handleEvent(Event event) {
    switch (event.type) {
      case SWT.PaintItem:
        paint(event);
        break;
      case SWT.EraseItem:
        erase(event);
        break;
    }
  }

  public boolean isDisposed() {
    return disposed;
  }

  public void redraw() {
    Rectangle rect = composite.getClientArea();
    composite.redraw(rect.x, rect.y, rect.width, rect.height, true);
  }

  public void redraw(StructCell cell) {
    Rectangle rect = cell.getBounds();
    composite.redraw(rect.x, rect.y, rect.width, rect.height, true);
  }

  protected TextLayout getTextLayout() {
    if (textLayout == null) {
      int orientation = composite.getStyle() & (SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT);
      textLayout = new TextLayout(composite.getDisplay());
      textLayout.setOrientation(orientation);
    } else {
      textLayout.setText("");
    }
    return textLayout;
  }

  protected void init() {
    eraseListeners = addListener(SWT.EraseItem);
    paintListeners = addListener(SWT.PaintItem);
    redraw();
  }

  protected void paint(Event event) {
    Item item = (Item) event.item;
    GC gc = event.gc;
    // remember colors to restore the GC later
    Color oldForeground = gc.getForeground();
    Color oldBackground = gc.getBackground();

    StructCell cell = provider.getCell(item, event.index);

    Color foreground = cell.getForeground();
    if (foreground != null) {
      gc.setForeground(foreground);
    }

    Color background = cell.getBackground();
    if (background != null) {
      gc.setBackground(background);
    }

    if (!ColorManager.getInstance().isUseNative() && cell.isSelected()) {
      gc.setBackground(ColorManager.getInstance().getTreeSelectionBg());
      gc.setForeground(ColorManager.getInstance().getTreeSelectionFg());
      gc.fillRectangle(cell.getBounds());
    }

    Image image = cell.getImage();
    if (image != null) {
      Rectangle imageBounds = cell.getImageBounds();
      if (imageBounds != null) {
        Rectangle bounds = image.getBounds();

        // center the image in the given space
        int x = imageBounds.x + Math.max(0, (imageBounds.width - bounds.width) / 2);
        int y = imageBounds.y + Math.max(0, (imageBounds.height - bounds.height) / 2);
        gc.drawImage(image, x, y);
      }
    }

    Rectangle textBounds = cell.getTextBounds();
    if (textBounds != null) {
      TextLayout layout = getTextLayout();
      layout.setText(cell.getText());
      layout.setFont(cell.getFont());

      StyleRange[] styles = cell.styles;
      for (StyleRange range : styles) {
        layout.setStyle(range, range.start, range.start + range.length - 1);
      }

      Rectangle layoutBounds = layout.getBounds();

      int x = textBounds.x;
      int avg = (textBounds.height - layoutBounds.height) / 2;
      int y = textBounds.y + Math.max(0, avg);

      layout.draw(gc, x, y);
    }

    gc.setForeground(oldForeground);
    gc.setBackground(oldBackground);
  }

  private Listener[] addListener(int event) {
    Listener[] listeners = composite.getListeners(event);
    // should never happen, but just in case
    if (listeners == null) {
      listeners = new Listener[0];
    }
    for (Listener listener : listeners) {
      composite.removeListener(event, listener);
    }
    composite.addListener(event, this);
    return listeners;
  }

}
