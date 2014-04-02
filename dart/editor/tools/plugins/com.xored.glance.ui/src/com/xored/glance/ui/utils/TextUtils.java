/**
 * 
 */
package com.xored.glance.ui.utils;

import com.xored.glance.ui.sources.ColorManager;
import com.xored.glance.ui.sources.Match;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;

import java.util.Arrays;
import java.util.Iterator;

/**
 * @author Yuri Strot
 */
public class TextUtils {

  public static void applyStyles(final TextPresentation presentation, final Match[] matches,
      final Match selected) {

    final StyleRange[] ranges = createStyleRanges(
        presentation.getExtent(),
        matches,
        ColorManager.getInstance().getBackgroundColor());

    presentation.mergeStyleRanges(ranges);

    if (selected != null) {
      final StyleRange[] selectedRanges = createStyleRanges(
          presentation.getExtent(),
          new Match[] {selected},
          ColorManager.getInstance().getSelectedBackgroundColor());
      presentation.mergeStyleRanges(selectedRanges);

    }
  }

  public static StyleRange copy(final StyleRange range) {
    final StyleRange result = new StyleRange(range);
    result.start = range.start;
    result.length = range.length;
    result.fontStyle = range.fontStyle;
    return result;
  }

  public static StyleRange[] copy(final StyleRange[] ranges) {
    final StyleRange[] result = new StyleRange[ranges.length];
    for (int i = 0; i < result.length; i++) {
      result[i] = copy(ranges[i]);
    }
    return result;
  }

  public static StyleRange[] getStyles(final TextPresentation presentation) {
    final StyleRange[] ranges = new StyleRange[presentation.getDenumerableRanges()];
    final Iterator<?> e = presentation.getAllStyleRangeIterator();
    for (int i = 0; e.hasNext(); i++) {
      ranges[i] = (StyleRange) e.next();
    }
    return ranges;
  }

  private static StyleRange[] createStyleRanges(final IRegion region, final Match[] matches,
      final Color color) {
    final Match[] regionMatches = getRangeMatches(region.getOffset(), region.getLength(), matches);
    final StyleRange[] ranges = new StyleRange[regionMatches.length];
    for (int i = 0; i < regionMatches.length; i++) {
      final StyleRange range = new StyleRange(
          regionMatches[i].getOffset(),
          regionMatches[i].getLength(),
          null,
          color);
      ranges[i] = range;
    }
    return ranges;
  }

  private static int getPosition(final int offset, final Match[] matches) {
    final int index = Arrays.binarySearch(matches, new Match(null, offset, 0));
    if (index >= 0) {
      return index;
    }
    return -index - 1;
  }

  private static Match[] getRangeMatches(final int start, final int length, final Match[] matches) {
    int from = getPosition(start, matches);
    if (from >= matches.length) {
      return Match.EMPTY;
    }
    if (from > 0) {
      final Match border = matches[from - 1];
      if (border.getLength() + border.getOffset() > start) {
        from--;
      }
    }
    final int to = getPosition(start + length, matches) - 1;
    if (from <= to) {
      final Match[] result = new Match[to - from + 1];
      System.arraycopy(matches, from, result, 0, result.length);
      return result;
    }
    return Match.EMPTY;
  }

}
