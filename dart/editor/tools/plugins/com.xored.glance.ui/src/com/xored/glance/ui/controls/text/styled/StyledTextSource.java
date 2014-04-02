/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and Implementation (Yuri Strot)
 *******************************************************************************/
package com.xored.glance.ui.controls.text.styled;

import com.xored.glance.ui.sources.ColorManager;
import com.xored.glance.ui.sources.Match;

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Yuri Strot
 */
public class StyledTextSource extends AbstractStyledTextSource {

  private RangeGroup[] previous;

  private RangeGroup selectedRange;

  public StyledTextSource(final StyledText text) {
    super(text);
  }

  @Override
  public void select(final Match match) {
    super.select(match);
    clearSelected();
    if (match != null) {
      final List<StyleRange> ranges = createRanges(
          match,
          ColorManager.getInstance().getSelectedBackgroundColor());
      selectedRange = new RangeGroup(
          match.getOffset(),
          match.getOffset() + match.getLength(),
          ranges.toArray(new StyleRange[ranges.size()]));
    }
  }

  @Override
  public void show(final Match[] matches) {
    clearAll();
    previous = new RangeGroup[matches.length];
    for (int i = 0; i < matches.length; i++) {
      final Match match = matches[i];
      final List<StyleRange> ranges = createRanges(
          match,
          ColorManager.getInstance().getBackgroundColor());
      previous[i] = new RangeGroup(
          match.getOffset(),
          match.getOffset() + match.getLength(),
          ranges.toArray(new StyleRange[ranges.size()]));
    }

    if (selected != null) {
      select(selected);
    }
  }

  protected void clearAll() {
    clearHighlight();
    clearSelected();
  }

  protected void clearHighlight() {
    if (previous != null && previous.length > 0) {
      for (int i = 0; i < previous.length; i++) {
        clearRangeGroup(previous[i]);
      }
    }
    previous = null;
  }

  protected void clearSelected() {
    if (selectedRange != null) {
      clearRangeGroup(selectedRange);
      selectedRange = null;
    }
  }

  @Override
  protected void doDispose() {
    super.doDispose();
    clearAll();
  }

  private void clearRangeGroup(final RangeGroup group) {
    getText().replaceStyleRanges(
        group.getStart(),
        group.getEnd() - group.getStart(),
        new StyleRange[0]);
    final StyleRange[] ranges = group.getRanges();
    for (final StyleRange range : ranges) {
      getText().replaceStyleRanges(range.start, range.length, new StyleRange[] {range});
    }
  }

  private List<StyleRange> createRanges(final Match match, final Color bg) {
    int index = match.getOffset();
    final int lastIndex = index + match.getLength();
    final StyleRange[] matchRanges = getText().getStyleRanges(index, match.getLength());
    final List<StyleRange> ranges = new ArrayList<StyleRange>();
    for (final StyleRange styleRange : matchRanges) {
      ranges.add(styleRange);
      if (styleRange.length < 0) {
        continue;
      }
      final StyleRange newStyleRange = new StyleRange(
          styleRange.start,
          styleRange.length,
          null,
          bg,
          styleRange.fontStyle);
      if (styleRange.start - index > 0) {
        getText().replaceStyleRanges(
            index,
            styleRange.start - index,
            new StyleRange[] {newStyleRange});
      }
      getText().replaceStyleRanges(
          styleRange.start,
          styleRange.length,
          new StyleRange[] {newStyleRange});
      index = styleRange.start + styleRange.length;
    }
    if (lastIndex - index > 0) {
      final StyleRange newStyleRange = new StyleRange(index, lastIndex - index, null, bg);
      getText().replaceStyleRanges(index, lastIndex - index, new StyleRange[] {newStyleRange});
    }
    return ranges;
  }
}
