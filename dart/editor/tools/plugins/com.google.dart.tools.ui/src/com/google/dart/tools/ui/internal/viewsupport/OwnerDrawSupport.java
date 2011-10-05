/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.viewsupport;

import com.google.dart.tools.ui.internal.viewsupport.ColoredString.StyleRange;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TreeItem;

import java.util.Iterator;

/**
 * Adding owner draw support to a control
 */
public abstract class OwnerDrawSupport implements Listener {

  private TextLayout fTextLayout;
  private final Control fControl;

  public OwnerDrawSupport(Control control) {
    fControl = control;
    fTextLayout = new TextLayout(control.getDisplay());

    control.addListener(SWT.PaintItem, this);
    control.addListener(SWT.EraseItem, this);
    control.addListener(SWT.Dispose, this);
  }

  public void dispose() {
    if (fTextLayout != null) {
      fTextLayout.dispose();
      fTextLayout = null;
    }
    if (!fControl.isDisposed()) {
      fControl.removeListener(SWT.PaintItem, this);
      fControl.removeListener(SWT.EraseItem, this);
      fControl.removeListener(SWT.Dispose, this);
    }
  }

  /**
   * Return the color for the given style
   * 
   * @param foregroundColorName the name of the color
   * @param display the current display
   * @return the color
   */
  public abstract Color getColor(String foregroundColorName, Display display);

  /**
   * Return the colored label for the given item.
   * 
   * @param item the item to return the colored label for
   * @return the colored string
   */
  public abstract ColoredString getColoredLabel(Item item);

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
   */
  @Override
  public void handleEvent(Event event) {
    if (event.type == SWT.PaintItem) {
      performPaint(event);
    } else if (event.type == SWT.EraseItem) {
      performErase(event);
    } else if (event.type == SWT.Dispose) {
      dispose();
    }
  }

  private void performErase(Event event) {
    event.detail &= ~SWT.FOREGROUND;
  }

  private void performPaint(Event event) {
    Item item = (Item) event.item;
    GC gc = event.gc;

    ColoredString coloredLabel = getColoredLabel(item);
    boolean isSelected = (event.detail & SWT.SELECTED) != 0 && fControl.isFocusControl();
    if (item instanceof TreeItem) {
      TreeItem treeItem = (TreeItem) item;
      Image image = treeItem.getImage(event.index);
      if (image != null) {
        processImage(image, gc, treeItem.getImageBounds(event.index));
      }
      Rectangle textBounds = treeItem.getTextBounds(event.index);
      Font font = treeItem.getFont(event.index);
      processColoredLabel(coloredLabel, gc, textBounds, isSelected, font);

      Rectangle bounds = treeItem.getBounds();
      if ((event.detail & SWT.FOCUSED) != 0) {
        gc.drawFocus(bounds.x, bounds.y, bounds.width, bounds.height);
      }
    } else if (item instanceof TableItem) {
      TableItem tableItem = (TableItem) item;
      Image image = tableItem.getImage(event.index);
      if (image != null) {
        processImage(image, gc, tableItem.getImageBounds(event.index));
      }
      Rectangle textBounds = tableItem.getTextBounds(event.index);
      Font font = tableItem.getFont(event.index);
      processColoredLabel(coloredLabel, gc, textBounds, isSelected, font);

      Rectangle bounds = tableItem.getBounds();
      if ((event.detail & SWT.FOCUSED) != 0) {
        gc.drawFocus(bounds.x, bounds.y, bounds.width, bounds.height);
      }
    }
  }

  private void processColoredLabel(ColoredString richLabel, GC gc, Rectangle textBounds,
      boolean isSelected, Font font) {
    String text = richLabel.getString();
    fTextLayout.setText(text);
    fTextLayout.setFont(font);

    if (!isSelected) {
      // apply the styled ranges only when element is not selected
      Display display = (Display) gc.getDevice();
      Iterator<StyleRange> ranges = richLabel.getRanges();
      while (ranges.hasNext()) {
        ColoredString.StyleRange curr = ranges.next();
        ColoredString.Style style = curr.style;
        if (style != null) {
          Color foreground = getColor(style.getForegroundColorName(), display);
          TextStyle textStyle = new TextStyle(null, foreground, null);
          fTextLayout.setStyle(textStyle, curr.offset, curr.offset + curr.length - 1);
        }
      }
    }

    Rectangle bounds = fTextLayout.getBounds();
    int x = textBounds.x;
    int y = textBounds.y + Math.max(0, (textBounds.height - bounds.height) / 2);

    fTextLayout.draw(gc, x, y);
    fTextLayout.setText(""); // clear all ranges //$NON-NLS-1$
  }

  private void processImage(Image image, GC gc, Rectangle imageBounds) {
    Rectangle bounds = image.getBounds();
    int x = imageBounds.x + Math.max(0, (imageBounds.width - bounds.width) / 2);
    int y = imageBounds.y + Math.max(0, (imageBounds.height - bounds.height) / 2);
    gc.drawImage(image, x, y);
  }
}
