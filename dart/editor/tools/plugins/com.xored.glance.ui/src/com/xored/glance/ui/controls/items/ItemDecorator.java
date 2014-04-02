/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and Implementation (Yuri Strot)
 *******************************************************************************/
package com.xored.glance.ui.controls.items;

import com.xored.glance.ui.sources.ColorManager;
import com.xored.glance.ui.sources.ITextSourceListener;
import com.xored.glance.ui.utils.TextUtils;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextPresentation;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * @author Yuri Strot
 */
public class ItemDecorator implements Listener {

  public static final int DEFAULT_STYLE = SWT.BACKGROUND | SWT.FOREGROUND | SWT.SELECTED | SWT.HOT;

  protected Composite composite;
  protected ItemProvider provider;
  protected int style;

  protected TextLayout textLayout;
  protected Map<ItemCell, StyleRange[]> itemToMatches;
  protected Map<ItemCell, StyleRange[]> cacheStyles;
  protected List<ItemCell> cells;
  protected HashSet<ItemCell> cellSet;

  private ListenerList listeners = new ListenerList();

  private boolean disposed;

  public ItemDecorator(Composite composite, ItemProvider provider) {
    this(composite, provider, DEFAULT_STYLE);
  }

  public ItemDecorator(Composite composite, ItemProvider provider, int style) {
    this.composite = composite;
    this.provider = provider;
    this.style = style;
    clearStyles();
    init();
  }

  public void addTextSourceListener(ITextSourceListener listener) {
    listeners.add(listener);
  }

  public void blocksChanged(ItemCell[] removed, ItemCell[] added) {
    for (ItemCell cell : removed) {
      cells.remove(cell);
      cellSet.remove(cell);
    }
    for (ItemCell cell : added) {
      cells.add(cell);
      cellSet.add(cell);
      cell.getItem().addListener(SWT.Dispose, this);
    }
    for (ITextSourceListener listener : getListeners()) {
      listener.blocksChanged(removed, added);
    }
  }

  public void clearStyles() {
    itemToMatches = new HashMap<ItemCell, StyleRange[]>();
    cacheStyles = new HashMap<ItemCell, StyleRange[]>();
  }

  public void dispose() {
    if (!disposed) {
      clearStyles();
      composite.removeListener(SWT.PaintItem, this);
      composite.removeListener(SWT.EraseItem, this);
      disposed = true;
      redraw();
    }
  }

  public void erase(Event event) {
    int style = SWT.BACKGROUND | SWT.FOREGROUND;
    if (!ColorManager.getInstance().isUseNative()) {
      style |= SWT.SELECTED | SWT.HOT;
    }

    event.detail &= ~style;
  }

  public List<ItemCell> getCells() {
    return cells;
  }

  public ITextSourceListener[] getListeners() {
    Object[] objects = listeners.getListeners();
    ITextSourceListener[] listeners = new ITextSourceListener[objects.length];
    System.arraycopy(objects, 0, listeners, 0, objects.length);
    return listeners;
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
      case SWT.Dispose:
        if (event.widget instanceof Item) {
          ItemCell cell = new ItemCell((Item) event.widget, event.index, provider);
          blocksChanged(new ItemCell[] {cell}, new ItemCell[0]);
        }
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

  public void redraw(ItemCell cell) {
    Rectangle rect = provider.getBounds(cell.getItem(), cell.getIndex());
    composite.redraw(rect.x, rect.y, rect.width, rect.height, true);
  }

  public void removeTextSourceListener(ITextSourceListener listener) {
    listeners.remove(listener);
  }

  public void setCells(List<ItemCell> cells) {
    this.cells = cells;
    for (ItemCell cell : cells) {
      cell.getItem().addListener(SWT.Dispose, this);
    }
    cellSet = new HashSet<ItemCell>(cells);
  }

  public void setStyles(ItemCell cell, StyleRange[] styles) {
    if (styles == null) {
      itemToMatches.remove(cell);
    } else {
      itemToMatches.put(cell, styles);
    }
    cacheStyles.put(cell, calculateStyles(cell));
  }

  protected StyleRange[] calculateStyles(ItemCell cell) {
    StyleRange[] cellStyles = cell.getStyles();
    StyleRange[] matchStyles = itemToMatches.get(cell);
    if (matchStyles == null || matchStyles.length == 0) {
      return cellStyles;
    }
    if (cellStyles.length == 0) {
      return matchStyles;
    }
    Region region = new Region(0, cell.getLength());
    int size = cellStyles.length + matchStyles.length;
    TextPresentation presentation = new TextPresentation(region, size);
    presentation.replaceStyleRanges(cellStyles);
    presentation.mergeStyleRanges(matchStyles);
    return TextUtils.getStyles(presentation);
  }

  protected StyleRange[] getRanges(ItemCell cell) {
    StyleRange[] ranges = cacheStyles.get(cell);
    if (ranges == null) {
      ranges = calculateStyles(cell);
      cacheStyles.put(cell, ranges);
    }
    return ranges;
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
    // FIXME
    composite.addListener(SWT.EraseItem, this);
    composite.addListener(SWT.PaintItem, this);
    redraw();
  }

  protected void paint(Event event) {
    Item item = (Item) event.item;
    GC gc = event.gc;
    // remember colors to restore the GC later
    Color oldForeground = gc.getForeground();
    Color oldBackground = gc.getBackground();

    Color foreground = provider.getForeground(item, event.index);
    if (foreground != null) {
      gc.setForeground(foreground);
    }

    Color background = provider.getBackground(item, event.index);
    if (background != null) {
      gc.setBackground(background);
    }

    if (!ColorManager.getInstance().isUseNative() && (event.detail & SWT.SELECTED) != 0) {
      gc.setBackground(ColorManager.getInstance().getTreeSelectionBg());
      gc.setForeground(ColorManager.getInstance().getTreeSelectionFg());
      gc.fillRectangle(provider.getBounds(item, event.index));
    }

    Image image = provider.getImage(item, event.index);
    if (image != null) {
      Rectangle imageBounds = provider.getImageBounds(item, event.index);
      if (imageBounds != null) {
        Rectangle bounds = image.getBounds();

        // center the image in the given space
        int x = imageBounds.x + Math.max(0, (imageBounds.width - bounds.width) / 2);
        int y = imageBounds.y + Math.max(0, (imageBounds.height - bounds.height) / 2);
        gc.drawImage(image, x, y);
      }
    }

    Rectangle textBounds = provider.getTextBounds(item, event.index);
    if (textBounds != null) {
      TextLayout layout = getTextLayout();
      layout.setText(provider.getText(item, event.index));
      layout.setFont(provider.getFont(item, event.index));

      ItemCell cell = new ItemCell(item, event.index, provider);
      if (!cellSet.contains(cell)) {
        blocksChanged(new ItemCell[0], new ItemCell[] {cell});
      }
      StyleRange[] ranges = getRanges(cell);
      for (StyleRange range : ranges) {
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

}
