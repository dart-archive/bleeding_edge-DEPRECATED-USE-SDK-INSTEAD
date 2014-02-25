/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and Implementation (Yuri Strot)
 *******************************************************************************/
package com.xored.glance.ui.controls.items;

import com.xored.glance.ui.sources.BaseTextSource;
import com.xored.glance.ui.sources.ColorManager;
import com.xored.glance.ui.sources.ITextBlock;
import com.xored.glance.ui.sources.ITextSourceListener;
import com.xored.glance.ui.sources.Match;
import com.xored.glance.ui.sources.SourceSelection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Yuri Strot
 */
public abstract class ItemSource extends BaseTextSource implements SelectionListener {

  protected ItemDecorator decorator;
  protected Composite composite;

  private Match selection;

  private Map<ItemCell, List<Match>> cellToMatches;

  public ItemSource(Composite composite) {
    decorator = new ItemDecorator(composite, getItemProvider());
    this.composite = composite;
  }

  @Override
  public void addTextSourceListener(ITextSourceListener listener) {
    decorator.addTextSourceListener(listener);
  }

  @Override
  public void dispose() {
    if (selection != null) {
      getItemProvider().select(getCell(selection).getItem());
    }
    decorator.dispose();
  }

  @Override
  public ITextBlock[] getBlocks() {
    return getCells().toArray(new ITextBlock[getCells().size()]);
  }

  public List<ItemCell> getCells() {
    List<ItemCell> cells = decorator.getCells();
    if (cells == null) {
      cells = new ArrayList<ItemCell>();
      collectCells(cells);
      decorator.setCells(cells);
    }
    return cells;
  }

  /**
   * @return the composite
   */
  public Composite getControl() {
    return composite;
  }

  public Font getFont() {
    return composite.getFont();
  }

  @Override
  public boolean isDisposed() {
    return decorator.isDisposed();
  }

  @Override
  public void removeTextSourceListener(ITextSourceListener listener) {
    decorator.removeTextSourceListener(listener);
  }

  @Override
  public void select(Match match) {
    if (match != null) {
      Item item = getCell(match).getItem();
      getItemProvider().show(item);
      getItemProvider().select(item);
    }

    setMatch(match);
  }

  public void setMatch(Match match) {
    ItemCell cell1 = null, cell2 = null;
    if (selection != null) {
      cell1 = getCell(selection);
      selection = null;
      updateCell(cell1);
    }
    selection = match;
    if (selection != null) {
      cell2 = getCell(selection);
      updateCell(cell2);
    }
    if (cell1 != null) {
      decorator.redraw(cell1);
    }
    if (cell2 != null && cell2 != cell1) {
      decorator.redraw(cell2);
    }
  }

  public void setMatches(Match[] matches) {
    cellToMatches = new HashMap<ItemCell, List<Match>>();
    for (Match match : matches) {
      ItemCell cell = getCell(match);
      List<Match> list = cellToMatches.get(cell);
      if (list == null) {
        list = new ArrayList<Match>();
        cellToMatches.put(cell, list);
      }
      list.add(match);
    }
    decorator.clearStyles();
    for (ItemCell cell : cellToMatches.keySet()) {
      updateCell(cell);
    }
    decorator.redraw();
  }

  @Override
  public void show(Match[] matches) {
    setMatches(matches);
  }

  @Override
  public void widgetDefaultSelected(SelectionEvent e) {
    fireSelectionChanged();
  }

  @Override
  public void widgetSelected(SelectionEvent e) {
    fireSelectionChanged();
  }

  protected abstract void collectCells(List<ItemCell> cells);

  protected void fireSelectionChanged() {
    SourceSelection selection = getSelection();
    ITextSourceListener[] listeners = decorator.getListeners();
    for (ITextSourceListener listener : listeners) {
      listener.selectionChanged(selection);
    }
  }

  protected ItemCell getCell(Match match) {
    return (ItemCell) match.getBlock();
  }

  protected abstract ItemProvider getItemProvider();

  protected void updateCell(ItemCell cell) {
    List<StyleRange> list = new ArrayList<StyleRange>();
    boolean selectionFound = false;
    if (cellToMatches != null) {
      List<Match> matches = cellToMatches.get(cell);
      if (matches != null) {
        for (Match match : matches) {
          boolean sel = match.equals(selection);
          selectionFound = selectionFound | sel;
          StyleRange range = createRange(match, sel);
          list.add(range);
        }
      }
    }
    if (!selectionFound && selection != null) {
      ItemCell selCell = (ItemCell) selection.getBlock();
      if (selCell.equals(cell)) {
        list.add(createRange(selection, true));
      }
    }
    StyleRange[] ranges = list.toArray(new StyleRange[list.size()]);
    decorator.setStyles(cell, ranges);
  }

  private StyleRange createRange(Match match, boolean selection) {
    Display display = composite.getDisplay();
    Color fgColor, bgColor;
    if (selection) {
      // Lighten to avoid search selection on system selection
      fgColor = ColorManager.lighten(display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT), 50);
      bgColor = ColorManager.getInstance().getSelectedBackgroundColor();
    } else {
      // To avoid white text on light-green background
      fgColor = display.getSystemColor(SWT.COLOR_BLACK);
      bgColor = ColorManager.getInstance().getBackgroundColor();
    }
    return new StyleRange(match.getOffset(), match.getLength(), fgColor, bgColor);
  }
}
