/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.ui.controls.decor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;

import com.xored.glance.ui.sources.ColorManager;
import com.xored.glance.ui.sources.ITextBlock;
import com.xored.glance.ui.sources.ITextSource;
import com.xored.glance.ui.sources.ITextSourceListener;
import com.xored.glance.ui.sources.Match;
import com.xored.glance.ui.sources.SourceSelection;
import com.xored.glance.ui.utils.TextUtils;

public abstract class StructSource implements ITextSource, IStructProvider, SelectionListener {

  private final StructDecorator decorator;
  private final Composite composite;
  protected IStructContent content;

  public StructSource(final Composite composite) {
    this.composite = composite;
    content = createContent();
    decorator = new StructDecorator(composite, this);
  }

  public StructCell getCell(final Item item, final int column) {
    final StructCell cell = createCell(item, column);
    StyleRange[] styles = cellToStyles.get(cell);
    if (styles == null) {
      styles = calcStyles(cell);
      cellToStyles.put(cell, styles);
    }
    cell.styles = styles;
    return cell;
  }

  public boolean isIndexRequired() {
    return true;
  }

  private StyleRange[] calcStyles(final StructCell cell) {
    final ITextBlock block = content.getContent(cell);
    blockToCell.put(block, cell);

    final StyleRange[] cellStyles = cell.nativeStyles();
    final StyleRange[] blockStyles = createStyles(block);

    if (blockStyles == null || blockStyles.length == 0)
      return cellStyles;
    if (cellStyles.length == 0)
      return blockStyles;
    final Region region = new Region(0, cell.getText().length());
    final int size = cellStyles.length + blockStyles.length;
    final TextPresentation presentation = new TextPresentation(region, size);
    presentation.replaceStyleRanges(cellStyles);
    presentation.mergeStyleRanges(blockStyles);
    return TextUtils.getStyles(presentation);
  }

  protected abstract StructCell createCell(Item item, int column);

  protected abstract IStructContent createContent();

  protected abstract SourceSelection getSourceSelection();

  /**
   * @return the composite
   */
  public Composite getControl() {
    return composite;
  }

  public ITextBlock[] getBlocks() {
    return content.getBlocks();
  }

  public void dispose() {
    content.dispose();
    decorator.dispose();
  }

  public boolean isDisposed() {
    return decorator.isDisposed();
  }

  public void index(final IProgressMonitor monitor) {
    content.index(monitor);
  }

  public void widgetDefaultSelected(final SelectionEvent e) {
    fireSelectionChanged();
  }

  public void widgetSelected(final SelectionEvent e) {
    fireSelectionChanged();
  }

  protected void fireSelectionChanged() {
    discardSelection();
    final SourceSelection selection = getSourceSelection();
    final ITextSourceListener[] listeners = content.getListeners();
    for (final ITextSourceListener listener : listeners) {
      listener.selectionChanged(selection);
    }
  }

  public void select(final Match match) {
    if (match != null) {
      discardSelection();
      path = content.getPath(match.getBlock());
      path.select(getControl());
    }
    setMatch(match);
  }

  public void show(final Match[] matches) {
    setMatches(matches);
  }

  public void addTextSourceListener(final ITextSourceListener listener) {
    content.addListener(listener);
  }

  public void removeTextSourceListener(final ITextSourceListener listener) {
    content.removeListener(listener);
  }

  private void discardSelection() {
    if (path != null) {
      path.discardSelection();
      path = null;
    }
  }

  private void setMatch(final Match match) {
    final StructCell oldSel = updateSelection(false);
    selection = match;
    final StructCell newSel = updateSelection(true);
    if (oldSel != null) {
      decorator.redraw(oldSel);
    }
    if (newSel != null && newSel != oldSel) {
      decorator.redraw(newSel);
    }
  }

  private StructCell updateSelection(final boolean add) {
    if (selection != null) {
      final ITextBlock block = selection.getBlock();
      final StructCell cell = blockToCell.get(block);
      if (cell != null) {
        cellToStyles.remove(cell);
        return cell;
      }
    }
    return null;
  }

  private void setMatches(final Match[] matches) {
    init();
    for (final Match match : matches) {
      final ITextBlock block = match.getBlock();
      List<Match> list = blockToMatches.get(block);
      if (list == null) {
        list = new ArrayList<Match>();
        blockToMatches.put(block, list);
      }
      list.add(match);
    }
    decorator.redraw();
  }

  private StyleRange[] createStyles(final ITextBlock block) {
    final List<StyleRange> list = new ArrayList<StyleRange>();
    boolean selectionFound = false;
    final List<Match> matches = blockToMatches.get(block);
    if (matches != null) {
      for (final Match match : matches) {
        final boolean sel = match.equals(selection);
        selectionFound = selectionFound | sel;
        final StyleRange range = createStyle(match, sel);
        list.add(range);
      }
    }

    if (!selectionFound && selection != null) {
      final ITextBlock sBlock = selection.getBlock();
      if (sBlock.equals(block)) {
        list.add(createStyle(selection, true));
      }
    }
    return list.toArray(new StyleRange[list.size()]);
  }

  private StyleRange createStyle(final Match match, final boolean selection) {
    final Display display = composite.getDisplay();
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

  public SourceSelection getSelection() {
    return null;
  }

  public void init() {
    blockToMatches = new HashMap<ITextBlock, List<Match>>();
    blockToCell = new HashMap<ITextBlock, StructCell>();
    cellToStyles = new HashMap<StructCell, StyleRange[]>();
  }

  private Match selection;
  private IPath path;

  private Map<ITextBlock, List<Match>> blockToMatches;
  private Map<ITextBlock, StructCell> blockToCell;
  private Map<StructCell, StyleRange[]> cellToStyles;
}
